(ns s-exp.legba.response
  (:require [clojure.string :as str]
            [exoscale.ex :as ex]
            [s-exp.legba.content-type :as content-type]
            [s-exp.legba.json :as json]
            [s-exp.legba.schema :as schema]))

(defn conform-response-body
  [{:as response
    :keys [status body headers]
    :or {status 200}}
   schema sub-schema opts]
  (let [ct-schema (or (get-in sub-schema ["responses" (str status)])
                      (get-in sub-schema ["responses" "default"]))
        content-type (or (some-> headers (update-keys str/lower-case) (get "content-type"))
                         "application/json")]
    (when-not ct-schema
      (throw (ex-info "Invalid response format for status"
                      {:type ::invalid-format-for-status
                       :schema sub-schema
                       :message "Invalid response format for status"})))
    (if-let [body-schema (content-type/match-schema-content-type ct-schema content-type)]
      (let [json-body (json/json-content-type? content-type)
            ;; if we have a json-body we convert it to jsonnode for validation
            ;; and later returning handler value
            body (cond-> body json-body json/clj->json-node)]
        (when-let [errors (schema/validate! schema
                                            body-schema
                                            body
                                            opts)]
          (throw (ex-info "Invalid Response Body"
                          {:type ::invalid-body
                           :schema body-schema
                           :errors errors})))
        ;; we converted the body, turn it into a string for the response
        (cond-> response
          json-body
          (assoc :body (json/json-node->str body))))
      (throw (ex-info "Invalid response content-type"
                      {:type ::invalid-content-type
                       :schema ct-schema
                       :message "Invalid Response Content-Type"})))))

(defn conform-response-headers
  [{:as response :keys [status] :or {status 200}}
   schema sub-schema opts]
  (when-let [headers-schema (or (some-> sub-schema (get-in ["responses" (str status) "headers"]))
                                (some-> sub-schema (get-in ["responses" "default" "headers"])))]
    (doseq [[header-name header-schema] headers-schema
            :let [header-val (get-in response [:headers header-name])]]
      (when-let [errors (schema/validate! schema header-schema
                                          (pr-str header-val)
                                          opts)]
        (throw (ex-info (format "Invalid Response Header: %s:%s"
                                header-name
                                header-val)
                        {:type ::invalid-header
                         :schema headers-schema
                         :errors errors})))))
  response)

(defn conform-response
  [response schema sub-schema opts]
  (-> response
      (conform-response-headers schema sub-schema opts)
      (conform-response-body schema sub-schema opts)))

(ex/derive ::invalid-header :s-exp.legba/invalid)
(ex/derive ::invalid-body :s-exp.legba/invalid)
(ex/derive ::invalid-format-for-status :s-exp.legba/invalid)
(ex/derive ::invalid-content-type :s-exp.legba/invalid)
