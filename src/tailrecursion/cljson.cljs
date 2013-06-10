(ns tailrecursion.cljson)

(extend-type Keyword
  IEncodeJS
  (-clj->js [x] x)
  (-key->js [x] x))

(extend-type Symbol
  IEncodeJS
  (-clj->js [x] x)
  (-key->js [x] x))

(defn- ->clj [x]
  (let [obj? #(identical? (type %) js/Object)]
    (cond (seq? x)    (doall (map ->clj x))
          (coll? x)   (into (empty x) (map ->clj x))
          (array? x)  (vec (map ->clj x))
          (obj? x)    (into {} (for [k (js-keys x)] [k (->clj (aget x k))]))
          :else       x)))

(defn js->clj [x]
  (if (satisfies? x IEncodeClojure) (-js->clj x) (->clj x)))

(defn clj->js [x] (cljs.core/clj->js x))
