(ns metadata-tool.csv2sql.util-test
  (:require [clojure.test :refer :all]
            [metadata-tool.csv2sql.util :as util]))

(deftest test-alphanumeric?
  (testing "Valid alphanumeric"
    (is (true? (util/alphanumeric? "abc012")))
    (is (true? (util/alphanumeric? "abc_012"))))

  (testing "Invalid alphanumeric"
    (is (false? (util/alphanumeric? "ABC_012" true)))
    (is (false? (util/alphanumeric? "abc-123")))))

(deftest test-spaces-to-underscores
  (testing "Valid"
    (is (true? (= (util/spaces-to-underscores "abc efg") "abc_efg")))
    (is (true? (= (util/spaces-to-underscores "  abc-012") "__abc-012")))))

(deftest test-periods-to-underscores
  (testing "Valid"
    (is (true? (= (util/periods-to-underscores "abc.efg") "abc_efg")))
    (is (true? (= (util/periods-to-underscores "..abc-012") "__abc-012")))))

(deftest test-camel-to-snake
  (testing "Valid"
    (is (true? (= (util/camel-to-snake "abc.efg") "abc_efg")))
    (is (true? (= (util/camel-to-snake "..abc-012") "__abc_012")))
    (is (true? (= (util/camel-to-snake "..ABC-012") "__abc_012")))))
