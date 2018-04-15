(ns diff-checker.core-test
  (:require [clojure.test :refer :all]
            [clojure.spec.test.alpha :as st]
            [diff-checker.core :as dc]))

(st/instrument)

(deftest create-task-test
  (testing "S"
    (is (= (dc/create-task "test-task" {:url "http://test-url"})
           {:name "test-task"
            :target {:url "http://test-url"}})))
  (testing "F"
    (is (thrown? java.lang.Exception
                 (dc/create-task 0 0)))))
