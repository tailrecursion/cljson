(defproject tailrecursion/cljson "0.1.0-SNAPSHOT"
  :description "Fast data exchange format for between Clojure and ClojureScript"
  :url "https://github.com/tailrecursion/cljson"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :manifest {"hoplon-provides" "cljson"}
  :dependencies [[org.clojure/clojure   "1.5.1"]
                 [cheshire "5.2.0"]
                 [org.clojure/data.generators "0.1.2"]]
  :repl-options {:init-ns tailrecursion.cljson})
