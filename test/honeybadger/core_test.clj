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

(deftest error-map-includes-causes
  (let [e (->> (Exception. "C")
               (Exception. "B")
               (Exception. "A"))
        {:keys [causes]} (sut/error-map e)]
    (is (= 2 (count causes)))
    (is (= "java.lang.Exception: B" (-> causes first  :message)))
    (is (= "java.lang.Exception: C" (-> causes second :message)))))
