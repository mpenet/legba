(ns s-exp.legba.router
  (:require [exoscale.ex :as ex]
            [reitit.core :as r]))

(defn router
  "Creates a reitit path router by method"
  [{:as _schema :keys [openapi-schema]} openapi-handlers & {:as _opts :keys [extra-routes]}]
  (-> (reduce (fn [routers-m [method & route]]
                (update routers-m
                        method
                        (fnil conj [])
                        (vec route)))
              {}
              (for [[path methods] (get openapi-schema "paths")
                    [method _parameters] methods
                    :let [method (keyword method)
                          openapi-handler (get openapi-handlers [method path])]]

                (do
                  (when-not openapi-handler
                    (ex/ex-incorrect! (format "Missing route definition in handlers for %s %s"
                                              (name method) path)))
                  [(keyword method)
                   path
                   {:path path
                    :method method
                    :handler
                    ((promise) openapi-handler)}])))

      (update-vals (fn [routes]
                     (r/router (merge routes extra-routes)
                               {:syntax :bracket})))))

(defn match-route
  [router method path]
  (when-let [r (get router method)]
    (when-let [{:as _match :keys [data path-params]} (r/match-by-path r path)]
      (cond-> data
        (seq path-params)
        (assoc :path-params (update-keys path-params keyword))
        :then
        (update :handler deref)))))
