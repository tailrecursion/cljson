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

(defmulti decode-tagged get-tag)

(defn encode [x]
  (cond (satisfies? EncodeTagged x) (-encode x)
        (keyword? x) (en-str "k" (subs (str x) 1))
        (symbol? x) (en-str "y" (str x))
        (vector? x) (into-array (map encode x))
        (seq? x) (en-coll "l" x)
        (map? x) (en-coll "m" x)
        (set? x) (en-coll "s" x)
        (or (string? x) (number? x) (nil? x)) x
        :else (throw (js/Error. (format "No cljson encoding for type '%s'." (type x))))))

(defn decode-tagged [o]
  (let [tag (get-tag o), val (aget o tag)]
    (case tag
      "m" (into {} (map decode val))
      "l" (apply list (map decode val))
      "s" (set (map decode val))
      "k" (keyword val)
      "y" (apply symbol (split val #"/"))
      (if-let [reader (or (get @*tag-table* tag) @*default-data-reader-fn*)] 
        (reader (decode val))
        (throw (js/Error. (format "No reader function for tag '%s'." tag)))))))

(defn decode [v]
  (cond (array? v) (mapv decode v) (object? v) (decode-tagged v) :else v))
