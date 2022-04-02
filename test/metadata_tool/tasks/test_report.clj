(ns metadata-tool.tasks.test-report
  (:require [clojure.test :refer :all]
            [metadata-tool.tasks.report :as report]))

(deftest test-format-embeded-item
  (testing "Valid data"
    (is (true? (= (report/format-embeded-item {:data {:a 1}})
                  {:data "{\"a\":1}"}))))

  (testing "Valid config"
    (is (true? (= (report/format-embeded-item {:config {:a 1}})
                  {:config "{\"a\":1}"}))))

  (testing "Valid embeded data"
    (is (true? (= (#'report/format-embeded-data report/format-embeded-item {:data {:a 1} :config {:b 1}})
                  {:data "{\"a\":1}" :config "{\"b\":1}"}))))

  (testing "Valid js code"
    (let [formatted (#'report/format-embeded-data report/format-embeded-item {:data {:a 1} :config {:b 1}
                                                                              :js_code (report/cache-resource-file! "test/test_js_code.js")})]
      ;; (println "Formatted data: ", formatted)
      (is (true? (= (select-keys formatted [:data :config]) {:data "{\"a\":1}" :config "{\"b\":1}"})))
      (is (true? (> (count (:js_code formatted)) 0)))))

  (testing "Valid icon"
    (let [formatted (report/format-embeded-item {:icon (report/cache-resource-file! "test/multireport-logo.png")})]
      ;; (println "Formatted data: ", formatted)
      (is (true? (> (count (:icon formatted)) 0))))))

(deftest test-base64-encode
  (testing "Convert icon to base64 string"
    (is (true? (some? (#'report/base64-encode (report/read-to-byte-array (report/cache-resource-file! "test/multireport-logo.png"))))))))
