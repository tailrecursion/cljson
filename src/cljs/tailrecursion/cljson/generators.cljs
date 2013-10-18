(ns tailrecursion.cljson.generators
  (:refer-clojure :exclude [rand-nth int char list vec set hash-map name symbol keyword shuffle]))

(.call (aget js/Math "seedrandom") js/Math "42")

(defn call-through
  "Recursively call x until it doesn't return a function."
  [x]
  (if (fn? x)
    (recur (x))
    x))

(defn reps
  "Returns sizer repetitions of f (or (f) if f is a fn)."
  [sizer f]
  (let [count (call-through sizer)]
    (if (fn? f)
      (repeatedly count f)
      (repeat count f))))

(defn geometric
  "Geometric distribution with mean 1/p."
  [p]
  (.ceil js/Math (/ (.log js/Math (.random js/Math))
                    (.log js/Math (- 1.0 p)))))

(defn uniform
  "Uniform distribution from lo (inclusive) to high (exclusive).
   Defaults to range of Java long."
  ([] (.random js/Math))
  ([lo hi]
     {:pre [(< lo hi)]}
     (.floor js/Math (+ lo (* (.random js/Math) (- hi lo))))))

(defn number
  []
  (uniform (.-MIN_VALUE js/Number) (.-MAX_VALUE js/Number)))

(defn int
  []
  (uniform -9007199254740992 9007199254740992))

(defn rand-nth
  "Replacement of core/rand-nth that allows control of the
   randomization basis (through binding *rnd*)."
  [coll]
  (nth coll (uniform 0 (count coll))))

(defn printable-ascii-char
  []
  (.fromCharCode js/String (uniform 32 127)))

(defn char
  []
  (.fromCharCode js/String (uniform 0 65536)))

(defn default-sizer
  "Default sizer used to run tests. If you want a specific distribution,
   create your own and pass it to a fn that wants a sizer."
  []
  (dec (geometric 0.02)))

(defn list
  "Create a list with elements from f and sized from sizer."
  ([f] (list f default-sizer))
  ([f sizer]
     (reps sizer f)))

(defn vec
  "Create a vec with elements from f and sized from sizer."
  ([f] (vec f default-sizer))
  ([f sizer]
     (into [] (reps sizer f))))

(defn set
  "Create a set with elements from f and sized from sizer."
  ([f] (set f default-sizer))
  ([f sizer]
     (into #{} (reps sizer f))))

(defn hash-map
  "Create a hash-map with keys from fk, vals from fv, and
   sized from sizer."
  ([fk fv] (hash-map fk fv default-sizer))
  ([fk fv sizer]
     (zipmap (reps sizer fk)
             (reps sizer fv))))

(defn string
  "Create a string with chars from v and sized from sizer."
  ([] (string printable-ascii-char))
  ([f] (string f default-sizer))
  ([f sizer] (apply str (reps sizer f))))

(def ascii-alpha
  (concat (range 65 (+ 65 26))
          (range 97 (+ 97 26))))

(def symbol-start
  (-> (concat ascii-alpha [\* \+ \! \- \_ \?])
      cljs.core/vec))

(def symbol-char
  (into symbol-start [\1 \2 \3 \4 \5 \6 \7 \8 \9 \0]))

(defn name-prefix
  []
  (str (rand-nth [(.fromCharCode js/String (rand-nth symbol-start)) ""])
       (.fromCharCode js/String (rand-nth ascii-alpha))))

(defn name-body
  [sizer]
  (string #(.fromCharCode js/String (rand-nth symbol-char)) sizer))

(defn name
  ([] (name default-sizer))
  ([sizer]
     (str (name-prefix)
          (name-body sizer))))

(defn symbol
  "Create a non-namepsaced symbol sized from sizer."
  ([] (symbol default-sizer))
  ([sizer] (cljs.core/symbol (name sizer))))

(defn keyword
  "Create a non-namespaced keyword sized from sizer."
  ([] (keyword default-sizer))
  ([sizer] (cljs.core/keyword (name sizer))))

(defn fisher-yates
  [coll]
  (let [as (into-array coll)]
    (loop [i (dec (alength as))]
      (if (< 1 i)
        (let [j (uniform 0 (inc i))
              t (aget as i)]
          (aset as i (aget as j))
          (aset as j t)
          (recur (dec i)))
        (into (empty coll) (seq as))))))

(defn shuffle
  [coll]
  (fisher-yates coll))

;;; TODO: uuid, date
