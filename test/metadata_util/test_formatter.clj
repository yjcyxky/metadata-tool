(ns metadata-util.test-formatter
  (:require [clojure.test :refer :all]
            [metadata-util.formatter :as f]))

(deftest test-format-embeded-item
  (testing "Valid data"
    (is (true? (= (f/format-embeded-item {:data {:a 1}})
                  {:data "{\"a\":1}"}))))

  (testing "Valid config"
    (is (true? (= (f/format-embeded-item {:config {:a 1}})
                  {:config "{\"a\":1}"}))))

  (testing "Valid embeded data"
    (is (true? (= (#'f/format-embeded-data f/format-embeded-item {:data {:a 1} :config {:b 1}})
                  {:data "{\"a\":1}" :config "{\"b\":1}"}))))

  (testing "Valid js code"
    (let [formatted (#'f/format-embeded-data f/format-embeded-item {:data {:a 1} :config {:b 1}
                                                                    :js_code (f/cache-resource-file! "test/test_js_code.js")})]
      ;; (println "Formatted data: ", formatted)
      (is (true? (= (select-keys formatted [:data :config]) {:data "{\"a\":1}" :config "{\"b\":1}"})))
      (is (true? (> (count (:js_code formatted)) 0)))))

  (testing "Valid icon"
    (let [formatted (f/format-embeded-item {:icon (f/cache-resource-file! "test/multireport-logo.png")})]
      ;; (println "Formatted data: ", formatted)
      (is (true? (> (count (:icon formatted)) 0))))))

(deftest test-base64-encode
  (testing "Convert icon to base64 string"
    (is (true? (some? (#'f/base64-encode (f/read-to-byte-array (f/cache-resource-file! "test/multireport-logo.png"))))))))
