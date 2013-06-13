(ns tailrecursion.cljson)

(defn clj->cljson [x]
  (let [mapa    #(apply array (map %1 %2))
        type-id #(cond (seq? %) 0 (vector? %) 1 (map? %) 2 (set? %) 3)]
    (cond (map?     x)  (array (type-id x) (mapa (partial map encode) x)) 
          (coll?    x)  (array (type-id x) (mapa encode x)) 
          :else         x)))

(defn cljson->clj [x]
  (let [ctor #(nth [() [] {} #{}] (first %)) 
        a?   #(js* "~{} instanceof Array" %)
        m?   #(and (a? %) (= 2 (first %)))
        seq* #(if (list? %) (reverse %) %)]
    (cond (m? x)  (into {} (mapv (partial map decode) (second x)))
          (a? x)  (seq* (into (ctor x) (mapv decode (second x)))) 
          :else   x)))
