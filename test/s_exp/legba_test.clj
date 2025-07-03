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
         {:body
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
                      (fn [_request] {:body list-items-response})
                      [:get "/search"]
                      (fn [_request] {:body search-items-response})
                      [:post "/items"] (fn [_request] post-items-response)}
                     :schema "schema/oas/3.1/petstore.json"))

(deftest requests-test
  (let [h (make-handler {})]
    (is (= 404 (:status (h {:request-method :get :uri "/yolo"}))))

    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Invalid Path Parameter"
                          (h {:request-method :get :uri "/item/ab"})))

    (is (string? (:body (h {:request-method :get :uri (str "/item/" item-id)}))))

    (is (= 201
           (:status (h {:request-method :post
                        :headers {"content-type" "application/json"}
                        :uri "/items"
                        :body "{\"name\": \"asdf\", \"value\":1}"}))))

    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"No matching content-type in schema"
                          (h {:request-method :post
                              :headers {"content-type" "application/boom"}
                              :uri "/items"
                              :body "{\"name\": \"asdf\", \"value\":1}"})))

    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"No matching content-type in schema"
                          (h {:request-method :post
                              :headers {"content-type" "application/boom"}
                              :uri "/items"
                              :body "{\"name\": \"asdf\", \"value\":1}"})))

    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Missing Required Query Parameter"
                          (h {:request-method :get
                              :uri "/search"
                              :params {}})))

    (is (h {:request-method :get
            :uri "/search"
            :params {:term "yolo"}}))))
