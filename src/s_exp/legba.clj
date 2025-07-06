(ns s-exp.legba
  (:require [clojure.string :as str]
            [exoscale.ex :as ex]
            [s-exp.legba.middleware :as m]
            [s-exp.legba.router :as router]
            [s-exp.legba.schema :as schema]))

(def default-options
  "Default options used by openapi-handler"
  {:not-found-response {:status 404 :body "Not found"}
   :key-fn keyword
   :query-string-params-key :params})

(defn- ensure-handler-coverage!
  "Checks that a map of openapi-handlers covers all paths defined by the schema"
  [openapi-handlers {:as _schema :keys [openapi-schema]}]
  (let [missing-handlers
        (for [[path methods] (get openapi-schema "paths")
              [method & _] methods
              :let [method (keyword method)
                    matching-entry (get openapi-handlers [(keyword method) path])]
              :when (not matching-entry)]
          [path method])]
    (when (seq missing-handlers)
      (ex/ex-incorrect! (format "Missing handlers for %s"
                                (str/join ", " missing-handlers))))))
(defn handlers
  "From a map of [method path] -> ring handler returns a map of [method path] ->
  openapi-wrapped-handler.

  Options:

  * `:key-fn` - Control map keys decoding when turning jackson JsonNodes to clj
    data for the handler - default to `keyword`

  * `:query-string-params-key` - where to find the decoded query-string
     parameters - defaults to `:params`

  * `:validation-result` - function that controls how to turn
    `com.networknt.schema.ValidationResult` into a clj -> json response. Defaults
    to `s-exp.legba.schema/validation-result`"
  [routes schema & {:as opts}]
  (let [opts (merge default-options opts)]
    (reduce (-> (fn [m [[method path :as coords] handler]]
                  (assoc m
                         coords
                         (m/wrap-validation handler
                                            schema
                                            method
                                            path
                                            opts))))
            {}
            routes)))

(defn routing-handler*
  "Takes a map of routes as [method path] -> ring-handler, turns them into a map
  of routes to openapi handlers then creates a handler that will dispatch on the
  appropriate openapi handler from a potential router match. If not match is
  found, returns `not-found-response`.

  Options:

  * `:not-found-response` - defaults to `{:status 404 :body " Not found "}`

  * `:key-fn` - Control map keys decoding when turning jackson JsonNodes to clj
    data for the handler - default to `keyword`

  * `:query-string-params-key` - where to find the decoded query-string
     parameters - defaults to `:params`

  * `:validation-result` - function that controls how to turn
    `com.networknt.schema.ValidationResult` into a clj -> json response. Defaults
    to `s-exp.legba.schema/validation-result`

  * `:extra-routes` - extra routes to be passed to the underlying reitit router
    (using `{:syntax :bracket}`)
  "
  [routes schema & {:as opts}]
  (let [{:as opts :keys [not-found-response]}
        (merge default-options opts)
        schema (schema/load-schema schema)
        handlers (handlers routes schema opts)
        _ (ensure-handler-coverage! handlers schema)
        router (router/router schema handlers opts)]
    (fn [{:as request :keys [request-method uri]}]
      (if-let [{:as _match :keys [handler path-params]}
               (router/match-route router request-method uri)]
        (handler (assoc request :path-params path-params))
        not-found-response))))

(defn routing-handler
  "Same as `routing-handler*` but wraps with
  `s-exp.legba.middleware/wrap-error-response` middleware turning exceptions
  into nicely formatted error responses"
  [routes schema & {:as opts}]
  (-> (routing-handler* routes schema opts)
      m/wrap-error-response))

(ex/derive ::invalid :exoscale.ex/invalid)
(ex/derive ::handler-undefined :exoscale.ex/fault)
