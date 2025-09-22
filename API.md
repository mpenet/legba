# Table of contents
-  [`s-exp.legba`](#s-exp.legba) 
    -  [`default-options`](#s-exp.legba/default-options) - Default options used by openapi-handler.
    -  [`handlers`](#s-exp.legba/handlers) - From a map of [method path] -> ring handler returns a map of [method path] -> openapi-wrapped-handler.
    -  [`routing-handler`](#s-exp.legba/routing-handler) - Same as <code>routing-handler*</code> but wraps with <code>s-exp.legba.middleware/wrap-error-response</code> middleware turning exceptions into nicely formatted error responses.
    -  [`routing-handler*`](#s-exp.legba/routing-handler*) - Takes a map of routes as [method path] -> ring-handler, turns them into a map of routes to openapi handlers then creates a handler that will dispatch on the appropriate openapi handler from a potential router match.
-  [`s-exp.legba.json`](#s-exp.legba.json)  - Simple utils to convert to and from jsonNode.
    -  [`-json-node->clj`](#s-exp.legba.json/-json-node->clj)
    -  [`JsonNodeToClj`](#s-exp.legba.json/JsonNodeToClj)
    -  [`clj->json-node`](#s-exp.legba.json/clj->json-node) - Takes a clj value and converts it to a Jackson JsonNode.
    -  [`json-content-type?`](#s-exp.legba.json/json-content-type?) - Returns true if <code>content-type</code> is <code>application/json</code>.
    -  [`json-node->clj`](#s-exp.legba.json/json-node->clj) - Takes a Jackson JsonNode returns an equivalent clj datastructure.
    -  [`json-node->str`](#s-exp.legba.json/json-node->str) - Takes a Jackson JsonNode and returns an equivalent String value.
    -  [`str->json-node`](#s-exp.legba.json/str->json-node) - Takes a json-str String and returns a Jackson JsonNode.
-  [`s-exp.legba.json-pointer`](#s-exp.legba.json-pointer)  - Implementation of https://datatracker.ietf.org/doc/html/rfc6901 parser and resolver.
    -  [`add-pointer`](#s-exp.legba.json-pointer/add-pointer) - Adds :json-pointer metadata to <code>node</code>.
    -  [`annotate-tree`](#s-exp.legba.json-pointer/annotate-tree) - Walks tree and add <code>:json-pointer</code> metadata to every node.
    -  [`decode-token`](#s-exp.legba.json-pointer/decode-token) - Decodes a single reference token according to RFC 6901.
    -  [`encode-token`](#s-exp.legba.json-pointer/encode-token) - Encodes a single reference token according to RFC 6901.
    -  [`parse-json-pointer`](#s-exp.legba.json-pointer/parse-json-pointer) - Parses a JSON Pointer string into a sequence of reference tokens.
    -  [`pointer-append`](#s-exp.legba.json-pointer/pointer-append) - Adds value to existing json-pointer and returns a new one.
    -  [`query`](#s-exp.legba.json-pointer/query) - Resolves a JSON Pointer against a given JSON data structure.
-  [`s-exp.legba.middleware`](#s-exp.legba.middleware) 
    -  [`ex->response`](#s-exp.legba.middleware/ex->response)
    -  [`wrap-error-response`](#s-exp.legba.middleware/wrap-error-response) - Wraps handler with error checking middleware that will transform validation Exceptions to equivalent http response, as infered per <code>ex-&gt;response</code>.
    -  [`wrap-error-response-fn`](#s-exp.legba.middleware/wrap-error-response-fn)
    -  [`wrap-validation`](#s-exp.legba.middleware/wrap-validation) - Takes a regular RING handler returns a handler that will apply openapi validation from the supplied <code>schema</code> for a given <code>method</code> and <code>path</code>.
-  [`s-exp.legba.mime-type`](#s-exp.legba.mime-type) 
    -  [`match-mime-type?`](#s-exp.legba.mime-type/match-mime-type?)
    -  [`match-schema-mime-type`](#s-exp.legba.mime-type/match-schema-mime-type) - Matches <code>content-type</code> with <code>schema</code>, return resulting <code>sub-schema</code>.
-  [`s-exp.legba.request`](#s-exp.legba.request) 
    -  [`cookie-params-schema`](#s-exp.legba.request/cookie-params-schema) - Matches <code>param-type</code> for "cookie".
    -  [`path-params-schema`](#s-exp.legba.request/path-params-schema) - Matches <code>param-type</code> for "path".
    -  [`query-params-schema`](#s-exp.legba.request/query-params-schema) - Matches <code>param-type</code> for "query".
    -  [`validate`](#s-exp.legba.request/validate) - Performs validation of RING request map.
    -  [`validate-body`](#s-exp.legba.request/validate-body) - Performs eventual validation of request <code>:body</code>.
    -  [`validate-cookie-params`](#s-exp.legba.request/validate-cookie-params) - Performs eventual validation of "parameters" of type "cookie".
    -  [`validate-path-params`](#s-exp.legba.request/validate-path-params) - Performs extensive validation of "path" "parameters".
    -  [`validate-query-params`](#s-exp.legba.request/validate-query-params) - Performs eventual validation of "parameters" of type "query".
-  [`s-exp.legba.response`](#s-exp.legba.response) 
    -  [`validate`](#s-exp.legba.response/validate) - Performs validation of RING response map.
    -  [`validate-response-body`](#s-exp.legba.response/validate-response-body) - Performs eventual validation of response body.
    -  [`validate-response-headers`](#s-exp.legba.response/validate-response-headers) - Performs validation of response headers.
-  [`s-exp.legba.router`](#s-exp.legba.router) 
    -  [`make-matcher`](#s-exp.legba.router/make-matcher) - Given set of routes, builds matcher structure.
    -  [`match`](#s-exp.legba.router/match) - Given <code>matcher</code> attempts to match against ring request, return match (tuple of <code>data</code> & <code>path-params</code>).
    -  [`router`](#s-exp.legba.router/router) - Creates a router that matches by method/path for a given <code>schema</code>.
-  [`s-exp.legba.schema`](#s-exp.legba.schema) 
    -  [`get-schema`](#s-exp.legba.schema/get-schema) - Returns json-schema from json-schema-factory at <code>schema-uri</code>/<code>json-pointer</code>.
    -  [`load-schema`](#s-exp.legba.schema/load-schema) - Loads JSON or YAML schema from <code>schema-uri</code> and returns map (of :openapi-schema, :schema-uri, :json-schema-factory) that contains all the necessary information to perform <code>validate!</code> calls later (minus a JSON pointer).
    -  [`schema-validator-config`](#s-exp.legba.schema/schema-validator-config)
    -  [`validate!`](#s-exp.legba.schema/validate!) - Validates a <code>val</code> against <code>schema</code>.
    -  [`validation-result`](#s-exp.legba.schema/validation-result) - Default validation result output function, can be overidden via <code>:validation-result</code> option of <code>s-exp.legba/*</code> calls.

-----
# <a name="s-exp.legba">s-exp.legba</a>






## <a name="s-exp.legba/default-options">`default-options`</a><a name="s-exp.legba/default-options"></a>




Default options used by openapi-handler
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba.clj#L9-L14">Source</a></sub></p>

## <a name="s-exp.legba/handlers">`handlers`</a><a name="s-exp.legba/handlers"></a>
``` clojure

(handlers routes schema & {:as opts})
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
    to [`s-exp.legba.schema/validation-result`](#s-exp.legba.schema/validation-result)
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba.clj#L29-L57">Source</a></sub></p>

## <a name="s-exp.legba/routing-handler">`routing-handler`</a><a name="s-exp.legba/routing-handler"></a>
``` clojure

(routing-handler routes schema & {:as opts})
```

Same as [`routing-handler*`](#s-exp.legba/routing-handler*) but wraps with
  [`s-exp.legba.middleware/wrap-error-response`](#s-exp.legba.middleware/wrap-error-response) middleware turning exceptions
  into nicely formatted error responses
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba.clj#L96-L105">Source</a></sub></p>

## <a name="s-exp.legba/routing-handler*">`routing-handler*`</a><a name="s-exp.legba/routing-handler*"></a>
``` clojure

(routing-handler* routes schema & {:as opts, :keys [path-params-key]})
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
    to [`s-exp.legba.schema/validation-result`](#s-exp.legba.schema/validation-result)

  * `:extra-routes` - extra routes to be passed to the underlying router
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba.clj#L59-L94">Source</a></sub></p>

-----
# <a name="s-exp.legba.json">s-exp.legba.json</a>


Simple utils to convert to and from jsonNode




## <a name="s-exp.legba.json/-json-node->clj">`-json-node->clj`</a><a name="s-exp.legba.json/-json-node->clj"></a>
``` clojure

(-json-node->clj node opts)
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json.clj#L43-L43">Source</a></sub></p>

## <a name="s-exp.legba.json/JsonNodeToClj">`JsonNodeToClj`</a><a name="s-exp.legba.json/JsonNodeToClj"></a>



<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json.clj#L42-L43">Source</a></sub></p>

## <a name="s-exp.legba.json/clj->json-node">`clj->json-node`</a><a name="s-exp.legba.json/clj->json-node"></a>
``` clojure

(clj->json-node x)
```

Takes a clj value and converts it to a Jackson JsonNode
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json.clj#L124-L127">Source</a></sub></p>

## <a name="s-exp.legba.json/json-content-type?">`json-content-type?`</a><a name="s-exp.legba.json/json-content-type?"></a>
``` clojure

(json-content-type? content-type)
```

Returns true if `content-type` is `application/json`
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json.clj#L129-L133">Source</a></sub></p>

## <a name="s-exp.legba.json/json-node->clj">`json-node->clj`</a><a name="s-exp.legba.json/json-node->clj"></a>
``` clojure

(json-node->clj node)
(json-node->clj node opts)
```

Takes a Jackson JsonNode returns an equivalent clj datastructure.
  `:key-fn` controls how map-entries keys are decoded, defaulting to `keyword`
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json.clj#L105-L111">Source</a></sub></p>

## <a name="s-exp.legba.json/json-node->str">`json-node->str`</a><a name="s-exp.legba.json/json-node->str"></a>
``` clojure

(json-node->str json-node)
```

Takes a Jackson JsonNode and returns an equivalent String value
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json.clj#L119-L122">Source</a></sub></p>

## <a name="s-exp.legba.json/str->json-node">`str->json-node`</a><a name="s-exp.legba.json/str->json-node"></a>
``` clojure

(str->json-node json-str)
```

Takes a json-str String and returns a Jackson JsonNode
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json.clj#L113-L117">Source</a></sub></p>

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
# <a name="s-exp.legba.middleware">s-exp.legba.middleware</a>






## <a name="s-exp.legba.middleware/ex->response">`ex->response`</a><a name="s-exp.legba.middleware/ex->response"></a>



<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/middleware.clj#L27-L29">Source</a></sub></p>

## <a name="s-exp.legba.middleware/wrap-error-response">`wrap-error-response`</a><a name="s-exp.legba.middleware/wrap-error-response"></a>
``` clojure

(wrap-error-response handler opts)
```

Wraps handler with error checking middleware that will transform validation
  Exceptions to equivalent http response, as infered per [`ex->response`](#s-exp.legba.middleware/ex->response)
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/middleware.clj#L58-L63">Source</a></sub></p>

## <a name="s-exp.legba.middleware/wrap-error-response-fn">`wrap-error-response-fn`</a><a name="s-exp.legba.middleware/wrap-error-response-fn"></a>
``` clojure

(wrap-error-response-fn handler req opts)
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/middleware.clj#L49-L56">Source</a></sub></p>

## <a name="s-exp.legba.middleware/wrap-validation">`wrap-validation`</a><a name="s-exp.legba.middleware/wrap-validation"></a>
``` clojure

(wrap-validation handler schema method path opts)
```

Takes a regular RING handler returns a handler that will apply openapi
  validation from the supplied `schema` for a given `method` and `path`
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/middleware.clj#L7-L25">Source</a></sub></p>

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
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/request.clj#L122-L129">Source</a></sub></p>

## <a name="s-exp.legba.request/validate-body">`validate-body`</a><a name="s-exp.legba.request/validate-body"></a>
``` clojure

(validate-body {:as request, :keys [body headers]} schema sub-schema opts)
```

Performs eventual validation of request `:body`
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/request.clj#L90-L120">Source</a></sub></p>

## <a name="s-exp.legba.request/validate-cookie-params">`validate-cookie-params`</a><a name="s-exp.legba.request/validate-cookie-params"></a>
``` clojure

(validate-cookie-params request schema sub-schema opts)
```

Performs eventual validation of "parameters" of type "cookie"
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/request.clj#L53-L72">Source</a></sub></p>

## <a name="s-exp.legba.request/validate-path-params">`validate-path-params`</a><a name="s-exp.legba.request/validate-path-params"></a>
``` clojure

(validate-path-params request schema sub-schema {:as opts, :keys [path-params-key]})
```

Performs extensive validation of "path" "parameters"
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/request.clj#L74-L88">Source</a></sub></p>

## <a name="s-exp.legba.request/validate-query-params">`validate-query-params`</a><a name="s-exp.legba.request/validate-query-params"></a>
``` clojure

(validate-query-params request schema sub-schema {:as opts, :keys [query-string-params-key]})
```

Performs eventual validation of "parameters" of type "query"
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/request.clj#L30-L51">Source</a></sub></p>

-----
# <a name="s-exp.legba.response">s-exp.legba.response</a>






## <a name="s-exp.legba.response/validate">`validate`</a><a name="s-exp.legba.response/validate"></a>
``` clojure

(validate response schema sub-schema opts)
```

Performs validation of RING response map
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/response.clj#L64-L69">Source</a></sub></p>

## <a name="s-exp.legba.response/validate-response-body">`validate-response-body`</a><a name="s-exp.legba.response/validate-response-body"></a>
``` clojure

(validate-response-body {:as response, :keys [status body headers], :or {status 200}} schema sub-schema opts)
```

Performs eventual validation of response body
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/response.clj#L8-L43">Source</a></sub></p>

## <a name="s-exp.legba.response/validate-response-headers">`validate-response-headers`</a><a name="s-exp.legba.response/validate-response-headers"></a>
``` clojure

(validate-response-headers {:as response, :keys [status], :or {status 200}} schema sub-schema opts)
```

Performs validation of response headers
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/response.clj#L45-L62">Source</a></sub></p>

-----
# <a name="s-exp.legba.router">s-exp.legba.router</a>






## <a name="s-exp.legba.router/make-matcher">`make-matcher`</a><a name="s-exp.legba.router/make-matcher"></a>
``` clojure

(make-matcher routes)
```

Given set of routes, builds matcher structure. See [`router`](#s-exp.legba.router/router)
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/router.clj#L78-L83">Source</a></sub></p>

## <a name="s-exp.legba.router/match">`match`</a><a name="s-exp.legba.router/match"></a>
``` clojure

(match matcher request)
```

Given `matcher` attempts to match against ring request, return match (tuple of
  `data` & `path-params`)
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/router.clj#L85-L89">Source</a></sub></p>

## <a name="s-exp.legba.router/router">`router`</a><a name="s-exp.legba.router/router"></a>
``` clojure

(router
 {:as _schema, :keys [openapi-schema]}
 openapi-handlers
 &
 {:as _opts, :keys [extra-routes], :or {extra-routes {}}})
```

Creates a router that matches by method/path for a given `schema`.
  `extra-routes` can be passed to add non openapi centric routes to the routing
  table
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/router.clj#L91-L103">Source</a></sub></p>

-----
# <a name="s-exp.legba.schema">s-exp.legba.schema</a>






## <a name="s-exp.legba.schema/get-schema">`get-schema`</a><a name="s-exp.legba.schema/get-schema"></a>
``` clojure

(get-schema json-schema-factory schema-uri json-pointer)
```

Returns json-schema from json-schema-factory at `schema-uri`/`json-pointer`
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/schema.clj#L30-L37">Source</a></sub></p>

## <a name="s-exp.legba.schema/load-schema">`load-schema`</a><a name="s-exp.legba.schema/load-schema"></a>
``` clojure

(load-schema schema-uri)
```

Loads JSON or YAML schema from `schema-uri` and returns
  map (of :openapi-schema, :schema-uri, :json-schema-factory) that contains all
  the necessary information to perform [`validate!`](#s-exp.legba.schema/validate!) calls later (minus a JSON
  pointer).
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/schema.clj#L39-L59">Source</a></sub></p>

## <a name="s-exp.legba.schema/schema-validator-config">`schema-validator-config`</a><a name="s-exp.legba.schema/schema-validator-config"></a>



<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/schema.clj#L20-L28">Source</a></sub></p>

## <a name="s-exp.legba.schema/validate!">`validate!`</a><a name="s-exp.legba.schema/validate!"></a>
``` clojure

(validate!
 {:as _schema, :keys [schema-uri json-schema-factory]}
 sub-schema
 val
 &
 {:as _opts, :keys [validation-result], :or {validation-result validation-result}})
```

Validates a `val` against `schema`
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/schema.clj#L75-L91">Source</a></sub></p>

## <a name="s-exp.legba.schema/validation-result">`validation-result`</a><a name="s-exp.legba.schema/validation-result"></a>
``` clojure

(validation-result r)
```

Default validation result output function, can be overidden via
  `:validation-result` option of `s-exp.legba/*` calls
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/schema.clj#L61-L73">Source</a></sub></p>
