(ns test-runner
  (:gen-class)
  (:require [clojure.test]
            eftest.report.pretty
            [eftest.runner :as ef]))

(def default-options
  {:dir "test"
   :selector (constantly true)
   :capture-output? false
   :fail-fast? false
   :multithread? false
   :reporters [eftest.report.pretty/report]})

(defn sort-vars
  [vars]
  (sort-by (fn [var]
             (let [{:keys [ns line]} (meta var)]
               [(str ns) line]))
           vars))

(defn- ret->exit-code
  [{:as _ret :keys [error fail]}]
  (System/exit
   (cond
     (and (pos? fail) (pos? error)) 30
     (pos? fail) 20
     (pos? error) 10
     :else 0)))

(defn combined-reporter
  "Combines the reporters by running first one directly,
  and others with clojure.test/*report-counters* bound to nil."
  [[report & rst]]
  (fn [m]
    (report m)
    (doseq [report rst]
      (binding [clojure.test/*report-counters* nil]
        (report m)))))

(defn find-tests
  [{:keys [selector dir]}]
  (->> (ef/find-tests dir)
       (filter #(-> % meta selector))
       sort-vars))

(defn setup-options
  [opts]
  (let [{:as opts :keys [reporters]} (merge default-options opts)]
    (cond-> opts
      (seq reporters)
      (assoc :report (combined-reporter reporters)))))

(defn run
  [options]
  (let [options (setup-options options)]
    (-> (find-tests options)
        (ef/run-tests options)
        ret->exit-code)))

#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn run-unit
  [options]
  (run (assoc options
              :selector
              (complement :integration))))

#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn run-integation
  [options]
  (run (assoc options :selector :integration)))
