(ns s-exp.legba.router
  (:require [exoscale.ex :as ex]
            [reitit.core :as r]))

(defn router
  "Creates a reitit path router by method"
  [{:as _schema :keys [openapi-schema]} handlers & {:as _opts :keys [extra-routes]}]
  (-> (reduce (fn [routers-m [method & route]]
                (update routers-m
                        method
                        (fnil conj [])
                        (vec route)))
              {}
              (for [[path methods] (get openapi-schema "paths")
                    [method parameters] methods
                    :let [method (keyword method)]]
                (do
                  (when-not (get handlers [method path])
                    (ex/ex-incorrect! (format "Missing route definition in handlers for %s %s"
                                              (name method) path)))
                  [(keyword method)
                   path
                   {:path path
                    :method method
                    :sub-schema
                   ;; to stop reitit from messing with my metadata...
                   ;; TODO just replace reitit with something less crazy (bidy, simple-router?)
                    ((promise) parameters)}])))
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
        (update :sub-schema deref)))))
