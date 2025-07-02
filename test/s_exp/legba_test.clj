(ns s-exp.legba-test
  (:require [clojure.test :refer [deftest testing is]]
            [s-exp.legba :as l]))

(def h (l/openapi-handler {[:get "/pet/{petId}"]
                           (fn [_request]
                             {:body {:id 1
                                     :name "foo"
                                     :photoUrls []}})
                           [:get "/pets"]
                           (fn [_request]
                             {:body [{:id "asd"}]
                              ;; :headers {"x-next" "asdf"}
                              })
                           [:post "/pet"]
                           (fn [request]
                             {:body {:name "yolo", :photoUrls []}
                              :status 200})}
                          :schema "schema/oas/3.1/petstore.json"))

;; (h {:request-method :get :uri "/pet/1"})
;; (h {:request-method :get :uri "/pet/asdf"})

(deftest a-test
  (is (= 404 (:status (h {:request-method :get :uri "/yolo"}))))

  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Invalid Path Parameters"
                        (h {:request-method :get :uri "/pet/a"})))

  (is (string? (:body (h {:request-method :get :uri "/pet/2"}))))

  (is (= 200
         (:status (h {:request-method :post
                      :headers {"content-type" "application/json"}
                      :uri "/pet"
                      :body "{\"name\": \"asdf\", \"id\":1, \"photoUrls\": []}"}))))

  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"No matching content-type in schema"
                        (h {:request-method :post
                            :uri "/pet"
                            :body "{\"name\": \"asdf\", \"id\":1, \"photoUrls\": []}"})))

  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"No matching content-type in schema"
                        (h {:request-method :post
                            :headers {"content-type" "boom"}
                            :uri "/pet"
                            :body "{\"name\": \"asdf\", \"id\":1, \"photoUrls\": []}"})))

;; (is (thrown-with-msg? clojure.lang.ExceptionInfo
  ;;                       (h {:request-method :post
  ;;                           :headers {"content-type" "application/json"}
  ;;                           :uri "/pet"
  ;;            ;; :body "{\"name\": \"asdf\", \"id\":1, \"photoUrls\": []}"
  ;;                           })))

  ;; (is (thrown-with-msg? clojure.lang.ExceptionInfo
  ;;                       #"Invalid Response Body"
  ;;                       (h {:request-method :get
  ;;                           :uri "/pet/2"})))
  )
;; (do (h {:request-method :post
;;         :headers {"content-type" "application/json"}
;;         :uri "/pet"
;;         :body "{\"id\": 1, \"name\":\"foo\", \"photoUrls\": []}"}))
