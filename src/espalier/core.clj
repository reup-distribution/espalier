(ns espalier.core
  (:require [clojure.string :as string]
            [flatland.ordered.map :refer [ordered-map]]
            [flatland.ordered.set :refer [ordered-set]]
            [garden.compiler :refer [CSSRenderer expand IExpandable render-css]]
            [garden.selectors :refer [descendant]]
            [garden.types :refer [map->CSSAtRule]]))

(defonce wrappers (atom #{}))

(defn var->sym [v]
  (let [{:keys [ns name]} (meta v)]
    (symbol (str (ns-name ns)) (str name))))

(defn wrap-fn
  [var-ref wrapper]
  (let [pair [(var->sym var-ref) (var->sym wrapper)]]
    (when-not (@wrappers pair)
      (swap! wrappers conj pair)
      (alter-var-root var-ref
        (fn [original]
          (fn [& args]
            (apply @wrapper original args)))))))


(def placeholders
  (atom (ordered-set)))

(defn reset-placeholders! []
  (doseq [sym @placeholders]
    (if-let [placeholder @(resolve sym)]
      (doseq [field [:media-queries :selectors]
              :let [field-atom (field placeholder)]]
        (swap! field-atom empty))
      (swap! placeholders disj sym))))

(reset-placeholders!)

;; Modify render-rule to prevent creation of nested blocks
;; when inside a placeholder's rendering
(defn render-rule-wrapper [render-rule [selector _ :as rule]]
  (let [rendered (render-rule rule)]
    (if (every? #(= '(::placeholder) %) selector)
        (-> rendered
            (string/replace #"^placeholder\s*\{\s*" "")
            (string/replace #"\s*\}$" ""))
        rendered)))

(wrap-fn #'garden.compiler/render-rule #'render-rule-wrapper)

;; Modify expand-stylesheet to:
;; - reset contexts captured by placeholders in a previous render (this would
;;   occur during `lein garden auto`)
;; - prevent laziness so that all selector contexts are extended before
;;   placeholders are rendered
(defn expand-stylesheet-wrapper [expand-stylesheet xs]
  (reset-placeholders!)
  (doall (expand-stylesheet xs)))

(wrap-fn #'garden.compiler/expand-stylesheet #'expand-stylesheet-wrapper)

(defn expand-selector-rules [selectors rules]
  (-> (vec selectors)
      (concat rules)
      vec
      expand))

(defn render-selectors [{:keys [selectors rules]}]
  (let [selectors* @selectors]
    (when-not (empty? selectors*)
      (let [expanded (expand-selector-rules selectors* rules)
            rendered (render-css expanded)]
        (->> rendered
             (remove nil?)
             (string/join "\n"))))))

(defn render-media-query [rules [media-query selectors]]
  (when-not (empty? selectors)
    (let [expanded (expand-selector-rules selectors rules)
          at-rule (map->CSSAtRule
                    {:identifier :media
                     :value {:media-queries media-query
                             :rules expanded}})]
       (render-css at-rule))))

(defn render-media-queries [{:keys [media-queries rules]}]
  (->> @media-queries
       (map (partial render-media-query rules))
       (remove nil?)
       (string/join "\n")))

(defrecord Placeholder [media-queries selectors rules]
  CSSRenderer
  (render-css [this]
    (->> [(render-selectors this) (render-media-queries this)]
         (remove nil?)
         (string/join "\n")))

  IExpandable
  (expand [this]
    (if-let [selector-context @#'garden.compiler/*selector-context*]
      (let [selector (@#'garden.compiler/render-selector selector-context)]
        ;; Ensure nested placeholder references are resolved
        (expand [:& rules])
        (if-let [media-query @#'garden.compiler/*media-query-context*]
          (if-let [existing-query (@media-queries media-query)]
            (swap! media-queries update-in [media-query] conj selector)
            (swap! media-queries assoc media-query (ordered-set selector)))
          (swap! selectors conj selector))
        nil)
      (list [(list '(::placeholder)) (list this)]))))

(defn emit-placeholders []
  (let [placeholders (map (comp map->Placeholder deref resolve) @placeholders)
        empty-atom (atom [])]
    (concat
      (map #(map->Placeholder (assoc % :media-queries empty-atom)) placeholders)
      (map #(map->Placeholder (assoc % :selectors empty-atom)) placeholders))))

(defmacro defplaceholder [name & rules]
  `(do
     (swap! placeholders conj '~(symbol (str (ns-name *ns*)) (str name)))
     (def ~(symbol name)
       (map->Placeholder
         {:media-queries (atom (ordered-map))
          :selectors (atom (ordered-set))
          :rules (list ~@rules)}))))
