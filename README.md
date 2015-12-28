# Espalier

![Espalier](./resources/espalier.png)

Placeholder style rules for [Garden](https://github.com/noprompt/garden).

Espalier placeholders are similar to [`@extend`-only selectors in Sass](http://sass-lang.com/documentation/file.SASS_REFERENCE.html#placeholders), providing a mechanism to reuse styles with a much smaller file size footprint.

### Usage

```clojure
;; Create placeholder rules

(require '[espalier.core :refer [defplaceholder]])

(defplaceholder my-placeholder
  {:some :css
   :rules :here}

  [:&.including {:nested :rules}])

;; Extend placeholders in place

(require '[espalier.core :refer [emit-placeholders]]
		 '[garden.def :refer [defstyles]]
         '[garden.stylesheet :refer [at-media]])

(defstyles my-styles
  ;; This will render all placeholders which have been used in your styles
  (emit-placeholders)

  [:some-selector my-placeholder]
  [:something-else
    [:some-child my-placeholder]]
  (at-media {:screen :only :min-width (em 60)}
    [:whatever my-placeholder]))

```

Given the above, Garden will produce CSS output like:

```css
some-selector,
something-else some-child {
    some: css;
    rules: here;
}

some-selector.including,
something-else some-child.including {
    nested: rules;
}

@media only screen and (min-width: 60em) {
    whatever {
        some: css;
        rules: here;
    }

    whatever.including {
        nested: rules;
    }
}
```
