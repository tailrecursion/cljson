(ns tailrecursion.cljson
  (:require
   [cheshire.core :refer [generate-string parse-string]]))

(declare decode)

;; PUBLIC ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol  Encode      (encode [o]))
(defmulti     decode-tag  (comp key first))

(defn clj->cljson "Convert clj data to JSON string." [v]
  (generate-string (encode v) {:escape-non-ascii true}))
(defn cljson->clj "Convert JSON string to clj data." [s]
  (decode (parse-string s)))

;; INTERNAL ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro extends-protocol [protocol & specs]
  (let [class? #(or (symbol? %) (nil? %))]
    `(extend-protocol ~protocol
       ~@(mapcat identity
           (for [[classes impls] (partition 2 (partition-by class? specs))
                 class classes]
             `(~class ~@impls))))))

(extends-protocol Encode
  clojure.lang.MapEntry
  clojure.lang.PersistentVector
  (encode [o] (mapv encode o))
  clojure.lang.PersistentArrayMap
  clojure.lang.PersistentHashMap
  (encode [o] {"m" (mapv encode o)})
  clojure.lang.ISeq
  clojure.lang.PersistentList
  (encode [o] {"l" (mapv encode o)})
  clojure.lang.PersistentHashSet
  (encode [o] {"s" (mapv encode o)})
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

(defmethod decode-tag "m" [m] (into {} (map decode (get m "m"))))
(defmethod decode-tag "l" [m] (apply list (map decode (get m "l"))))
(defmethod decode-tag "s" [m] (set (map decode (get m "s"))))

(defmethod decode-tag :default [m]
  (let [[tag val] (first m)
        reader-fn (merge default-data-readers *data-readers*)
        reader    (or (get reader-fn (symbol tag)) *default-data-reader-fn*)]
    (if reader (reader (decode val))
        (throw (Exception. (format "No reader function for tag '%s'." tag))))))

(defn decode [v]
  (cond (or (seq? v)
            (vector? v))
        (mapv decode v)
        (map? v)
        (decode-tag v)
        (and (string? v)
             (not (.isEmpty ^String v)))
        (case (.charAt ^String v 0)
          \ufdd0 (keyword (subs v 2))
          \ufdd1 (symbol (subs v 2))
          v)
        :else v))
