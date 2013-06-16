(ns tailrecursion.cljson
  (:require-macros [tailrecursion.cljson :refer [extends-protocol]])
  (:require [cljs.reader :as reader :refer [*tag-table* *default-data-reader-fn*]]
            [goog.date.DateTime :as date]
            [clojure.string :refer [split]]))

(declare encode decode)

;; PUBLIC ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol EncodeTagged
  "Encode a cljs thing o as a JS tagged literal of the form {tag: value}, where
  value is composed of JS objects that can be encoded as JSON."
  (-encode [o]))

(defn clj->cljson
  "Convert clj data to JSON string."
  [v]
  (.stringify js/JSON (encode v)))

(defn cljson->clj
  "Convert JSON string to clj data."
  [s]
  (decode (.parse js/JSON s)))

;; INTERNAL ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn enc-atom
  ([tag val] 
   (doto (make-array 2) (aset 0 tag) (aset 1 val)))
  ([tag val1 val2]
   (doto (make-array 3) (aset 0 tag) (aset 1 val1) (aset 2 val2))))

(defn enc-coll [tag val]
  (let [len (count val)
        out (make-array (inc len))]
    (aset out 0 tag)
    (loop [i 0, c val]
      (if (< i len)
        (let [i (inc i)]
          (aset out i (first c))
          (recur i (rest c)))
        out))))

(extend-protocol EncodeTagged
  js/Date
  (-encode [o] (enc-atom "inst" (subs (pr-str o) 7 36)))
  cljs.core.UUID
  (-encode [o] (enc-atom "uuid" (.-uuid o))))

(defn interpret
  "Attempts to encode an object that does not satisfy EncodeTagged,
  but for which the printed representation contains a tag."
  [x]
  (when-let [match (second (re-matches #"#([^<].*)" (pr-str x)))]
    (let [tag (reader/read-string match)
          val (reader/read-string (subs match (.-length (str tag))))]
      (enc-atom (str tag) (encode val)))))

(defn encode [x]
  (if-let [m (and *print-meta* (meta x))]
    (enc-atom "z" (encode m) (encode (with-meta x nil)))
    (cond (satisfies? EncodeTagged x) (-encode x)
          (keyword? x) (enc-atom "k" (subs (str x) 1))
          (symbol? x) (enc-atom "y" (str x))
          (vector? x) (enc-coll "v" (map encode x))
          (seq? x) (enc-coll "l" (map encode x))
          (and (map? x) (not (satisfies? cljs.core/IRecord x)))
          (enc-coll "m" (map encode (apply concat x)))
          (set? x) (enc-coll "s" (map encode x))
          (or (string? x) (number? x) (nil? x)) x
          :else (or (interpret x)
                    (throw (js/Error. (format "No cljson encoding for type '%s'." (type x))))))))

(defn decode-tagged [o]
  (let [tag (aget o 0)]
    (case tag
      "v" (loop [i 1, len (alength o), out (transient [])]
            (if (< i len)
              (recur (inc i) len (conj! out (decode (aget o i)))) 
              (persistent! out)))
      "m" (loop [i 1, len (alength o), out (transient {})]
            (if (< i len)
              (recur (+ i 2) len (assoc! out (decode (aget val i)) (decode (aget val (inc i)))))
              (persistent! out)))
      "l" (rest o) 
      "s" (loop [i 1, len (alength o), out (transient #{})]
            (if (< i len)
              (recur (inc i) len (conj! out (decode (aget o i))))))
      "k" (keyword (aget o 1))
      "y" (let [val (aget o 1)
                idx (.indexOf val "/")]
            (if (neg? idx)
              (symbol val)
              (symbol (.slice val 0 idx) (.slice val (inc idx)))))
      "z" (let [m (decode (aget o 1)), v (decode (aget o 2))] (with-meta v m))
      (if-let [reader (or (get @*tag-table* tag) @*default-data-reader-fn*)]
        (reader (decode (aget o 1)))
        (throw (js/Error. (format "No reader function for tag '%s'." tag)))))))

(defn decode [v]
  (if (array? v) (decode-tagged v) v))
