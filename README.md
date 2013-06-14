# cljson

Use cljson to send data between Clojure and ClojureScript applications using JSON
as the data transfer format. The cljson library has implementations for Clojure and
ClojureScript and supports all the data types that ClojureScript supports. No support
for tagged literals yet.

## Why?

Parsing edn with `#'read-string` and stringifying with `#'pr-str` is slow in the
browser, but they have fast, native JSON parsers and stringifiers.

## Install

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
tailrecursion.cljson=> (clj->cljson [1 2 3])
"[1,2,3]"

tailrecursion.cljson=> (cljson->clj "[1,2,3]")
[1 2 3]

tailrecursion.cljson=> (clj->cljson {[1 2 3] :foo 'bar #{"bar"}})
"{\"m\":[[[1,2,3],\" 'foo\"],[\" 'bar\",{\"s\":[\"bar\"]}]]}"

tailrecursion.cljson=> (cljson->clj "{\"m\":[[[1,2,3],\" 'foo\"],[\" 'bar\",{\"s\":[\"bar\"]}]]}")
{[1 2 3] :foo, bar #{"bar"}}
```

## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
