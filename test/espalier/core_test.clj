(ns espalier.core-test
  (:require [speclj.core :refer :all]
            [garden.core :refer [css]]
            [garden.stylesheet :refer [at-media]]
            [espalier.core :refer :all]))

(def flags {:pretty-print? false})

(defplaceholder bold
  {:font-weight :bold})

(defplaceholder italic
  {:font-style :italic})

(defplaceholder no-selectors
  {:what :ever})

(defplaceholder red
  {:color :red})

(defplaceholder underline
  {:text-decoration :underline})

(defplaceholder flex
  {:display :flex})

(defplaceholder block
  {:display :block})

(defplaceholder outer
  bold
  italic)

(defplaceholder affects-children
  {:color :red}
  [:.warning {:color :yellow}])

(describe "Placeholder style rules"
  (it "includes supplied rules for each selector referencing the placeholder"
    (let [styles (list
                   bold
                   [:p bold]
                   [:div bold])]
      (should=
        "p,div{font-weight:bold}"
        (css flags styles))))

  (it "supports descendant selectors"
    (let [styles (list
                   italic
                   [:.foo [:em italic]]
                   [:p :i italic])]
      (should=
        ".foo em,p,i{font-style:italic}"
        (css flags styles))))

  (it "omits a placeholder with no selectors"

    (should= "" (css flags no-selectors)))

  (it "emits all defined placeholders"
    (let [styles (list
                   (emit-placeholders)
                   [:.error red]
                   [:a underline])]
      (should=
        ".error{color:red}a{text-decoration:underline}"
        (css flags styles))))

  (it "respects media query context"
    (let [styles (list
                   (emit-placeholders)
                   [:b bold]
                   (at-media {:screen :only :min-width "10em"}
                     [:strong bold]))]
      (should=
        "b{font-weight:bold}@media only screen and (min-width:10em){strong{font-weight:bold}}"
        (css flags styles))))


  (it "renders media queries after bare selectors"
    (let [styles (list
                   (emit-placeholders)
                   [:ul block]
                   (at-media {:screen :only :min-width "10em"}
                     [:ul flex]))]
      (should=
        "ul{display:block}@media only screen and (min-width:10em){ul{display:flex}}"
        (css flags styles))))

  (it "resolves nested placeholder references"
    (let [styles (list
                   (emit-placeholders)
                   [:p outer])]
      (should=
        "p{font-weight:bold}p{font-style:italic}"
        (css flags styles))))

  (it "resolves child selectors"
    (let [styles (list
                   (emit-placeholders)
                   [:.notice affects-children])]
      (should=
        ".notice{color:red}.notice .warning{color:yellow}"
        (css flags styles))))

  (it "resolves child selectors in media queries"
    (let [styles (list
                   (emit-placeholders)
                   (at-media {:screen :only}
                     [:.notice affects-children]))]
      (should=
        "@media only screen{.notice{color:red}.notice .warning{color:yellow}}"
        (css flags styles)))))
