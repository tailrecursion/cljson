(ns tailrecursion.cljson
  (:require
   [cheshire.core :refer [generate-string parse-string]]))

(declare encode decode)

;; PUBLIC ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol EncodeTagged (-encode [o]))
(defmulti decode-tagged (comp key first))

(defn clj->cljson
  "Convert clj data to JSON string."
  [v]
  (generate-string (encode v) {:escape-non-ascii true}))

(defn cljson->clj
  "Convert JSON string to clj data."
  [s]
  (decode (parse-string s)))

;; INTERNAL ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro extends-protocol [protocol & specs]
  (let [class? #(or (symbol? %) (nil? %))]
    `(extend-protocol ~protocol
       ~@(mapcat identity
           (for [[classes impls] (partition 2 (partition-by class? specs))
                 class classes]
             `(~class ~@impls))))))

(extends-protocol EncodeTagged
  clojure.lang.MapEntry
  clojure.lang.PersistentVector
  (-encode [o] (mapv encode o))
  clojure.lang.PersistentArrayMap
  clojure.lang.PersistentHashMap
  (-encode [o] {"m" (mapv encode o)})
  clojure.lang.ISeq
  clojure.lang.PersistentList
  (-encode [o] {"l" (mapv encode o)})
  clojure.lang.PersistentHashSet
  (-encode [o] {"s" (mapv encode o)})
  clojure.lang.Keyword
  (-encode [o] {"k" (subs (str o) 1)})
  clojure.lang.Symbol
  (-encode [o] {"y" (str o)})
  String, Boolean, Long, Double, nil
  (-encode [o] o))

(defn interpret [printed]
  (when-let [match (re-matches #"#([^<]\S+)\s+(.*)" printed)]
    (let [[_ tag printed-val] match]
      {tag (encode (read-string printed-val))})))

(defn encode [x]
  (if (satisfies? EncodeTagged x)
    (-encode x)
    (let [printed (pr-str x)]
      (or (interpret printed)
          (throw (IllegalArgumentException.
                  (format "No cljson encoding for '%s'." printed)))))))

(defmethod decode-tagged "m" [m] (into {} (map decode (get m "m"))))
(defmethod decode-tagged "l" [m] (apply list (map decode (get m "l"))))
(defmethod decode-tagged "s" [m] (set (map decode (get m "s"))))
(defmethod decode-tagged "k" [m] (keyword (get m "k")))
(defmethod decode-tagged "y" [m] (symbol (get m "y")))

(defmethod decode-tagged :default [m]
  (let [[tag val] (first m)
        reader-fn (merge default-data-readers *data-readers*)
        reader    (or (get reader-fn (symbol tag)) *default-data-reader-fn*)]
    (if reader (reader (decode val))
        (throw (Exception. (format "No reader function for tag '%s'." tag))))))

(defn decode [v]
  (cond (or (vector? v) (seq? v)) (mapv decode v) (map? v) (decode-tagged v) :else v))
