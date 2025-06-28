(ns s-exp.legba.validator
  "Clojure native json-schema (draft2020-12) validator. Assumes the schema was
  expanded/resolved first"
  (:require [clojure.set :as set]))

;; TODO
;; * if-then-else
;; * ensure :path is always updated and default correct
;; * contentMediaType and contentEncoding
;; * normalize error messages (maybe even allow then to be customized via opts
;; * finish coercion
;; * use official test suite to generate tests:
;;  https://github.com/json-schema-org/JSON-Schema-Test-Suite/tree/main/tests/draft2020-12

(def base-ctx
  {:path []
   :errors []})

(defn result
  ([x errors]
   (assert (or (nil? errors)
               (sequential? errors)))
   (cond-> {:val x}
     (seq errors)
     (assoc :errors errors)))
  ([x]
   (result x nil)))

(defn error?
  [result]
  (some-> result :errors seq))

(defn merge-results
  [r1 & rs]
  (update r1
          :errors
          (fn [errors results]
            (into (or errors [])
                  (comp (keep
                         (fn [result]
                           (not-empty (:errors result))))
                        cat)
                  results))
          rs))

(defn error
  [msg type path & [data]]
  (merge {:msg msg
          :type type
          :path path}
         data))

(def formatter nil)
(defmulti formatter (fn [spec x] (:format spec)))
(defmethod formatter "keyword"
  [_ x]
  (keyword x))

(defmethod formatter :default
  [_ x]
  x)

(def schema-keys #{:enum :const :not :allOf :anyOf :oneOf :properties})
(def validate* nil)
(defmulti validate*
  (fn [ctx {:as spec :keys [type]} _x]
    (assert (map? ctx) "Invalid context")
    (assert (map? spec) "Invalid spec")
    (or (if (sequential? type)
          :types
          type)
        (some #(when (contains? schema-keys %)
                 %)
              (keys spec)))))

(defmethod validate* "null"
  [ctx spec x]
  (if (nil? x)
    (result x)
    (result x [(error "Invalid Null"
                      :com.s-exp.legba.validator.error/null
                      (:path ctx)
                      {:val x :spec spec})])))

;; (validate* base-ctx {:type "null"} 1)

(defmethod validate* :types
  [ctx {:keys [type] :as _spec} x]
  (some (fn [spec]
          (validate* ctx spec x))
        type))

(defmethod validate* :const
  [ctx {:keys [const] :as spec} x]
  (if (= x const)
    (result x)
    (result x [(error "Invalid value for Const"
                      :com.s-exp.legba.validator.error/const
                      (:path ctx)
                      {:val x
                       :spec spec})])))

;; (validate* base-ctx {:const 1} 2)

(defmethod validate* :enum
  [ctx {:keys [enum]} x]
  (if (contains? (set enum) x)
    (result x)
    (result x [(error
                "Invalid enum value"
                :com.s-exp.legba.validator.error/invalid-enum-value
                (:path ctx)
                {:val x
                 :spec {:enum enum}})])))

;; (validate* base-ctx {:enum [1 2]} 1)

(defmethod validate* "boolean"
  [ctx spec x]
  (if (boolean? x)
    (result x)
    (result [(error "Invalid value for boolean"
                    :com.s-exp.legba.validator.error/boolean
                    (:path ctx)
                    {:val x :spec spec})])))

;; (validate* base-ctx {:type "boolean"} true)

(validate {:type "string" :format "keyword"} "a")

(defmethod validate* "string"
  [ctx {:keys [minLength maxLength pattern]
        :as spec} x]
  (if (string? x)
    (result (formatter spec x)
            (cond-> []
              (and minLength (< (count x) minLength))
              (conj (error "String too short"
                           :com.s-exp.legba.validator.error/string-min-length
                           (:path ctx)
                           {:spec spec :val x}))

              (and maxLength (> (count x) maxLength))
              (conj (error "String too long"
                           :com.s-exp.legba.validator.error/string-max-length
                           (:path ctx)
                           {:spec spec :val x}))

              (and pattern (not (re-find (re-pattern pattern) x)))
              (conj (error "String doesn't match pattern"
                           :com.s-exp.legba.validator.error/string-pattern
                           (:path ctx)
                           {:spec spec :val x}))))
    (result x [(error "Invalid value for string"
                      :com.s-exp.legba.validator.error/string
                      (:path ctx)
                      {:val x :spec spec})])))

;; (validate* base-ctx {:type "string" :minLength 1 :pattern "a.*"} "")

(defn- validate-required
  [ctx required x]
  (when required
    (let [diff (set/difference (set required)
                               (set (keys x)))]
      (when-not (empty? diff)
        (result x
                [(error "Missing required keys"
                        :com.s-exp.legba.validator.error/missing-required-key
                        ctx
                        {:missing diff})])))))

(defn- validate-properties
  [ctx {:as _spec :keys [properties additionalProperties
                         patternProperties]} x]
  (let [path (:path ctx)
        ptn-props (update-keys patternProperties (fn [k] (re-pattern k)))]
    (reduce (fn [result' [k x]]
              (let [spec (or (get properties k)
                             (some (fn [[ptn spec]]
                                     (when (re-find ptn (name k))
                                       spec))
                                   ptn-props))
                    ctx (assoc ctx :path (conj path k))]
                (if spec
                  (merge-results result' (validate* ctx spec x))
                  (cond
                    (map? additionalProperties)
                    (merge-results result' (validate* ctx additionalProperties x))
                    (false? additionalProperties)
                    (merge-results result'
                                   (result x
                                           [(error "Found extra key when additionalProperties are not allowed"
                                                   :com.s-exp.legba.validator.error/extra-property
                                                   (:path ctx)
                                                   {:spec _spec
                                                    :key k
                                                    :val x})]))
                    :else
                    result'))))
            (result x)
            x)))

(defn validate-dependent-required
  [ctx dependentRequired x]
  (result x
          (seq (for [[depending dependent-keys] dependentRequired
                     :let [entry (find x depending)]
                     :when entry
                     dependent-key dependent-keys
                     :let [error (when-not (find x dependent-key)
                                   (error (str "Missing dependent key " dependent-key)
                                          :com.s-exp.legba.validator.error/dependent-key
                                          (:path ctx)
                                          {:spec dependentRequired
                                           :dependent-key dependent-key
                                           :val x}))]
                     :when error]
                 error))))
;; https://json-schema.org/understanding-json-schema/reference/conditionals
(defn validate-dependent-schemas
  [ctx dependentSchemas x]
  (result x
          (seq (for [[depending dependent-schema] dependentSchemas
                     :let [entry (find x depending)]
                     :when entry
                     error (:errors (validate* ctx dependent-schema x))]
                 error))))

;; (validate* base-ctx
;;           {:type "object"
;;            :properties {:a {:type "string"}}
;;            :dependentSchemas {:a {:properties {:c {:type "string"}}}}}
;;           {:a "1" :c 1})

;; (validate* {}
;;            {:properties {:c {:type "string"}}}
;;            {:a 1, :c 1, :d 1})

;; (validate* base-ctx
;;            {:type "object"
;;             :properties {:a {:type "string"}}}
;;            {"a" 1 "c" "asdf" "d" 1})

;; ;; (validate {} {:type "object" :properties {:a {:type "integer"}} :patternProperties {"S_*" {:type "string"}}} {:a 1 :S_1 "1"})

;; (validate* base-ctx {:type "object"
;;                      :additionalProperties false
;;                      ;; :patternProperties {"b*" {:type "string"}}
;;                      :properties {:a {:type "string"}}} {:a 1 :b 1})

;; (validate* base-ctx {:type "object"
;;                      :additionalProperties {:type "string"}
;;                      :properties {:a {:type "string"}}} {:a 1 :b 1})

;; (validate {:type "object"
;;            :required [:a]
;;            :additionalProperties {:type "string"}
;;            :properties {:a {:type "string"}}} {:b 1
;;                                                :c "1"})

;; (validate* base-ctx {:type "string"} 1)

(defmethod validate* :properties
  [ctx spec x]
  (validate-properties ctx spec x))

(defmethod validate* "object"
  [ctx {:as spec :keys [required dependentRequired dependentSchemas]} x]
  (if (map? x)
    (merge-results (result x)
                   (validate-required ctx required x)
                   (validate-properties ctx spec x)
                   (validate-dependent-required ctx dependentRequired x)
                   (validate-dependent-schemas ctx dependentSchemas x))
    (result x [(error "Expecting a map"
                      :com.s-exp.legba.validator.error/expecting-map
                      (:path ctx)
                      {:val x :spec spec})])))

;; validate* returns a result of {:val ?:errors }

;; (validate {} {:type "integer"} "1")

(defn- validate-num-range
  [ctx {:as spec
        :keys [multipleOf
               minimum maximum
               exclusiveMaximum exclusiveMinimum]} x]
  (result x
          (cond-> []
            (and multipleOf
                 (rem x multipleOf))
            (conj (error "Not a valid multiple-of "
                         :com.s-exp.legba.validator.error/num-multiple-of
                         (:path ctx)
                         {:val x :spec spec}))

            (and minimum (not (>= x minimum)))
            (conj (error "Value under minimum"
                         :com.s-exp.legba.validator.error/num-range-minimum
                         (:path ctx)
                         {:val x :spec spec}))

            (and maximum (not (<= x maximum)))
            (conj (error "Value over maximum"
                         :com.s-exp.legba.validator.error/num-range-maximum
                         (:path ctx)
                         {:val x :spec spec}))

            (and exclusiveMinimum (not (> x exclusiveMinimum)))
            (conj (error "Value under exclusiveMinimum"
                         :com.s-exp.legba.validator.error/num-range-exclusive-minimum
                         (:path ctx)
                         {:val x :spec spec}))

            (and exclusiveMaximum (not (< x exclusiveMaximum)))
            (conj (error "Value over exclusiveMaximum"
                         :com.s-exp.legba.validator.error/num-range-exclusive-maximum
                         (:path ctx)
                         {:val x :spec spec})))))

(defmethod validate* "integer"
  [ctx {:keys [_format] :as spec} x]
  (if (integer? x)
    (merge-results (result x) (validate-num-range ctx spec x))
    (result x
            [(error "Invalid Integer"
                    :com.s-exp.legba.validator.error/integer
                    (:path ctx)
                    {:val x :spec spec})])))

;; (validate* base-ctx {:type "integer"
;;                      :minimum 2}
;;            "s")

(defmethod validate* "number"
  [ctx spec x]
  (if (number? x)
    (merge-results (result x) (validate-num-range ctx spec x))
    (result x
            [(error "Invalid Number"
                    :com.s-exp.legba.validator.error/number
                    (:path ctx)
                    {:val x :spec spec})])))

;; (validate* {} {:type "number"} "a")

(defn validate-array-length
  [ctx {:as spec
        :keys [minItems maxItems prefixItems]} xs]
  (let [xs-len (count xs)]
    (result xs
            (cond-> []
              (and prefixItems (> (count prefixItems) xs-len))
              (conj (error "Array too short, prefixLength > size"
                           :com.s-exp.legba.validator.error/array-prefix-items
                           (:path ctx)
                           {:val xs :spec spec}))

              (and maxItems (> xs-len maxItems))
              (conj (error "Array too large"
                           :com.s-exp.legba.validator.error/array-max-items
                           (:path ctx)
                           {:val xs :spec spec}))

              (and minItems (< xs-len minItems))
              (conj (error "Array too small"
                           :com.s-exp.legba.validator.error/array-min-items
                           (:path ctx)
                           {:val xs :spec spec}))))))

(defn validate-array-uniqueness
  [ctx {:as spec :keys [uniqueItems]} xs]
  (result xs
          (cond-> []
            (and uniqueItems
                 (not= (count xs)
                       (count (distinct xs))))
            (conj (error "Array values not distinct"
                         :com.s-exp.legba.validator.error/array-distinct
                         (:path ctx)
                         {:val xs :spec spec})))))

(defn validate-array-contains
  [ctx {:as spec
        :keys [contains minContains maxContains]
        :or {minContains 1}}
   xs]
  (if contains
    (let [path (:path ctx)
          matches (count (filter
                          (fn [x]
                            (not (seq (:errors (validate* (assoc ctx :path (conj path -1)) contains x)))))
                          xs))]
      (result xs
              (cond-> []
                (< matches minContains)
                (conj (error "Not enough values according to `minContains`"
                             :com.s-exp.legba.validator.error/min-contains
                             (:path ctx)
                             {:matches matches :val xs :spec spec}))

                (and maxContains (> matches maxContains))
                (conj (error "Too many values according to `maxContains`"
                             :com.s-exp.legba.validator.error/max-contains
                             (:path ctx)
                             {:matches matches :val xs :spec spec})))))
    (result xs)))

;; (validate base-ctx {:type "array" :contains {:type "integer"} :maxContains 1} [1 1 "_"])

(defn validate-array-items
  [ctx {:as spec
        :keys [items prefixItems unevaluatedItems]}
   xs]
  (let [thing-len (fn [x]
                    (cond
                      (false? x) 0
                      (seq x) (count x)
                      :else 0))
        path (:path ctx)
        xs-len (count xs)
        items-len (thing-len items)
        prefix-items-len (thing-len prefixItems)]
    (reduce (fn [result' [idx x]]
              (let [ctx (assoc ctx :path (conj path idx))]
                (cond
                  (< idx prefix-items-len)
                  (merge-results result'
                                 (validate* ctx
                                            (nth prefixItems idx)
                                            x))
                  ;; (unevaluated)items=false and over idx
                  (and (>= idx items-len)
                       (or (false? unevaluatedItems)
                           (false? items)))
                  (merge-results result'
                                 (result xs
                                         [(error "No extra array items allowed"
                                                 :com.s-exp.legba.validator.error/array-no-extra-item
                                                 (:path ctx)
                                                 {:val x :spec spec})]))

                  ;; (unevaluated)items!=false and over idx
                  (and (>= idx prefix-items-len)
                       (or items unevaluatedItems))
                  ;; TODO items vs unevaluatedItems diff
                  (let [spec (or items unevaluatedItems)]
                    (merge-results result' (validate* ctx spec x))))))
            (result xs)
            (->> (interleave (range xs-len)
                             xs)
                 (partition-all 2)))))

;; (validate* {} {:type "array" :prefixItems [{:type "integer"} {:type "string"}]
;;                :unevaluatedItems false} [1 1])

;; (validate* {} {:type "array" :prefixItems [{:type "integer"} {:type "string"}]
;;                :unevaluatedItems false} [1])

;; (validate* {} {:type "array" :prefixItems [{:type "integer"}
;;                                            {:type "string"}]} [1 "2"])

;; (validate* base-ctx {:type "array" :items {:type "string"}} [])

;; allOf: (AND) Must be valid against all of the subschemas
    ;; anyOf: (OR) Must be valid against any of the subschemas
    ;; oneOf: (XOR) Must be valid against exactly one of the subschemas
    ;; not: (NOT) Must not be valid against the given schema
(defmethod validate* :not
  [ctx spec x]
  (if (seq (:errors (validate* ctx (:not spec) x)))
    (result x)
    (result x
            [(error "Shouldn't match negation"
                    :com.s-exp.legba.validator.error/not
                    (:path ctx)
                    {:spec spec
                     :val x})])))

(defmethod validate* :allOf
  [ctx {:as specs :keys [allOf]} x]
  (reduce (fn [result' spec]
            (merge-results result' (validate* ctx spec x)))
          (result x)
          allOf))

(defmethod validate* :oneOf
  [ctx {:as specs :keys [oneOf]} x]
  ;; oneOf: (XOR) Must be valid against exactly one of the subschemas
  (let [matches (count (filter (fn [spec]
                                 (seq (:errors (validate* ctx spec x))))
                               oneOf))]
    (cond
      (= 1 matches)
      (result x)

      (zero? matches)
      (result x [(error "No match with oneOf"
                        :com.s-exp.legba.validator.error/one-of
                        (:path ctx)
                        {:val x :spec oneOf})])

      (> matches 1)
      (result x [(error "More than one value match with oneOf"
                        :com.s-exp.legba.validator.error/one-of
                        (:path ctx)
                        {:val x :spec oneOf :matches matches})]))))

(defmethod validate* :anyOf
  [ctx {:as specs :keys [anyOf]} x]
  (reduce (fn [result' spec]
            (if (empty? (:errors (validate* ctx spec x)))
              (reduced (result x))
              result'))
          (result x [(error "No match with anyOf"
                            :com.s-exp.legba.validator.error/any-of
                            (:path ctx)
                            {:val x :spec anyOf})])
          anyOf))

;; (validate* base-ctx {:anyOf [{:type "string"} {:type "string"}]} "")
;; (validate* base-ctx {:oneOf [{:type "string"} {:type "string"}]} 1)

;; (validate-array-items {} {:items {:type "string"}} [])
;; (validate-array-items {} {:items {:type "string"}} ["a" 1])
;; (validate-array-items {} {:items {:type "string"}} ["a" "d"])
;; (validate-array-items {} {:prefixItems [{:type "string"} {:type "string"}]
;;                           :items {:type "boolean"}
;;                           ;; :unevaluatedItems {:type "boolean"}
;;                           }
;;                       ["a" "d" true])

;; (validate-array-items {} {:prefixItems [{:type "string"} {:type "string"}]
;;                           :items false
;;                           ;; :unevaluatedItems {:type "boolean"}
;;                           }
;;                       ["a" "d" true])

;; (validate-array-items {} {:prefixItems [{:type "string"} {:type "string"}]
;;                           :items false
;;                           ;; :unevaluatedItems {:type "boolean"}
;;                           }
;;                       ["a" "d" ""])

;; (let [xs [1 2]]
;;   (interleave (range (count xs))
;;               xs))

(defmethod validate* "array"
  [ctx spec xs]
  (if (empty? xs)
    (result xs)
    (merge-results
     (result xs)
     (validate-array-length ctx spec xs)
     (validate-array-items ctx spec xs)
     (validate-array-uniqueness ctx spec xs)
     (validate-array-contains ctx spec xs))))

;; (validate
;;  {:type "array"
;;             ;; :items {:type "string"}
;;   :contains {:type "string"}
;;   :maxContains 3}
;;  [1 "1" "1" "1" "1"])
(validate* {} {:type "array" :items {:type "integer" :coerce :string}} ["" ""])
(validate {:type "array" :items {:type "string" :format "keyword"}} ["a" "b"])

(defmethod validate* :default
  [ctx spec x]
  (throw (ex-info "unimplemented" {:ctx ctx :spec spec :x x})))

(defn validate
  "Validates `x` against json-schema, returns a `result` map with potential `:errors`"
  ([schema x]
   (validate schema x nil))
  ([schema x _opts]
   (validate* base-ctx schema x)))

(defn conform
  "Validates `x` against `json-schema`, returns a potentially modified `x` after
  validation/coercion. Upon encountering validation errors throws an ExceptionInfo/:type=::validation-failure"
  ([schema x]
   (conform schema x nil))
  ([schema x _opts]
   (let [result (validate* base-ctx schema x)]
     (when (error? result)
       (throw (ex-info "Validation failed"
                       (merge {:type :s-exp.legba.validator/validation-failure}
                              result)))))))
