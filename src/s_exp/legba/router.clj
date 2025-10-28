(ns s-exp.legba.router
  (:require [s-exp.appia :as router]))

(defn router
  "Creates a router that matches by method/path for a given `schema`.
  `extra-routes` can be passed to add non openapi centric routes to the routing
  table"
  [{:as _schema :keys [openapi-schema]} openapi-handlers
   & {:as _opts :keys [extra-routes]
      :or {extra-routes {}}}]
  (router/matcher
   (into extra-routes
         (for [[path methods] (get openapi-schema "paths")
               [method _parameters] methods
               :let [k [(keyword method) path]]]
           [k (get openapi-handlers k)]))))

