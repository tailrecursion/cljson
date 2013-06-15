(ns tailrecursion.cljson
  (:require-macros [tailrecursion.cljson :refer [extends-protocol]])
  (:require [cljs.reader :as reader :refer [*default-data-reader-fn*]]
            [goog.date.DateTime :as date]))

(declare encode decode get-tag)

;; PUBLIC ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol EncodeTagged
  "Encode a cljs thing o as a JS tagged literal of the form {tag: value}, where
  value is composed of JS objects that can be encoded as JSON."
  (-encode [o]))

(def tag-table
  (atom {"m" (into {} (map decode (aget o "m")))
         "l" (apply list (map decode (aget o "l")))
         "s" (set (map decode (aget o "s")))
         "k" (apply keyword (aget o "k")) 
         "y" (apply symbol (aget o "y"))}))

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
(def array?   #(js* "(~{} instanceof Array)" %))
(def object?  #(js* "(~{} instanceof Object)" %))
(def en-coll  #(doto (js-obj) (aset %1 (into-array (map encode %2)))))
(def en-str   #(doto (js-obj) (aset %1 (into-array [(namespace %2) (name %2)]))))

(defn encode [x]
  (cond (satisfies? EncodeTagged x) (-encode x)
        (keyword? x) (en-str "k" x)
        (symbol? x) (en-str "y" x)
        (vector? x) (into-array (map encode x))
        (seq? x) (en-coll "l" x)
        (map? x) (en-coll "m" x)
        (set? x) (en-coll "s" x)
        (or (string? x) (number? x) (nil? x)) x
        :else (throw (js/Error. (format "No cljson encoding for type '%s'." (type x))))))


(defmethod decode-tagged :default [o]
  (let [[tag val] [(get-tag o) (aget o tag)]]
    (if-let [reader (or (get @reader/tag-table tag) @*default-data-reader-fn*)] 
      (reader (decode val))
      (throw (js/Error. (format "No reader function for tag '%s'." tag))))))

(defn decode [v]
  (cond (array? v) (mapv decode v) (object? v) (decode-tagged v) :else v))
