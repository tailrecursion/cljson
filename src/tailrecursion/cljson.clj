(ns tailrecursion.cljson
  (:require
    [clojure.data.json  :as j]
    [clojure.walk       :as w]))

(extend-type clojure.lang.Keyword
  j/JSONWriter
  (-write [this out]
    (#'j/write-string (format "\ufdd0'%s" (name this)) out)))

(extend-type clojure.lang.Symbol
  j/JSONWriter
  (-write [this out]
    (#'j/write-string (format "\ufdd1'%s" (name this)) out)))

(defn- write-key-fn [x]
  (cond
    (keyword? x) (format "\ufdd0'%s" (name x))
    (symbol?  x) (format "\ufdd1'%s" (name x))
    :else        (str x)))

(defn- read-key-fn [x]
  (let [kwsym? #(and (string? %) (< 2 (count %)))
        kw     #(keyword (subs % 2))
        sym    #(symbol  (subs % 2))
        kwsym  #(case (first %) \ufdd0 (kw %) \ufdd1 (sym %) %)]
    (if (kwsym? x) (kwsym x) x)))

(defn write-str [x]
  (j/write-str x :key-fn write-key-fn))

(defn read-str [x]
  (w/postwalk read-key-fn (j/read-str x :key-fn read-key-fn)))
