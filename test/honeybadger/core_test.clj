(ns honeybadger.core-test
  (:require [clojure.test :refer :all]
            [honeybadger.core :as hb]))

(defn- format-metadata [metadata]
  (-> (#'hb/event->notice
       (#'hb/normalize-event "123"
                             "development"
                             "test"
                             metadata))
      :request
      :cgi-data))

(deftest cgi-data
  (are [out in] (= out (format-metadata in))
    {}                                   {}
    {:foo "bar"}                         {:cgi-data {"foo" "bar"}}
    {"REQUEST_METHOD" "GET"}             {:request {:method :get}}
    {"REQUEST_METHOD" "GET", :foo "bar"} {:request {:method :get} :cgi-data {"foo" "bar"}}))
