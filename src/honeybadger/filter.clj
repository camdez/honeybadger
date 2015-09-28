(ns honeybadger.filter
  (:refer-clojure :exclude [instance?])
  (:require [honeybadger.schemas :refer [Event EventFilter EventPredicate]]
            [honeybadger.utils :refer [settify update-contained-in]]
            [schema.core :as s]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Predicates

(s/defn env? :- EventPredicate
  [envs :- (s/either s/Keyword #{s/Keyword})]
  (fn [{:keys [env]}]
    ((settify (keyword envs)) env)))

(s/defn instance? :- EventPredicate
  [c :- Class]
  (fn [{:keys [exception]}]
    (clojure.core/instance? c exception)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Combinators

(s/defn only :- EventFilter
  [pred :- EventPredicate]
  (fn [e]
    (when (pred e)
      e)))

(s/defn except :- EventFilter
  [pred :- EventPredicate]
  (only (complement pred)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Transformers

(s/defn debug :- Event
  [x :- Event]
  (println x)
  x)

(s/defn obscure-params :- EventFilter
  ([pks]
   (obscure-params pks (constantly "[FILTERED]")))
  ([pks :- [[s/Keyword]]
    f   :- (s/=> s/Any s/Any)]
   (fn [e]
     (reduce #(update-contained-in %1 (concat [:metadata :request :params] %2) f)
             e pks))))
