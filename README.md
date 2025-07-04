# Legba 

<img src="https://github.com/user-attachments/assets/7b36b294-8ada-4ef6-bbcc-4e9be4b101f7" width="100" height="100" style="float:left;">

/!\ WIP: Here be dragons, it's not finished (but it's close).

*Legba* is a library aimed at building **fully OpenApi 3.1 compliant** services
in Clojure.

It leverages
[networknt/json-schema-validator](https://github.com/networknt/json-schema-validator)
for the schema validation part, ensuring **[complete and up-to-date spec
compliance](https://www.creekservice.org/json-schema-validation-comparison/functional)**,
not to mention **[great
performance](https://www.creekservice.org/json-schema-validation-comparison/performance)**.

*Legba* works by allowing you to construct a RING handler from an openapi json
file, that handler will be aware of the routing necesary and perform all the
checks that you specified via the openapi file provided .

Requests **and** Responses will be validated fully against the schema, covering
all aspects of the spec such as body contents, path parameters, query-string
parameters, headers, status codes, etc... Upon errors it will return an error
message with details about the failure and part of the schema concerned.

What it doesn't aim to do (at this time):

* Coercing payloads (ex: uuids, they get validated but you get a string)

* Supporting other means of validation, it's intentionally limited by what
  json-schema allows you to express and is agnostic of any clojure specific
  validation framework.
  
## Installation 

For now it's tools.deps only until an alpha is out.

```clj 
com.s-exp/legba {:git/url "https://github.com/mpenet/legba.git" :git/sha "..."}
```

## Usage 

TODO

## License

Copyright © 2024-2025 Max Penet

Distributed under the Eclipse Public License version 1.0.
