(ns s-exp.legba-test
  (:require [clojure.test :refer [deftest is]]
            [s-exp.legba :as l]
            [s-exp.legba.json :as json]
            [s-exp.legba.json-pointer :as jp]
            [s-exp.legba.mime-type :as mime-type]))

(def item-id (str (random-uuid)))

(defn make-handler
  [{:as _response-mocks
    :keys [item-by-id-response
           list-items-response
           post-items-response
           search-items-response
           schema-path]
    :or {schema-path "classpath://schema/oas/3.1/store.json"
         item-by-id-response
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
  (l/routing-handler {[:get "/item/{itemId}"]
                      (fn [_request] item-by-id-response)
                      [:get "/items"]
                      (fn [_request] list-items-response)
                      [:get "/search"]
                      (fn [_request] search-items-response)
                      [:post "/items"] (fn [_request] post-items-response)}
                     schema-path))

(deftest requests-test-yaml
  (let [h (make-handler {:schema-path "classpath://schema/oas/3.1/store.yaml"})]
    (is (= 404 (:status (h {:request-method :get :uri "/yolo"}))))
    (is (string? (:body (h {:request-method :get :uri (str "/item/" item-id)}))))))

(deftest requests-test
  (let [h (make-handler {})]
    (is (= 404 (:status (h {:request-method :get :uri "/yolo"}))))
    (is (= {:status 400,
            :content-type "application/json",
            :body
            {:type :s-exp.legba.request/invalid-path-parameters,
             :schema
             {:itemId
              {"schema" {"format" "uuid" "type" "string"},
               "name" "itemId",
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
                {"type" "object"
                 "properties"
                 {"name" {"description" "Name of the new item."
                          "type" "string"},
                  "value"
                  {"description" "Numerical value for the new item.",
                   "format" "float",
                   "type" "number"}},
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
                {"type" "object"
                 "properties"
                 {"name" {"description" "Name of the new item."
                          "type" "string"},
                  "value"
                  {"description" "Numerical value for the new item.",
                   "format" "float"
                   "type" "number"}},
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
             {"schema" {"type" "string"},
              "name" "term",
              "required" true,
              "description" "Search term",
              "in" "query"},
             :message "Missing Required Query Parameter"}}
           (h {:request-method :get
               :uri "/search"})))
    (is (= 200 (:status (h {:request-method :get
                            :uri "/search"
                            :query-string "term=yolo"}))))))

(deftest response-test
  (let [h (make-handler {:post-items-response {}})
        {:as _r :keys [status body]}
        (h {:request-method :post
            :headers {"content-type" "application/json"}
            :uri "/items"
            :body "{\"name\": \"asdf\", \"value\":1}"})]
    (is (= status 400))
    (is (= {:type :s-exp.legba.response/invalid-format-for-status,
            :schema
            {"responses"
             {"201"
              {"content"
               {"application/json"
                {"schema"
                 {"type" "object"
                  "properties" {"id" {"format" "uuid" "type" "string"},
                                "name" {"type" "string"},
                                "value" {"type" "number"}}}}},
               "description" "Item created successfully."},
              "400" {"description" "Invalid input."}},
             "summary" "Create a new item",
             "requestBody"
             {"content"
              {"application/json"
               {"schema"
                {"type" "object"
                 "properties"
                 {"name" {"description" "Name of the new item."
                          "type" "string"},
                  "value"
                  {"description" "Numerical value for the new item.",
                   "format" "float"
                   "type" "number"}},
                 "required" ["name" "value"]},
                "examples"
                {"newItem"
                 {"summary" "Example of a new item to create",
                  "value" {"name" "New Item C", "value" 15.75}}}}},
              "required" true},
             "description" "Adds a new item to the system."},
            :message "Invalid response format for status"}
           body)))

  (let [h (make-handler {:post-items-response {:headers {"content-type" "application/json"}
                                               :status 201}})
        {:as _r :keys [status body]}
        (h {:request-method :post
            :headers {"content-type" "application/json"}
            :uri "/items"
            :body "{\"name\": \"asdf\", \"value\":1}"})]
    (is (= status 400))
    (is (= {:message "Invalid Response Body",
            :schema
            {"type" "object"
             "properties"
             {"id" {"type" "string"
                    "format" "uuid"},
              "name" {"type" "string"},
              "value" {"type" "number"}}},
            :type :s-exp.legba.response/invalid-body,
            :errors [{:type "type",
                      :path "$",
                      :error "null found, object expected",
                      :message "$: null found, object expected"}]}
           body)))

  (let [h (make-handler {:post-items-response {:headers {"content-type" "application/xx"}
                                               :status 201}})
        {:as _r :keys [status body]}
        (h {:request-method :post
            :headers {"content-type" "application/json"}
            :uri "/items"
            :body "{\"name\": \"asdf\", \"value\":1}"})]
    (is (= 400 status))
    (is (= {:type :s-exp.legba.response/invalid-content-type,
            :schema
            {"content"
             {"application/json"
              {"schema"
               {"type" "object"
                "properties"
                {"id"
                 {"type" "string" "format" "uuid"},
                 "name" {"type" "string"},
                 "value" {"type" "number"}}}}},
             "description" "Item created successfully."},
            :message "Invalid response content-type"} body))))

(deftest error-output-test
  (is true))

(deftest json-pointer-test
  ;; Taken from https://datatracker.ietf.org/doc/html/rfc6901
  (let [doc {"foo" ["bar" "baz"]
             "" 0
             "a/b" 1
             "c%d" 2
             "e^f" 3
             "g|h" 4
             "i\\j" 5
             "k\"l" 6
             " " 7
             "m~n" 8}
        values
        {"" doc
         "/foo" ["bar", "baz"]
         "/foo/0" "bar"
         "/" 0
         "/a~1b" 1
         "/c%d" 2
         "/e^f" 3
         "/g|h" 4
         "/i\\j" 5
         "/k\"l" 6
         "/ " 7
         "/m~0n" 8}]
    (doseq [[q r] values]
      (is (= (jp/query doc q) r)))))

(deftest json-marshaling-test
  (let [d {:a 1 :b [true [nil 1.0]]}]
    (is (= d
           (json/json-node->clj
            (json/str->json-node "{\"a\":1,\"b\":[true,[null,1.0]]}"))))

    (is (= {"a" 1 "b" [true [nil 1.0]]}
           (json/json-node->clj
            (json/str->json-node "{\"a\":1,\"b\":[true,[null,1.0]]}")
            {:key-fn identity})))

    (is (= d (json/json-node->clj (json/clj->json-node {:a 1 :b [true [nil 1.0]]}))))))

(deftest json-content-type-test
  (is (json/json-content-type? "application/json"))
  (is (json/json-content-type? "charset=utf8;application/json"))
  (is (json/json-content-type? "application/json;charset=utf8;"))
  (is (not (json/json-content-type? "application/jsonp"))))

(deftest mime-type-matching-test
  (is (mime-type/match-mime-type? "*" "foo/bar"))
  (is (mime-type/match-mime-type? "*/*" "foo/bar"))
  (is (mime-type/match-mime-type? "*/bar" "foo/bar"))
  (is (mime-type/match-mime-type? "foo/*" "foo/bar"))
  (is (mime-type/match-mime-type? "foo/bar" "foo/bar"))
  (is (not (mime-type/match-mime-type? "*/bar" "foo/baz")))
  (is (not (mime-type/match-mime-type? "foo/*" "foox/bar")))
  (is (not (mime-type/match-mime-type? "foo/bar" "fo/br")))
  (is (not (mime-type/match-mime-type? "foo/bar" "fooo/barr"))))
