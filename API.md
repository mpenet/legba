# Table of contents
-  [`s-exp.legba`](#s-exp.legba) 
    -  [`default-options`](#s-exp.legba/default-options) - Default options used by openapi-handler.
    -  [`ensure-route-coverage!`](#s-exp.legba/ensure-route-coverage!) - Checks that a map of openapi-handlers covers all paths defined by the schema.
    -  [`handlers`](#s-exp.legba/handlers) - From a map of [method path] -> ring handler returns a map of [method path] -> openapi-wrapped-handler.
    -  [`middlewares`](#s-exp.legba/middlewares) - From a sequence of [method path] tuples returns a map of [method path] -> openapi validation middlewares.
    -  [`routing-handler`](#s-exp.legba/routing-handler) - Same as <code>routing-handler*</code> but wraps with <code>s-exp.legba.middleware/wrap-error-response</code> middleware turning exceptions into nicely formatted error responses.
    -  [`routing-handler*`](#s-exp.legba/routing-handler*) - Takes a map of routes as [method path] -> ring-handler, turns them into a map of routes to openapi handlers then creates a handler that will dispatch on the appropriate openapi handler from a potential router match.
-  [`s-exp.legba.json`](#s-exp.legba.json)  - Simple utils to convert to and from jsonNode.
    -  [`-json-node->clj`](#s-exp.legba.json/-json-node->clj)
    -  [`JsonNodeToClj`](#s-exp.legba.json/JsonNodeToClj)
    -  [`clj->json-node`](#s-exp.legba.json/clj->json-node) - Takes a clj value and converts it to a Jackson JsonNode.
    -  [`json-content-type?`](#s-exp.legba.json/json-content-type?) - Returns true if <code>content-type</code> is <code>application/json</code>.
    -  [`json-mapper-default`](#s-exp.legba.json/json-mapper-default)
    -  [`json-node->clj`](#s-exp.legba.json/json-node->clj) - Takes a Jackson JsonNode returns an equivalent clj datastructure.
    -  [`json-node->str`](#s-exp.legba.json/json-node->str) - Takes a Jackson JsonNode and returns an equivalent String value.
    -  [`set-mapper-defaults!`](#s-exp.legba.json/set-mapper-defaults!) - Sets sane defaults on jsonista, without this jsonista will do mad stuff such as serializing POJO fields as json attributes.
    -  [`str->json-node`](#s-exp.legba.json/str->json-node) - Takes a json-str String and returns a Jackson JsonNode.
-  [`s-exp.legba.json-pointer`](#s-exp.legba.json-pointer)  - Implementation of https://datatracker.ietf.org/doc/html/rfc6901 parser and resolver.
    -  [`add-pointer`](#s-exp.legba.json-pointer/add-pointer) - Adds :json-pointer metadata to <code>node</code>.
    -  [`annotate-tree`](#s-exp.legba.json-pointer/annotate-tree) - Walks tree and add <code>:json-pointer</code> metadata to every node.
    -  [`decode-token`](#s-exp.legba.json-pointer/decode-token) - Decodes a single reference token according to RFC 6901.
    -  [`encode-token`](#s-exp.legba.json-pointer/encode-token) - Encodes a single reference token according to RFC 6901.
    -  [`parse-json-pointer`](#s-exp.legba.json-pointer/parse-json-pointer) - Parses a JSON Pointer string into a sequence of reference tokens.
    -  [`pointer-append`](#s-exp.legba.json-pointer/pointer-append) - Adds value to existing json-pointer and returns a new one.
    -  [`query`](#s-exp.legba.json-pointer/query) - Resolves a JSON Pointer against a given JSON data structure.
-  [`s-exp.legba.json-schema`](#s-exp.legba.json-schema)  - JSON Schema validation utilities.
    -  [`schema`](#s-exp.legba.json-schema/schema) - Loads and builds a JSON Schema validator instance from a URI or file path.
    -  [`schema-registry-config`](#s-exp.legba.json-schema/schema-registry-config) - Default reusable <code>SchemaRegistryConfig</code> instance.
    -  [`validate`](#s-exp.legba.json-schema/validate) - Validates a value against a previously loaded or constructed schema.
    -  [`validate!`](#s-exp.legba.json-schema/validate!) - Validates a value against a given JSON Schema and throws if invalid.
    -  [`validation-result`](#s-exp.legba.json-schema/validation-result) - Extracts and formats schema validation errors from a ValidationResult object.
-  [`s-exp.legba.middleware`](#s-exp.legba.middleware) 
    -  [`ex->response`](#s-exp.legba.middleware/ex->response)
    -  [`ex->rfc9457-type`](#s-exp.legba.middleware/ex->rfc9457-type)
    -  [`wrap-error-response`](#s-exp.legba.middleware/wrap-error-response) - Wraps handler with error checking middleware that will transform validation Exceptions to equivalent http response, as infered per <code>ex-&gt;response</code>.
    -  [`wrap-error-response-fn`](#s-exp.legba.middleware/wrap-error-response-fn)
    -  [`wrap-validation`](#s-exp.legba.middleware/wrap-validation) - Middleware that wraps a standard RING handler with OpenAPI request and response validation.
-  [`s-exp.legba.mime-type`](#s-exp.legba.mime-type) 
    -  [`match-mime-type?`](#s-exp.legba.mime-type/match-mime-type?)
    -  [`match-schema-mime-type`](#s-exp.legba.mime-type/match-schema-mime-type) - Matches <code>content-type</code> with <code>schema</code>, return resulting <code>sub-schema</code>.
-  [`s-exp.legba.openapi-schema`](#s-exp.legba.openapi-schema) 
    -  [`get-schema`](#s-exp.legba.openapi-schema/get-schema) - Returns json-schema from schema-registry at <code>schema-uri</code>/<code>json-pointer</code>.
    -  [`load-schema`](#s-exp.legba.openapi-schema/load-schema) - Loads JSON or YAML schema from <code>schema-uri</code> and returns map (of :openapi-schema, :schema-uri, :schema-registry) that contains all the necessary information to perform <code>validate!</code> calls later (minus a JSON pointer).
    -  [`schema->routes-schema`](#s-exp.legba.openapi-schema/schema->routes-schema)
    -  [`schema-registry-config`](#s-exp.legba.openapi-schema/schema-registry-config) - Default reusable <code>SchemaRegistryConfig</code> instance.
    -  [`validate`](#s-exp.legba.openapi-schema/validate) - Validates a <code>val</code> against <code>schema</code>.
    -  [`validate-schema!`](#s-exp.legba.openapi-schema/validate-schema!) - Validates a user-provided JSON schema against the OpenAPI 3.1 base schema.
-  [`s-exp.legba.overlay`](#s-exp.legba.overlay)  - Provides overlay manipulation utilities for OpenAPI schemas, enabling dynamic updates or removals on schema documents using OpenAPI Overlay instructions.
    -  [`apply`](#s-exp.legba.overlay/apply) - Apply overlay actions (update/remove) to the given OpenAPI schema string.
    -  [`conf`](#s-exp.legba.overlay/conf)
-  [`s-exp.legba.request`](#s-exp.legba.request) 
    -  [`cookie-params-schema`](#s-exp.legba.request/cookie-params-schema) - Matches <code>param-type</code> for "cookie".
    -  [`path-params-schema`](#s-exp.legba.request/path-params-schema) - Matches <code>param-type</code> for "path".
    -  [`query-params-schema`](#s-exp.legba.request/query-params-schema) - Matches <code>param-type</code> for "query".
    -  [`validate`](#s-exp.legba.request/validate) - Performs validation of RING request map.
    -  [`validate-body`](#s-exp.legba.request/validate-body) - Performs eventual validation of request <code>:body</code>.
    -  [`validate-cookie-params`](#s-exp.legba.request/validate-cookie-params) - Performs eventual validation of "parameters" of type "cookie".
    -  [`validate-method-params`](#s-exp.legba.request/validate-method-params) - Performs extensive validation of "path" "parameters".
    -  [`validate-path-params`](#s-exp.legba.request/validate-path-params) - Performs extensive validation of "path" "parameters".
    -  [`validate-query-params`](#s-exp.legba.request/validate-query-params) - Performs eventual validation of "parameters" of type "query".
-  [`s-exp.legba.response`](#s-exp.legba.response) 
    -  [`validate`](#s-exp.legba.response/validate) - Performs validation of RING response map.
    -  [`validate-response-body`](#s-exp.legba.response/validate-response-body) - Performs eventual validation of response body.
    -  [`validate-response-headers`](#s-exp.legba.response/validate-response-headers) - Performs validation of response headers.
-  [`s-exp.legba.router`](#s-exp.legba.router) 
    -  [`match`](#s-exp.legba.router/match)
    -  [`router`](#s-exp.legba.router/router) - Creates a router that matches by method/path for a given <code>schema</code>.

-----
# <a name="s-exp.legba">s-exp.legba</a>






## <a name="s-exp.legba/default-options">`default-options`</a><a name="s-exp.legba/default-options"></a>




Default options used by openapi-handler
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba.clj#L9-L16">Source</a></sub></p>

## <a name="s-exp.legba/ensure-route-coverage!">`ensure-route-coverage!`</a><a name="s-exp.legba/ensure-route-coverage!"></a>
``` clojure

(ensure-route-coverage! routes {:as _schema, :keys [openapi-schema]})
```

Checks that a map of openapi-handlers covers all paths defined by the schema
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba.clj#L18-L31">Source</a></sub></p>

## <a name="s-exp.legba/handlers">`handlers`</a><a name="s-exp.legba/handlers"></a>
``` clojure

(handlers routes schema-path & {:as opts})
```

From a map of [method path] -> ring handler returns a map of [method path] ->
  openapi-wrapped-handler.

  Options:

  * `:key-fn` - Control map keys decoding when turning jackson JsonNodes to clj
    data for the handler - default to `keyword`

  * `:query-string-params-key` - where to find the decoded query-string
     parameters - defaults to `:query-params`

  * `:validation-result` - function that controls how to turn
  `com.networknt.schema.ValidationResult` into a clj -> json response. Defaults
  to `s-exp.legba.openapi-schema/validation-result`

  * `:include-schema`: - adds the path-relevant schema portion to the
  request-map under `:s-exp.legba/schema` (`false` by default)
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba.clj#L66-L93">Source</a></sub></p>

## <a name="s-exp.legba/middlewares">`middlewares`</a><a name="s-exp.legba/middlewares"></a>
``` clojure

(middlewares schema-path & {:as opts})
```

From a sequence of [method path] tuples returns a map of [method path] ->
  openapi validation middlewares.

  Options:

  * `:key-fn` - Control map keys decoding when turning jackson JsonNodes to clj
    data for the handler - default to `keyword`

  * `:query-string-params-key` - where to find the decoded query-string
     parameters - defaults to `:query-params`

  * `:validation-result` - function that controls how to turn
  `com.networknt.schema.ValidationResult` into a clj -> json response. Defaults
  to `s-exp.legba.openapi-schema/validation-result`

  * `:include-schema`: - adds the path-relevant schema portion to the
  request-map under `:s-exp.legba/schema` (`false` by default)
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba.clj#L33-L64">Source</a></sub></p>

## <a name="s-exp.legba/routing-handler">`routing-handler`</a><a name="s-exp.legba/routing-handler"></a>
``` clojure

(routing-handler routes schema-path & {:as opts})
```

Same as [`routing-handler*`](#s-exp.legba/routing-handler*) but wraps with
  [`s-exp.legba.middleware/wrap-error-response`](#s-exp.legba.middleware/wrap-error-response) middleware turning exceptions
  into nicely formatted error responses
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba.clj#L132-L141">Source</a></sub></p>

## <a name="s-exp.legba/routing-handler*">`routing-handler*`</a><a name="s-exp.legba/routing-handler*"></a>
``` clojure

(routing-handler* routes schema-path & {:as opts, :keys [path-params-key]})
```

Takes a map of routes as [method path] -> ring-handler, turns them into a map
  of routes to openapi handlers then creates a handler that will dispatch on the
  appropriate openapi handler from a potential router match. If not match is
  found, returns `not-found-response`.

  Options:

  * `:not-found-response` - defaults to `{:status 404 :body " Not found "}`

  * `:key-fn` - Control map keys decoding when turning jackson JsonNodes to clj
    data for the handler - default to `keyword`

  * `:query-string-params-key` - where to find the decoded query-string
     parameters - defaults to `:query-params`

  * `:validation-result` - function that controls how to turn
    `com.networknt.schema.ValidationResult` into a clj -> json response. Defaults
    to `s-exp.legba.openapi-schema/validation-result`

  * `:extra-routes` - extra routes to be passed to the underlying router

  throw and assocs the error on the ring response as response-validation-error.  
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba.clj#L95-L130">Source</a></sub></p>

-----
# <a name="s-exp.legba.json">s-exp.legba.json</a>


Simple utils to convert to and from jsonNode




## <a name="s-exp.legba.json/-json-node->clj">`-json-node->clj`</a><a name="s-exp.legba.json/-json-node->clj"></a>
``` clojure

(-json-node->clj node opts)
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json.clj#L61-L61">Source</a></sub></p>

## <a name="s-exp.legba.json/JsonNodeToClj">`JsonNodeToClj`</a><a name="s-exp.legba.json/JsonNodeToClj"></a>



<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json.clj#L60-L61">Source</a></sub></p>

## <a name="s-exp.legba.json/clj->json-node">`clj->json-node`</a><a name="s-exp.legba.json/clj->json-node"></a>
``` clojure

(clj->json-node x)
```

Takes a clj value and converts it to a Jackson JsonNode
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json.clj#L142-L145">Source</a></sub></p>

## <a name="s-exp.legba.json/json-content-type?">`json-content-type?`</a><a name="s-exp.legba.json/json-content-type?"></a>
``` clojure

(json-content-type? content-type)
```

Returns true if `content-type` is `application/json`
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json.clj#L147-L151">Source</a></sub></p>

## <a name="s-exp.legba.json/json-mapper-default">`json-mapper-default`</a><a name="s-exp.legba.json/json-mapper-default"></a>



<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json.clj#L35-L37">Source</a></sub></p>

## <a name="s-exp.legba.json/json-node->clj">`json-node->clj`</a><a name="s-exp.legba.json/json-node->clj"></a>
``` clojure

(json-node->clj node)
(json-node->clj node opts)
```

Takes a Jackson JsonNode returns an equivalent clj datastructure.
  `:key-fn` controls how map-entries keys are decoded, defaulting to `keyword`
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json.clj#L123-L129">Source</a></sub></p>

## <a name="s-exp.legba.json/json-node->str">`json-node->str`</a><a name="s-exp.legba.json/json-node->str"></a>
``` clojure

(json-node->str json-node)
```

Takes a Jackson JsonNode and returns an equivalent String value
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json.clj#L137-L140">Source</a></sub></p>

## <a name="s-exp.legba.json/set-mapper-defaults!">`set-mapper-defaults!`</a><a name="s-exp.legba.json/set-mapper-defaults!"></a>
``` clojure

(set-mapper-defaults! object-mapper)
```

Sets sane defaults on jsonista, without this jsonista will do mad stuff such as
  serializing POJO fields as json attributes
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json.clj#L23-L33">Source</a></sub></p>

## <a name="s-exp.legba.json/str->json-node">`str->json-node`</a><a name="s-exp.legba.json/str->json-node"></a>
``` clojure

(str->json-node json-str)
```

Takes a json-str String and returns a Jackson JsonNode
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json.clj#L131-L135">Source</a></sub></p>

-----
# <a name="s-exp.legba.json-pointer">s-exp.legba.json-pointer</a>


Implementation of https://datatracker.ietf.org/doc/html/rfc6901 parser and
  resolver




## <a name="s-exp.legba.json-pointer/add-pointer">`add-pointer`</a><a name="s-exp.legba.json-pointer/add-pointer"></a>
``` clojure

(add-pointer node pointer)
```

Adds :json-pointer metadata to `node`
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json_pointer.clj#L53-L56">Source</a></sub></p>

## <a name="s-exp.legba.json-pointer/annotate-tree">`annotate-tree`</a><a name="s-exp.legba.json-pointer/annotate-tree"></a>
``` clojure

(annotate-tree node)
(annotate-tree node pointer)
```

Walks tree and add `:json-pointer` metadata to every node
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json_pointer.clj#L58-L81">Source</a></sub></p>

## <a name="s-exp.legba.json-pointer/decode-token">`decode-token`</a><a name="s-exp.legba.json-pointer/decode-token"></a>
``` clojure

(decode-token token)
```

Decodes a single reference token according to RFC 6901.
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json_pointer.clj#L6-L11">Source</a></sub></p>

## <a name="s-exp.legba.json-pointer/encode-token">`encode-token`</a><a name="s-exp.legba.json-pointer/encode-token"></a>
``` clojure

(encode-token token)
```

Encodes a single reference token according to RFC 6901.
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json_pointer.clj#L13-L18">Source</a></sub></p>

## <a name="s-exp.legba.json-pointer/parse-json-pointer">`parse-json-pointer`</a><a name="s-exp.legba.json-pointer/parse-json-pointer"></a>
``` clojure

(parse-json-pointer pointer)
```

Parses a JSON Pointer string into a sequence of reference tokens.
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json_pointer.clj#L25-L35">Source</a></sub></p>

## <a name="s-exp.legba.json-pointer/pointer-append">`pointer-append`</a><a name="s-exp.legba.json-pointer/pointer-append"></a>
``` clojure

(pointer-append pointer val)
```

Adds value to existing json-pointer and returns a new one
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json_pointer.clj#L20-L23">Source</a></sub></p>

## <a name="s-exp.legba.json-pointer/query">`query`</a><a name="s-exp.legba.json-pointer/query"></a>
``` clojure

(query json-data pointer)
```

Resolves a JSON Pointer against a given JSON data structure.
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json_pointer.clj#L37-L51">Source</a></sub></p>

-----
# <a name="s-exp.legba.json-schema">s-exp.legba.json-schema</a>


JSON Schema validation utilities.
   Provides helpers to load, cache, and validate JSON Schemas




## <a name="s-exp.legba.json-schema/schema">`schema`</a><a name="s-exp.legba.json-schema/schema"></a>
``` clojure

(schema schema-uri & {:as _opts, :keys [schema-registry-config], :or {schema-registry-config schema-registry-config}})
```

Loads and builds a JSON Schema validator instance from a URI or file path.

  Arguments:
    - `schema-uri` (string): Location of the JSON Schema (file:/..., http:/...,
  classpath:/..., etc)

    - `:schema-registry-config` (optional): Custom
  `SchemaRegistryConfig` (default: this namespace's [`schema-registry-config`](#s-exp.legba.json-schema/schema-registry-config)).

  Returns:

    An instance of the JSON Schema validator (`com.networknt.schema.JsonSchema`).
    This object can be reused to validate multiple values or payloads.

  Example:
    (schema "classpath:///tmp/foo.schema.json")
    (schema "file:///data/schema/bar.json")
    (schema "https://schemas.org/example.schema.json")
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json_schema.clj#L37-L65">Source</a></sub></p>

## <a name="s-exp.legba.json-schema/schema-registry-config">`schema-registry-config`</a><a name="s-exp.legba.json-schema/schema-registry-config"></a>




Default reusable `SchemaRegistryConfig` instance.

  This configures the validator to:
  - Enable JSON Schema reference preloading and caching
  - Enable format assertions (per the JSON Schema spec)
  - Set maximum reference nesting depth to 40
  - Handle `nullable` fields correctly
  - Use `JSON_PATH` for error paths in validation results

  This config can be reused for schema load/validation for performance and consistency.
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json_schema.clj#L18-L35">Source</a></sub></p>

## <a name="s-exp.legba.json-schema/validate">`validate`</a><a name="s-exp.legba.json-schema/validate"></a>
``` clojure

(validate schema val & {:as _opts, :keys [validation-result], :or {validation-result validation-result}})
```

Validates a value against a previously loaded or constructed schema.

  Arguments:
    - [[`schema`](#s-exp.legba.json-schema/schema)](#s-exp.legba.json-schema/schema): The schema object returned from [[`schema`](#s-exp.legba.json-schema/schema)](#s-exp.legba.json-schema/schema)
    - `val`: The input to validate. Can be a JsonNode or a JSON string.
    - `:validation-result` (optional): Override for the function used to extract
  validation errors (defaults to this namespace's [[`validation-result`](#s-exp.legba.json-schema/validation-result)](#s-exp.legba.json-schema/validation-result)).

  Returns:
    A sequence of error maps (see [[`validation-result`](#s-exp.legba.json-schema/validation-result)](#s-exp.legba.json-schema/validation-result)), or nil if valid.

  Example:
    (validate myschema "{"foo":42}")
    (validate myschema my-jackson-json-node)
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json_schema.clj#L91-L116">Source</a></sub></p>

## <a name="s-exp.legba.json-schema/validate!">`validate!`</a><a name="s-exp.legba.json-schema/validate!"></a>
``` clojure

(validate! schema val & {:as opts})
```

Validates a value against a given JSON Schema and throws if invalid.

  Arguments:
    - [[`schema`](#s-exp.legba.json-schema/schema)](#s-exp.legba.json-schema/schema) (JsonSchema): A schema instance created by the [[`schema`](#s-exp.legba.json-schema/schema)](#s-exp.legba.json-schema/schema) function.
    - `val`: The data to validate (can be a Jackson JsonNode or a JSON string).
    - Optional keyword arguments:
        - `:validation-result`: Custom function to extract validation errors
          (defaults to this namespace's [[`validation-result`](#s-exp.legba.json-schema/validation-result)](#s-exp.legba.json-schema/validation-result)).

  Behavior:
    - If validation succeeds (no errors), returns nil.
    - If validation fails, throws an ex-info exception with:
        :type   --> :s-exp.legba.json-schema/failed-validation
        :errors --> Sequence of error maps (see [[`validation-result`](#s-exp.legba.json-schema/validation-result)](#s-exp.legba.json-schema/validation-result)).
        :val    --> The input value that failed validation.

  Example:
    (validate! myschema "{"name":42}") ; throws if invalid
    (validate! myschema my-jackson-json-node)

  Useful for workflows where validation failure should abort or be handled via exception.
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json_schema.clj#L118-L145">Source</a></sub></p>

## <a name="s-exp.legba.json-schema/validation-result">`validation-result`</a><a name="s-exp.legba.json-schema/validation-result"></a>
``` clojure

(validation-result r)
```

Extracts and formats schema validation errors from a ValidationResult object.

  Takes a NetworkNT ValidationResult and returns a vector of error maps,
  where each map contains:
    - :path     (string)   JSON path of the error location
    - :pointer  (string)   JSON pointer to the schema fragment
    - :location (string)   Instance location in the JSON document
    - :detail   (string)   Validation error message/detail

  Returns nil if there are no errors. Useful for turning validator output into
  a more consumable shape for clients, APIs, or error reporting.
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json_schema.clj#L67-L89">Source</a></sub></p>

-----
# <a name="s-exp.legba.middleware">s-exp.legba.middleware</a>






## <a name="s-exp.legba.middleware/ex->response">`ex->response`</a><a name="s-exp.legba.middleware/ex->response"></a>



<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/middleware.clj#L51-L53">Source</a></sub></p>

## <a name="s-exp.legba.middleware/ex->rfc9457-type">`ex->rfc9457-type`</a><a name="s-exp.legba.middleware/ex->rfc9457-type"></a>



<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/middleware.clj#L36-L37">Source</a></sub></p>

## <a name="s-exp.legba.middleware/wrap-error-response">`wrap-error-response`</a><a name="s-exp.legba.middleware/wrap-error-response"></a>
``` clojure

(wrap-error-response handler opts)
```

Wraps handler with error checking middleware that will transform validation
  Exceptions to equivalent http response, as infered per [`ex->response`](#s-exp.legba.middleware/ex->response)
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/middleware.clj#L82-L87">Source</a></sub></p>

## <a name="s-exp.legba.middleware/wrap-error-response-fn">`wrap-error-response-fn`</a><a name="s-exp.legba.middleware/wrap-error-response-fn"></a>
``` clojure

(wrap-error-response-fn handler req {:as opts})
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/middleware.clj#L73-L80">Source</a></sub></p>

## <a name="s-exp.legba.middleware/wrap-validation">`wrap-validation`</a><a name="s-exp.legba.middleware/wrap-validation"></a>
``` clojure

(wrap-validation handler schema method path {:as opts, :keys [include-schema]})
```

Middleware that wraps a standard RING handler with OpenAPI request and response validation.
  Validates both the incoming request and outgoing response according to the
  provided `schema`, for the specified HTTP `method` and `path`. Additional
  options:, such as including the validation schema with each
  request (`include-schema`).
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/middleware.clj#L8-L34">Source</a></sub></p>

-----
# <a name="s-exp.legba.mime-type">s-exp.legba.mime-type</a>






## <a name="s-exp.legba.mime-type/match-mime-type?">`match-mime-type?`</a><a name="s-exp.legba.mime-type/match-mime-type?"></a>
``` clojure

(match-mime-type? mime-type-ptn mime-type)
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/mime_type.clj#L10-L20">Source</a></sub></p>

## <a name="s-exp.legba.mime-type/match-schema-mime-type">`match-schema-mime-type`</a><a name="s-exp.legba.mime-type/match-schema-mime-type"></a>
``` clojure

(match-schema-mime-type schema content-type)
```

Matches `content-type` with `schema`, return resulting `sub-schema`
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/mime_type.clj#L22-L30">Source</a></sub></p>

-----
# <a name="s-exp.legba.openapi-schema">s-exp.legba.openapi-schema</a>






## <a name="s-exp.legba.openapi-schema/get-schema">`get-schema`</a><a name="s-exp.legba.openapi-schema/get-schema"></a>
``` clojure

(get-schema schema-registry schema-uri json-pointer)
```

Returns json-schema from schema-registry at `schema-uri`/`json-pointer`
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/openapi_schema.clj#L84-L90">Source</a></sub></p>

## <a name="s-exp.legba.openapi-schema/load-schema">`load-schema`</a><a name="s-exp.legba.openapi-schema/load-schema"></a>
``` clojure

(load-schema schema-uri & {:as _opts, :keys [validate-schema], :or {validate-schema true}})
```

Loads JSON or YAML schema from `schema-uri` and returns
  map (of :openapi-schema, :schema-uri, :schema-registry) that contains all the
  necessary information to perform `validate!` calls later (minus a JSON
  pointer).
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/openapi_schema.clj#L92-L114">Source</a></sub></p>

## <a name="s-exp.legba.openapi-schema/schema->routes-schema">`schema->routes-schema`</a><a name="s-exp.legba.openapi-schema/schema->routes-schema"></a>
``` clojure

(schema->routes-schema {:as _schema, :keys [openapi-schema]})
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/openapi_schema.clj#L21-L27">Source</a></sub></p>

## <a name="s-exp.legba.openapi-schema/schema-registry-config">`schema-registry-config`</a><a name="s-exp.legba.openapi-schema/schema-registry-config"></a>




Default reusable `SchemaRegistryConfig` instance.

  This configures the validator to:
  - Enable JSON Schema reference preloading and caching
  - Enable format assertions (per the JSON Schema spec)
  - Set maximum reference nesting depth to 40
  - Handle `nullable` fields correctly
  - Use `JSON_PATH` for error paths in validation results

  This config can be reused for schema load/validation for performance and consistency.
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/openapi_schema.clj#L29-L47">Source</a></sub></p>

## <a name="s-exp.legba.openapi-schema/validate">`validate`</a><a name="s-exp.legba.openapi-schema/validate"></a>
``` clojure

(validate
 {:as _schema, :keys [schema-uri schema-registry]}
 sub-schema
 val
 &
 {:as _opts, :keys [validation-result], :or {validation-result json-schema/validation-result}})
```

Validates a `val` against `schema`
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/openapi_schema.clj#L116-L131">Source</a></sub></p>

## <a name="s-exp.legba.openapi-schema/validate-schema!">`validate-schema!`</a><a name="s-exp.legba.openapi-schema/validate-schema!"></a>
``` clojure

(validate-schema! schema-registry schema-uri)
```

Validates a user-provided JSON schema against the OpenAPI 3.1 base schema.

  Parameters:
  - schema-registry: An instance of SchemaRegistry containing preloaded schemas.
  - schema-uri: URI identifying the schema to validate (String).

  Throws:
  - ex-incorrect! if the provided schema does not conform to the OpenAPI specification.
  - The exception contains a map of validation errors in :errors.

  This function uses the OpenAPI 3.1 base schema loaded from the local classpath
  to validate the user schema and reports any problems found in a structured way.
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/openapi_schema.clj#L49-L82">Source</a></sub></p>

-----
# <a name="s-exp.legba.overlay">s-exp.legba.overlay</a>


Provides overlay manipulation utilities for OpenAPI schemas, enabling dynamic
  updates or removals on schema documents using OpenAPI Overlay instructions.
https://spec.openapis.org/overlay/latest.html  




## <a name="s-exp.legba.overlay/apply">`apply`</a><a name="s-exp.legba.overlay/apply"></a>
``` clojure

(apply openapi-string overlay-string)
```

Apply overlay actions (update/remove) to the given OpenAPI schema string.

  Parameters:
  - openapi-string: String, JSON representation of the OpenAPI schema
  - overlay-string: String, JSON representation of the overlay instructions

  Returns the OpenAPI schema with all overlay actions applied as String
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/overlay.clj#L55-L74">Source</a></sub></p>

## <a name="s-exp.legba.overlay/conf">`conf`</a><a name="s-exp.legba.overlay/conf"></a>



<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/overlay.clj#L16-L19">Source</a></sub></p>

-----
# <a name="s-exp.legba.request">s-exp.legba.request</a>






## <a name="s-exp.legba.request/cookie-params-schema">`cookie-params-schema`</a><a name="s-exp.legba.request/cookie-params-schema"></a>




Matches `param-type` for "cookie"
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/request.clj#L26-L28">Source</a></sub></p>

## <a name="s-exp.legba.request/path-params-schema">`path-params-schema`</a><a name="s-exp.legba.request/path-params-schema"></a>




Matches `param-type` for "path"
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/request.clj#L22-L24">Source</a></sub></p>

## <a name="s-exp.legba.request/query-params-schema">`query-params-schema`</a><a name="s-exp.legba.request/query-params-schema"></a>




Matches `param-type` for "query"
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/request.clj#L18-L20">Source</a></sub></p>

## <a name="s-exp.legba.request/validate">`validate`</a><a name="s-exp.legba.request/validate"></a>
``` clojure

(validate request schema sub-schema opts)
```

Performs validation of RING request map
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/request.clj#L150-L158">Source</a></sub></p>

## <a name="s-exp.legba.request/validate-body">`validate-body`</a><a name="s-exp.legba.request/validate-body"></a>
``` clojure

(validate-body {:as request, :keys [body headers]} schema sub-schema opts)
```

Performs eventual validation of request `:body`
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/request.clj#L117-L148">Source</a></sub></p>

## <a name="s-exp.legba.request/validate-cookie-params">`validate-cookie-params`</a><a name="s-exp.legba.request/validate-cookie-params"></a>
``` clojure

(validate-cookie-params request schema sub-schema opts)
```

Performs eventual validation of "parameters" of type "cookie"
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/request.clj#L56-L80">Source</a></sub></p>

## <a name="s-exp.legba.request/validate-method-params">`validate-method-params`</a><a name="s-exp.legba.request/validate-method-params"></a>
``` clojure

(validate-method-params request schema sub-schema {:as opts, :keys [path-params-key]})
```

Performs extensive validation of "path" "parameters"
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/request.clj#L82-L96">Source</a></sub></p>

## <a name="s-exp.legba.request/validate-path-params">`validate-path-params`</a><a name="s-exp.legba.request/validate-path-params"></a>
``` clojure

(validate-path-params request schema {:as _sub-schema, :keys [path-parameters]} {:as opts, :keys [path-params-key]})
```

Performs extensive validation of "path" "parameters"
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/request.clj#L98-L115">Source</a></sub></p>

## <a name="s-exp.legba.request/validate-query-params">`validate-query-params`</a><a name="s-exp.legba.request/validate-query-params"></a>
``` clojure

(validate-query-params request schema sub-schema {:as opts, :keys [query-string-params-key]})
```

Performs eventual validation of "parameters" of type "query"
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/request.clj#L30-L54">Source</a></sub></p>

-----
# <a name="s-exp.legba.response">s-exp.legba.response</a>






## <a name="s-exp.legba.response/validate">`validate`</a><a name="s-exp.legba.response/validate"></a>
``` clojure

(validate response schema sub-schema opts)
```

Performs validation of RING response map
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/response.clj#L68-L73">Source</a></sub></p>

## <a name="s-exp.legba.response/validate-response-body">`validate-response-body`</a><a name="s-exp.legba.response/validate-response-body"></a>
``` clojure

(validate-response-body {:as response, :keys [status body headers], :or {status 200}} schema sub-schema opts)
```

Performs eventual validation of response body
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/response.clj#L8-L46">Source</a></sub></p>

## <a name="s-exp.legba.response/validate-response-headers">`validate-response-headers`</a><a name="s-exp.legba.response/validate-response-headers"></a>
``` clojure

(validate-response-headers {:as response, :keys [status], :or {status 200}} schema sub-schema opts)
```

Performs validation of response headers
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/response.clj#L48-L66">Source</a></sub></p>

-----
# <a name="s-exp.legba.router">s-exp.legba.router</a>






## <a name="s-exp.legba.router/match">`match`</a><a name="s-exp.legba.router/match"></a>



<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/router.clj#L4-L4">Source</a></sub></p>

## <a name="s-exp.legba.router/router">`router`</a><a name="s-exp.legba.router/router"></a>
``` clojure

(router handlers & {:as _opts, :keys [extra-routes], :or {extra-routes {}}})
```

Creates a router that matches by method/path for a given `schema`.
  `extra-routes` can be passed to add non openapi centric routes to the routing
  table
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/router.clj#L6-L12">Source</a></sub></p>
