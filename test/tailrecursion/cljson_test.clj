(ns tailrecursion.cljson-test
  (:require [clojure.tools.reader.edn :as e]
            [clojure.test :refer :all]
            [cheshire.core :refer [generate-string parse-string]]
            [tailrecursion.cljson :refer [encode decode clj->cljson cljson->clj]]
            [clojure.data.generators :as g])
  (:refer-clojure :exclude [list]))

(def scalars [(constantly nil)
              g/long
              g/boolean
              g/string
              g/symbol
              g/keyword
              g/uuid
              g/date])

(defn scalar []
  (@#'g/call-through (g/rand-nth scalars)))

(def collections
  [[g/vec [scalars]]
   [g/set [scalars]]
   [g/hash-map [scalars scalars]]
   [g/list [scalars]]])

(defn collection
  "Returns a collection of scalar elements based on *rnd*."
  []
  (let [[coll args] (g/rand-nth collections)]
    (apply coll (map g/rand-nth args))))

(defn deep-collection
  ([breadth depth]
   (deep-collection breadth depth 0))
  ([breadth depth nparents] 
   (let [pcoll (/ (- depth nparents) depth)
         pscal (- 1 pcoll)
         ncoll (if (pos? pcoll) (g/geometric (/ 1 (* pcoll breadth))) 0) 
         nscal (if (pos? pscal) (g/geometric (/ 1 (* pscal breadth))) 0)
         colls (for [_ (range ncoll)] (deep-collection breadth depth (inc nparents)))
         scals (for [_ (range nscal)] (scalar))
         items (g/shuffle (concat colls scals))
         base  (g/rand-nth [{} [] #{} ()])]
     (into base (if (map? base) (map (partial apply vector) (partition 2 items)) items)))))

(def ^:dynamic *magic* 1000)

(deftest scalar-roundtrip
  (dotimes [_ *magic*]
    (let [x (scalar)]
      (is (= x (-> x clj->cljson cljson->clj))))))

(deftest collection-roundtrip
  (dotimes [_ *magic*]
    (let [x (collection)]
      (is (= x (-> x clj->cljson cljson->clj))))))

(defrecord Person [name])

(deftest tag-interpretation
  (let [bob (Person. "Bob")]
    (binding [*data-readers* {`Person map->Person}]
      (is (= bob (-> bob clj->cljson cljson->clj))))))

(deftest meta-roundtrip
  (let [m {:abc 123}
        s (with-meta {:x 1} m)]
    (binding [*print-meta* true]
      (is (= (meta (cljson->clj (clj->cljson s))) m)))))

(deftest nested-perf
  (let [x (deep-collection 10 4)]
    (is (= x (-> x clj->cljson cljson->clj)))))

;;; benchmark

(def bench-colls (take *magic* (repeatedly collection)))

(deftest native-perf
  (let [x (atom nil)]
    (println "clojure.core/pr-str") 
    (reset! x (time (mapv #(doall (pr-str %)) bench-colls)))
    (println "clojure.tools.reader.edn/read-string") 
    (time (mapv #(doall (e/read-string %)) @x))))

(deftest cljson-perf
  (let [x (atom nil)]
    (println "clj->cljson")
    (reset! x (time (mapv #(doall (clj->cljson %)) bench-colls))) 
    (println "cljson->clj") 
    (time (mapv cljson->clj @x))
    (let [x (atom (mapv #(doall (encode %)) bench-colls))]
      (println "generate-string")
      (reset! x (time (mapv #(doall (generate-string %)) @x)))
      (println "parse-string")
      (time (mapv #(doall (parse-string %)) @x)))))
