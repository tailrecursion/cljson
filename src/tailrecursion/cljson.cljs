(ns tailrecursion.cljson)

(defn encode [x]
  (let [type-id #(cond (seq? %) 0 (vector? %) 1 (map? %) 2 (set? %) 3)]
    (cond (map?     x)  [(type-id x) (mapv (partial mapv encode) x)]
          (coll?    x)  [(type-id x) (mapv encode x)]
          :else         x)))

(defn decode [x]
  (let [ctor #(nth [() [] {} #{}] (first %)) 
        m?   #(and (vector? %) (= 2 (first %)))
        kw?  #(and (sequential? %) (= \ufdd0 (first %))) 
        sym? #(and (sequential? %) (= \ufdd1 (first %))) 
        seq* #(if (list? %) (reverse %) %)]
    (cond (m?       x)  (into {} (mapv (partial mapv decode) (second x)))
          (vector?  x)  (seq* (into (ctor x) (mapv decode (second x)))) 
          :else         x)))

(defn clj->cljson [x] (clj->js (encode x)))
(defn cljson->clj [x] (decode (js->clj x)))
