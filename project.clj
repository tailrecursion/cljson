(defproject tailrecursion/cljson "1.0.3"
  :description "Fast data exchange format for between Clojure and ClojureScript"
  :url "https://github.com/tailrecursion/cljson"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [cheshire "5.2.0" :exclusions [org.clojure/clojure]]]
  :source-paths ["src/clj"]
  :repl-options {:init-ns tailrecursion.cljson}
  :plugins [[lein-cljsbuild "0.3.4"]]
  :profiles {:test {:dependencies [[org.clojure/tools.reader "0.7.9"
                                    :exclusions [org.clojure/clojure]]
                                   [org.clojure/data.generators "0.1.2"
                                    :exclusions [org.clojure/clojure]]]}}
  :cljsbuild {:source-paths ["src/cljs"]
              :builds
              {:test
               {:source-paths ["src/cljs" "test"]
                :compiler {:output-to "test/test.js"
                           :optimizations :advanced}
                :jar false}
               :debug
               {:source-paths ["src/cljs" "test"]
                :compiler {:output-to "test/test.js"
                           :optimizations :whitespace}
                :jar false}}})
