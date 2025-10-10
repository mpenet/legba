(ns s-exp.legba.request
  (:require [exoscale.ex :as ex]
            [s-exp.legba.json :as json]
            [s-exp.legba.mime-type :as mime-type]
            [s-exp.legba.openapi-schema :as schema]))

(defn- match->params-schema-fn
  "Returns a fn that will match schema \"parameters\" by `param-type`"
  [param-type]
  (fn [sub-schema]
    (not-empty
     (into {}
           (keep (fn [param]
                   (when (= param-type (get param "in"))
                     [(keyword (get param "name")) param])))
           (get sub-schema "parameters")))))

(def query-params-schema
  "Matches `param-type` for \"query\""
  (match->params-schema-fn "query"))

(def path-params-schema
  "Matches `param-type` for \"path\""
  (match->params-schema-fn "path"))

(def cookie-params-schema
  "Matches `param-type` for \"cookie\""
  (match->params-schema-fn "cookie"))

(defn validate-query-params
  "Performs eventual validation of \"parameters\" of type \"query\""
  [request schema sub-schema {:as opts :keys [query-string-params-key]}]
  (when-let [m-query-params (query-params-schema sub-schema)]
    (doseq [[schema-key {:as query-schema :strs [required]}] m-query-params
            :let [param-val (get-in request [query-string-params-key
                                             (name schema-key)]
                                    :s-exp.legba.request/missing)
                  _ (when (and required (= :s-exp.legba.request/missing param-val))
                      (throw (ex-info "Missing Required Query Parameter"
                                      {:type :s-exp.legba.request/missing-query-parameter
                                       :errors [{:pointer (-> query-schema meta :json-pointer)
                                                 :detail "required query parameter missing"}]
                                       :schema query-schema})))]]
      (when-let [errors (schema/validate schema
                                         (get query-schema "schema")
                                         (pr-str (or param-val ""))
                                         opts)]
        (throw (ex-info "Invalid Query Parameters"
                        {:type :s-exp.legba.request/invalid-query-parameters
                         :schema m-query-params
                         :errors errors})))))
  request)

(defn validate-cookie-params
  "Performs eventual validation of \"parameters\" of type \"cookie\""
  [request schema sub-schema opts]
  (when-let [m-cookie-params (cookie-params-schema sub-schema)]
    (doseq [[schema-key {:as cookie-schema :strs [required]}] m-cookie-params
            :let [param-val (get-in request [:cookies schema-key]
                                    :s-exp.legba.request/missing)
                  _ (when (and required (= :s-exp.legba.request/missing param-val))
                      (throw (ex-info "Missing Required Cookie Parameter"
                                      {:type :s-exp.legba.request/missing-cookie-parameter
                                       :errors [{:pointer (-> cookie-schema meta :json-pointer)
                                                 :detail "required cookie parameter missing"}]
                                       :schema cookie-schema})))]]
      (when-let [errors (schema/validate schema
                                         (get cookie-schema "schema")
                                         (pr-str (or param-val ""))
                                         opts)]
        (throw (ex-info "Invalid Cookie Parameters"
                        {:type :s-exp.legba.request/invalid-cookie-parameters
                         :schema m-cookie-params
                         :errors errors})))))
  request)

(defn validate-path-params
  "Performs extensive validation of \"path\" \"parameters\""
  [request schema sub-schema {:as opts :keys [path-params-key]}]
  (when-let [m-path-params (path-params-schema sub-schema)]
    (doseq [[schema-key param-schema] m-path-params
            :let [param-val (get-in request [path-params-key schema-key])]]
      (when-let [errors (schema/validate schema
                                         (get param-schema "schema")
                                         (pr-str param-val)
                                         opts)]
        (throw (ex-info "Invalid Path Parameters"
                        {:type :s-exp.legba.request/invalid-path-parameters
                         :schema m-path-params
                         :errors errors})))))
  request)

(defn validate-body
  "Performs eventual validation of request `:body`"
  [{:as request :keys [body headers]} schema sub-schema opts]
  (let [req-body-schema (get sub-schema "requestBody")]
    (if (get req-body-schema "required")
      (let [content-type (get headers "content-type")]
        (if-let [body-schema (mime-type/match-schema-mime-type req-body-schema
                                                               content-type)]
          ;; we must ensure we don't force double parsing of the input json, if
          ;; conten-type is json we load into jsonnode and pass it along to
          ;; validator and then later turn that jsonnode into a clj thing
          (let [json-body (json/json-content-type? content-type)
                body (if json-body
                       (-> body slurp json/str->json-node)
                       body)]
            (when-let [errors (schema/validate schema
                                               body-schema
                                               body
                                               opts)]
              (throw (ex-info "Invalid Request Body"
                              {:type :s-exp.legba.request/invalid-body
                               :schema body-schema
                               :errors errors})))
            (cond-> request
              json-body
              (assoc :body (json/json-node->clj body opts))))
          (throw (ex-info "Invalid content type for request"
                          {:type :s-exp.legba.request/invalid-content-type
                           :errors [{:pointer (-> req-body-schema meta :json-pointer)
                                     :detail "No matching content-type"}]
                           :schema req-body-schema}))))
      request)))

(defn validate
  "Performs validation of RING request map"
  [request schema sub-schema opts]
  (-> request
      (validate-path-params schema sub-schema opts)
      (validate-query-params schema sub-schema opts)
      (validate-cookie-params schema sub-schema opts)
      (validate-body schema sub-schema opts)))

;; Derive our own sub-types, the error middleware will catch on
;; `:s-exp.legba/invalid` and we later dispath on error response multimethod,
;; `s-exp.legba.middleware/ex->response`, with the leaf types
(run! #(ex/derive % :s-exp.legba/invalid)
      [:s-exp.legba.request/invalid
       :s-exp.legba.request/invalid-body
       :s-exp.legba.request/invalid-path-parameters
       :s-exp.legba.request/invalid-query-parameters
       :s-exp.legba.request/invalid-cookie-parameters
       :s-exp.legba.request/invalid-content-type
       :s-exp.legba.request/missing-cookie-parameter
       :s-exp.legba.request/missing-query-parameter])
