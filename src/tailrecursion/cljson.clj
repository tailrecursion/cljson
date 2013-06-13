(ns tailrecursion.cljson
  (:require
    [clojure.data.json  :as j]))

(defn encode [x]
  (let [type-id #(cond (seq? %) 0 (vector? %) 1 (map? %) 2 (set? %) 3)]
    (cond (coll?    x)  [(type-id x) (mapv encode x)]
          (keyword? x)  (format "\ufdd0'%s" (subs (str x) 1))
          (symbol?  x)  (format "\ufdd1'%s" (str x))
          :else         x)))


(defn decode [x]
  (let [ctor #(nth [() [] {} #{}] (first %)) 
        kw?  #(= \ufdd0 (first %))
        sym? #(= \ufdd1 (first %))
        seq* #(if (list? %) (reverse %) %)]
    (cond (vector?  x)  (seq* (into (ctor x) (mapv decode (second x)))) 
          (kw?      x)  (keyword (subs x 2))
          (sym?     x)  (symbol (subs x 2))
          :else         x)))

(defn clj->cljson [x] (j/write-str (encode x)))
(defn cljson->clj [x] (decode (j/read-str x)))
