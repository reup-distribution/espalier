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

### Changes

- 0.1.1
  - Fixes an issue where a placeholder using an `&` selector would only be applied to the last in a series of string selectors.
  - Fixed an issue where saving certain files while running `lein garden auto` corrupted subsequent compilations.

### License

Copyright (c) 2015, ReUp Distribution Inc
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

