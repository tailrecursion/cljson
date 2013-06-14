(ns tailrecursion.cljson
  (:require-macros [tailrecursion.cljson :refer [extends-protocol]]))

(defprotocol Collection
  (tag [o]))

(defprotocol Encode
  (encode [o]))

(extends-protocol Collection
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
  (encode [o] (doto (js-obj)
                (aset (tag o) (into-array (map encode o)))))
  js/String, js/Boolean, js/Number, nil
  (encode [o] o)))

(defn clj->cljson
  [v]
  (.stringify js/JSON (encode v)))

(declare decode)

(defmulti decode-coll
  #(js* "(function(o){for(var k in o) return k;})(~{})" %))

(defmethod decode-coll "m" [o]
  (into {} (map decode (aget o "m"))))

(defmethod decode-coll "l" [o]
  (apply list (map decode (aget o "l"))))

(defmethod decode-coll "s" [o]
  (set (map decode (aget o "s"))))

(defn array? [o]
  (js* "(~{} instanceof Array)" o))

(defn object? [o]
  (js* "(~{} instanceof Object)" o))

(defn decode
  [v]
  (cond (array? v)  (mapv decode v)
        (object? v) (decode-coll v)
        :else v))

(defn cljson->clj
  [x]
  (decode (.parse js/JSON x)))
