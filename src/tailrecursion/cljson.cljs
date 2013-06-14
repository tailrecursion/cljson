(ns tailrecursion.cljson
  (:require-macros [tailrecursion.cljson :refer [extends-protocol]])
  (:require [cljs.reader :as reader]
            [goog.date.DateTime :as date]))

(defprotocol Tagged
  (tag [o]))

(defprotocol Encode
  (encode [o]))

(extends-protocol Tagged
  cljs.core.PersistentArrayMap
  cljs.core.PersistentHashMap
  (tag [_] "m")
  cljs.core.ISeq
  (tag [_] "l")
  cljs.core.PersistentHashSet
  (tag [_] "s"))

(extends-protocol Encode
  cljs.core.Vector
  (encode [o] (into-array (map encode o))
  cljs.core.PersistentArrayMap
  cljs.core.PersistentHashMap
  cljs.core.ISeq
  cljs.core.PersistentHashSet
  (encode [o] (doto (js-obj) (aset (tag o) (into-array (map encode o)))))
  js/Date
  (encode [o] (doto (js-obj)
                (aset "inst" (.toUTCIsoString (date/fromRfc822String (str o))))))
  cljs.core/UUID
  (encode [o] (doto (js-obj) (aset "uuid" (.-uuid o))))
  js/String, js/Boolean, js/Number, nil
  (encode [o] o)))

(defn clj->cljson
  [v]
  (.stringify js/JSON (encode v)))

(declare decode)

(defn get-tag [o]
  (js* "(function(o){for(var k in o) return k;})(~{})" %))

(defmulti decode-tag get-tag)

(defmethod decode-tag "m" [o]
  (into {} (map decode (aget o "m"))))

(defmethod decode-tag "l" [o]
  (apply list (map decode (aget o "l"))))

(defmethod decode-tag "s" [o]
  (set (map decode (aget o "s"))))

(defmethod decode-tag :default [o]
  (let [tag (get-tag o)
        val (decode (aget o tag))]
    (if-let [reader (or (get @reader/tag-table tag)
                        @reader/*default-data-reader-fn*)]
      (reader val)
      (throw (js/Error. (str "No reader function for tag " tag))))))

(defn array? [o]
  (js* "(~{} instanceof Array)" o))

(defn object? [o]
  (js* "(~{} instanceof Object)" o))

(defn decode
  [v]
  (cond (array? v)  (mapv decode v)
        (object? v) (decode-tag v)
        :else v))

(defn cljson->clj
  [x]
  (decode (.parse js/JSON x)))
