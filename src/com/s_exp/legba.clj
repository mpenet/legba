(ns s-exp.legba
  (:require [charred.api :as charred]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [exoscale.coax :as cx]
            [exoscale.ex :as ex]
            [reitit.core :as r]
            [s-exp.pact :as pact])
  (:import (io.swagger.v3.core.util Json)
           (io.swagger.v3.parser OpenAPIV3Parser)
           (io.swagger.v3.parser.core.models ParseOptions)))

(defn- load-schema
  [^String schema-str]
  (let [parse-options (doto (ParseOptions.)
                        (.setResolveFully true)
                        (.setValidateExternalRefs true))]
    (-> (.readContents (OpenAPIV3Parser.) schema-str
                       nil
                       parse-options)
        (Json/pretty)
        (charred/read-json)
        (get "openAPI"))))

(defn router
  "Creates a reitit path router by method"
  [schema & {:as _opts :keys [extra-routes]}]
  (-> (reduce (fn [routers-m [method & route]]
                (update routers-m
                        method
                        (fnil conj [])
                        (vec route)))
              {}
              (for [[path methods] (get schema "paths")
                    [method parameters] methods
                    :let [method (keyword method)]]
                [(keyword method)
                 path
                 {:path path :method method :openapi parameters}]))
      (update-vals (fn [routes]
                     (r/router
                      (merge routes extra-routes)
                      {:syntax :bracket})))
      (vary-meta assoc :schema schema)))

(defn match-route
  [router method path]
  (when-let [r (get router method)]
    (when-let [{:as _match :keys [data path-params]} (r/match-by-path r path)]
      (cond-> data
        (seq path-params)
        (assoc :path-params (update-keys path-params keyword))))))

(defn m->spec
  [m]
  (some-> (get m "x-spec") keyword))

(defn m->fn
  [m]
  (some-> (get m "x-fn") keyword))

(defn spec-conform
  [schema val {:as _opts :keys [error-message
                                ex-type
                                ex-data]
               :or {error-message "Invalid request"
                    ex-type ::invalid-request}}]
  (let [spec (m->spec schema)
        coerced (cx/coerce spec val)]
    (when-not (s/valid? spec coerced)
      (throw (ex-info error-message
                      (merge {:type ex-type
                              :explain (s/explain-data spec coerced)}
                             ex-data))))
    coerced))

(defn fn-conform
  [schema val opts]
  (if-let [f (requiring-resolve (m->fn schema))]
    (f schema val opts)
    (throw (ex-info "Coudln't resolve `x-fn` attributes for schema"
                    {:type ::x-fn-not-found
                     :schema schema
                     :val val}))))

(defn conform
  [schema val & {:as opts}]
  (let [spec-schema (m->spec schema)
        fn-schema (m->fn schema)]
    (cond
      spec-schema
      (spec-conform schema val opts)
      fn-schema
      (fn-conform schema val opts)
      :else
      (throw (ex-info "Missing x-spec or x-fn attributes for schema"
                      {:schema schema
                       :val val})))))

(defn- match->params-schema-fn [param-type]
  (fn [request]
    (not-empty
     (into {}
           (keep (fn [param]
                   (when (= param-type (get param "in"))
                     [(keyword (get param "name")) param])))
           (-> request ::match :openapi (get "parameters"))))))

(def request->query-params-schema (match->params-schema-fn "query"))
(def request->path-params-schema (match->params-schema-fn "path"))

(defn request->conform-query-params
  [request]
  (if-let [m-query-params (request->query-params-schema request)]
    (reduce (fn [request [query-param-key query-param-val]]
              (let [query-param-schema (get (get m-query-params query-param-key)
                                            "schema")]
                (assoc-in request [:params query-param-key]
                          (conform query-param-schema
                                   query-param-val
                                   {:ex-type ::invalid-query-parameters
                                    :message "Invalid query params"}))))
            request
            (:params request))
    request))

(defn request->conform-path-params
  [request]
  (if-let [m-path-params (request->path-params-schema request)]
    (reduce (fn [request [path-param-key path-param-val]]
              (let [path-param-schema (get (get m-path-params path-param-key)
                                           "schema")]
                (assoc-in request [:path-params path-param-key]
                          (conform path-param-schema
                                   path-param-val
                                   {:ex-type ::invalid-path-parameters
                                    :message "Invalid path parameter"}))))
            request
            (:path-params (::match request)))
    request))

(defn- match-content-type?
  [ptn s]
  (loop [[ptn0 & ptn :as rptn] ptn
         [s0 & s :as rs] s]
    (cond
      (and (zero? (count ptn))
           (zero? (count s)))
      true

      (= ptn0 s0)
      (recur ptn s)

      (and (= ptn0 \*) s0)
      (or
       (match-content-type? ptn rs)
       (recur rptn s)))))

