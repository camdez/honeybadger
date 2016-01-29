(ns honeybadger.utils-test
  (:require [honeybadger.utils :refer :all]
            [clojure.test :refer :all]))

(deftest underscore-handles-keywords
  (are [in out] (= out (underscore in))
    :foo     "foo"
    :bar-baz "bar_baz"))

(deftest underscore-handles-strings
  (are [in out] (= out (underscore in))
    "foo"     "foo"
    "bar-baz" "bar_baz"))

(deftest underscore-handles-numbers
  (is (= "2" (underscore 2))))
