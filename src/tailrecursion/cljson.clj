(ns tailrecursion.cljson
  (:require
    [clojure.data.json  :as j]))

(defn encode [x]
  (let [type-id #(cond (seq? %) "l" (map? %) "m" (set? %) "s")]
    (cond (vector?  x)  (mapv encode x)
          (coll?    x)  {(type-id x) (mapv encode x)} 
          (keyword? x)  (format "\ufdd0'%s" (subs (str x) 1))
          (symbol?  x)  (format "\ufdd1'%s" (str x))
          :else         x)))

(defn decode [x]
  (let [ctor {"m" {} "s" #{}}
        l?   #(and (map? %) (= "l" (first (first %))))
        kw?  #(and (string? %) (= \ufdd0 (first %))) 
        sym? #(and (string? %) (= \ufdd1 (first %)))]
    (cond (vector?  x)  (mapv decode x)
          (l?       x)  (map decode (second (first x)))
          (map?     x)  (let [[k v] (first x)] (into (ctor k) (mapv decode v)))
          (kw?      x)  (keyword (subs x 2))
          (sym?     x)  (symbol (subs x 2))
          :else         x)))

(defn clj->cljson [x] (j/write-str (encode x)))
(defn cljson->clj [x] (decode (j/read-str x)))
