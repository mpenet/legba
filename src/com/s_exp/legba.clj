(ns s-exp.legba
  (:require [charred.api :as charred]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [exoscale.ex :as ex]
            [reitit.core :as r]
            [s-exp.legba.json-pointer :as json-pointer])
  (:import (com.networknt.schema JsonSchemaFactory
                                 SchemaValidatorsConfig
                                 InputFormat
                                 OutputFormat
                                 SchemaLocation)
           (com.networknt.schema SpecVersion$VersionFlag PathType)
           (com.networknt.schema.oas OpenApi31)
           (io.swagger.v3.core.util Json)
           (io.swagger.v3.parser OpenAPIV3Parser)
           (io.swagger.v3.parser.core.models ParseOptions)))

(defn add-pointer
  [node pointer]
  (vary-meta node assoc :json-pointer pointer))

(defn write-json-pointers
  ([node]
   (write-json-pointers node ""))
  ([node pointer]
   (cond
     (map? node)
     (-> (reduce-kv (fn [m k v]
                      (assoc m
                             k
                             (write-json-pointers v
                                                  (json-pointer/pointer-append pointer k))))
                    {}
                    node)
         (add-pointer pointer))

     (sequential? node)
     (-> (into []
               (map-indexed (fn [idx x]
                              (write-json-pointers x
                                                   (json-pointer/pointer-append pointer
                                                                                idx))))
               node)
         (add-pointer pointer))

     :else node)))

;; (meta (get-in (write-json-pointers {"a" {"b" {"c" [{"a" 1} {"b" {"b" {"c" [{"a" 1} {"b" "2"}]}}} {"a" 1} {"b" {"b" {"c" [{"a" 1} {"b" "2"}]}}} {"a" 1} {"b" {"b" {"c" [{"a" 1} {"b" "2"}]}}} {"a" 1} {"b" {"b" {"c" [{"a" 1} {"b" "2"}]}}} {"a" 1} {"b" {"b" {"c" [{"a" 1} {"b" "2"}]}}} {"a" 1} {"b" {"b" {"c" [{"a" 1} {"b" "2"}]}}} {"a" 1} {"b" {"b" {"c" [{"a" 1} {"b" "2"}]}}}]}}}) ["a" "b" "c" 1 "b" "b" "c" 0]))

(defn- load-schema
  [^String schema-resource-file]
  (let [schema-str (slurp (io/resource schema-resource-file))
        parse-options (doto (ParseOptions.)
                        (.setResolveFully true)
                        (.setValidateExternalRefs true))
        json-schema-raw (-> (.readContents (OpenAPIV3Parser.) schema-str
                                           nil
                                           parse-options)
                            (Json/pretty)
                            (charred/read-json)
                            (get "openAPI"))
        openapi-schema (write-json-pointers json-schema-raw)]
    {:openapi-schema openapi-schema
     :schema-resource-file schema-resource-file
     :json-schema-factory
     (JsonSchemaFactory/getInstance
      SpecVersion$VersionFlag/V202012
      (fn [builder]
        (doto builder
          (.metaSchema (OpenApi31/getInstance))
          (.defaultMetaSchemaIri (.getIri (OpenApi31/getInstance)))
          (.enableSchemaCache true))))}))

(defn router
  "Creates a reitit path router by method"
  [{:as schema :keys [openapi-schema]} & {:as _opts :keys [extra-routes]}]
  (-> (reduce (fn [routers-m [method & route]]
                (update routers-m
                        method
                        (fnil conj [])
                        (vec route)))
              {}
              (for [[path methods] (get openapi-schema "paths")
                    [method parameters] methods
                    :let [method (keyword method)]]
                [(keyword method)
                 path
                 {:path path
                  :method method
                  :sub-schema
                  ;; to stop reitit from messing with my metadata...
                  ((promise) parameters)}]))
      (update-vals (fn [routes]
                     (r/router (merge routes extra-routes)
                               {:syntax :bracket})))
      (vary-meta assoc :schema schema)))

(defn match-route
  [router method path]
  (when-let [r (get router method)]
    (when-let [{:as _match :keys [data path-params]} (r/match-by-path r path)]
      (cond-> data
        (seq path-params)
        (assoc :path-params (update-keys path-params keyword))
        :then
        (update :sub-schema deref)))))

(defn- match->params-schema-fn [param-type]
  (fn [request sub-schema]
    (not-empty
     (into {}
           (keep (fn [param]
                   (when (= param-type (get param "in"))
                     [(keyword (get param "name")) param])))
           (-> request meta :match :sub-schema (get "parameters"))))))

(def request->query-params-schema (match->params-schema-fn "query"))
(def request->path-params-schema (match->params-schema-fn "path"))

(def schema-validator-config
  (doto (SchemaValidatorsConfig.)
    (.setPreloadJsonSchema true)
    (.setCacheRefs true)
    (.setFormatAssertionsEnabled true)
    (.setTypeLoose true)
    (.setPreloadJsonSchemaRefMaxNestingDepth 40)
    (.setPathType PathType/JSON_PATH)))

(defn validate!
  [{:as schema :keys [schema-resource-file]} sub-schema val]
  (let [ptr (:json-pointer (meta sub-schema))
        schema (.getSchema (:json-schema-factory schema)
                           (.resolve (SchemaLocation/of (format "classpath://%s" schema-resource-file))
                                     (str "#" ptr))
                           schema-validator-config)]
    (not-empty (.validate schema val
                          InputFormat/JSON
                          OutputFormat/DEFAULT))))