(defn match-schema-content-type
  [schema content-type]
  (let [content (get schema "content")
        content-types (str/split content-type #";" 1)]
    (reduce (fn [_ content-type]
              (when-let [ret (or (get-in content [content-type "schema"])
                                 (reduce (fn [_ [ct-key ct-val]]
                                           (when (match-content-type? ct-key content-type)
                                             (reduced (get ct-val "schema"))))
                                         nil
                                         content))]
                (reduced ret)))
            nil
            content-types)))

(defn request->conform-body
  [{:as request :keys [body content-type] ::keys [match]}]
  (let [req-body-schema (get match "requestBody")]
    (if (get req-body-schema "required")
      (if-let [schema (match-schema-content-type req-body-schema content-type)]
        (assoc request :body
               (conform schema
                        body
                        {:ex-type ::invalid-request-body
                         :message "Invalid Payload"}))
        (throw (ex-info "No matching content-type in schema for request"
                        {:type ::invalid-request-content-type
                         :message "Invalid content type for request"})))
      request)))

(defn conform-request
  [request]
  ;; coerce & validate params
  (-> request
      request->conform-path-params
      request->conform-query-params
      request->conform-body))

(defn handler-for-request
  [handlers {:as _request ::keys [match]}]
  (or (get handlers [(:method match) (:path match)])
      (throw (ex-info "No handler registered for request"
                      {:type ::no-handler-for-request}))))

(defn conform-response-body
  [{:as response
    :keys [status content-type body]
    ::keys [match]
    :or {status 200
         content-type "application/json"}}]
  (let [ct-schema (or (get-in match [:openapi "responses" (str status)])
                      (get-in match [:openapi "responses" "default"]))]
    (when-not ct-schema
      (throw (ex-info "Invalid response format for status"
                      {:type ::invalid-response-format-for-status
                       :message "Invalid response format for status"})))
    (if-let [schema (match-schema-content-type ct-schema content-type)]
      (assoc response :body
             (conform schema
                      body
                      {:ex-type ::invalid-response-body
                       :message "Invalid Response Body"}))
      (throw (ex-info "Invalid response format for content-type"
                      {:type ::invalid-response-format
                       :message "Invalid response format"})))))

(defn conform-response-headers
  [{:as response
    :keys [status]
    ::keys [match]
    :or {status 200}}]
  (if-let [headers-schema (get-in match [:openapi "responses" (str status) "headers"])]
    (reduce (fn [response [header-name header-schema]]
              (update-in response
                         [:headers header-name]
                         (fn [header-val]
                           (conform (get header-schema "schema")
                                    header-val
                                    :ex-type ::invalid-response-header
                                    :message (format "Invalid Response Header: %s:%s"
                                                     header-name
                                                     header-val)))))
            response
            headers-schema)
    response))

(defn conform-response
  [response]
  (-> response conform-response-headers conform-response-body))

(defn openapi-handler
  [handlers & {:as _opts
               :keys [schema not-found-response]
               :or {not-found-response {:status 404 :body "Not found"}}}]
  (let [router (router schema)]
    (fn [{:as request :keys [request-method uri]}]
      (if-let [match (match-route router request-method uri)]
        (let [request (assoc request ::match match)
              request (conform-request request)
              handler (handler-for-request handlers request)
              response (assoc (handler request) ::match match)
              response (conform-response response)]
          (dissoc response ::match))
        not-found-response))))

(defn- spec->component-file-name
  [spec]
  [(str/replace (namespace spec) "." "/")
   (format "%s.json" (name spec))])

(defn register-spec!
  [spec-key & {:as _opts
               :keys [assets-path pact-opts schema-opts]
               :or {assets-path "assets/openapi/components"}}]
  (let [[path file] (spec->component-file-name spec-key)
        full-path (io/file assets-path path)]
    (when-not (.exists full-path)
      (.mkdirs full-path))
    (spit (io/file full-path file)
          (charred/write-json-str
           (merge (pact/json-schema spec-key (merge {:add-x-spec true} pact-opts))
                  {:x-spec (format "%s/%s"
                                   (namespace spec-key)
                                   (name spec-key))}
                  schema-opts))))

  spec-key)

(ex/derive ::invalid-request :exoscale.ex/invalid)
(ex/derive ::invalid-response-header :exoscale.ex/invalid)
(ex/derive ::invalid-response-body :exoscale.ex/invalid)
(ex/derive ::invalid-request-body :exoscale.ex/invalid)
(ex/derive ::invalid-path-parameters :exoscale.ex/invalid)
(ex/derive ::invalid-query-parameters :exoscale.ex/invalid)
(ex/derive ::invalid-request-content-type :exoscale.ex/invalid)
(ex/derive ::x-fn-not-found :exoscale.ex/fault)
(ex/derive ::x-spec-not-found :exoscale.ex/fault)
(ex/derive ::no-handler-for-request :exoscale.ex/fault)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Playground                                                             ;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; "https://raw.githubusercontent.com/OAI/OpenAPI-Specification/refs/heads/main/examples/v3.0/petstore.json"

(-> (s/def :s-exp.legba.pet/id int?)
    (register-spec!))

(-> (s/def :s-exp.legba/pets (s/coll-of map?))
    (register-spec!))

(-> (s/def :s-exp.legba/pet (s/keys :req-un [:s-exp.legba.pet/id]))
    (register-spec!))

(-> (s/def :s-exp.legba/next-page string?)
    (register-spec!))

(-> (s/def :s-exp.legba/error map?)
    register-spec!)

(-> (s/def :s-exp.legba.pets.query/limit (s/int-in 0 100))
    register-spec!)

(def schema (load-schema (slurp (io/resource "petstore.json"))))
(do schema)

(prn ((openapi-handler {[:get "/pets/{petId}"]
                        (fn [_request]
                          {:body {:id 1
                                  :name "foo"}})
                        [:get "/pets"]
                        (fn [_request]
                          {:body [{:id "asd"}]
                           :headers {"x-next" "asdf"}})
                        [:post "/pets"]
                        (fn [_request] {:body "post pet" :status 200})}
                       :schema schema)
      {:request-method :get :uri "/pets/1"}
      ;; {:request-method :get :uri "/pets"}
      ;; {:request-method :get :uri "/pets/1"}
      ;; {:request-method :get :uri "/pets/asdf" :body "asdf"}
      ))
