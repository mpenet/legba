(ns s-exp.legba.overlay-test
  (:require [clojure.test :refer [deftest is testing]]
            [jsonista.core :as json]
            [s-exp.legba.overlay :as overlay]))

(def base-schema
  ;; Minimal OpenAPI schema as JSON string
  "{\"openapi\": \"3.1.0\",
    \"info\": {\"title\": \"Test API\", \"version\": \"1.0.0\"},
    \"paths\": {\"/item\": {\"get\": {\"x-private\": \"secret-data\"}}}}")

(def overlay-rm-x-private
  "{\"actions\": [
      {\"target\": \"$..['x-private']\", \"remove\": true}
    ]}")

(def overlay-update-title
  "{\"actions\": [
      {\"target\": \"$.info.title\", \"update\": \"Overlayed Title\"}
    ]}")

(deftest apply-overlay-remove-test
  (testing "removes x-private from schema"
    (let [out-json (overlay/apply base-schema overlay-rm-x-private)
          out-map (json/read-value out-json)]
      (is (nil? (get-in out-map ["paths" "/item" "get" "x-private"]))))))

(deftest apply-overlay-update-test
  (testing "updates info.title in schema"
    (let [out-json (overlay/apply base-schema overlay-update-title)
          out-map (json/read-value out-json)]
      (is (= "Overlayed Title" (get-in out-map ["info" "title"]))))))

(deftest apply-overlay-combined-test
  (testing "removes x-private and updates info.title"
    (let [overlay-combined
          "{\"actions\": [
                {\"target\": \"$.info.title\", \"update\": \"Overlayed Title\"},
                {\"target\": \"$..['x-private']\", \"remove\": true}
            ]}"
          out-json (overlay/apply base-schema overlay-combined)
          out-map (json/read-value out-json)]
      (is (= "Overlayed Title" (get-in out-map ["info" "title"])))
      (is (nil? (get-in out-map ["paths" "/item" "get" "x-private"]))))))
