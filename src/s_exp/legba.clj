(ns s-exp.legba
  (:require [exoscale.ex :as ex]
            [s-exp.legba.handler :as handler]
            [s-exp.legba.middleware :as m]
            [s-exp.legba.router :as router]
            [s-exp.legba.schema :as schema]))

(def default-options
  "Default options used by openapi-handler"
  {:not-found-response {:status 404 :body "Not found"}
   :key-fn keyword
   :query-string-params-key :params})

(defn openapi-handler*
  "Takes a map of routes as [method path] -> ring-handler, turns them into a map
  of routes to openapi handlers then creates a handler that will dispatch on the
  appropriate openapi handler from a potential router match. If not match is
  found, returns `not-found-response` (opts)"
  [routes & {:as opts}]
  (let [{:as opts :keys [schema not-found-response]}
        (merge default-options opts)
        schema (schema/load-schema schema)
        openapi-routes (handler/openapi-routes routes schema opts)
        router (router/router schema openapi-routes opts)]
    (fn [{:as request :keys [request-method uri]}]
      (if-let [{:as _match :keys [handler path-params]}
               (router/match-route router request-method uri)]
        (handler (assoc request :path-params path-params))
        not-found-response))))

(defn openapi-handler
  "Same as `openapi-handler*` but wraps with
  `s-exp.legba.middleware/wrap-error-response` middleware turning exceptions
  into nicely formatted error responses"
  [handlers & {:as opts}]
  (-> (openapi-handler* handlers opts)
      m/wrap-error-response))

(ex/derive ::invalid :exoscale.ex/invalid)
(ex/derive ::handler-undefined :exoscale.ex/fault)
