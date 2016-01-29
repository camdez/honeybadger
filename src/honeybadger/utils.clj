(ns honeybadger.utils
  (:require [clojure.string :as str]))

(defn contains-in? [m ks]
  (not= (get-in m ks ::not-found) ::not-found))

(defn deep-merge
  "Recursively merge maps. At each level, if there are any non-map
  vals, the last value (of any type) is used."
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(defn settify [s]
  (if (coll? s)
    (set s)
    #{s}))

(defn some-chain
  "Successively applies functions `fs` from left to right (as
  with `(apply comp (reverse fs))`), halting the chain and returning
  nil if any function returns nil."
  [x fs]
  (if (seq fs)
    (when-let [x' ((first fs) x)]
      (recur x' (rest fs)))
    x))

(defn underscore [key]
  (-> (if (instance? clojure.lang.Named key)
        (name key)
        (str key))
      (str/replace "-" "_")))

(defn update-contained-in
  "Like `update-in` but `f` is only applied if map entry
  exists. Intermediate keys are never created."
  [m ks f]
  (if (contains-in? m ks)
    (update-in m ks f)
    m))
