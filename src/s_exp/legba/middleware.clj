(ns s-exp.legba.middleware
  (:require [exoscale.ex :as ex]))

(defmulti ex->response
  #(some-> % ex/ex-type)
  :hierarchy ex/hierarchy)

(defmethod ex->response
  :s-exp.legba/invalid
  [e]
  (let [data (ex-data e)]
    {:status 400
     :content-type "application/json"
     :body (assoc data :message (ex-message e))}))

(defn wrap-error-response
  [handler]
  (fn [req]
    (ex/try+
      (handler req)
      #_{:clj-kondo/ignore [:unresolved-symbol]}
      (catch :exoscale.ex/invalid _
        #_{:clj-kondo/ignore [:unresolved-symbol]}
        (ex->response &ex)))))
