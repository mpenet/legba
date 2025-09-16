(ns s-exp.legba.middleware
  (:require [exoscale.ex :as ex]
            [jsonista.core :as json]
            [s-exp.legba.request :as request]
            [s-exp.legba.response :as response]))

(defn wrap-validation
  "Takes a regular RING handler returns a handler that will apply openapi
  validation from the supplied `schema` for a given `method` and `path`"
  [handler schema method path opts]
  (let [sub-schema (get-in schema [:openapi-schema "paths" path (name method)])]
    (-> (fn [request]
          (let [request (request/validate
                         request
                         schema
                         sub-schema
                         opts)
                response (handler request)]
            (response/validate response
                               schema
                               sub-schema
                               opts)))
        (vary-meta assoc
                   :schema schema
                   :sub-schema sub-schema))))

(def ex->response nil)
(defmulti ex->response
  #(some-> % ex/ex-type)
  :hierarchy ex/hierarchy)

(defmethod ex->response
  :s-exp.legba/invalid
  [e]
  (let [data (ex-data e)]
    {:status 400
     :content-type "application/json"
     :body (json/write-value-as-string (assoc data :message (ex-message e)))}))

(defn wrap-error-response-fn
  [handler req]
  (ex/try+
    (handler req)
    #_{:clj-kondo/ignore [:unresolved-symbol]}
    (catch :s-exp.legba/invalid _
      #_{:clj-kondo/ignore [:unresolved-symbol]}
      (ex->response &ex))))

(defn wrap-error-response
  "Wraps handler with error checking middleware that will transform validation
  Exceptions to equivalent http response, as infered per `ex->response`"
  [handler]
  (fn [req]
    (wrap-error-response-fn handler req)))
