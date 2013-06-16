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

(def object?  #(js* "(~{} instanceof Object)" %))
(def enc      #(doto (array) (aset 0 %1) (aset 1 %2)))

(extend-protocol EncodeTagged
  js/Date
  (-encode [o] (enc "inst" (subs (pr-str o) 7 36)))
  cljs.core.UUID
  (-encode [o] (enc "uuid" (.-uuid o))))

(defn interpret
  "Attempts to encode an object that does not satisfy EncodeTagged,
  but for which the printed representation contains a tag."
  [x]
  (when-let [match (second (re-matches #"#([^<].*)" (pr-str x)))]
    (let [tag (reader/read-string match)
          val (reader/read-string (subs match (.-length (str tag))))]
      (enc (str tag) (encode val)))))

(defn encode [x]
  (if-let [m (and *print-meta* (meta x))]
    (enc "z" (into-array [(encode m) (encode (with-meta x nil))]))
    (cond (satisfies? EncodeTagged x) (-encode x)
          (keyword? x) (enc "k" (subs (str x) 1))
          (symbol? x) (enc "y" (str x))
          (vector? x) (enc "v" (into-array (map encode x)))
          (seq? x) (enc "l" (into-array (map encode x)))
          (and (map? x) (not (satisfies? cljs.core/IRecord x)))
          (enc "m" (into-array (map encode (apply concat x))))
          (set? x) (enc "s" (into-array (map encode x)))
          (or (string? x) (number? x) (nil? x)) x
          :else (or (interpret x)
                    (throw (js/Error. (format "No cljson encoding for type '%s'." (type x))))))))

(defn decode-tagged [o]
  (let [tag (aget o 0), val (aget o 1)]
    (case tag
      "v" (persistent!
           (areduce val i out (transient []) (conj! out (decode (aget val i)))))
      "m" (loop [i 0, out (transient {})]
            (if (< i (alength val))
              (recur (+ i 2) (assoc! out (decode (aget val i)) (decode (aget val (inc i)))))
              (persistent! out)))
      "l" (loop [i (dec (alength val)), out ()]
            (if (neg? i)
              out
              (recur (dec i) (conj out (decode (aget val i))))))
      "s" (persistent!
           (areduce val i out (transient #{}) (conj! out (decode (aget val i)))))
      "k" (keyword val)
      "y" (let [idx (.indexOf val "/")]
            (if (neg? idx)
              (symbol val)
              (symbol (.slice val 0 idx) (.slice val (inc idx)))))
      "z" (let [[m v] (map decode val)] (with-meta v m))
      (if-let [reader (or (get @*tag-table* tag) @*default-data-reader-fn*)]
        (reader (decode val))
        (throw (js/Error. (format "No reader function for tag '%s'." tag)))))))

(defn decode [v]
  (if (array? v) (decode-tagged v) v))
