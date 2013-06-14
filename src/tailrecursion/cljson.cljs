(ns tailrecursion.cljson)

(defn encode [x]
  (let [mapa    #(apply array (map %1 %2))
        type-id #(cond (seq? %) "l" (map? %) "m" (set? %) "s")]
    (cond (vector? x) (mapa encode x)
          (coll?   x) (js-obj (type-id x) (mapa encode x)) 
          :else       x)))

(defn decode [x]
  (let [ctor      {"l" () "m" {} "s" #{}}
        a?        #(js* "~{} instanceof Array" %)
        o?        #(js* "~{} instanceof Object" %)
        l?        #(when (.hasOwnProperty % "l") "l")
        m?        #(when (.hasOwnProperty % "m") "m")
        s?        #(when (.hasOwnProperty % "s") "s")
        kw?       #(and (string? %) (= \ufdd0 (first %))) 
        sym?      #(and (string? %) (= \ufdd1 (first %))) 
        seq*      #(if (list? %) (reverse %) %)
        coll-typ  (and (o? x) (or (m? x) (l? x) (s? x)))]
    (cond (a?   x)  (mapv decode x)
          coll-typ  (seq* (into (ctor coll-typ) (mapv decode (aget x coll)))) 
          (kw?  x)  (keyword (subs x 2))
          (sym? x)  (symbol (subs x 2))
          :else     x)))

(defn clj->cljson [x] (.stringify js/JSON (encode x)))
(defn cljson->clj [x] (decode (.parse js/JSON x)))
