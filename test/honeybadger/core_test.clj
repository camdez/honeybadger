(ns honeybadger.core-test
  (:require [clojure.test :refer :all]
            [honeybadger.core :refer :all]))

(deftest ex-chain-test
  (let [e (->> (Exception. "C")
               (Exception. "B")
               (Exception. "A"))
        [a b c] (ex-chain e)]
    (is (= e a))
    (is (= (.getCause e) b))
    (is (= (.. e getCause getCause) c))))
