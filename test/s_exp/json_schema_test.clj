(ns s-exp.json-schema-test
  (:require [clojure.test :refer [deftest is testing]]
            [s-exp.legba.json-schema :as js]))

(def schema-path "classpath:test.json")

(deftest schema-validation-test
  (let [schema (js/schema schema-path)]
    (testing "Valid input passes validation"
      (let [valid1 "{\"fruits\":[\"apple\",\"banana\"],\"vegetables\":[{\"veggieName\":\"potato\",\"veggieLike\":true}]}"
            valid2 "{\"fruits\":[],\"vegetables\":[]}"]
        (is (nil? (js/validate schema valid1)))
        (is (nil? (js/validate schema valid2)))))
    (testing "Missing required veggie fields triggers error"
      (let [invalid "{\"fruits\":[],\"vegetables\":[{\"veggieLike\":true}]}"
            errors (js/validate schema invalid)]
        (is (coll? errors))
        (is (seq errors))
        (is (some #(= (:type %) "required") errors))))
    (testing "Wrong type errors for arrays or objects"
      (let [invalid "{\"fruits\": [1,2,3], \"vegetables\": []}"
            errors (js/validate schema invalid)]
        (is (coll? errors))
        (is (seq errors))
        (is (some #(= (:type %) "type") errors))))))

(deftest validation-result-test
  (let [schema (js/schema schema-path)]
    (testing "Extracts error info"
      (let [invalid "{\"vegetables\":[{\"veggieLike\":false}]}"
            errors (js/validate schema invalid)]
        (is (every? #(contains? % :type) errors))
        (is (every? #(contains? % :path) errors))
        (is (every? #(contains? % :message) errors))))))
