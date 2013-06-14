(ns cljson-test
  (:require [tailrecursion.cljson :refer [clj->cljson cljson->clj]]
            [generators :as g]))

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

  ;; (println "testing collection roundtrip")
  ;; (dotimes [_ *magic*]
  ;;   (let [x (scalar)]
  ;;     (assert (= x (-> x clj->cljson cljson->clj)))))
  
  ;; (println "testing collection roundtrip")
  ;; (dotimes [_ *magic*]
  ;;   (let [x (collection)]
  ;;     (assert (= x (-> x clj->cljson cljson->clj)))))

  (println "Done.")

  )

