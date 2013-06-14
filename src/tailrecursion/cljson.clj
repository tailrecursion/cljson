(ns tailrecursion.cljson
  (:require
    [clojure.data.json :refer [write-str read-str]]))

(defprotocol Collection
  (tag [o]))

(defprotocol Encode
  (encode [o]))

(defmacro extends-protocol
  [protocol & specs]
  `(extend-protocol ~protocol
     ~@(mapcat identity
         (for [[classes impls] (partition 2 (partition-by symbol? specs))
               class classes]
           (list* class impls)))))

(extends-protocol Collection
  clojure.lang.PersistentArrayMap
  clojure.lang.PersistentHashMap
  (tag [_] "m")
  clojure.lang.ISeq
  clojure.lang.PersistentList
  (tag [_] "l")
  clojure.lang.PersistentHashSet
  (tag [_] "s"))

(extends-protocol Encode
  clojure.lang.MapEntry
  clojure.lang.PersistentVector
  (encode [o] (mapv encode o))
  clojure.lang.PersistentArrayMap
  clojure.lang.PersistentHashMap
  clojure.lang.ISeq
  clojure.lang.PersistentList
  clojure.lang.PersistentHashSet
  (encode [o] {(tag o) (mapv encode o)})
  clojure.lang.Keyword
  (encode [o] (format "\ufdd0'%s" (subs (str o) 1)))
  clojure.lang.Symbol
  (encode [o] (format "\ufdd1'%s" o))
  String, Boolean, Long, Double
  (encode [o] o))

(defn clj->cljson
  [v]
  (write-str (encode v)))

(declare decode)

(defmulti decode-coll (comp key first))

(defmethod decode-coll "m" [m]
  (into {} (map decode (get m "m"))))

(defmethod decode-coll "l" [m]
  (apply list (map decode (get m "l"))))

(defmethod decode-coll "s" [m]
  (set (map decode (get m "s"))))

(defmulti decode-str #(.charAt ^String % 0))

(defmethod decode-str \ufdd0 [s]
  (keyword (subs s 2)))

(defmethod decode-str \ufdd1 [s]
  (symbol (subs s 2)))

(defn decode
  [v]
  (cond (vector? v) (mapv decode v)
        (map? v)    (decode-coll v)
        (string? v) (decode-str v)
        :else v))

(defn cljson->clj
  [json]
  (decode (read-str json)))
