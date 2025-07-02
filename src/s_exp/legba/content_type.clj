(ns s-exp.legba.content-type
  (:require
   [clojure.string :as str]))

(defn- match?
  [ptn s]
  (loop [[ptn0 & ptn :as rptn] ptn
         [s0 & s :as rs] s]
    (cond
      (and (zero? (count ptn))
           (zero? (count s)))
      true

      (= ptn0 s0)
      (recur ptn s)

      (and (= ptn0 \*) s0)
      (or
       (match? ptn rs)
       (recur rptn s)))))

(defn match-schema-content-type
  [schema content-type]
  (let [content (get schema "content")
        content-types (some-> content-type (str/split #";" 1))]
    (reduce (fn [_ content-type]
              (when-let [ret (or (get-in content [content-type "schema"])
                                 (reduce (fn [_ [ct-key ct-val]]
                                           (when (match? ct-key content-type)
                                             (reduced (get ct-val "schema"))))
                                         nil
                                         content))]
                (reduced ret)))
            nil
            content-types)))
