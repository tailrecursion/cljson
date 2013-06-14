(ns tailrecursion.cljson)

(defn encode [x]
  (let [mapa    #(apply array (map %1 %2))
        type-id #(cond (seq? %) "l" (map? %) "m" (set? %) "s")]
    (cond (vector? x) (mapa encode x)
          (coll?   x) (js-obj (type-id x) (mapa encode x)) 
          :else       x)))

(defn decode [x]
  (let [ctor  {"m" {} "s" #{}}
        a?    #(js* "~{} instanceof Array" %)
        o?    #(js* "~{} instanceof Object" %)
        l?    #(and (o? %) (.hasOwnProperty % "l"))
        m?    #(and (.hasOwnProperty % "m") "m")
        s?    #(and (.hasOwnProperty % "s") "s")
        coll  (and (o? x) (or (m? x) (s? x)))]
    (cond (a? x)  (mapv decode x)
          (l? x)  (map decode x)
          coll    (into (ctor coll) (mapv decode (aget x coll))) 
          :else     x)))

(defn clj->cljson [x] (.stringify js/JSON (encode x)))
(defn cljson->clj [x] (decode (.parse js/JSON x)))
