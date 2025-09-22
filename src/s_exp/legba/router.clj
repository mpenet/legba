(ns s-exp.legba.router
  (:require [clojure.string :as str]))

;; Adapted from https://github.com/tonsky/clj-simple-router/tree/main
;; Copyright 2023 Nikita Prokopov
;; Licensed under MIT License.

;; This is basically simpler router with modified behavior so that instead of
;; matching on * it matches on named parameters

(defn- compare-masks [as bs]
  (let [a (first as)
        b (first bs)]
    (cond
      (= nil as bs) 0
      (= nil as) -1
      (= nil bs) 1
      (= "*" a b) (recur (next as) (next bs))
      (= "*" a) 1
      (= "*" b) -1
      (and (symbol? a) (symbol? b))
      (recur (next as) (next bs))
      (symbol? a) 1
      (symbol? b) -1
      :else (recur (next as) (next bs)))))

(defn pattern-mask
  [m]
  (some-> (re-matches #"^\{(\S+)}$" m)
          second
          symbol))

(defn split-route-eduction
  [path]
  (eduction (keep #(when-not (str/blank? %)
                     (str/trim %)))
            (str/split path #"/+")))

(defn- split-req
  [{:as _request :keys [request-method uri]}]
  (into [request-method] (split-route-eduction uri)))

(defn- split-route [[method path :as _route]]
  (into [method]
        (map (fn [mask]
               (or (pattern-mask mask)
                   mask)))
        (split-route-eduction path)))

(defn make-matcher
  "Given set of routes, builds matcher structure. See `router`"
  [routes]
  (->> routes
       (map (fn [[mask v]] [(split-route mask) v]))
       (sort-by first compare-masks)))

(defn- matches? [mask path]
  (loop [mask mask
         path path
         params {}]
    (let [m (first mask)
          p (first path)]
      (cond
        (= "*" m) (if path
                    (assoc params :* (str/join "/" path))
                    params)
        (= nil mask path) params
        (= nil mask) nil
        (= nil path) nil
        (symbol? m)
        (recur (next mask) (next path) (assoc params (keyword m) p))
        (= m p)
        (recur (next mask) (next path) params)))))

(defn- match-impl [matcher path]
  (reduce (fn [_ [mask v]]
            (when-some [params (matches? mask path)]
              (reduced [v params])))
          nil
          matcher))

(defn match
  [matcher request]
  (match-impl matcher (split-req request)))

(defn router
  "Creates a router that matches by method/path for a given `schema`.
  `extra-routes` can be passed to add non openapi centric routes to the routing
  table"
  [{:as _schema :keys [openapi-schema]} openapi-handlers
   & {:as _opts :keys [extra-routes]
      :or {extra-routes {}}}]
  (make-matcher
   (into extra-routes
         (for [[path methods] (get openapi-schema "paths")
               [method _parameters] methods
               :let [k [(keyword method) path]]]
           [k (get openapi-handlers k)]))))
