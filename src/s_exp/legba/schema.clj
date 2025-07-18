(ns s-exp.legba.schema
  (:require [s-exp.legba.json :as json]
            [s-exp.legba.json-pointer :as json-pointer])
  (:import (com.fasterxml.jackson.databind JsonNode)
           (com.networknt.schema JsonSchemaFactory
                                 JsonSchemaFactory$Builder
                                 JsonSchema
                                 SchemaValidatorsConfig
                                 InputFormat
                                 OutputFormat
                                 SchemaLocation)
           (com.networknt.schema ValidationResult
                                 ValidationMessage
                                 SpecVersion$VersionFlag
                                 PathType)
           (com.networknt.schema.oas OpenApi31)))

(set! *warn-on-reflection* true)

(def schema-validator-config
  (doto (SchemaValidatorsConfig.)
    (.setPreloadJsonSchema true)
    (.setCacheRefs true)
    (.setFormatAssertionsEnabled true)
    (.setPreloadJsonSchemaRefMaxNestingDepth 40)
    (.setHandleNullableField true)
    (.setOpenAPI3StyleDiscriminators true)
    (.setPathType PathType/JSON_PATH)))

(defn get-schema
  "Returns json-schema from json-schema-factory at `schema-uri`/`json-pointer`"
  ^JsonSchema
  [^JsonSchemaFactory json-schema-factory schema-uri json-pointer]
  (.getSchema json-schema-factory
              (.resolve (SchemaLocation/of schema-uri)
                        (str "#" json-pointer))
              ^SchemaValidatorsConfig schema-validator-config))

(defn load-schema
  "Loads JSON or YAML schema from `schema-uri` and returns
  map (of :openapi-schema, :schema-uri, :json-schema-factory) that contains all
  the necessary information to perform `validate!` calls later (minus a JSON
  pointer)."
  [^String schema-uri]
  (let [schema-factory (JsonSchemaFactory/getInstance
                        SpecVersion$VersionFlag/V202012
                        (fn [^JsonSchemaFactory$Builder builder]
                          (doto builder
                            (.metaSchema (OpenApi31/getInstance))
                            (.defaultMetaSchemaIri (.getIri (OpenApi31/getInstance)))
                            (.enableSchemaCache true))))
        openapi-schema (-> schema-factory
                           (get-schema schema-uri "")
                           .getSchemaNode
                           (json/json-node->clj {:key-fn identity})
                           (json-pointer/annotate-tree))]
    {:openapi-schema openapi-schema
     :schema-uri schema-uri
     :json-schema-factory schema-factory}))

(defn validation-result
  "Default validation result output function, can be overidden via
  `:validation-result` option of `s-exp.legba/*` calls"
  [^ValidationResult r]
  (let [vms (.getValidationMessages r)]
    (when-not (empty? vms)
      (into []
            (map (fn [^ValidationMessage m]
                   {:type (.getType m)
                    :path (.toString (.getInstanceLocation m))
                    :error (.getError m)
                    :message (.getMessage m)}))
            vms))))

(defn validate!
  "Validates a `val` against `schema`"
  [{:as _schema :keys [schema-uri json-schema-factory]}
   sub-schema val
   & {:as _opts
      :keys [validation-result]
      :or {validation-result validation-result}}]
  (let [ptr (:json-pointer (meta sub-schema))
        ^JsonSchema schema (get-schema json-schema-factory
                                       schema-uri ptr)]
    (validation-result
     (if (instance? JsonNode val)
       (.validate schema ^JsonNode val
                  OutputFormat/RESULT)
       (.validate schema (or ^String val "")
                  InputFormat/JSON
                  OutputFormat/RESULT)))))
