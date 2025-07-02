(ns s-exp.legba.request
  (:require [exoscale.ex :as ex]
            [s-exp.legba.content-type :as content-type]
            [s-exp.legba.json :as json]
            [s-exp.legba.schema :as schema]))

(defn- match->params-schema-fn [param-type]
  (fn [sub-schema]
    (not-empty
     (into {}
           (keep (fn [param]
                   (when (= param-type (get param "in"))
                     [(keyword (get param "name")) param])))
           (get sub-schema "parameters")))))

(def query-params-schema (match->params-schema-fn "query"))
(def path-params-schema (match->params-schema-fn "path"))

(defn request->conform-query-params
  [request schema sub-schema]
  (when-let [m-query-params (query-params-schema sub-schema)]
    (doseq [[query-param-key query-param-val] (:params request)]
      (when-let [query-param-schema (get m-query-params [query-param-key "schema"])]
        (when-let [errors (schema/validate! schema query-param-schema (pr-str query-param-val))]
          (throw (ex-info "Invalid query-parameters"
                          {:type ::invalid-query-parameters
                           :schema m-query-params
                           :errors errors}))))))
  request)

(defn request->conform-path-params
  [request schema sub-schema]
  (when-let [m-path-params (path-params-schema sub-schema)]
    (doseq [[path-param-key path-param-val] (:path-params request)]
      (when-let [path-param-schema (get-in m-path-params [path-param-key "schema"])]
        (when-let [errors (schema/validate! schema path-param-schema (pr-str path-param-val))]
          (throw (ex-info "Invalid path-parameters"
                          {:type ::invalid-path-parameters
                           :schema m-path-params
                           :errors (into [] (map str) errors)}))))))
  request)

(defn request->conform-body
  [{:as request :keys [body headers]} schema sub-schema]
  (let [req-body-schema (get sub-schema "requestBody")]
    (if (get req-body-schema "required")
      (let [content-type (get headers "content-type")]
        (if-let [body-schema (content-type/match-schema-content-type
                              req-body-schema
                              content-type)]
          ;; we must ensure we don't force double parsing of the input json, if
          ;; conten-type is json we load into jsonnode and pass it along to
          ;; validator and then later turn that jsonnode into a clj thing
          (let [json-body (json/json-content-type? content-type)
                body (cond-> body json-body json/str->json-node)]
            (when-let [errors (schema/validate! schema
                                                body-schema
                                                body)]
              (throw (ex-info "Invalid body"
                              {:type ::invalid-body
                               :schema body-schema
                               :errors (into [] (map str) errors)})))
            (cond-> request
              json-body
              (assoc :body (json/json-node->clj body))))
          (throw (ex-info "No matching content-type in schema for request"
                          {:type ::invalid-content-type
                           :schema req-body-schema
                           :message "Invalid content type for request"}))))
      request)))

(defn conform-request
  [request schema sub-schema]
  ;; validate params
  (-> request
      (request->conform-path-params schema sub-schema)
      (request->conform-query-params schema sub-schema)
      (request->conform-body schema sub-schema)))

(ex/derive ::invalid :exoscale.ex/invalid)
(ex/derive ::invalid-body :exoscale.ex/invalid)
(ex/derive ::invalid-path-parameters :exoscale.ex/invalid)
(ex/derive ::invalid-query-parameters :exoscale.ex/invalid)
(ex/derive ::invalid-content-type :exoscale.ex/invalid)
