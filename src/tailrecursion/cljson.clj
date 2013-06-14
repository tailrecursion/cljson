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
  (let [ctor {"l" () "m" {} "s" #{}}
        m?   #(and (map? %) (= "m" (first (first %))))
        kw?  #(and (string? %) (= \ufdd0 (first %))) 
        sym? #(and (string? %) (= \ufdd1 (first %))) 
        seq* #(if (list? %) (reverse %) %)]
    (cond (vector?  x)  (mapv decode x)
          (map?     x)  (let [[k v] (first x)] (into (ctor k) (mapv decode v)))
          (kw?      x)  (keyword (subs x 2))
          (sym?     x)  (symbol (subs x 2))
          :else         x)))

(defn clj->cljson [x] (j/write-str (encode x) :escape-unicode false :escape-slash false))
(defn cljson->clj [x] (decode (j/read-str x)))
