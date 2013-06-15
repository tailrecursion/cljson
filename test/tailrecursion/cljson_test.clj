(ns tailrecursion.cljson-test
  (:require [clojure.test :refer :all]
            [tailrecursion.cljson :refer [clj->cljson cljson->clj]]
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

;;; benchmark

(def bench-colls (take *magic* (repeatedly collection)))

(deftest native-perf
  (println "clojure.core/pr-str")
  (time
   (doseq [c bench-colls]
     (pr-str c)))
  (println "clojure.core/read-string")
  (time
   (doseq [c bench-colls]
     (read-string (pr-str c)))))

(deftest cljson-perf
  (println "clj->cljson")
  (time
   (doseq [c bench-colls]
     (clj->cljson c)))
  (println "cljson->clj")
  (time
   (doseq [c bench-colls]
     (cljson->clj (clj->cljson c)))))
