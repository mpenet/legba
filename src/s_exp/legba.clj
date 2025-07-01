(ns s-exp.legba
  (:require [charred.api :as charred]
            [exoscale.ex :as ex]
            [s-exp.legba.request :as request]
            [s-exp.legba.response :as response]
            [s-exp.legba.router :as router]
            [s-exp.legba.schema :as schema]))

(defn handler-for-request
  [handlers {:keys [method path] :as _match}]
  (let [req-key [method path]]
    (or (get handlers req-key)
        (throw (ex-info (format "Handler not defined for request %s"
                                req-key)
                        {:type ::handler-undefined})))))

(defn openapi-handler
  [handlers & {:as _opts
               :keys [schema not-found-response]
               :or {not-found-response {:status 404 :body "Not found"}}}]
  (let [schema (schema/load-schema schema)
        router (router/router schema)]
    (fn [{:as request :keys [request-method uri]}]
      (if-let [{:as match :keys [sub-schema path-params]} (router/match-route router request-method uri)]
        (let [request (request/conform-request (cond-> request
                                                 path-params
                                                 (assoc :path-params path-params))
                                               schema sub-schema)
              handler (handler-for-request handlers match)
              response (handler request)
              response (response/conform-response response schema sub-schema)]
          (vary-meta response dissoc :match :schema))
        not-found-response))))

(ex/derive ::handler-undefined :exoscale.ex/fault)

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ;;;; Playground                                                             ;;;;
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; ;; "https://raw.githubusercontent.com/OAI/OpenAPI-Specification/refs/heads/main/examples/v3.0/petstore.json"

(comment
  (def h (openapi-handler {[:get "/pet/{petId}"]
                           (fn [_request]
                             {:body (charred/write-json-str
                                     {:id 1
                                      :name "foo"})})
                           [:get "/pets"]
                           (fn [_request]
                             {:body (charred/write-json-str [{:id "asd"}])
                             ;; :headers {"x-next" "asdf"}
                              })
                           [:post "/pet"]
                           (fn [_request]
                             {:body (charred/write-json-str {:name "yolo", :photoUrls []})
                              :status 200})}
                          :schema "schema/oas/3.1/petstore.json"))

  (h {:request-method :get :uri "/pet/2"}))

;; {:request-method :post
;;  :headers {:content-type "application/json"}
;;  :uri "/pet"
;;  :body "{\"name\": \"asdf\", \"id\":1, \"photoUrls\": []}"}
