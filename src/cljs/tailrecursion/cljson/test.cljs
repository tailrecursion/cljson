(ns tailrecursion.cljson.test
  (:require [tailrecursion.cljson :refer [clj->cljson cljson->clj]]
            [tailrecursion.cljson.generators :as g]
            [cljs.reader :as reader]))

(defn setup! []
  (set! cljs.core/*print-fn*
        (if (undefined? (aget js/window "dump"))
          ;; phantomjs
          (fn [& args]
            (.apply (.-log js/console)
                    (.-console js/window)
                    (apply array args)))
          ;; firefox
          (fn [& args]
            (.apply (aget js/window "dump")
                    js/window
                    (apply array args))))))

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

(defn map-scalar []
  (g/call-through (g/rand-nth map-scalars)))

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

(defn deep-collection
  ([breadth depth]
     (let [s (atom 0)
           c (atom 0)
           v (deep-collection breadth depth 0 s c)]
       (with-meta v {:collections @c :scalars @s})))
  ([breadth depth nparents nscalars ncollections] 
     (let [base  (g/rand-nth [{} [] #{} ()])
           pcoll (/ (- depth nparents) depth)
           pscal (- 1 pcoll)
           ncoll (if (pos? pcoll) (g/geometric (/ 1 (* pcoll breadth))) 0) 
           nscal (if (pos? pscal) (g/geometric (/ 1 (* pscal breadth))) 0)
           colls (for [_ (range ncoll)]
                   (deep-collection breadth depth (inc nparents) nscalars ncollections))
           scals (for [_ (range nscal)] (if (map? base) (map-scalar) (scalar)))
           items (g/shuffle (concat colls scals))]
       (swap! nscalars + nscal)
       (swap! ncollections + ncoll)
       (into base (if (map? base) (map (partial apply vector) (partition 2 items)) items)))))

(def ^:dynamic *magic* 1)

(defn ^:export err [x y z]
  (js/Error. (str "\nx: " (pr-str x) ",\ny: " (pr-str y) ",\nz: " (pr-str z) ".\n")))

(defn ^:export start []

  (setup!)

  (dotimes [_ *magic*]
    (let [x (scalar)
          y (clj->cljson x)
          z (cljson->clj y)]
      (when-not (= x z) (throw (err x y z)))))

  (dotimes [_ *magic*]
    (let [x (collection)
          y (clj->cljson x)
          z (cljson->clj y)]
      (when-not (= x z) (throw (err x y z)))))

  (defrecord Person [name])
  (swap! reader/*tag-table* assoc "tailrecursion.cljson.test.Person" map->Person)

  (let [x (Person. "Bob")
        y (clj->cljson x)
        z (cljson->clj y)]
    (when-not (= x z) (throw (err x y z))))

  (let [x (into cljs.core.PersistentQueue.EMPTY [1 2 3])
        y (clj->cljson x)
        z (cljson->clj y)]
    (when-not (= x z) (throw (err x y z))))

  (let [x [true false "true" 1.22 100 -1 nil]
        y (clj->cljson x)
        z (cljson->clj y)]
    (when-not (= x z) (throw (err x y z))))

  (let [x (with-meta {:x 1} {:abc 123})
        y (binding [*print-meta* true] (clj->cljson x))
        z (cljson->clj y)]
    (when-not (= (meta x) (meta z)) (throw (err x y z))))

  ;; benchmark

  (def breadth      8)
  (def depth        6)
  (def bench-colls  (deep-collection breadth depth))

  (let [{c :collections s :scalars} (meta bench-colls)]
    (print (str "Deep collection " breadth " x " depth ": " c " collections and " s " scalars.\n")))

  (print "cljs.core/pr-str")
  (.profile js/console "cljs.core/pr-str")
  (time (pr-str bench-colls))
  (.profileEnd js/console)

  (def pr-colls (pr-str bench-colls))
  (print "cljs.reader/read-string")
  (.profile js/console "cljs.reader/read-string")
  (time (reader/read-string pr-colls))
  (.profileEnd js/console)

  (print "clj->cljson")
  (.profile js/console "clj->cljson")
  (time (clj->cljson bench-colls))
  (.profileEnd js/console)

  (def cljson-colls (clj->cljson bench-colls))
  (print "cljson->clj")
  (.profile js/console "cljson->clj")
  (time (cljson->clj cljson-colls))
  (.profileEnd js/console)

  (def stringify-colls (.parse js/JSON cljson-colls))
  (print "JSON/stringify (no encode)")
  (time (.stringify js/JSON stringify-colls))

  (print "JSON/parse (no decode)")
  (time (.parse js/JSON cljson-colls))

  )
