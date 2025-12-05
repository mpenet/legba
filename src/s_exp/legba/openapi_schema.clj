(ns s-exp.legba.openapi-schema
  (:require [exoscale.ex :as ex]
            [s-exp.legba.json :as json]
            [s-exp.legba.json-pointer :as json-pointer]
            [s-exp.legba.json-schema :as json-schema])
  (:import (com.fasterxml.jackson.databind JsonNode)
           (com.networknt.schema
            Schema
            SchemaRegistry
            SchemaRegistry$Builder
            SchemaRegistryConfig
            InputFormat
            OutputFormat
            SchemaLocation)
           (com.networknt.schema.dialect Dialects)
           (com.networknt.schema.path PathType)
           (com.networknt.schema.resource SchemaIdResolvers$Builder)))

(set! *warn-on-reflection* true)

(defn schema->routes-schema
  [{:as _schema :keys [openapi-schema]}]
  (into {}
        (for [[path methods] (get openapi-schema "paths")
              [method sub-schema] methods
              :when (#{"delete" "get" "head" "options" "patch" "post" "put" "trace"} method)]
          {[(keyword method) path] sub-schema})))

(def schema-registry-config
  "Default reusable `SchemaRegistryConfig` instance.

  This configures the validator to:
  - Enable JSON Schema reference preloading and caching
  - Enable format assertions (per the JSON Schema spec)
  - Set maximum reference nesting depth to 40
  - Handle `nullable` fields correctly
  - Use `JSON_PATH` for error paths in validation results

  This config can be reused for schema load/validation for performance and consistency."
  (let [b (SchemaRegistryConfig/builder)]
    (doto b
      (.preloadSchema true)
      (.cacheRefs true)
      (.typeLoose true)
      (.formatAssertionsEnabled true)
      (.pathType PathType/JSON_PATH))
    (.build b)))

(defn validate-schema!
  "Validates a user-provided JSON schema against the OpenAPI 3.1 base schema.

  Parameters:
  - schema-registry: An instance of SchemaRegistry containing preloaded schemas.
  - schema-uri: URI identifying the schema to validate (String).

  Throws:
  - ex-incorrect! if the provided schema does not conform to the OpenAPI specification.
  - The exception contains a map of validation errors in :errors.

  This function uses the OpenAPI 3.1 base schema loaded from the local classpath
  to validate the user schema and reports any problems found in a structured way."
  [^SchemaRegistry schema-registry schema-uri]
  (let [registry (SchemaRegistry/withDefaultDialect
                  com.networknt.schema.SpecificationVersion/DRAFT_2020_12
                  (fn [^SchemaRegistry$Builder builder]
                    (.schemaIdResolvers
                     builder
                     (fn [^SchemaIdResolvers$Builder schema-id-resolvers]
                       (.mapPrefix schema-id-resolvers
                                   "https://spec.openapis.org/oas/3.1"
                                   "classpath:legba/oas/3.1")))))
        openapi-schema (.getSchema
                        registry
                        (SchemaLocation/of "https://spec.openapis.org/oas/3.1/schema-base/2022-10-07"))
        user-schema (.getSchemaNode (.getSchema
                                     schema-registry
                                     (SchemaLocation/of schema-uri)))
        errors (-> openapi-schema
                   (.validate user-schema OutputFormat/RESULT)
                   json-schema/validation-result)]
    (when (seq errors)
      (ex/ex-incorrect! "Schema invalid" {:errors errors}))))

(defn get-schema
  "Returns json-schema from schema-registry at `schema-uri`/`json-pointer`"
  ^Schema
  [^SchemaRegistry schema-registry schema-uri json-pointer]
  (.getSchema schema-registry
              (.resolve (SchemaLocation/of schema-uri)
                        (str "#" json-pointer))))

(defn load-schema
  "Loads JSON or YAML schema from `schema-uri` and returns
  map (of :openapi-schema, :schema-uri, :schema-registry) that contains all the
  necessary information to perform `validate!` calls later (minus a JSON
  pointer)."
  [^String schema-uri & {:as _opts :keys [validate-schema]
                         :or {validate-schema true}}]
  (let [schema-registry (SchemaRegistry/withDialect
                         (Dialects/getOpenApi31)
                         (fn [^SchemaRegistry$Builder builder]
                           (doto builder
                             (.schemaCacheEnabled true)
                             (.schemaRegistryConfig schema-registry-config))))
        _ (when validate-schema
            (validate-schema! schema-registry schema-uri))
        openapi-schema (-> schema-registry
                           (get-schema schema-uri "")
                           .getSchemaNode
                           (json/json-node->clj {:key-fn identity})
                           (json-pointer/annotate-tree))]
    {:openapi-schema openapi-schema
     :schema-uri schema-uri
     :schema-registry schema-registry}))

(defn validate
  "Validates a `val` against `schema`"
  [{:as _schema :keys [schema-uri schema-registry]}
   sub-schema val
   & {:as _opts
      :keys [validation-result]
      :or {validation-result json-schema/validation-result}}]
  (let [ptr (:json-pointer (meta sub-schema))
        schema (get-schema schema-registry schema-uri ptr)]
    (validation-result
     (if (instance? JsonNode val)
       (.validate schema ^JsonNode val
                  OutputFormat/RESULT)
       (.validate schema (or ^String val "")
                  InputFormat/JSON
                  OutputFormat/RESULT)))))
