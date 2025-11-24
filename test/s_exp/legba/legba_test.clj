(ns s-exp.legba.legba-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [jsonista.core :as jsonista]
            [s-exp.legba :as l]
            [s-exp.legba.json :as json]
            [s-exp.legba.json-pointer :as jp]
            [s-exp.legba.mime-type :as mime-type]
            [s-exp.legba.openapi-schema :as oas]))

(def item-id (str (random-uuid)))

(defn input-stream
  [s]
  (io/input-stream (.getBytes s)))

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
                 :value 1.0}
          :status 200}
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
          :status 201}}}
   & [opts]]
  (l/routing-handler {[:get "/item/{itemId}"]
                      (fn [_request] item-by-id-response)
                      [:post "/item/{itemId}"]
                      (fn [_request] post-items-response)
                      [:get "/items"]
                      (fn [_request] list-items-response)
                      [:get "/search"]
                      (fn [_request] search-items-response)
                      [:post "/items"] (fn [_request] post-items-response)}
                     schema-path
                     opts))

(deftest requests-test-yaml
  (let [h (make-handler {:schema-path "classpath://schema/oas/3.1/store.yaml"})]
    (is (= 404 (:status (h {:request-method :get :uri "/yolo"}))))
    (is (string? (:body (h {:request-method :get :uri (str "/item/" item-id)}))))))

(defn read-body-as-edn
  [response]
  (update response :body jsonista/read-value))

(deftest requests-test
  (let [h (make-handler {} {})]
    (is (= 404 (:status (h {:request-method :get :uri "/yolo"}))))
    (is (= {:status 400,
            :headers {"Content-Type" "application/problem+json"},
            :body
            {"errors"
             [{"detail" "does not match the uuid pattern must be a valid RFC 4122 UUID",
               "path" "$.format",
               "pointer" "#/paths/~1item~1{itemId}/get/parameters/0/schema/format",
               "location" "$"}],
             "title" "Invalid Path Parameters",
             "type" "#/http-problem-types/request-invalid-path-parameters"}}
           (read-body-as-edn (h {:request-method :get :uri "/item/ab"}))))

    (is (string? (:body (h {:request-method :get :uri (str "/item/" item-id)}))))

    (is (= 201
           (:status (h {:request-method :post
                        :headers {"content-type" "application/json"}
                        :uri "/items"
                        :body (input-stream "{\"name\": \"asdf\", \"value\":1}")}))))

    (is (= {:status 400,
            :headers {"Content-Type" "application/problem+json"},
            :body {"errors"
                   [{"detail" "No matching content-type",
                     "pointer" "/paths/~1items/post/requestBody"}],
                   "title" "Invalid content type for request",
                   "type" "#/http-problem-types/request-invalid-content-type"}}
           (read-body-as-edn
            (h {:request-method :post
                :headers {"content-type" "application/boom"}
                :uri "/items"
                :body (input-stream "{\"name\": \"asdf\", \"value\":1}")}))))

    (is (= {:status 400,
            :headers {"Content-Type" "application/problem+json"},
            :body {"errors"
                   [{"detail" "No matching content-type",
                     "pointer" "/paths/~1items/post/requestBody"}],
                   "title" "Invalid content type for request",
                   "type" "#/http-problem-types/request-invalid-content-type"}}
           (read-body-as-edn
            (h {:request-method :post
                :headers {"content-type" "application/boom"}
                :uri "/items"
                :body (input-stream "{\"name\": \"asdf\", \"value\":1}")}))))

    (is (= {:status 400,
            :headers {"Content-Type" "application/problem+json"},
            :body {"title" "Missing Required Query Parameter",
                   "type" "#/http-problem-types/request-missing-query-parameter"
                   "errors" [{"detail" "required query parameter missing",
                              "pointer" "/paths/~1search/get/parameters/0"}]}}
           (read-body-as-edn
            (h {:request-method :get
                :uri "/search"}))))
    (is (= 200 (:status (h {:request-method :get
                            :uri "/search"
                            :query-string "term=yolo"}))))))

(deftest path-params-test
  (let [h (make-handler {:schema-path "classpath://test_path_params.json"})]
    (is (= 200
           (:status (h {:request-method :get
                        :path-params {:itemId item-id}
                        :uri (str "/item/" item-id)}))))
    (is (= {:status 400,
            :headers {"Content-Type" "application/problem+json"},
            :body
            "{\"type\":\"#/http-problem-types/request-invalid-path-parameters\",\"errors\":[{\"path\":\"$.format\",\"pointer\":\"#/paths/~1item~1{itemId}/parameters/0/schema/format\",\"location\":\"$\",\"detail\":\"does not match the uuid pattern must be a valid RFC 4122 UUID\"}],\"title\":\"Invalid Path Parameters\"}"}
           (h {:request-method :get
               :path-params {:itemId "yolo"}
               :uri "/item/yolo"})
           (h {:request-method :post
               :path-params {:itemId "yolo"}
               :uri "/item/yolo"})))))

