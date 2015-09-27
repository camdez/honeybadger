(ns honeybadger.schemas
  (:require [schema.core :as s]))

(def Event
  "A standard format for uniform processing by filters."
  {:api-key   s/Str
   :env       (s/maybe s/Keyword)
   :exception (s/either s/Str Throwable)
   :metadata  {:tags      #{s/Keyword}
               :request   {(s/optional-key :method)  s/Keyword
                           (s/optional-key :url)     s/Str
                           (s/optional-key :params)  {s/Keyword s/Any}
                           (s/optional-key :session) {s/Keyword s/Any}}
               :context   {s/Keyword s/Any}
               :component (s/maybe s/Str)
               :action    (s/maybe s/Str)}})

(def EventFilter (s/=> (s/maybe Event) [Event]))
