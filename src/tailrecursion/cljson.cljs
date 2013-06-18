(ns tailrecursion.cljson
  (:require-macros [tailrecursion.cljson :refer [extends-protocol]])
  (:require [cljs.reader :as reader :refer [*tag-table* *default-data-reader-fn*]]
            [goog.date.DateTime :as date]
            [clojure.string :refer [split]]))

(declare encode decode encode! decode!)

;; PUBLIC ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol EncodeTagged
  "Encode a cljs thing o as a JS tagged literal of the form {tag: value}, where
  value is composed of JS objects that can be encoded as JSON."
  (-encode [x v]))

(defn clj->cljson
  "Convert clj data to JSON string."
  [x]
  (.stringify js/JSON (encode x)))

(defn cljson->clj
  "Convert JSON string to clj data."
  [x]
  (decode (.parse js/JSON x)))

;; INTERNAL ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def types {\ufdd1 :keyword
            \ufdd2 :symbol
            \ufdd3 :meta
            \ufdd4 :vector
            \ufdd5 :map
            \ufdd6 :list
            \ufdd7 :set
            \ufdd8 :tagged})

(def tags (into {} (map (fn [[k v]] [v k]) types)))

(defn encode-collection! [tag v items]
  (let [len (count items)]
    (loop [i 0, xs items, out (doto v (.push (tags tag)) (.push len))]
      (if (< i len)
        (recur (inc i) (rest xs) (encode! (first xs) out))
        out))))

(defn encode-scalar! [tag v value]
  (doto v (.push (tags tag)) (.push value)))

(defn encode-tagged! [tag v item]
  (encode! item (doto v (.push (:tagged tags)) (.push tag))))

(defn encode-native! [v value]
  (doto v (.push value)))

(extend-protocol EncodeTagged
  js/Date
  (-encode [x v] (encode-tagged! "inst" v (subs (pr-str x) 7 36)))
  cljs.core.UUID
  (-encode [x v] (encode-tagged! "uuid" v (.-uuid x))))

(defn interpret!
  "Attempts to encode an object that does not satisfy EncodeTagged,
  but for which the printed representation contains a tag."
  [printed v]
  (when-let [match (second (re-matches #"#([^<].*)" printed))]
    (let [tag (str (reader/read-string match)) 
          val (reader/read-string (subs match (.-length (str tag))))]
      (encode-tagged! tag v val))))

(defn encode! [x v]
  (if-let [m (and *print-meta* (meta x))]
    (->> (doto v (.push (:meta tags))) 
      (encode! m)
      (encode! (with-meta x nil))) 
    (cond (satisfies? EncodeTagged x) (-encode x v)
          (keyword? x) (encode-scalar! :keyword v (subs (str x) 1))
          (symbol? x) (encode-scalar! :symbol v (str x))
          (vector? x) (encode-collection! :vector v x)
          (seq? x) (encode-collection! :list v x)
          (and (map? x) (not (satisfies? cljs.core/IRecord x)))
          (encode-collection! :map v (apply concat x))
          (set? x) (encode-collection! :set v x)
          (or (string? x) (number? x) (nil? x)) (encode-native! v x) 
          :else (let [printed (pr-str x)]
                  (or (interpret! printed v)
                      (throw (js/Error.
                               (format "No cljson encoding for '%s'." printed))))))))

(defn decode-collection! [v u coll]
  (let [len (aget v (inc @u))]
    (swap! u + 2)
    (persistent!
      (loop [i 0, out (transient coll)]
        (if (< i len) (recur (inc i) (conj! out (decode! v u))) out)))))

(defn decode! [v u]
  (let [tag (aget v @u)]
    (case (and (string? tag) (types tag))
      :meta     (do (swap! u inc) (let [m (decode! v u)] (with-meta (decode! v u) m)))
      :keyword  (let [out (keyword (aget v (+ 1 @u)))] (swap! u + 2) out) 
      :symbol   (let [out (symbol (aget v (+ 1 @u)))] (swap! u + 2) out) 
      :vector   (decode-collection! v u [])
      :map      (let [len (aget v (+ 1 @u))]
                  (swap! u + 2)
                  (persistent! (loop [i 0, out (transient {})]
                                 (if (< i len)
                                   (let [mk (decode! v u), mv (decode! v u)]
                                     (recur (+ i 2) (assoc! out mk mv))) 
                                   out))))
      :list     (let [len (aget v (+ 1 @u))]
                  (swap! u + 2)
                  (reverse (loop [i 0, out ()]
                             (if (< i len)
                               (recur (inc i) (cons (decode! v u) out))
                               out))))
      :set      (decode-collection! v u #{})
      :tagged   (let [tag (aget v (+ 1 @u))]
                  (swap! u + 2)
                  (if-let [reader (or (get @*tag-table* tag) @*default-data-reader-fn*)]
                    (reader (decode! v u))
                    (throw (js/Error. (format "No reader function for tag '%s'." tag)))))
      (do (swap! u inc) tag))))

(defn encode [x] (encode! x (array)))
(defn decode [x] (decode! x (atom 0)))
