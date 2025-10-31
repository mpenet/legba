(ns s-exp.legba.json-schema
  "JSON Schema validation utilities.
   Provides helpers to load, cache, and validate JSON Schemas"
  (:import (com.fasterxml.jackson.databind JsonNode)
           (com.networknt.schema Result SpecificationVersion)
           (com.networknt.schema Schema
                                 SchemaRegistry
                                 SchemaRegistry$Builder
                                 SchemaRegistryConfig
                                 InputFormat
                                 OutputFormat
                                 SchemaLocation)
           (com.networknt.schema.dialect Dialect)
           (com.networknt.schema.path PathType)))

(set! *warn-on-reflection* true)

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
      (.formatAssertionsEnabled true)
      (.pathType PathType/JSON_PATH))
    (.build b)))

(defn schema
  "Loads and builds a JSON Schema validator instance from a URI or file path.

  Arguments:
    - `schema-uri` (string): Location of the JSON Schema (file:/..., http:/...,
  classpath:/..., etc)

    - `:schema-registry-config` (optional): Custom
  `SchemaRegistryConfig` (default: this namespace's `schema-registry-config`).

  Returns:

    An instance of the JSON Schema validator (`com.networknt.schema.JsonSchema`).
    This object can be reused to validate multiple values or payloads.

  Example:
    (schema \"classpath:///tmp/foo.schema.json\")
    (schema \"file:///data/schema/bar.json\")
    (schema \"https://schemas.org/example.schema.json\")"
  [^String schema-uri
   & {:as _opts :keys [schema-registry-config]
      :or {schema-registry-config schema-registry-config}}]
  (let [schema-registry (SchemaRegistry/withDefaultDialect
                         SpecificationVersion/DRAFT_2020_12
                         (fn [^SchemaRegistry$Builder builder]
                           (doto builder
                             (.schemaCacheEnabled true)
                             (.schemaRegistryConfig schema-registry-config))))]
    (.getSchema schema-registry (SchemaLocation/of schema-uri))))

(defn validation-result
  "Extracts and formats schema validation errors from a ValidationResult object.

  Takes a NetworkNT ValidationResult and returns a vector of error maps,
  where each map contains:
    - :path     (string)   JSON path of the error location
    - :pointer  (string)   JSON pointer to the schema fragment
    - :location (string)   Instance location in the JSON document
    - :detail   (string)   Validation error message/detail

  Returns nil if there are no errors. Useful for turning validator output into
  a more consumable shape for clients, APIs, or error reporting."

  [^Result r]
  (let [errors (.getErrors r)]
    (when-not (empty? errors)
      (into []
            (map (fn [^com.networknt.schema.Error m]
                   {:path (.toString (.getEvaluationPath m))
                    :pointer (str "#" (.getFragment (.getSchemaLocation m)))
                    :location (.toString (.getInstanceLocation m))
                    :detail (.getMessage m)}))
            errors))))

(defn validate
  "Validates a value against a previously loaded or constructed schema.

  Arguments:
    - `schema`: The schema object returned from `schema`
    - `val`: The input to validate. Can be a JsonNode or a JSON string.
    - `:validation-result` (optional): Override for the function used to extract
  validation errors (defaults to this namespace's `validation-result`).

  Returns:
    A sequence of error maps (see `validation-result`), or nil if valid.

  Example:
    (validate myschema \"{\"foo\":42}\")
    (validate myschema my-jackson-json-node)"
  [^Schema schema val
   & {:as _opts
      :keys [validation-result]
      :or {validation-result validation-result}}]
  (validation-result
   (if (instance? JsonNode val)
     (.validate schema ^JsonNode val
                OutputFormat/RESULT)
     (.validate schema (or ^String val "")
                InputFormat/JSON
                OutputFormat/RESULT))))

(defn validate!
  "Validates a value against a given JSON Schema and throws if invalid.

  Arguments:
    - `schema` (JsonSchema): A schema instance created by the `schema` function.
    - `val`: The data to validate (can be a Jackson JsonNode or a JSON string).
    - Optional keyword arguments:
        - `:validation-result`: Custom function to extract validation errors
          (defaults to this namespace's `validation-result`).

  Behavior:
    - If validation succeeds (no errors), returns nil.
    - If validation fails, throws an ex-info exception with:
        :type   --> :s-exp.legba.json-schema/failed-validation
        :errors --> Sequence of error maps (see `validation-result`).
        :val    --> The input value that failed validation.

  Example:
    (validate! myschema \"{\"name\":42}\") ; throws if invalid
    (validate! myschema my-jackson-json-node)

  Useful for workflows where validation failure should abort or be handled via exception."
  [^Dialect schema val & {:as opts}]
  (when-let [errors (validate schema val opts)]
    (throw (ex-info "Invalid value"
                    {:type :s-exp.legba.json-schema/invalid-value
                     :errors errors
                     :val val}))))
