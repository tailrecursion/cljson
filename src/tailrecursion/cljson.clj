(ns tailrecursion.cljson
  (:require
    [clojure.data.json :refer [write-str read-str]]))

(set! *warn-on-reflection* true)
(defprotocol Tagged
  (tag [o]))

(defprotocol Encode
  (encode [o]))

(defmacro extends-protocol
  [protocol & specs]
  (let [class? #(or (symbol? %) (nil? %))]
    `(extend-protocol ~protocol
       ~@(mapcat identity
           (for [[classes impls] (partition 2 (partition-by class? specs))
                 class classes]
             `(~class ~@impls))))))

(extends-protocol Tagged
  clojure.lang.PersistentArrayMap
  clojure.lang.PersistentHashMap
  (tag [_] "cljson/map")
  clojure.lang.ISeq
  clojure.lang.PersistentList
  (tag [_] "cljson/list")
  clojure.lang.PersistentHashSet
  (tag [_] "cljson/set"))

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
  java.util.Date
  (encode [o] {"inst" (.format (.get @#'clojure.instant/thread-local-utc-date-format) o)})
  java.util.UUID
  (encode [o] {"uuid" (str o)})
  clojure.lang.Keyword
  (encode [o] (format "\ufdd0'%s" (subs (str o) 1)))
  clojure.lang.Symbol
  (encode [o] (format "\ufdd1'%s" o))
  String, Boolean, Long, Double, nil
  (encode [o] o))

(defn clj->cljson
  [v]
  (write-str (encode v)))

(declare decode)

(defmulti decode-tag (comp key first))

(defmethod decode-tag "cljson/map" [m]
  (into {} (map decode (get m "cljson/map"))))

(defmethod decode-tag "cljson/list" [m]
  (apply list (map decode (get m "cljson/list"))))

(defmethod decode-tag "cljson/set" [m]
  (set (map decode (get m "cljson/set"))))

(defmethod decode-tag :default [m]
  (let [[tag val] (first m)]
    (if-let [reader (or (get (merge default-data-readers *data-readers*) (symbol tag))
                        *default-data-reader-fn*)]
      (reader (decode val))
      (throw (RuntimeException. (str "No reader function for tag " tag))))))

(defmulti decode-str #(.charAt ^String % 0))

(defmethod decode-str \ufdd0 [s]
  (keyword (subs s 2)))

(defmethod decode-str \ufdd1 [s]
  (symbol (subs s 2)))

(defmethod decode-str :default [s] s)

(defn decode
  [v]
  (cond (vector? v) (mapv decode v)
        (map? v)    (decode-tag v)
        (string? v) (decode-str v)
        :else v))

(defn cljson->clj
  [json]
  (decode (read-str json)))
