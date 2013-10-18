(defproject tailrecursion/cljson "1.0.3"
  :description "Fast data exchange format for between Clojure and ClojureScript"
  :url "https://github.com/tailrecursion/cljson"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[cheshire "5.2.0" :exclusions [org.clojure/clojure]]]
  :source-paths ["src/clj"]
  :repl-options {:init-ns tailrecursion.cljson}
  :plugins [[lein-cljsbuild "0.3.4"]]
  :profiles {:test {:dependencies [[org.clojure/clojure "1.5.1"]
                                   [org.clojure/data.generators "0.1.2"]
                                   [org.clojure/tools.reader "0.7.9"]]}}
  :cljsbuild {:test-commands {"unit" ["test/run.sh"]}
              :builds [{:source-paths ["src/cljs"]
                        :compiler {:output-to "test/test.js"
                                   :optimizations :advanced}}]})
