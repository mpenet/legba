(ns s-exp.legba.middleware
  (:require [clojure.string :as str]
            [exoscale.ex :as ex]
            [jsonista.core :as json]
            [s-exp.legba.request :as request]
            [s-exp.legba.response :as response]))

(defn wrap-validation
  "Middleware that wraps a standard RING handler with OpenAPI request and response validation.
  Validates both the incoming request and outgoing response according to the
  provided `schema`, for the specified HTTP `method` and `path`. Additional
  options:, such as including the validation schema with each
  request (`include-schema`)."
  [handler schema method path {:as opts :keys [include-schema]}]
  (let [path-schema (get-in schema [:openapi-schema "paths" path])
        path-parameters (select-keys path-schema ["parameters"])
        sub-schema (cond-> (get path-schema (name method))
                     (seq path-parameters)
                     ;; we could have validation at the path level, before the method scope
                     (assoc :path-parameters path-parameters))]
    (-> (fn [request]
          (let [request (request/validate
                         request
                         schema
                         sub-schema
                         opts)
                request (cond-> request
                          include-schema
                          (assoc :s-exp.legba/schema sub-schema))
                response (handler request)]
            (response/validate response schema sub-schema opts)))
        (vary-meta assoc
                   :schema schema
                   :sub-schema sub-schema))))

(defmulti ex->rfc9457-type ex/ex-type
  :hierarchy ex/hierarchy)

(def ^:private keyword->rfc9457-type
  (memoize
   (fn [k]
     (str/replace (namespace k) "s-exp.legba." ""))))

(defmethod ex->rfc9457-type :default
  [ex]
  (when-let [t (ex/ex-type ex)]
    (format "#/http-problem-types/%s-%s"
            (keyword->rfc9457-type t)
            (name t))))

(defmulti ex->response
  (fn [e _opts] (some-> e ex/ex-type))
  :hierarchy ex/hierarchy)

(defmethod ex->response
  :s-exp.legba/invalid
  [e {:as _opts
      :keys [include-error-schema rfc9457-type-fn]
      :or {rfc9457-type-fn ex->rfc9457-type}}]
  (let [data (ex-data e)
        type' (rfc9457-type-fn e)]
    {:status 400
     :headers {"Content-Type" "application/problem+json"}
     :body (json/write-value-as-string
            (cond-> (dissoc data :response)
              (not include-error-schema)
              (dissoc :schema)
              type'
              (assoc :type type')
              :then
              (assoc :title (ex-message e))))}))

(defn wrap-error-response-fn
  [handler req {:as opts}]
  (ex/try+
    (handler req)
    #_{:clj-kondo/ignore [:unresolved-symbol]}
    (catch :s-exp.legba/invalid _
      #_{:clj-kondo/ignore [:unresolved-symbol]}
      (ex->response &ex opts))))

(defn wrap-error-response
  "Wraps handler with error checking middleware that will transform validation
  Exceptions to equivalent http response, as infered per `ex->response`"
  [handler opts]
  (fn [req]
    (wrap-error-response-fn handler req opts)))
