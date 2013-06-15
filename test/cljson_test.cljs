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

  ;; benchmark

  (def bench-colls (doall (take *magic* (repeatedly collection))))

  (println "cljs.core/pr-str")
  (time
   (doseq [c bench-colls]
     (pr-str c)))

  (def pr-decode (mapv pr-str bench-colls))
  (println "cljs.reader/read-string")
  (time
   (doseq [c pr-decode]
     (reader/read-string c)))

  (println "clj->cljson")
  (time
   (doseq [c bench-colls]
     (clj->cljson c)))

  (def to-decode (mapv clj->cljson bench-colls))
  
  (println "cljson->clj")
  (time
   (doseq [c to-decode]
     (cljson->clj c)))

  (println "Done.")

  )
