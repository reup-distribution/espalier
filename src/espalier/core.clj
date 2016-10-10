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


(defonce placeholders
  (atom (ordered-set)))

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

(defn purge-removed-placeholders! []
  (doseq [sym @placeholders]
    (when-not @(resolve sym)
      (swap! placeholders disj sym))))

;; Modify expand-stylesheet to:
;; - purges removed placeholders (this would occur in `lein garden auto`)
;; - prevent laziness so that all selector contexts are extended before
;;   placeholders are rendered
(defn expand-stylesheet-wrapper [expand-stylesheet xs]
  (purge-removed-placeholders!)
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

;; This is gross and hacky, but exists specifically to denote its
;; side-effecting nature: because Placeholder's expand method is
;; itself stateful, forcing an expansion of child rules here ensures
;; that nested placeholders are expanded.
(defn expand-nested-placeholders!
  [rules]
  (expand [:& rules]))

(defrecord Placeholder [media-queries selectors rules]
  CSSRenderer
  (render-css [this]
    (->> [(render-selectors this) (render-media-queries this)]
         (remove nil?)
         (string/join "\n")))

  IExpandable
  ;; HERE BE DRAGONS: if there is a selector/media query context during
  ;; Garden's rendering, we capture that context for rendering the final
  ;; Placeholder. If there's no context, the expansion is returned.
  (expand [this]
    (if-let [selector-context @#'garden.compiler/*selector-context*]
      (let [selectors* (map @#'garden.compiler/space-separated-list selector-context)]
        (expand-nested-placeholders! rules)
        (if-let [media-query @#'garden.compiler/*media-query-context*]
          (if-let [existing-query (@media-queries media-query)]
            (swap! media-queries update-in [media-query] into selectors*)
            (swap! media-queries assoc media-query (into (ordered-set) selectors*)))
          (swap! selectors into selectors*))
        nil)
      (list [(list '(::placeholder)) (list this)]))))

(defn emit-placeholders []
  (let [placeholders (map (comp map->Placeholder deref resolve) @placeholders)]
    (concat
      (map #(map->Placeholder (assoc % :media-queries (atom (ordered-map)))) placeholders)
      (map #(map->Placeholder (assoc % :selectors (atom (ordered-set)))) placeholders))))

(defmacro defplaceholder [name & rules]
  `(let [sym# '~(symbol (str (ns-name *ns*)) (str name))
         existing# (some-> sym# resolve deref)
         rules# (list ~@rules)
         existing-rules# (:rules existing#)]
     (swap! placeholders conj sym#)
     (when-not (= rules# existing-rules#)
       (def ~(symbol name)
         (map->Placeholder
           {:media-queries (atom (ordered-map))
            :selectors (atom (ordered-set))
            :rules (list ~@rules)})))))
