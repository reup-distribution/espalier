(defproject espalier "0.1.1"
  :license {:name "BSD 2-clause \"Simplified\" License"
            :url "http://opensource.org/licenses/BSD-2-Clause"
            :year 2015
            :key "bsd-2-clause"}
  :description "Placeholder style rules for Garden"
  :url "https://github.com/reup-distribution/espalier"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [garden "1.3.0"]
                 [org.flatland/ordered "1.5.3"]]
  :profiles {:dev {:dependencies [[speclj "3.3.1"]]
                   :plugins [[speclj "3.3.1"]]}}
  :aliases {"test" ["spec"]})
