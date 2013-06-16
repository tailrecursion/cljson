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
  (-encode [o] (list* "\ufdd0" (count o) (apply concat (map encode o))))
  ;; clojure.lang.PersistentArrayMap
  ;; clojure.lang.PersistentHashMap
  ;; (-encode [o] (list* "\ufdd1" (* 2 (count o)) (map encode (apply concat o))))
  ;; clojure.lang.ISeq
  ;; clojure.lang.PersistentList
  ;; (-encode [o] (list* "\ufdd2" (count o) (map encode o)))
  ;; clojure.lang.PersistentHashSet
  ;; (-encode [o] (list* "\ufdd3" (count o) (map encode o)))
  ;; java.util.Date
  ;; (-encode [o] (list "\ufdd4" 2 "inst" (.format date-format o)))
  ;; java.util.UUID
  ;; (-encode [o] (list "\ufdd4" 2 "uuid" (str o)))
  ;; clojure.lang.Keyword
  ;; (-encode [o] (list "\ufdd5" 1 (subs (str o) 1)))
  ;; clojure.lang.Symbol
  ;; (-encode [o] (list "\ufdd6" 1 (str o)))
  String, Boolean, Number, nil
  (-encode [o] [o]))

(defn encode [x]
  (-encode x))

(def tags #{"\ufdd0" ;; "\ufdd1" "\ufdd2" "\ufdd3" "\ufdd4" "\ufdd5" "\ufdd6"
            })

(defn decode [v]
  (if-let [tag (tags (first v))]
    (case tag
      "\ufdd0" (loop [n (second v) more (nnext v) out (transient [])]
                 (if (zero? n)
                   (persistent! out)
                   (if-let [tag2 (tags (first more))]
                     (recur (dec n) (drop (+ 2 (second more)) more)
                            (conj! out (decode more)))
                     (recur (dec n) (rest more) (conj! out (first more)))))))
    (first v)))

(comment
  (encode [1 [2] [3 [4]]]) ;=> ("﷐" 3 1 "﷐" 1 2 "﷐" 2 3 "﷐" 1 4)
  (decode (encode [1 [2] [3 [4]]])) ;=> [1 [2] [3 [4]]]
  )

