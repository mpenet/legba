(ns s-exp.legba
  (:require [exoscale.ex :as ex]
            [s-exp.legba.request :as request]
            [s-exp.legba.response :as response]
            [s-exp.legba.router :as router]
            [s-exp.legba.schema :as schema]))

(defn handler-for-request
  [handlers {:keys [method path] :as _match} _opts]
  (let [req-key [method path]]
    (or (get handlers req-key)
        (throw (ex-info (format "Handler not defined for request %s"
                                req-key)
                        {:type ::handler-undefined})))))

(def default-options
  {:not-found-response {:status 404 :body "Not found"}
   :key-fn keyword
   :query-string-params-key [:params]})

(defn openapi-handler
  [handlers & {:as opts}]
  (let [{:as opts :keys [schema not-found-response]}
        (merge default-options opts)
        schema (schema/load-schema schema)
        router (router/router schema opts)]
    (fn [{:as request :keys [request-method uri]}]
      (if-let [{:as match :keys [sub-schema path-params]}
               (router/match-route router request-method uri)]
        (let [request (request/conform-request
                       (cond-> request
                         path-params
                         (assoc :path-params path-params))
                       schema
                       sub-schema
                       opts)
              _ (prn :match match)
              handler (handler-for-request handlers match opts)
              response (handler request)
              response (response/conform-response response
                                                  schema
                                                  sub-schema
                                                  opts)]
          (vary-meta response dissoc :match :schema))
        not-found-response))))

(ex/derive ::handler-undefined :exoscale.ex/fault)

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ;;;; Playground                                                             ;;;;
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; ;; "https://raw.githubusercontent.com/OAI/OpenAPI-Specification/refs/heads/main/examples/v3.0/petstore.json"

;; (def h (openapi-handler {[:get "/pet/{petId}"]
;;                          (fn [_request]
;;                            {:body {:id 1
;;                                    :name "foo"}})
;;                          [:get "/pets"]
;;                          (fn [_request]
;;                            {:body [{:id "asd"}]
;;                       ;; :headers {"x-next" "asdf"}
;;                             })
;;                          [:put "/pet"]
;;                          (fn [_request]
;;                            {:body {:name "yolo-put", :photoUrls []}
;;                             :status 200})
;;                          [:post "/pet"]
;;                          (fn [request]
;;                            {:body {:name "yolo-post", :photoUrls []}
;;                             :status 200})}
;;                         :schema "schema/oas/3.1/petstore.json"))

;; (do
;;   (prn :_---------------------)
;; (h
;;  {:request-method :put
;;   :headers {"content-type" "application/json"}
;;   :uri "/pet"
;;   :body nil})
  ;; {:request-method :get :uri "/pet/2"}
   ;; ))
