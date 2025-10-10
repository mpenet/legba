(ns s-exp.legba.middleware
  (:require [clojure.string :as str]
            [exoscale.ex :as ex]
            [jsonista.core :as json]
            [s-exp.legba.request :as request]
            [s-exp.legba.response :as response]))

(defn wrap-validation
  "Takes a regular RING handler returns a handler that will apply openapi
  validation from the supplied `schema` for a given `method` and `path`"
  [handler schema method path {:as opts
                               :keys [soft-response-validation]}]
  (let [sub-schema (get-in schema [:openapi-schema "paths" path (name method)])]
    (-> (fn [request]
          (let [request (request/validate
                         request
                         schema
                         sub-schema
                         opts)
                response (handler request)]
            (if soft-response-validation
              (try
                (response/validate response schema sub-schema opts)
                (catch Exception e
                  (assoc response :response-validation-error e)))
              (response/validate response schema sub-schema opts))))
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
    (format "%s:%s"
            (keyword->rfc9457-type t)
            (name t))))

(defmulti ex->response
  (fn [e _opts] (some-> e ex/ex-type))
  :hierarchy ex/hierarchy)

(defmethod ex->response
  :s-exp.legba/invalid
  [e {:as _opts
      :keys [include-error-schema]
      :or {include-error-schema true}}]
  (let [data (ex-data e)
        type' (ex->rfc9457-type e)]
    {:status 400
     :content-type "application/json"
     :body (json/write-value-as-string
            (cond-> data
              (not include-error-schema)
              (dissoc :schema)
              type'
              (assoc :type type')
              :then
              (assoc :title (ex-message e))))}))

(defn wrap-error-response-fn
  [handler req opts]
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
