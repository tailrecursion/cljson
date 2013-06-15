(ns cljson-test
  (:require [tailrecursion.cljson :refer [clj->cljson cljson->clj]]
            [generators :as g]
            [cljs.reader :as r]))

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
              g/symbol
              g/keyword
              ;; todo: uuid, date
              ])

(defn scalar []
  (g/call-through (g/rand-nth scalars)))

(def collections
  [[g/vec [scalars]]
   [g/set [scalars]]
   [g/hash-map [scalars scalars]]
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

  ;; benchmark

  (def bench-colls (take *magic* (repeatedly collection)))
  
  (println "cljs.core/pr-str")
  (time
   (doseq [c bench-colls]
     (pr-str c)))

  ;; BROKEN because of maps that read-string can't deal with.
  ;; omit g/hash-map from collections above to test without maps.
  ;; (println "cljs.reader/read-string")
  ;; (time
  ;;  (doseq [c bench-colls]
  ;;    (r/read-string (pr-str c))))

  (println "clj->cljson")
  (time
   (doseq [c bench-colls]
     (clj->cljson c)))

  (println "cljson->clj")
  (time
   (doseq [c bench-colls]
     (cljson->clj (clj->cljson c))))

  (println "Done.")

  )
