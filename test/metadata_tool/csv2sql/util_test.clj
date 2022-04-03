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

