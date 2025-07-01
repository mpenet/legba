(ns s-exp.legba.response
  (:require [exoscale.ex :as ex]
            [s-exp.legba.content-type :as content-type]
            [s-exp.legba.schema :as schema]))

(defn conform-response-body
  [{:as response
    :keys [status content-type body]
    :or {status 200 content-type "application/json"}}
   schema sub-schema]
  (let [ct-schema (or (get-in sub-schema ["responses" (str status)])
                      (get-in sub-schema ["responses" "default"]))]
    (when-not ct-schema
      (throw (ex-info "Invalid response format for status"
                      {:type ::invalid-format-for-status
                       :schema sub-schema
                       :message "Invalid response format for status"})))
    (if-let [body-schema (content-type/match-schema-content-type ct-schema content-type)]
      (when-let [errors (schema/validate! schema body-schema body)]
        (throw (ex-info "Invalid response body"
                        {:type ::invalid-body
                         :schema body-schema
                         :errors (into [] (map str) errors)})))
      (throw (ex-info "Invalid response format for content-type"
                      {:type ::invalid-format
                       :schema ct-schema
                       :message "Invalid response format"})))
    response))

(defn conform-response-headers
  [{:as response :keys [status] :or {status 200}}
   schema sub-schema]
  (when-let [headers-schema (some-> sub-schema (get-in ["responses" (str status) "headers"]))]
    (doseq [[header-name header-schema] headers-schema
            :let [header-val (get-in response [:headers header-name])]]
      (when-let [errors (schema/validate! schema header-schema (pr-str header-val))]
        (throw (ex-info (format "Invalid Response Header: %s:%s"
                                header-name
                                header-val)
                        {:type ::invalid-header
                         :schema headers-schema
                         :errors errors})))))
  response)

(defn conform-response
  [response schema sub-schema]
  (-> response
      (conform-response-headers schema sub-schema)
      (conform-response-body schema sub-schema)))

(ex/derive ::invalid-header :exoscale.ex/invalid)
(ex/derive ::invalid-body :exoscale.ex/invalid)
