(ns s-exp.legba.json-schema
  "JSON Schema validation utilities.
   Provides helpers to load, cache, and validate JSON Schemas"
  (:require [exoscale.ex :as ex])
  (:import (com.fasterxml.jackson.databind JsonNode)
           (com.networknt.schema JsonSchema
                                 JsonSchemaFactory
                                 JsonSchemaFactory$Builder
                                 SchemaValidatorsConfig
                                 InputFormat
                                 OutputFormat
                                 SchemaLocation)
           (com.networknt.schema ValidationResult
                                 ValidationMessage
                                 SpecVersion$VersionFlag
                                 PathType)))

(set! *warn-on-reflection* true)

(def schema-validator-config
  "Default reusable `SchemaValidatorsConfig` instance.

  This configures the validator to:
  - Enable JSON Schema reference preloading and caching
  - Enable format assertions (per the JSON Schema spec)
  - Set maximum reference nesting depth to 40
  - Handle `nullable` fields correctly
  - Use `JSON_PATH` for error paths in validation results

  This config can be reused for schema load/validation for performance and consistency."
  (doto (SchemaValidatorsConfig.)
    (.setPreloadJsonSchema true)
    (.setCacheRefs true)
    (.setFormatAssertionsEnabled true)
    (.setPreloadJsonSchemaRefMaxNestingDepth 40)
    (.setHandleNullableField true)
    (.setPathType PathType/JSON_PATH)))

(defn schema
  "Loads and builds a JSON Schema validator instance from a URI or file path.

  Arguments:
    - `schema-uri` (string): Location of the JSON Schema (file:/..., http:/...,
  classpath:/..., etc)

    - `:schema-validator-config` (optional): Custom
  `SchemaValidatorsConfig` (default: this namespace's
  `schema-validator-config`).

  Returns:

    An instance of the JSON Schema validator (`com.networknt.schema.JsonSchema`).
    This object can be reused to validate multiple values or payloads.

  Example:
    (schema \"classpath:///tmp/foo.schema.json\")
    (schema \"file:///data/schema/bar.json\")
    (schema \"https://schemas.org/example.schema.json\")"
  [^String schema-uri
   & {:as _opts :keys [schema-validator-config]
      :or {schema-validator-config schema-validator-config}}]
  (let [schema-factory (JsonSchemaFactory/getInstance
                        SpecVersion$VersionFlag/V202012
                        (fn [^JsonSchemaFactory$Builder builder]
                          (doto builder (.enableSchemaCache true))))]
    (.getSchema schema-factory
                (SchemaLocation/of schema-uri)
                ^SchemaValidatorsConfig schema-validator-config)))

(defn validation-result
  "Extracts validation errors from a ValidationResult object.

  Arguments:
    - `r` (ValidationResult): The result object returned by a validation call.

  Returns:
    A sequence of maps, one for each validation error, containing:
    - `:type`: Error type string from validation
    - `:path`: JSON path to offending element
    - `:error`: Error code string
    - `:message`: Human-friendly error message
    Returns `nil` if the validation result contains no errors."
  [^ValidationResult r]
  (let [vms (.getValidationMessages r)]
    (when-not (empty? vms)
      (into []
            (map (fn [^ValidationMessage m]
                   {:path (.toString (.getEvaluationPath m))
                    :pointer (str "#" (.getFragment (.getSchemaLocation m)))
                    :location (.toString (.getInstanceLocation m))
                    :detail (.getError m)}))
            vms))))

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
  [^JsonSchema schema val
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
  [^JsonSchema schema val
   & {:as opts}]
  (when-let [errors (validate schema val opts)]
    (throw (ex-info "Invalid value"
                    {:type :s-exp.legba.json-schema/invalid-value
                     :errors errors
                     :val val}))))

(ex/derive :s-exp.legba.json-schema/invalid-value
           :s-exp.legba/invalid)
