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
  [request schema sub-schema {:as _opts :keys [query-string-params-key]}]
  (when-let [m-query-params (query-params-schema sub-schema)]
    (doseq [[schema-key {:as query-schema :strs [required]}] m-query-params
            :let [param-val (get-in request [query-string-params-key
                                             schema-key]
                                    ::missing)
                  _ (prn :req request :k schema-key :o [query-string-params-key
                                                        schema-key])
                  _ (when (and required (= ::missing param-val))
                      (throw (ex-info "Missing Required Query Parameter"
                                      {:type ::missing-query-parameter
                                       :schema query-schema})))]]

      (when-let [errors (schema/validate! schema
                                          (get query-schema "schema")
                                          (pr-str (or param-val "")))]
        (throw (ex-info "Invalid Query Parameters"
                        {:type ::invalid-query-parameters
                         :schema m-query-params
                         :errors errors})))))
  request)

(defn request->conform-path-params
  [request schema sub-schema _opts]
  (when-let [m-path-params (path-params-schema sub-schema)]
    (doseq [[schema-key param-schema] m-path-params
            :let [param-val (get-in request [:path-params schema-key])]]
      (when-let [errors (schema/validate! schema
                                          (get param-schema "schema")
                                          (pr-str param-val))]
        (throw (ex-info "Invalid Path Parameters"
                        {:type ::invalid-path-parameters
                         :schema m-path-params
                         :errors errors})))))
  request)

(defn request->conform-body
  [{:as request :keys [body headers]} schema sub-schema opts]
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
              (throw (ex-info "Invalid Request Body"
                              {:type ::invalid-body
                               :schema body-schema
                               :errors (into [] (map str) errors)})))
            (cond-> request
              json-body
              (assoc :body (json/json-node->clj body opts))))
          (throw (ex-info "No matching content-type in schema for request"
                          {:type ::invalid-content-type
                           :schema req-body-schema
                           :message "Invalid content type for request"}))))
      request)))

(defn conform-request
  [request schema sub-schema opts]
  ;; validate params
  (-> request
      (request->conform-path-params schema sub-schema opts)
      (request->conform-query-params schema sub-schema opts)
      (request->conform-body schema sub-schema opts)))

(ex/derive ::invalid :s-exp.legba/invalid)
(ex/derive ::invalid-body :s-exp.legba/invalid)
(ex/derive ::invalid-path-parameters :s-exp.legba/invalid)
(ex/derive ::invalid-query-parameters :s-exp.legba/invalid)
(ex/derive ::invalid-content-type :s-exp.legba/invalid)
(ex/derive ::missing-query-parameter :s-exp.legba/invalid)
