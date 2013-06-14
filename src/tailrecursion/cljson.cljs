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

(def get-tag    #(js* "(function(o){for(var k in o) return k;})(~{})" %))
(def array?     #(js* "(~{} instanceof Array)" %))
(def object?    #(js* "(~{} instanceof Object)" %))
(def str?       #(= "string" (js* "(typeof ~{})" %)))
(def enc-coll   #(doto (js-obj) (aset %1 (into-array (map encode %2)))))
(def inst-str   #(date/fromRfc822String (str %)))
(def decode-str #(let [s (subs % 2)]
                   (case (first %) \ufdd0 (keyword s) \ufdd1 (symbol s) %)))

(extends-protocol Encode
  cljs.core.Vector
  cljs.core.PersistentVector
  (encode [o] (into-array (map encode o)))
  cljs.core.PersistentArrayMap
  cljs.core.PersistentHashMap
  (encode [o] (enc-coll "m" o))
  cljs.core.IndexedSeq
  cljs.core.RSeq
  cljs.core.List
  cljs.core.EmptyList
  cljs.core.Cons
  cljs.core.LazySeq
  cljs.core.ChunkedCons
  cljs.core.ChunkedSeq
  cljs.core.Range
  (encode [o] (enc-coll "l" o))
  cljs.core.PersistentHashSet
  (encode [o] (enc-coll "s" o))
  js/Date
  (encode [o] (doto (js-obj) (.toUTCIsoString (inst-str o))))
  cljs.core/UUID
  (encode [o] (doto (js-obj) (aset "uuid" (.-uuid o))))
  js/String, js/Boolean, js/Number, nil
  (encode [o] o))

(defmethod decode-tag "m" [o] (into {} (map decode (aget o "m"))))
(defmethod decode-tag "l" [o] (apply list (map decode (aget o "l"))))
(defmethod decode-tag "s" [o] (set (map decode (aget o "s"))))

(defmethod decode-tag :default [o]
  (let [[tag val] [(get-tag o) (aget o tag)] 
        reader    (or (get @tag-table tag) @*default-data-reader-fn*)
        read-ex   #(js/Error. (format "No reader function for tag '%s'." %))]
    (if reader (reader (decode val)) (throw (read-ex tag)))))

(defn decode [v]
  (cond (str?    v) (decode-str v)
        (array?  v) (mapv decode v)
        (object? v) (decode-tag v)
        :else       v))
