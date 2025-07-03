(ns s-exp.legba-test
  (:require [clojure.test :refer [deftest testing is]]
            [s-exp.legba :as l]))

(def item-id (str (random-uuid)))

(defn make-handler
  [{:as _response-mocks
    :keys [item-by-id-response
           list-items-response
           post-items-response
           search-items-response]
    :or {item-by-id-response
         {:body {:id item-id
                 :name "foo"
                 :value 1.0}}
         list-items-response
         {:body
          [{:id item-id
            :name "foo"
            :value 1.0}]}
         search-items-response
         {:status 200
          :body
          [{:id item-id
            :name "foo"
            :value 1.0}]}
         post-items-response
         {:body {:name "yolo"
                 :id item-id
                 :value 1.0}
          :status 201}}}]
  (l/openapi-handler {[:get "/item/{itemId}"]
                      (fn [_request] item-by-id-response)
                      [:get "/items"]
                      (fn [_request] list-items-response)
                      [:get "/search"]
                      (fn [_request] search-items-response)
                      [:post "/items"] (fn [_request] post-items-response)}
                     :schema "schema/oas/3.1/petstore.json"))

;; ((make-handler {}) {:request-method :get :uri "/item/ab"})

(deftest requests-test
  (let [h (make-handler {})]
    (is (= 404 (:status (h {:request-method :get :uri "/yolo"}))))

    (is (= {:status 400,
            :content-type "application/json",
            :body
            {:type :s-exp.legba.request/invalid-path-parameters,
             :schema
             {:itemId
              {"schema" {"format" "uuid"},
               "name" "itemId",
               "style" "simple",
               "explode" false,
               "required" true,
               "description" "ID of the item to retrieve.",
               "in" "path"}},
             :errors
             [{:type "format",
               :path "$",
               :error
               "does not match the uuid pattern must be a valid RFC 4122 UUID",
               :message
               "$: does not match the uuid pattern must be a valid RFC 4122 UUID"}]
             :message "Invalid Path Parameters"}}
           (h {:request-method :get :uri "/item/ab"})))

    (is (string? (:body (h {:request-method :get :uri (str "/item/" item-id)}))))

    (is (= 201
           (:status (h {:request-method :post
                        :headers {"content-type" "application/json"}
                        :uri "/items"
                        :body "{\"name\": \"asdf\", \"value\":1}"}))))

    (is (= {:status 400,
            :content-type "application/json",
            :body
            {:type :s-exp.legba.request/invalid-content-type,
             :schema
             {"content"
              {"application/json"
               {"schema"
                {"properties"
                 {"name" {"description" "Name of the new item."},
                  "value"
                  {"description" "Numerical value for the new item.",
                   "format" "float"}},
                 "required" ["name" "value"]},
                "examples"
                {"newItem"
                 {"summary" "Example of a new item to create",
                  "value" {"name" "New Item C", "value" 15.75}}}}},
              "required" true},
             :message "No matching content-type in schema for request"}}
           (h {:request-method :post
               :headers {"content-type" "application/boom"}
               :uri "/items"
               :body "{\"name\": \"asdf\", \"value\":1}"})))

    (is (= {:status 400,
            :content-type "application/json",
            :body
            {:type :s-exp.legba.request/invalid-content-type,
             :schema
             {"content"
              {"application/json"
               {"schema"
                {"properties"
                 {"name" {"description" "Name of the new item."},
                  "value"
                  {"description" "Numerical value for the new item.",
                   "format" "float"}},
                 "required" ["name" "value"]},
                "examples"
                {"newItem"
                 {"summary" "Example of a new item to create",
                  "value" {"name" "New Item C", "value" 15.75}}}}},
              "required" true},
             :message "No matching content-type in schema for request"}}
           (h {:request-method :post
               :headers {"content-type" "application/boom"}
               :uri "/items"
               :body "{\"name\": \"asdf\", \"value\":1}"})))

    (is (= {:status 400,
            :content-type "application/json",
            :body
            {:type :s-exp.legba.request/missing-query-parameter,
             :schema
             {"schema" {},
              "name" "term",
              "style" "form",
              "explode" true,
              "required" true,
              "description" "Search term",
              "in" "query"},
             :message "Missing Required Query Parameter"}}
           (h {:request-method :get
               :uri "/search"
               :params {}})))

    (is (= 200 (:status (h {:request-method :get
                            :uri "/search"
                            :params {:term "yolo"}}))))))
