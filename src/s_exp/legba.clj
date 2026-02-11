(ns s-exp.legba
  (:require [clojure.string :as str]
            [exoscale.ex :as ex]
            [ring.middleware.params :as ring-params]
            [s-exp.legba.middleware :as m]
            [s-exp.legba.openapi-schema :as schema]
            [s-exp.legba.router :as router]))

(def default-options
  "Default options used by openapi-handler"
  {:not-found-response {:status 404 :body "Not found"}
   :key-fn keyword
   :query-string-params-key :query-params
   :path-params-key :path-params
   :write-response-json-body true
   :read-request-json-body true})

(defn ensure-route-coverage!
  "Checks that a map of openapi-handlers covers all paths defined by the schema"
  [routes {:as _schema :keys [openapi-schema]}]
  (let [missing-routes
        (for [[path methods] (get openapi-schema "paths")
              [method & _] methods
              :when (#{"delete" "get" "head" "options" "patch" "post" "put" "trace"} method)
              :let [method (keyword method)
                    matching-entry (get routes [(keyword method) path])]
              :when (not matching-entry)]
          [path method])]
    (when (seq missing-routes)
      (ex/ex-incorrect! (format "Missing handlers for %s"
                                (str/join ", " missing-routes))))))

(defn middlewares
  "From a sequence of [method path] tuples returns a map of [method path] ->
  openapi validation middlewares.

  Options:

  * `:key-fn` - Control map keys decoding when turning jackson JsonNodes to clj
    data for the handler - default to `keyword`

  * `:query-string-params-key` - where to find the decoded query-string
     parameters - defaults to `:query-params`

  * `:validation-result` - function that controls how to turn
  `com.networknt.schema.ValidationResult` into a clj -> json response. Defaults
  to `s-exp.legba.openapi-schema/validation-result`

  * `:include-schema`: - adds the path-relevant schema portion to the
  request-map under `:s-exp.legba/schema` (`false` by default)"
  [schema-path & {:as opts}]
  (let [schema (schema/load-schema schema-path)
        paths (keys (schema/schema->routes-schema schema))
        opts (merge default-options opts)]
    (reduce (-> (fn [m [method path :as coords]]
                  (assoc m
                         coords
                         (fn validation-middleware [handler]
                           (let [h (m/wrap-validation handler schema method path opts)]
                             (vary-meta (ring-params/wrap-params h)
                                        merge (meta h)))))))

            {}
            paths)))

(defn handlers
  "From a map of [method path] -> ring handler returns a map of [method path] ->
  openapi-wrapped-handler.

  Options:

  * `:key-fn` - Control map keys decoding when turning jackson JsonNodes to clj
    data for the handler - default to `keyword`

  * `:query-string-params-key` - where to find the decoded query-string
     parameters - defaults to `:query-params`

  * `:validation-result` - function that controls how to turn
  `com.networknt.schema.ValidationResult` into a clj -> json response. Defaults
  to `s-exp.legba.openapi-schema/validation-result`

  * `:include-schema`: - adds the path-relevant schema portion to the
  request-map under `:s-exp.legba/schema` (`false` by default)"
  [routes schema-path & {:as opts}]
  (let [opts (merge default-options opts)
        middlewares* (middlewares schema-path opts)
        _ (ensure-route-coverage! routes middlewares*)]
    (reduce (-> (fn [m [coords middleware]]
                  (if-let [handler (get routes coords)]
                    (assoc m coords (middleware handler))
                    m)))
            {}
            middlewares*)))

(defn routing-handler*
  "Takes a map of routes as [method path] -> ring-handler, turns them into a map
  of routes to openapi handlers then creates a handler that will dispatch on the
  appropriate openapi handler from a potential router match. If not match is
  found, returns `not-found-response`.

  Options:

  * `:not-found-response` - defaults to `{:status 404 :body \" Not found \"}`

  * `:key-fn` - Control map keys decoding when turning jackson JsonNodes to clj
    data for the handler - default to `keyword`

  * `:query-string-params-key` - where to find the decoded query-string
     parameters - defaults to `:query-params`

  * `:validation-result` - function that controls how to turn
    `com.networknt.schema.ValidationResult` into a clj -> json response. Defaults
    to `s-exp.legba.openapi-schema/validation-result`

  * `:extra-routes` - extra routes to be passed to the underlying router

  throw and assocs the error on the ring response as response-validation-error.  "
  [routes schema-path & {:as opts
                         :keys [path-params-key]}]
  (let [{:as opts :keys [not-found-response]}
        (merge default-options opts)
        handlers (handlers routes schema-path opts)
        router (router/router handlers opts)]
    (fn [{:as request}]
      (if-let [[handler path-params] (router/match router request)]
        (cond-> request
          path-params
          (assoc path-params-key path-params)
          :then handler)
        not-found-response))))

(defn routing-handler
  "Same as `routing-handler*` but wraps with
  `s-exp.legba.middleware/wrap-error-response` middleware turning exceptions
  into nicely formatted error responses"
  [routes schema-path & {:as opts}]
  (let [opts (merge default-options opts)]
    (-> (routing-handler* routes
                          schema-path
                          opts)
        (m/wrap-error-response opts))))

(ex/derive :s-exp.legba/invalid :exoscale.ex/incorrect)
(ex/derive :s-exp.legba/handler-undefined :exoscale.ex/fault)
