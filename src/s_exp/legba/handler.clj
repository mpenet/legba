(ns s-exp.legba.handler
  (:require [s-exp.legba.request :as request]
            [s-exp.legba.response :as response]))

(defn make-handler
  "Takes a regular RING handler returns a handler that will apply openapi
  validation from the supplied `schema` for a set of `coords` (method path)"
  [handler schema [method path] opts]
  (let [sub-schema (get-in schema [:openapi-schema "paths" path (name method)])]
    (-> (fn [{:as request :keys [path-params]}]
          (let [request (request/conform-request
                         (cond-> request
                           path-params
                           (assoc :path-params path-params))
                         schema
                         sub-schema
                         opts)
                response (handler request)]
            (response/conform-response response
                                       schema
                                       sub-schema
                                       opts)))
        (vary-meta assoc
                   :schema schema
                   :sub-schema sub-schema))))
