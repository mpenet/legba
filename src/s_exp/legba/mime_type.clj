(ns s-exp.legba.mime-type)

(defn- parse-mime-type
  [s]
  (next (re-find #"(?i)([a-z0-9\+\-\.\*]+)\/([a-z0-9\+\-\.\*]+)" s)))

(def ^:private memo-parse-mime-type
  (memoize parse-mime-type))

(defn match-mime-type?
  [mime-type-ptn mime-type]
  (or (= mime-type-ptn mime-type)
      (= mime-type-ptn "*")
      (= mime-type-ptn "*/*")
      (let [[ptn-mime-ns ptn-mime-val] (memo-parse-mime-type mime-type-ptn)
            [mime-ns mime-val] (parse-mime-type mime-type)]
        (and (or (= "*" ptn-mime-ns)
                 (= mime-ns ptn-mime-ns))
             (or (= "*" ptn-mime-val)
                 (= mime-val ptn-mime-val))))))

(defn match-schema-mime-type
  "Matches `content-type` with `schema`, return resulting `sub-schema`"
  [schema content-type]
  (let [schema-mime-types (get schema "content")]
    (reduce (fn [_ [schema-mime-type-key schema-mime-type-val]]
              (when (match-mime-type? schema-mime-type-key content-type)
                (reduced (get schema-mime-type-val "schema"))))
            nil
            schema-mime-types)))
