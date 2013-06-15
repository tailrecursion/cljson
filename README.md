# cljson

Use cljson to send data between Clojure and ClojureScript applications using JSON
as the data transfer format. The cljson library has implementations for Clojure and
ClojureScript and supports all the data types that ClojureScript supports, including
tagged literals and metadata.

<img src="https://docs.google.com/a/dipert.org/spreadsheet/oimg?key=0AveuiOwXIG2PdEFRYXo0RV9YTjIwa1lPaDVNSzU1M1E&oid=1&zx=vqnilcwh4nup" />

## Install [![Build Status](https://travis-ci.org/tailrecursion/cljson.png?branch=master)](https://travis-ci.org/tailrecursion/cljson)

Artifacts are published on [Clojars](http://clojars.org/tailrecursion/cljson).

Leiningen:
```clojure
[tailrecursion/cljson "0.1.0-SNAPSHOT"]
```

Maven:
```xml
<dependency>
  <groupId>tailrecursion</groupId>
  <artifactId>cljson</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Usage

There are two functions exported by this library: `clj->cljson` and `cljson->clj`.
They convert Clojure data to and from JSON strings.

```clojure
user=> (use 'tailrecursion.cljson)
nil

user=> (clj->cljson [1 2 3])
"[1,2,3]"

user=> (cljson->clj "[1,2,3]")
[1 2 3]

user=> (clj->cljson '(1 2 3))
"{\"l\":[1,2,3]}"

user=> (cljson->clj "{\"l\":[1,2,3]}")
(1 2 3)

user=> (clj->cljson {[1 2 3] :foo 'bar #{"bar"}})
"{\"m\":[[1,2,3],{\"k\":\"foo\"},{\"y\":\"bar\"},{\"s\":[\"bar\"]}]}"

user=> (cljson->clj "{\"m\":[[1,2,3],{\"k\":\"foo\"},{\"y\":\"bar\"},{\"s\":[\"bar\"]}]}")
{[1 2 3] :foo, bar #{"bar"}}
```

### Tagged Literals

Cljson provides the `EncodeTagged` protocol which can be extended to user types
and records. This protocol is used to transform a Clojure/ClojureScript thing into
JSON-ready data.

If a type does not satisfy this protocol then cljson will use the core printer to
obtain a printed representation of the thing. If the printed representation is a
tagged literal then the data part is reread and converted to JSON-ready data.

Reading of tagged literals is done via the normal tagged literal mechanisms built
into Clojure and ClojureScript.

Have a look at _cljson.clj_ and _cljson.cljs_ to see examples of this.

### Metadata

Bind `*print-meta*` to `true` to have metadata included in the JSON output.

## License

Copyright © 2013 Alan Dipert and Micha Niskin

Distributed under the Eclipse Public License, the same as Clojure.
