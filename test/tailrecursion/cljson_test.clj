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

(defn list
  "Create a list with elements from f and sized from sizer."
  ([f] (list f g/default-sizer))
  ([f sizer] (into () (g/reps sizer f))))

(def collections
  [[g/vec [scalars]]
   [g/set [scalars]]
   [g/hash-map [scalars scalars]]
   [list [scalars]]])

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

;;; perf

(deftest native-perf
  (println "native-write")
  (time
   (dotimes [_ *magic*]
     (pr-str (scalar))))
  (time
   (dotimes [_ *magic*]
     (pr-str (collection))))
  (println "native-read")
  (time
   (dotimes [_ *magic*]
     (read-string (pr-str (scalar)))))
  (time
   (dotimes [_ *magic*]
     (read-string (pr-str (collection))))))

(deftest cljson-perf
  (println "cljson-write")
  (time
   (dotimes [_ *magic*]
     (clj->cljson (scalar))))
  (time
   (dotimes [_ *magic*]
     (clj->cljson (collection))))
  (println "cljson-read")
  (time
   (dotimes [_ *magic*]
     (cljson->clj (clj->cljson (scalar)))))
  (time
   (dotimes [_ *magic*]
     (cljson->clj (clj->cljson (collection))))))
