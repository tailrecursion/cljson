(ns tailrecursion.cljson
  (:require
    [clojure.set :refer [map-invert]]
    [cheshire.core :refer [generate-string parse-string]]))

(declare encode decode encode! decode!)

;; PUBLIC ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol EncodeTagged (-encode [x v]))

(defn clj->cljson
  "Convert clj data to JSON string."
  [v]
  (generate-string (encode v) {:escape-non-ascii true}))

(defn cljson->clj
  "Convert JSON string to clj data."
  [s]
  (decode (parse-string s)))

;; INTERNAL ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def types {\ufdd0  :keyword
            \ufdd1  :symbol
            \ufdd2  :meta
            \ufdd3  :vector
            \ufdd4  :map
            \ufdd5  :list
            \ufdd6  :set
            \ufdd7  :tagged})

(def tags (map-invert types))

(def date-format (.get @#'clojure.instant/thread-local-utc-date-format))

(defn encode-collection! [tag v items]
  (let [len (count items)]
    (loop [i 0, xs items, out (-> v (conj! (tags tag)) (conj! len))]
      (if (< i len)
        (recur (inc i) (rest xs) (encode! (first xs) out))
        out))))

(defn encode-scalar! [tag v value]
  (-> v (conj! (tags tag)) (conj! value)))

(defn encode-tagged! [tag v item]
  (encode! item (-> v (conj! (:tagged tags)) (conj! tag))))

(defn encode-native! [v value]
  (conj! v value))

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
  (-encode [x v] (encode-collection! :vector v x))
  clojure.lang.PersistentArrayMap
  clojure.lang.PersistentHashMap
  (-encode [x v] (encode-collection! :map v (apply concat x)))
  clojure.lang.ISeq
  clojure.lang.PersistentList
  (-encode [x v] (encode-collection! :list v x))
  clojure.lang.PersistentHashSet
  (-encode [x v] (encode-collection! :set v x))
  java.util.Date
  (-encode [x v] (encode-tagged! "inst" v (.format date-format x)))
  java.util.UUID
  (-encode [x v] (encode-tagged! "uuid" v (str x)))
  clojure.lang.Keyword
  (-encode [x v] (encode-scalar! :keyword v (subs (str x) 1)))
  clojure.lang.Symbol
  (-encode [x v] (encode-scalar! :symbol v (str x)))
  String, Boolean, Number, nil
  (-encode [x v] (encode-native! v x)))

(defn interpret
  "Attempts to encode an object that does not satisfy EncodeTagged,
  but for which the printed representation contains a tag."
  [printed]
  (when-let [match (second (re-matches #"#([^<].*)" printed))]
    (let [tag (read-string match)
          val (read-string (subs match (.length (str tag))))]
      [tag (encode val)])))

(defn encode! [x v]
  (if-let [m (and *print-meta* (meta x))]
    (->> (conj! v (:meta tags))
      (encode! m)
      (encode! (with-meta x nil))) 
    (if (satisfies? EncodeTagged x)
      (-encode x v)
      :not-implemented
      )))

(defn decode-collection! [v coll]
  (let [len (second @v)]
    (swap! v (partial drop 2))
    (persistent!
      (loop [i 0, out (transient coll)]
        (if (< i len) (recur (inc i) (conj! out (decode! v))) out)))))

(defn decode! [v]
  (let [tag (first @v)]
    (case (types tag) 
      :meta     (do (swap! v rest) (let [m (decode! v)] (with-meta (decode! v) m)))
      :keyword  (let [out (keyword (second @v))] (swap! v (partial drop 2)) out) 
      :symbol   (let [out (symbol (second @v))] (swap! v (partial drop 2)) out) 
      :vector   (decode-collection! v [])
      :map      (let [len (second @v)]
                  (swap! v (partial drop 2))
                  (persistent! (loop [i 0, out (transient {})]
                                 (if (< i len)
                                   (let [mk (decode! v), mv (decode! v)]
                                     (recur (+ i 2) (assoc! out mk mv))) 
                                   out))))
      :list     (let [len (second @v)]
                  (swap! v (partial drop 2))
                  (reverse (loop [i 0, out ()]
                             (if (< i len)
                               (recur (inc i) (cons (decode! v) out))
                               out))))
      :set      (decode-collection! v #{})
      :tagged   (let [tag   (symbol (second @v))
                      rdrs  (merge default-data-readers *data-readers*)]
                  (swap! v (partial drop 2))
                  (if-let [reader (or (get rdrs tag) *default-data-reader-fn*)]
                    (reader (decode! v))
                    (throw (Exception. (format "No reader function for tag '%s'." tag)))))
      (do (swap! v rest) tag))))

(defn encode [x] (persistent! (encode! x (transient []))))
(defn decode [v] (decode! (atom v)))

;
;(defn encode [x]
;  (if-let [m (and *print-meta* (meta x))] 
;    ["z" (encode m) (encode (with-meta x nil))]
;    (if (satisfies? EncodeTagged x)
;      (-encode x)
;      (let [printed (pr-str x)]
;        (or (interpret printed)
;            (throw (IllegalArgumentException.
;                     (format "No cljson encoding for '%s'." printed))))))))
;
;(defn decode-tagged [[tag & val]]
;  (case tag
;    "v" (mapv decode val)
;    "m" (apply hash-map (map decode val))
;    "l" (apply list (map decode val))
;    "s" (set (map decode val))
;    "k" (keyword (first val))
;    "y" (symbol (first val))
;    "z" (let [[m v] (map decode val)] (with-meta v m))
;
;      ))
;
;(defn decode [v] (if (sequential? v) (decode-tagged v) v))
