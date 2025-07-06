(ns s-exp.legba.handler
  (:require [clojure.string :as str]
            [exoscale.ex :as ex]
            [s-exp.legba.request :as request]
            [s-exp.legba.response :as response]))

(defn make-handler
  "Takes a regular RING handler returns a handler that will apply openapi
  validation from the supplied `schema` for a given `method` and `path`"
  [handler schema method path opts]
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

(defn ensure-handler-coverage!
  "Checks that a map of openapi-handlers covers all paths defined by the schema"
  [openapi-handlers {:as _schema :keys [openapi-schema]}]
  (let [missing-handlers
        (for [[path methods] (get openapi-schema "paths")
              [method & _] methods
              :let [method (keyword method)
                    matching-entry (get openapi-handlers [(keyword method) path])]
              :when (not matching-entry)]
          [path method])]
    (when (seq missing-handlers)
      (ex/ex-incorrect! (format "Missing handlers for %s"
                                (str/join ", " missing-handlers))))))
