(ns tailrecursion.cljson
  (:require
    [clojure.data.json  :as j]
    [clojure.walk       :as w]))

(defmulti ->str type)
(defmulti ->clj #(and (string? %) (case (first %) \ufdd0 0 \ufdd1 1 false)))

(defmethod ->str :default             [x] (str x))
(defmethod ->str clojure.lang.Keyword [x] (format "\ufdd0'%s" (name x)))
(defmethod ->str clojure.lang.Symbol  [x] (format "\ufdd1'%s" (name x)))
(defmethod ->clj false                [x] x)
(defmethod ->clj 0                    [x] (keyword (subs x 2)))
(defmethod ->clj 1                    [x] (symbol  (subs x 2)))

(extend-type clojure.lang.Keyword
  j/JSONWriter
  (-write [this out] (#'j/write-string (->str this) out)))

(extend-type clojure.lang.Symbol
  j/JSONWriter
  (-write [this out] (#'j/write-string (->str this) out)))

(defn write-str [x] (j/write-str x :key-fn ->str))
(defn read-str  [x] (w/postwalk ->clj (j/read-str x :key-fn ->clj)))
