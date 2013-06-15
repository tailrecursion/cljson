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
  java.util.Date
  (-encode [o] {"inst" (.format (.get @#'clojure.instant/thread-local-utc-date-format) o)})
  java.util.UUID
  (-encode [o] {"uuid" (str o)})
  clojure.lang.Keyword
  (-encode [o] {"k" (subs (str o) 1)})
  clojure.lang.Symbol
  (-encode [o] {"y" (str o)})
  String, Boolean, Long, Double, nil
  (-encode [o] o))

(defn interpret
  "Attempts to encode an object that does not satisfy EncodeTagged,
  but for which the printed representation contains a tag."
  [printed]
  (when-let [match (second (re-matches #"#([^<].*)" printed))]
    (let [tag (read-string match)
          val (read-string (subs match (.length (str tag))))]
      {tag (encode val)})))

(defn encode [x]
  (if (satisfies? EncodeTagged x)
    (-encode x)
    (let [printed (pr-str x)]
      (or (interpret printed)
          (throw (IllegalArgumentException.
                  (format "No cljson encoding for '%s'." printed)))))))

(defn decode-tagged [o]
  (let [[tag val] (first o)]
    (case tag
      "m" (into {} (map decode val))
      "l" (apply list (map decode val))
      "s" (set (map decode val))
      "k" (keyword val)
      "y" (apply symbol (.split ^String val "/"))
      (if-let [reader (or (get (merge default-data-readers *data-readers*) (symbol tag))
                          *default-data-reader-fn*)]
        (reader (decode val))
        (throw (Exception. (format "No reader function for tag '%s'." tag)))))))

(defn decode [v]
  (cond (or (vector? v) (seq? v)) (mapv decode v) (map? v) (decode-tagged v) :else v))