(deftest response-body-test
  (let [h (make-handler {:post-items-response {:body {:name "yolo"
                                                      :id item-id
                                                      :value ""}
                                               :status 201}}
                        {})
        {:as _r :keys [status body]}
        (read-body-as-edn
         (h {:request-method :post
             :headers {"content-type" "application/json"}
             :uri "/items"
             :body (input-stream "{\"name\": \"asdf\", \"value\":1.0}")}))]
    (is (= status 400))

    (is (= {"errors"
            [{"detail" "string found, number expected",
              "location" "$.value",
              "path" "$.properties.value.type",
              "pointer" "#/paths/~1items/post/responses/201/content/application~1json/schema/properties/value/type"}],
            "title" "Invalid Response Body",
            "type" "#/http-problem-types/response-invalid-body"}
           body))))

(deftest response-test
  (let [h (make-handler {:post-items-response {}}
                        {})
        {:as _r :keys [status body]}
        (read-body-as-edn
         (h {:request-method :post
             :headers {"content-type" "application/json"}
             :uri "/items"
             :body (input-stream "{\"name\": \"asdf\", \"value\":1}")}))]
    (is (= status 400))

    (is (= {"title" "Invalid response format for status",
            "type" "#/http-problem-types/response-invalid-format-for-status"}
           body)))

  (let [h (make-handler {:post-items-response {:headers {"content-type" "application/json"}
                                               :status 201}}
                        {})
        {:as _r :keys [status body]}
        (read-body-as-edn
         (h {:request-method :post
             :headers {"content-type" "application/json"}
             :uri "/items"
             :body (input-stream "{\"name\": \"asdf\", \"value\":1}")}))]
    (is (= status 400))
    (is (= {"errors"
            [{"detail" "null found, object expected",
              "path" "$.type",
              "pointer" "#/paths/~1items/post/responses/201/content/application~1json/schema/type"
              "location" "$"}],
            "title" "Invalid Response Body",
            "type" "#/http-problem-types/response-invalid-body"}
           body)))

  (let [h (make-handler {:post-items-response {:headers {"content-type" "application/xx"}
                                               :status 201}}
                        {})
        {:as _r :keys [status body]}
        (read-body-as-edn
         (h {:request-method :post
             :headers {"content-type" "application/json"}
             :uri "/items"
             :body (input-stream "{\"name\": \"asdf\", \"value\":1}")}))]
    (is (= 400 status))
    (is (= {"title" "Invalid response content-type",
            "type" "#/http-problem-types/response-invalid-content-type"}
           body)))

  (let [h (make-handler {:post-items-response {:headers {"content-type" "application/xx"}
                                               :status 201}}
                        {:include-error-schema true})
        {:as _r :keys [status body]}
        (read-body-as-edn
         (h {:request-method :post
             :headers {"content-type" "application/json"}
             :uri "/items"
             :body (input-stream "{\"name\": \"asdf\", \"value\":1}")}))]
    (is (= 400 status))
    (is (= {"schema"
            {"content"
             {"application/json"
              {"schema"
               {"properties"
                {"id" {"type" "string", "format" "uuid"},
                 "name" {"type" "string"},
                 "value" {"type" "number"}},
                "type" "object"}}},
             "description" "Item created successfully."},
            "title" "Invalid response content-type",
            "type" "#/http-problem-types/response-invalid-content-type"}
           body)) "test schema is included in output"))

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

(deftest broken-schema-load-test
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Schema invalid"
                        (oas/load-schema "classpath://test-broken.json"))))

(deftest path-properties-test
  (let [routes {[:get "/{id}"] (fn [_] {:status 200
                                        :body {:status "OK"}})
                [:put "/{id}"] (fn [_] {:status 200
                                        :body {:status "OK"}})
                [:post "/{id}"] (fn [_] {:status 201
                                         :body {:status "Created"}})
                [:patch "/{id}"] (fn [_] {:status 200
                                          :body {:status "OK"}})
                [:delete "/{id}"] (fn [_] {:status 204})
                [:head "/{id}"] (fn [_] {:status 200})
                [:options "/{id}"] (fn [_] {:status 204
                                            :headers {"Allow" ["GET" "PUT" "POST"
                                                               "PATCH" "DELETE" "HEAD"
                                                               "OPTIONS" "TRACE"]}})
                [:trace "/{id}"] (fn [_] {:status 405})}]
    (is (fn? (l/routing-handler routes "classpath://schema/oas/3.1/all-path-properties-and-methods.yaml")))))
