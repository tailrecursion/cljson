(ns cljson-test
  (:require [tailrecursion.cljson :refer [clj->cljson cljson->clj]]
            [generators :as g]
            [cljs.reader :as reader]))

(defn setup! []
  (set! cljs.core/*print-fn*
        (if (undefined? (aget js/window "dump"))
          ;; phantomjs
          #(.apply (.-log js/console)
                   (.-console js/window)
                   (apply array %&))
          ;; firefox
          #(.apply (aget js/window "dump")
                   js/window
                   (apply array %&)))))

(def scalars [(constantly nil)
              g/number
              g/int
              g/string
              g/keyword
              g/symbol
              ;; todo: uuid, date
              ])

(defn scalar []
  (g/call-through (g/rand-nth scalars)))

(def map-scalars
  "Because of a mysterious bug in read-string these are the only
  scalars we put in maps. TODO: investigate."
  [(constantly nil)
   g/number
   g/int
   g/string])

(def collections
  [[g/vec [scalars]]
   [g/set [scalars]]
   [g/hash-map [map-scalars map-scalars]]
   [g/list [scalars]]])

(defn collection
  "Returns a collection of scalar elements"
  []
  (let [[coll args] (g/rand-nth collections)]
    (apply coll (map g/rand-nth args))))

(def ^:dynamic *magic* 1000)

(defn ^:export start []

  (setup!)

  (dotimes [_ *magic*]
    (let [x (scalar)
          y (clj->cljson x)
          z (cljson->clj y)]
      (assert (= x z))))

  (dotimes [_ *magic*]
    (let [x (collection)
          y (clj->cljson x)
          z (cljson->clj y)]
      (assert (= x z))))

  (defrecord Person [name])

  (let [bob (Person. "Bob")
        q (into cljs.core.PersistentQueue/EMPTY [1 2 3])]
    (swap! reader/*tag-table* assoc "cljson-test.Person" map->Person)
    (assert (= bob (-> bob clj->cljson cljson->clj)))
    (assert (= q (-> q clj->cljson cljson->clj))))

  (let [m {:abc 123}
        s (with-meta {:x 1} m)]
    (binding [*print-meta* true]
      (assert (= (meta (cljson->clj (clj->cljson s))) m))))

  ;; benchmark

  (def bench-colls (into-array (take *magic* (repeatedly collection))))

  (println "cljs.core/pr-str")
  (time
   (do (.profile js/console "cljs.core/pr-str")
       (loop [i 0]
         (when (< i *magic*)
           (pr-str (aget bench-colls i))
           (recur (inc i))))
       (.profileEnd js/console)))

  (def pr-colls (into-array (map pr-str bench-colls)))
  (println "cljs.reader/read-string")
  (time
   (do (.profile js/console "cljs.reader/read-string")
       (loop [i 0]
         (when (< i *magic*)
           (reader/read-string (aget pr-colls i))
           (recur (inc i))))
       (.profileEnd js/console)))

  (println "clj->cljson")
  (time
   (do (.profile js/console "clj->cljson")
       (loop [i 0]
         (when (< i *magic*)
           (clj->cljson (aget bench-colls i))
           (recur (inc i))))
       (.profileEnd js/console)))

  (def cljson-colls (into-array (map clj->cljson bench-colls)))
  (println "cljson->clj")
  (time
   (do (.profile js/console "cljson->clj")
       (loop [i 0]
         (when (< i *magic*)
           (cljson->clj (aget cljson-colls i))
           (recur (inc i))))
       (.profileEnd js/console)))

  (def stringify-colls (into-array (map #(.parse js/JSON %) cljson-colls)))
  (println "JSON/stringify (no encode)")
  (time (loop [i 0]
          (when (< i *magic*)
            (.stringify js/JSON (aget stringify-colls i))
            (recur (inc i)))))

  (println "JSON/parse (no decode)")
  (time (loop [i 0]
          (when (< i *magic*)
            (.parse js/JSON (aget cljson-colls i))
            (recur (inc i)))))

  (println "Done.")

  )
