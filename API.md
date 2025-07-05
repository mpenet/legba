# Table of contents
-  [`s-exp.legba`](#s-exp.legba) 
    -  [`default-options`](#s-exp.legba/default-options) - Default options used by openapi-handler.
    -  [`openapi-handler`](#s-exp.legba/openapi-handler) - Same as <code>openapi-handler*</code> but wraps with <code>s-exp.legba.middleware/wrap-error-response</code> middleware turning exceptions into nicely formatted error responses.
    -  [`openapi-handler*`](#s-exp.legba/openapi-handler*) - Takes a map of routes as [method path] -> ring-handler, turns them into a map of routes to openapi handlers then creates a handler that will dispatch on the appropriate openapi handler from a potential router match.

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
  `s-exp.legba.middleware/wrap-error-response` middleware turning exceptions
  into nicely formatted error responses
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba.clj#L31-L37">Source</a></sub></p>

## <a name="s-exp.legba/openapi-handler*">`openapi-handler*`</a><a name="s-exp.legba/openapi-handler*"></a>
``` clojure

(openapi-handler* routes & {:as opts})
```

Takes a map of routes as [method path] -> ring-handler, turns them into a map
  of routes to openapi handlers then creates a handler that will dispatch on the
  appropriate openapi handler from a potential router match. If not match is
  found, returns `not-found-response` (opts)
<p><sub><a href="https://github.com/mpenet/legba/blob/main/src/s_exp/legba.clj#L14-L29">Source</a></sub></p>