(defn request->conform-query-params
  [request schema sub-schema]
  (when-let [m-query-params (request->query-params-schema request sub-schema)]
    (doseq [[query-param-key query-param-val] (:params request)]
      (when-let [query-param-schema (get m-query-params [query-param-key "schema"])]
        (when-let [errors (validate! schema query-param-schema (pr-str query-param-val))]
          (throw (ex-info "Invalid query-parameters"
                          {:type ::invalid-query-parameters
                           :errors errors}))))))
  request)

(defn request->conform-path-params
  [request schema sub-schema]
  (when-let [m-path-params (request->path-params-schema request sub-schema)]
    (doseq [[path-param-key path-param-val] (:path-params request)]
      (when-let [path-param-schema (get-in m-path-params [path-param-key "schema"])]
        (when-let [errors (validate! schema path-param-schema (pr-str path-param-val))]
          (throw (ex-info "Invalid path-parameters"
                          {:type ::invalid-path-parameters
                           :errors (into [] (map str) errors)}))))))
  request)

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
  [{:as request :keys [body headers]} schema sub-schema]
  (let [req-body-schema (get sub-schema "requestBody")]
    (when (get req-body-schema "required")
      (if-let [body-schema (match-schema-content-type req-body-schema
                                                      (get headers :content-type))]
        (when-let [errors (validate! schema body-schema body)]
          (throw (ex-info "Invalid body"
                          {:type ::invalid-request-body
                           :errors (into [] (map str) errors)})))
        (throw (ex-info "No matching content-type in schema for request"
                        {:type ::invalid-request-content-type
                         :message "Invalid content type for request"}))))
    request))

(defn conform-request
  [request schema sub-schema]
  ;; validate params
  (-> request
      (request->conform-path-params schema sub-schema)
      (request->conform-query-params schema sub-schema)
      (request->conform-body schema sub-schema)))

(defn handler-for-request
  [handlers match]
  (or (get handlers [(:method match) (:path match)])
      (throw (ex-info "No handler registered for request"
                      {:type ::no-handler-for-request}))))

(defn conform-response-body
  [{:as response
    :keys [status content-type body]
    :or {status 200 content-type "application/json"}}
   schema sub-schema]
  (let [ct-schema (or (get-in sub-schema ["responses" (str status)])
                      (get-in sub-schema ["responses" "default"]))]
    (when-not ct-schema
      (throw (ex-info "Invalid response format for status"
                      {:type ::invalid-response-format-for-status
                       :message "Invalid response format for status"})))
    (if-let [body-schema (match-schema-content-type ct-schema content-type)]
      (when-let [errors (validate! schema body-schema body)]
        (throw (ex-info "Invalid response body"
                        {:type ::invalid-response-body
                         :errors (into [] (map str) errors)})))
      (throw (ex-info "Invalid response format for content-type"
                      {:type ::invalid-response-format
                       :message "Invalid response format"})))
    response))

(defn conform-response-headers
  [{:as response :keys [status] :or {status 200}}
   schema sub-schema]
  (when-let [headers-schema (some-> sub-schema (get-in ["responses" (str status) "headers"]))]
    (doseq [[header-name header-schema] headers-schema
            :let [header-val (get-in response [:headers header-name])]]
      (when-let [errors (validate! schema header-schema (pr-str header-val))]
        (throw (ex-info (format "Invalid Response Header: %s:%s"
                                header-name
                                header-val)
                        {:type ::invalid-response-header
                         :errors errors})))))
  response)

(defn conform-response
  [response schema sub-schema]
  (-> response
      (conform-response-headers schema sub-schema)
      (conform-response-body schema sub-schema)))

(defn openapi-handler
  [handlers & {:as _opts
               :keys [schema not-found-response]
               :or {not-found-response {:status 404 :body "Not found"}}}]
  (let [router (router schema)]
    (fn [{:as request :keys [request-method uri]}]
      (if-let [{:as match :keys [sub-schema path-params]} (match-route router request-method uri)]
        (let [request (vary-meta request
                                 assoc
                                 :match match
                                 :schema schema)
              request (conform-request (cond-> request
                                         path-params
                                         (assoc :path-params path-params))
                                       schema sub-schema)
              handler (handler-for-request handlers match)
              response (handler request)
              response (conform-response response schema sub-schema)]
          (vary-meta response dissoc :match :schema))
        not-found-response))))

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

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ;;;; Playground                                                             ;;;;
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; ;; "https://raw.githubusercontent.com/OAI/OpenAPI-Specification/refs/heads/main/examples/v3.0/petstore.json"

;; (def schema (load-schema "petstore.json"))
;; (do schema)

((openapi-handler {[:get "/pet/{petId}"]
                   (fn [_request]
                     {:body (charred/write-json-str
                             {:id 1
                              :name "foo"})})
                   [:get "/pets"]
                   (fn [_request]
                     {:body (charred/write-json-str [{:id "asd"}])
                      ;; :headers {"x-next" "asdf"}
                      })
                   [:post "/pet"]
                   (fn [_request]
                     {:body (charred/write-json-str {:name "yolo", :photoUrls []})
                      :status 200})}
                  :schema (load-schema "schema/oas/3.1/petstore.json"))
 ;; {:request-method :get :uri "/pet/2"}
 {:request-method :post
  :headers {:content-type "application/json"}
  :uri "/pet"
  :body "{\"name\": \"asdf\", \"id\":1, \"photoUrls\": []}"})
