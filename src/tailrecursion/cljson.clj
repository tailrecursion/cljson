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

(defmacro extends-protocol [protocol & specs]
  (let [class? #(or (symbol? %) (nil? %))]
    `(extend-protocol ~protocol
       ~@(mapcat identity
           (for [[classes impls] (partition 2 (partition-by class? specs))
                 class classes]
             `(~class ~@impls))))))

(defn enc [s coll]
  (let [elems (mapcat encode coll)]
    (cons (+ s (count elems)) elems)))

(extends-protocol EncodeTagged
  clojure.lang.ISeq
  clojure.lang.PersistentList
  (-encode [o] (into ["l"] (enc 2 o)))
  clojure.lang.MapEntry
  clojure.lang.PersistentVector
  (-encode [o] (into ["v"] (enc 2 o)))
  clojure.lang.PersistentArrayMap
  clojure.lang.PersistentHashMap
  (-encode [o] (into ["m"] (enc 2 o)))
  String, Boolean, Number, nil
  (-encode [o] ["s" 3 o]))

(defn encode [x]
  (-encode x))

(def tags #{"l" "v" "m" "s"})

(defn decode
  ([start end v]
     (case (get v start)
       ;; scalars
       "s" (get v (+ start 2))
       ;; vectors
       "v" (if (= 2 (get v (inc start)))
             []
             (let [content-start (+ 2 start)
                   content-end   (+ content-start (- (get v 1) 2))]
               (loop [elem-start content-start, out []]
                 (let [elem-end (+ elem-start (get v (inc elem-start)))]
                   (if (= elem-end content-end)
                     (conj out (decode elem-start elem-end v))
                     (recur elem-end (conj out (decode elem-start elem-end v))))))))))
  ([v] (decode 0 (count v) v)))

;; (defn decode-coll [ out]
;;   (loop [n (first v) more (rest v) out (transient out)]
;;     (if (zero? n)
;;       (persistent! out)
;;       (recur (dec n) (drop (+ 2 (second more)) more) (conj! out (decode more))))))

;; (defn decode [[tag & [n & content]]]
;;   (if (tags tag)
;;     (case tag
;;       "XXfdd1" (loop [cnt n, more content, out []]
;;                  (if (zero? n)
;;                    out
;;                    (recur (dec cnt) ))))
;;     tag))

;; (defn decode-coll [v out]
;;   (loop [n (first v) more (rest v) out (transient out)]
;;     (if (zero? n)
;;       (persistent! out)
;;       (recur (dec n) (drop (+ 2 (second more)) more) (conj! out (decode more))))))

;; (defn decode [[tag? & more]]
;;   (if-let [tag (tags tag?)]
;;     (case tag
;;       "XXfdd0" (decode-coll more [])
;;       "XXfdd1" (decode-coll more {})
;;       "XXfdd2" (apply list (decode-coll more []))
;;       "XXfdd3" (decode-coll more #{})
;;       "XXfdd4" (keyword (second more))
;;       "XXfdd5" (symbol (second more))
;;       "XXfdd6" (let [[_ edn-tag & val] more]
;;                  (if-let [reader (or (get (merge default-data-readers *data-readers*)
;;                                           (symbol edn-tag))
;;                                      *default-data-reader-fn*)]
;;                    (reader (decode val))
;;                    (throw (Exception. (format "No reader function for tag '%s'." edn-tag)))))
;;       "XXfdd7" (second more)) 
;;     tag?))

(comment
  {9060365806611651666 :C5TLcAU8, 1459932648456178208 :Nb_MlUoHyyLNw0g256Lm8rpKGyOiI0E99oNN-PKWNLAo3kMZ}
  (encode [1 [2] [3 [4]]]) ;=> ("﷐" 3 1 "﷐" 1 2 "﷐" 2 3 "﷐" 1 4)
  (decode (encode [1 [2] [3 [4]]])) ;=> [1 [2] [3 [4]]]
  )

