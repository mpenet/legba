(ns s-exp.legba.router
  (:require [reitit.core :as r]))

(defn router
  "Creates a reitit router by method/path"
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
                [(keyword method)
                 path
                 {:path path
                  :method method
                  :handler
                  ((promise) openapi-handler)}]))

      (update-vals (fn [routes]
                     (r/router (merge routes extra-routes)
                               {:syntax :bracket})))))

(defn match-route
  "Matches `method` `path` on `router`"
  [router method path]
  (when-let [r (get router method)]
    (when-let [{:as _match :keys [data path-params]} (r/match-by-path r path)]
      (cond-> data
        (seq path-params)
        (assoc :path-params (update-keys path-params keyword))
        :then
        (update :handler deref)))))
