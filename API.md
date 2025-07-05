# Table of contents
-  [`s-exp.legba`](#s-exp.legba) 
    -  [`default-options`](#s-exp.legba/default-options) - Default options used by openapi-handler.
    -  [`openapi-handler`](#s-exp.legba/openapi-handler) - Same as <code>openapi-handler*</code> but wraps with <code>s-exp.legba.middleware/wrap-error-response</code> middleware turning exceptions into nicely formatted error responses.
    -  [`openapi-handler*`](#s-exp.legba/openapi-handler*) - Takes a map of routes as [method path] -> ring-handler, turns them into a map of routes to openapi handlers then creates a handler that will dispatch on the appropriate openapi handler from a potential router match.
    -  [`openapi-routes`](#s-exp.legba/openapi-routes) - From a map of [method path] -> ring handler returns a map of [method path] -> openapi-wrapped-handler.
-  [`s-exp.legba.content-type`](#s-exp.legba.content-type) 
    -  [`match-schema-content-type`](#s-exp.legba.content-type/match-schema-content-type)
-  [`s-exp.legba.handler`](#s-exp.legba.handler) 
    -  [`make-handler`](#s-exp.legba.handler/make-handler) - Takes a regular RING handler returns a handler that will apply openapi validation from the supplied <code>schema</code> for a set of <code>coords</code> (method path).
-  [`s-exp.legba.json`](#s-exp.legba.json)  - Simple utils to convert to and from jsonNode.
    -  [`-json-node->clj`](#s-exp.legba.json/-json-node->clj)
    -  [`JsonNodeToClj`](#s-exp.legba.json/JsonNodeToClj)
    -  [`clj->json-node`](#s-exp.legba.json/clj->json-node)
    -  [`json-content-type?`](#s-exp.legba.json/json-content-type?)
    -  [`json-node->clj`](#s-exp.legba.json/json-node->clj)
    -  [`json-node->str`](#s-exp.legba.json/json-node->str)
    -  [`str->json-node`](#s-exp.legba.json/str->json-node)
-  [`s-exp.legba.json-pointer`](#s-exp.legba.json-pointer)  - Implementation of https://datatracker.ietf.org/doc/html/rfc6901 parser and resolver.
    -  [`add-pointer`](#s-exp.legba.json-pointer/add-pointer)
    -  [`annotate-tree`](#s-exp.legba.json-pointer/annotate-tree) - Walks tree and add :json-pointer to every node.
    -  [`decode-token`](#s-exp.legba.json-pointer/decode-token) - Decodes a single reference token according to RFC 6901.
    -  [`encode-token`](#s-exp.legba.json-pointer/encode-token) - Encodes a single reference token according to RFC 6901.
    -  [`parse-json-pointer`](#s-exp.legba.json-pointer/parse-json-pointer) - Parses a JSON Pointer string into a sequence of reference tokens.
    -  [`pointer-append`](#s-exp.legba.json-pointer/pointer-append)
    -  [`query`](#s-exp.legba.json-pointer/query) - Resolves a JSON Pointer against a given JSON data structure.
-  [`s-exp.legba.middleware`](#s-exp.legba.middleware) 
    -  [`ex->response`](#s-exp.legba.middleware/ex->response)
    -  [`wrap-error-response`](#s-exp.legba.middleware/wrap-error-response)
-  [`s-exp.legba.request`](#s-exp.legba.request) 
    -  [`conform-request`](#s-exp.legba.request/conform-request)
    -  [`cookie-params-schema`](#s-exp.legba.request/cookie-params-schema)
    -  [`path-params-schema`](#s-exp.legba.request/path-params-schema)
    -  [`query-params-schema`](#s-exp.legba.request/query-params-schema)
    -  [`request->conform-body`](#s-exp.legba.request/request->conform-body)
    -  [`request->conform-cookie-params`](#s-exp.legba.request/request->conform-cookie-params)
    -  [`request->conform-path-params`](#s-exp.legba.request/request->conform-path-params)
    -  [`request->conform-query-params`](#s-exp.legba.request/request->conform-query-params)
-  [`s-exp.legba.response`](#s-exp.legba.response) 
    -  [`conform-response`](#s-exp.legba.response/conform-response)
    -  [`conform-response-body`](#s-exp.legba.response/conform-response-body)
    -  [`conform-response-headers`](#s-exp.legba.response/conform-response-headers)
-  [`s-exp.legba.router`](#s-exp.legba.router) 
    -  [`match-route`](#s-exp.legba.router/match-route) - Matches <code>method</code> <code>path</code> on <code>router</code>.
    -  [`router`](#s-exp.legba.router/router) - Creates a reitit router by method/path.
-  [`s-exp.legba.schema`](#s-exp.legba.schema) 
    -  [`get-schema`](#s-exp.legba.schema/get-schema)
    -  [`load-schema`](#s-exp.legba.schema/load-schema)
    -  [`schema-validator-config`](#s-exp.legba.schema/schema-validator-config)
    -  [`validate!`](#s-exp.legba.schema/validate!)
    -  [`validation-result`](#s-exp.legba.schema/validation-result)

-----
# <a name="s-exp.legba">s-exp.legba</a>






## <a name="s-exp.legba/default-options">`default-options`</a><a name="s-exp.legba/default-options"></a>




Default options used by openapi-handler
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba.clj#L8-L12">Source</a></sub></p>

## <a name="s-exp.legba/openapi-handler">`openapi-handler`</a><a name="s-exp.legba/openapi-handler"></a>
``` clojure

(openapi-handler handlers & {:as opts})
```

Same as [`openapi-handler*`](#s-exp.legba/openapi-handler*) but wraps with
  [`s-exp.legba.middleware/wrap-error-response`](#s-exp.legba.middleware/wrap-error-response) middleware turning exceptions
  into nicely formatted error responses
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba.clj#L46-L52">Source</a></sub></p>

## <a name="s-exp.legba/openapi-handler*">`openapi-handler*`</a><a name="s-exp.legba/openapi-handler*"></a>
``` clojure

(openapi-handler* routes & {:as opts})
```

Takes a map of routes as [method path] -> ring-handler, turns them into a map
  of routes to openapi handlers then creates a handler that will dispatch on the
  appropriate openapi handler from a potential router match. If not match is
  found, returns `not-found-response` (opts)
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba.clj#L29-L44">Source</a></sub></p>

## <a name="s-exp.legba/openapi-routes">`openapi-routes`</a><a name="s-exp.legba/openapi-routes"></a>
``` clojure

(openapi-routes routes schema opts)
```

From a map of [method path] -> ring handler returns a map of [method path] ->
  openapi-wrapped-handler
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba.clj#L14-L27">Source</a></sub></p>

-----
# <a name="s-exp.legba.content-type">s-exp.legba.content-type</a>






## <a name="s-exp.legba.content-type/match-schema-content-type">`match-schema-content-type`</a><a name="s-exp.legba.content-type/match-schema-content-type"></a>
``` clojure

(match-schema-content-type schema content-type)
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/content_type.clj#L23-L36">Source</a></sub></p>

-----
# <a name="s-exp.legba.handler">s-exp.legba.handler</a>






## <a name="s-exp.legba.handler/make-handler">`make-handler`</a><a name="s-exp.legba.handler/make-handler"></a>
``` clojure

(make-handler handler schema [method path] opts)
```

Takes a regular RING handler returns a handler that will apply openapi
  validation from the supplied `schema` for a set of `coords` (method path)
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/handler.clj#L5-L25">Source</a></sub></p>

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
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json.clj#L120-L122">Source</a></sub></p>

## <a name="s-exp.legba.json/json-content-type?">`json-content-type?`</a><a name="s-exp.legba.json/json-content-type?"></a>
``` clojure

(json-content-type? content-type)
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json.clj#L124-L127">Source</a></sub></p>

## <a name="s-exp.legba.json/json-node->clj">`json-node->clj`</a><a name="s-exp.legba.json/json-node->clj"></a>
``` clojure

(json-node->clj node)
(json-node->clj node opts)
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json.clj#L105-L109">Source</a></sub></p>

## <a name="s-exp.legba.json/json-node->str">`json-node->str`</a><a name="s-exp.legba.json/json-node->str"></a>
``` clojure

(json-node->str json-node)
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json.clj#L116-L118">Source</a></sub></p>

## <a name="s-exp.legba.json/str->json-node">`str->json-node`</a><a name="s-exp.legba.json/str->json-node"></a>
``` clojure

(str->json-node json-str)
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json.clj#L111-L114">Source</a></sub></p>

-----
# <a name="s-exp.legba.json-pointer">s-exp.legba.json-pointer</a>


Implementation of https://datatracker.ietf.org/doc/html/rfc6901 parser and
  resolver




## <a name="s-exp.legba.json-pointer/add-pointer">`add-pointer`</a><a name="s-exp.legba.json-pointer/add-pointer"></a>
``` clojure

(add-pointer node pointer)
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json_pointer.clj#L52-L54">Source</a></sub></p>

## <a name="s-exp.legba.json-pointer/annotate-tree">`annotate-tree`</a><a name="s-exp.legba.json-pointer/annotate-tree"></a>
``` clojure

(annotate-tree node)
(annotate-tree node pointer)
```

Walks tree and add :json-pointer to every node
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json_pointer.clj#L56-L79">Source</a></sub></p>

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
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json_pointer.clj#L24-L34">Source</a></sub></p>

## <a name="s-exp.legba.json-pointer/pointer-append">`pointer-append`</a><a name="s-exp.legba.json-pointer/pointer-append"></a>
``` clojure

(pointer-append pointer val)
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json_pointer.clj#L20-L22">Source</a></sub></p>

## <a name="s-exp.legba.json-pointer/query">`query`</a><a name="s-exp.legba.json-pointer/query"></a>
``` clojure

(query json-data pointer)
```

Resolves a JSON Pointer against a given JSON data structure.
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/json_pointer.clj#L36-L50">Source</a></sub></p>

-----
# <a name="s-exp.legba.middleware">s-exp.legba.middleware</a>






## <a name="s-exp.legba.middleware/ex->response">`ex->response`</a><a name="s-exp.legba.middleware/ex->response"></a>



<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/middleware.clj#L4-L6">Source</a></sub></p>

## <a name="s-exp.legba.middleware/wrap-error-response">`wrap-error-response`</a><a name="s-exp.legba.middleware/wrap-error-response"></a>
``` clojure

(wrap-error-response handler)
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/middleware.clj#L16-L24">Source</a></sub></p>

-----
# <a name="s-exp.legba.request">s-exp.legba.request</a>






## <a name="s-exp.legba.request/conform-request">`conform-request`</a><a name="s-exp.legba.request/conform-request"></a>
``` clojure

(conform-request request schema sub-schema opts)
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/request.clj#L107-L114">Source</a></sub></p>

## <a name="s-exp.legba.request/cookie-params-schema">`cookie-params-schema`</a><a name="s-exp.legba.request/cookie-params-schema"></a>



<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/request.clj#L18-L18">Source</a></sub></p>

## <a name="s-exp.legba.request/path-params-schema">`path-params-schema`</a><a name="s-exp.legba.request/path-params-schema"></a>



<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/request.clj#L17-L17">Source</a></sub></p>

## <a name="s-exp.legba.request/query-params-schema">`query-params-schema`</a><a name="s-exp.legba.request/query-params-schema"></a>



<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/request.clj#L16-L16">Source</a></sub></p>

## <a name="s-exp.legba.request/request->conform-body">`request->conform-body`</a><a name="s-exp.legba.request/request->conform-body"></a>
``` clojure

(request->conform-body {:as request, :keys [body headers]} schema sub-schema opts)
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/request.clj#L77-L105">Source</a></sub></p>

## <a name="s-exp.legba.request/request->conform-cookie-params">`request->conform-cookie-params`</a><a name="s-exp.legba.request/request->conform-cookie-params"></a>
``` clojure

(request->conform-cookie-params request schema sub-schema opts)
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/request.clj#L42-L60">Source</a></sub></p>

## <a name="s-exp.legba.request/request->conform-path-params">`request->conform-path-params`</a><a name="s-exp.legba.request/request->conform-path-params"></a>
``` clojure

(request->conform-path-params request schema sub-schema opts)
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/request.clj#L62-L75">Source</a></sub></p>

## <a name="s-exp.legba.request/request->conform-query-params">`request->conform-query-params`</a><a name="s-exp.legba.request/request->conform-query-params"></a>
``` clojure

(request->conform-query-params request schema sub-schema {:as opts, :keys [query-string-params-key]})
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/request.clj#L20-L40">Source</a></sub></p>

-----
# <a name="s-exp.legba.response">s-exp.legba.response</a>






## <a name="s-exp.legba.response/conform-response">`conform-response`</a><a name="s-exp.legba.response/conform-response"></a>
``` clojure

(conform-response response schema sub-schema opts)
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/response.clj#L62-L66">Source</a></sub></p>

## <a name="s-exp.legba.response/conform-response-body">`conform-response-body`</a><a name="s-exp.legba.response/conform-response-body"></a>
``` clojure

(conform-response-body {:as response, :keys [status body headers], :or {status 200}} schema sub-schema opts)
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/response.clj#L8-L42">Source</a></sub></p>

## <a name="s-exp.legba.response/conform-response-headers">`conform-response-headers`</a><a name="s-exp.legba.response/conform-response-headers"></a>
``` clojure

(conform-response-headers {:as response, :keys [status], :or {status 200}} schema sub-schema opts)
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/response.clj#L44-L60">Source</a></sub></p>

-----
# <a name="s-exp.legba.router">s-exp.legba.router</a>






## <a name="s-exp.legba.router/match-route">`match-route`</a><a name="s-exp.legba.router/match-route"></a>
``` clojure

(match-route router method path)
```

Matches `method` `path` on [`router`](#s-exp.legba.router/router)
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/router.clj#L34-L43">Source</a></sub></p>

## <a name="s-exp.legba.router/router">`router`</a><a name="s-exp.legba.router/router"></a>
``` clojure

(router {:as _schema, :keys [openapi-schema]} openapi-handlers & {:as _opts, :keys [extra-routes]})
```

Creates a reitit router by method/path
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/router.clj#L5-L32">Source</a></sub></p>

-----
# <a name="s-exp.legba.schema">s-exp.legba.schema</a>






## <a name="s-exp.legba.schema/get-schema">`get-schema`</a><a name="s-exp.legba.schema/get-schema"></a>
``` clojure

(get-schema schema schema-resource-file ptr)
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/schema.clj#L57-L62">Source</a></sub></p>

## <a name="s-exp.legba.schema/load-schema">`load-schema`</a><a name="s-exp.legba.schema/load-schema"></a>
``` clojure

(load-schema schema-resource-file)
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/schema.clj#L32-L55">Source</a></sub></p>

## <a name="s-exp.legba.schema/schema-validator-config">`schema-validator-config`</a><a name="s-exp.legba.schema/schema-validator-config"></a>



<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/schema.clj#L24-L30">Source</a></sub></p>

## <a name="s-exp.legba.schema/validate!">`validate!`</a><a name="s-exp.legba.schema/validate!"></a>
``` clojure

(validate!
 {:as schema, :keys [schema-resource-file]}
 sub-schema
 val
 &
 {:as _opts, :keys [validation-result], :or {validation-result validation-result}})
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/schema.clj#L76-L89">Source</a></sub></p>

## <a name="s-exp.legba.schema/validation-result">`validation-result`</a><a name="s-exp.legba.schema/validation-result"></a>
``` clojure

(validation-result r)
```
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba/schema.clj#L64-L74">Source</a></sub></p>
