(ns tailrecursion.cljson
  (:require
   [cheshire.core :refer [generate-string parse-string]]))

(declare encode decode)

;; PUBLIC ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol EncodeTagged (-encode [o]))

(defn clj->cljson
  "Convert clj data to JSON string."
  [v]
  (generate-string (encode v) {:escape-non-ascii true}))

(defn cljson->clj
  "Convert JSON string to clj data."
  [s]
  (decode (parse-string s)))

;; INTERNAL ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def date-format (.get @#'clojure.instant/thread-local-utc-date-format))

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
  (-encode [o] (list* "\ufdd0" (count o) (mapcat encode o)))
  clojure.lang.PersistentArrayMap
  clojure.lang.PersistentHashMap
  (-encode [o] (list* "\ufdd1" (count o) (mapcat encode o)))
  clojure.lang.ISeq
  clojure.lang.PersistentList
  (-encode [o] (list* "\ufdd2" (count o) (mapcat encode o)))
  clojure.lang.PersistentHashSet
  (-encode [o] (list* "\ufdd3" (count o) (mapcat encode o)))
  clojure.lang.Keyword
  (-encode [o] (list "\ufdd4" 1 (subs (str o) 1)))
  clojure.lang.Symbol
  (-encode [o] (list "\ufdd5" 1 (str o)))
  String, Boolean, Number, nil
  (-encode [o] [o])
  java.util.Date
  (-encode [o] (list "\ufdd6" 2 "inst" (.format date-format o)))
  java.util.UUID
  (-encode [o] (list "\ufdd6" 2 "uuid" (str o))))

(defn interpret
  "Attempts to encode an object that does not satisfy EncodeTagged,
  but for which the printed representation contains a tag."
  [printed]
  (when-let [match (second (re-matches #"#([^<].*)" printed))]
    (let [tag (read-string match)
          val (read-string (subs match (.length (str tag))))]
      (list* "\ufdd6" 2 tag (encode val)))))

(defn encode [x]
  (if (satisfies? EncodeTagged x)
    (-encode x)
    (let [printed (pr-str x)]
      (or (interpret printed)
          (throw (IllegalArgumentException.
                  (format "No cljson encoding for '%s'." printed)))))))

(def tags #{"\ufdd0" "\ufdd1" "\ufdd2" "\ufdd3" "\ufdd4" "\ufdd5" "\ufdd6" "\ufdd7"})

(defn decode-coll [v out]
  (loop [n (first v) more (rest v) out (transient out)]
    (if (zero? n)
      (persistent! out)
      (if (tags (first more))
        (recur (dec n)
               (drop (+ 2 (second more)) more)
               (conj! out (decode more)))
        (recur (dec n) (rest more) (conj! out (first more)))))))

(defn decode [[tag? & more]]
  (if-let [tag (tags tag?)]
    (case tag
      "\ufdd0" (decode-coll more [])
      "\ufdd1" (decode-coll more {})
      "\ufdd2" (apply list (decode-coll more []))
      "\ufdd3" (decode-coll more #{})
      "\ufdd4" (keyword (second more))
      "\ufdd5" (symbol (second more))
      "\ufdd6" (let [[_ edn-tag & val] more]
                 (if-let [reader (or (get (merge default-data-readers *data-readers*)
                                          (symbol edn-tag))
                                     *default-data-reader-fn*)]
                   (reader (decode val))
                   (throw (Exception. (format "No reader function for tag '%s'." edn-tag)))))
      ) 
    tag?))

(comment
  (encode [1 [2] [3 [4]]]) ;=> ("﷐" 3 1 "﷐" 1 2 "﷐" 2 3 "﷐" 1 4)
  (decode (encode [1 [2] [3 [4]]])) ;=> [1 [2] [3 [4]]]
  )

