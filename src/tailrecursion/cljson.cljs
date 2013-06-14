(ns tailrecursion.cljson
  (:require-macros [tailrecursion.cljson :refer [extends-protocol]])
  (:require [cljs.reader :as reader :refer [tag-table *default-data-reader-fn*]]
            [goog.date.DateTime :as date]))

(declare encode decode get-tag)

;; PUBLIC ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol Encode (encode [o]))
(defmulti decode-tag get-tag)

(defn clj->cljson "Convert clj data to JSON string." [v] (.stringify js/JSON (encode v)))
(defn cljson->clj "Convert JSON string to clj data." [s] (decode (.parse js/JSON s)))

;; INTERNAL ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-tag [o] (js* "(function(o){for(var k in o) return k;})(~{})" %))
(defn array?  [o] (js* "(~{} instanceof Array)" o))
(defn object? [o] (js* "(~{} instanceof Object)" o))

(extends-protocol Encode
  cljs.core.Vector
  (encode [o] (into-array (map encode o))
  cljs.core.PersistentArrayMap
  cljs.core.PersistentHashMap
  (encode [o] (doto (js-obj) (aset "m" (into-array (map encode o)))))
  cljs.core.ISeq
  (encode [o] (doto (js-obj) (aset "l" (into-array (map encode o)))))
  cljs.core.PersistentHashSet
  (encode [o] (doto (js-obj) (aset "s" (into-array (map encode o)))))
  js/Date
  (encode [o] (doto (js-obj)
                (aset "inst" (.toUTCIsoString (date/fromRfc822String (str o))))))
  cljs.core/UUID
  (encode [o] (doto (js-obj) (aset "uuid" (.-uuid o))))
  js/String, js/Boolean, js/Number, nil
  (encode [o] o)))

(defmethod decode-tag "m" [o] (into {} (map decode (aget o "m"))))
(defmethod decode-tag "l" [o] (apply list (map decode (aget o "l"))))
(defmethod decode-tag "s" [o] (set (map decode (aget o "s"))))

(defmethod decode-tag :default [o]
  (let [[tag val] [(get-tag o) (aget o tag)] 
        reader    (or (get @tag-table tag) @*default-data-reader-fn*)]
    (if reader (reader (decode val))
        (throw (js/Error. (format "No reader function for tag '%s'." tag))))))

(defn decode [v]
  (cond (array? v) (mapv decode v) (object? v) (decode-tag v) :else v))
