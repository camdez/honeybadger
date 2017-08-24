(ns honeybadger.core-test
  (:require [clojure.test :refer :all]
            [honeybadger.core :as sut]))

(deftest ex-chain-builds-sequence-of-exception-causes
  (let [e (->> (Exception. "C")
               (Exception. "B")
               (Exception. "A"))
        [a b c] (sut/ex-chain e)]
    (is (= a e))
    (is (= b (.getCause e)))
    (is (= c (.. e getCause getCause)))))
