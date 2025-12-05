(ns s-exp.legba.router
  (:require [s-exp.appia :as appia]))

(def match appia/match)

(defn router
  "Creates a router that matches by method/path for a given `schema`.
  `extra-routes` can be passed to add non openapi centric routes to the routing
  table"
  [handlers & {:as _opts :keys [extra-routes]
               :or {extra-routes {}}}]
  (appia/router (into extra-routes handlers)))
