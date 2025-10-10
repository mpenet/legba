(ns s-exp.legba.json-schema-test
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
      (let [invalid "{\"fruits\":[],\"vegetables\":[{\"veggieLike\":true}]}"]
        (is (= (js/validate schema invalid)
               [{:detail "required property 'veggieName' not found",
                 :path "$.properties.vegetables.items['$ref'].required",
                 :location "$.vegetables[0]"
                 :pointer "#/$defs/veggie/required"}]))))
    (testing "Wrong type errors for arrays or objects"
      (let [invalid "{\"fruits\": [1,2,3], \"vegetables\": []}"]
        (is (= (js/validate schema invalid)
               [{:detail "integer found, string expected",
                 :path "$.properties.fruits.items.type",
                 :location "$.fruits[0]"
                 :pointer "#/properties/fruits/items/type"}
                {:detail "integer found, string expected",
                 :path "$.properties.fruits.items.type",
                 :location "$.fruits[1]"
                 :pointer "#/properties/fruits/items/type"}
                {:detail "integer found, string expected",
                 :location "$.fruits[2]"
                 :path "$.properties.fruits.items.type",
                 :pointer "#/properties/fruits/items/type"}]))))))

(deftest validation-result-test
  (let [schema (js/schema schema-path)]
    (testing "Extracts error info"
      (let [invalid "{\"vegetables\":[{\"veggieLike\":false}]}"]
        (is (= (js/validate schema invalid)
               [{:detail "required property 'veggieName' not found",
                 :location "$.vegetables[0]"
                 :path "$.properties.vegetables.items['$ref'].required",
                 :pointer "#/$defs/veggie/required"}]))))))
