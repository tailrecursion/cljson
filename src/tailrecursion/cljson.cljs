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

(def get-tag  #(js* "(function(o){for(var k in o) return k;})(~{})" %))
(def object?  #(js* "(~{} instanceof Object)" %))
(def en-str   #(doto (js-obj) (aset %1 %2)))
(def en-coll  #(doto (js-obj) (aset %1 (into-array (map encode %2)))))

(extend-protocol EncodeTagged
  js/Date
  (-encode [o] (doto (js-obj) (aset "inst" (subs (pr-str o) 7 36))))
  cljs.core.UUID
  (-encode [o] (doto (js-obj) (aset "uuid" (.-uuid o)))))

(defn interpret
  "Attempts to encode an object that does not satisfy EncodeTagged,
  but for which the printed representation contains a tag."
  [x]
  (when-let [match (second (re-matches #"#([^<].*)" (pr-str x)))]
    (let [tag (reader/read-string match)
          val (reader/read-string (subs match (.-length (str tag))))]
      (doto (js-obj) (aset (str tag) (encode val))))))

(defn encode [x]
  (if-let [m (and *print-meta* (meta x))]
    (doto (js-obj) (aset "z" (into-array [(encode m) (encode (with-meta x nil))])))
    (cond (satisfies? EncodeTagged x) (-encode x)
          (keyword? x) (en-str "k" (subs (str x) 1))
          (symbol? x) (en-str "y" (str x))
          (vector? x) (into-array (map encode x))
          (seq? x) (en-coll "l" x)
          (and (map? x) (not (satisfies? cljs.core/IRecord x)))
          (doto (js-obj) (aset "m" (into-array (map encode (apply concat x)))))
          (set? x) (en-coll "s" x)
          (or (string? x) (number? x) (nil? x)) x
          :else (or (interpret x)
                    (throw (js/Error. (format "No cljson encoding for type '%s'." (type x))))))))

(defn decode-tagged [o]
  (let [tag (get-tag o), val (aget o tag)]
    (case tag
      "m" (loop [i 0, out (transient {})]
            (if (< i (alength val))
              (recur (+ i 2) (assoc! out (decode (aget val i)) (decode (aget val (inc i)))))
              (persistent! out)))
      "l" (loop [i (dec (alength val)), out ()]
            (if (neg? i)
              out
              (recur (dec i) (conj out (decode (aget val i))))))
      "s" (loop [i 0, out (transient #{})]
            (if (< i (alength val))
              (recur (inc i) (conj! out (decode (aget val i))))
              (persistent! out)))
      "k" (keyword val)
      "y" (let [idx (.indexOf val "/")]
            (if (neg? idx)
              (symbol val)
              (symbol (.slice val 0 idx) (.slice val (inc idx)))))
      "z" (let [[m v] (decode val)] (with-meta v m))
      (if-let [reader (or (get @*tag-table* tag) @*default-data-reader-fn*)]
        (reader (decode val))
        (throw (js/Error. (format "No reader function for tag '%s'." tag)))))))

(defn decode [v]
  (cond (array? v)
        (loop [i 0, out (transient [])]
          (if (< i (alength v))
            (recur (inc i) (conj! out (decode (aget v i))))
            (persistent! out)))
        (object? v)
        (decode-tagged v)
        :else v))
