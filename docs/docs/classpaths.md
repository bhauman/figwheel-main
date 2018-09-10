---
title: Classpath
layout: docs
category: docs
order: 4
---

# The Classpath and Project Layout

<div class="lead-in"> The Classpath is integral to how Clojure and the
ClojureScript compiler finds dependencies and source files.</div>

It will help if we take some time and solidify our knowledge of the
Classpath and how it relates to a common ClojureScript project
layout.

## The JVM classpath

When I was first introduced to the JVM in college, it constantly
seemed like I was always having problems with the Classpath. Even
though these problems were constantly popping up I never really took
the time to understand it. Luckily when you are working Clojure and
ClojureScript you can get by with a simple mental model of the
Classpath for 99% of the work you do.

The primary purpose of [Leiningen][lein] and [CLI Tools][cli-tools] is
to fetch the needed dependencies, and then invoke `java` with a
classpath to create an environment where these dependencies are
available. Tools like this make the classpath problems that I used to
experience mostly a thing of the past.

First let's take a look at a Classpath. As an example let's use the
`project.clj` or the `deps.edn` we created in the
[Installation](installation) chapter.

### Printing the Classpath with Leiningen

Assuming you are in the root directory of your project using a
`project.clj` file as detailed in the last chapter. You can examine
the classpath with the following shell command:

```shell
$ lein classpath
```

And you should get this lengthy result:

<div class="classpath">
src:target:/Users/bhauman/.m2/repository/com/cognitect/transit-java/0.8.332/transit-java-0.8.332.jar:/Users/bhauman/.m2/repository/org/clojure/data.json/0.2.6/data.json-0.2.6.jar:/Users/bhauman/.m2/repository/org/clojure/clojure/1.9.0/clojure-1.9.0.jar:/Users/bhauman/.m2/repository/joda-time/joda-time/2.8.2/joda-time-2.8.2.jar:/Users/bhauman/.m2/repository/commons-codec/commons-codec/1.10/commons-codec-1.10.jar:/Users/bhauman/.m2/repository/com/bhauman/figwheel-repl/0.1.4/figwheel-repl-0.1.4.jar:/Users/bhauman/.m2/repository/org/eclipse/jetty/jetty-servlet/9.2.21.v20170120/jetty-servlet-9.2.21.v20170120.jar:/Users/bhauman/.m2/repository/ring/ring-devel/1.6.3/ring-devel-1.6.3.jar:/Users/bhauman/.m2/repository/com/google/errorprone/error_prone_annotations/2.0.18/error_prone_annotations-2.0.18.jar:/Users/bhauman/.m2/repository/org/clojure/core.specs.alpha/0.1.24/core.specs.alpha-0.1.24.jar:/Users/bhauman/.m2/repository/co/deps/ring-etag-middleware/0.2.0/ring-etag-middleware-0.2.0.jar:/Users/bhauman/.m2/repository/expound/expound/0.7.0/expound-0.7.0.jar:/Users/bhauman/.m2/repository/org/clojure/spec.alpha/0.1.143/spec.alpha-0.1.143.jar:/Users/bhauman/.m2/repository/commons-fileupload/commons-fileupload/1.3.3/commons-fileupload-1.3.3.jar:/Users/bhauman/.m2/repository/com/bhauman/figwheel-main/0.1.4/figwheel-main-0.1.4.jar:/Users/bhauman/.m2/repository/ring/ring-ssl/0.3.0/ring-ssl-0.3.0.jar:/Users/bhauman/.m2/repository/org/codehaus/mojo/animal-sniffer-annotations/1.14/animal-sniffer-annotations-1.14.jar:/Users/bhauman/.m2/repository/org/eclipse/jetty/jetty-http/9.2.21.v20170120/jetty-http-9.2.21.v20170120.jar:/Users/bhauman/.m2/repository/org/eclipse/jetty/jetty-util/9.2.21.v20170120/jetty-util-9.2.21.v20170120.jar:/Users/bhauman/.m2/repository/net/incongru/watchservice/barbary-watchservice/1.0/barbary-watchservice-1.0.jar:/Users/bhauman/.m2/repository/ring-cors/ring-cors/0.1.12/ring-cors-0.1.12.jar:/Users/bhauman/.m2/repository/org/jline/jline-terminal-jansi/3.5.1/jline-terminal-jansi-3.5.1.jar:/Users/bhauman/.m2/repository/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar:/Users/bhauman/.m2/repository/ring/ring-defaults/0.3.1/ring-defaults-0.3.1.jar:/Users/bhauman/.m2/repository/org/clojure/google-closure-library/0.0-20170809-b9c14c6b/google-closure-library-0.0-20170809-b9c14c6b.jar:/Users/bhauman/.m2/repository/com/bhauman/rebel-readline/0.1.4/rebel-readline-0.1.4.jar:/Users/bhauman/.m2/repository/cljfmt/cljfmt/0.5.7/cljfmt-0.5.7.jar:/Users/bhauman/.m2/repository/rewrite-clj/rewrite-clj/0.5.2/rewrite-clj-0.5.2.jar:/Users/bhauman/.m2/repository/org/clojure/clojurescript/1.10.339/clojurescript-1.10.339.jar:/Users/bhauman/.m2/repository/org/eclipse/jetty/websocket/websocket-api/9.2.21.v20170120/websocket-api-9.2.21.v20170120.jar:/Users/bhauman/.m2/repository/ring/ring-jetty-adapter/1.6.3/ring-jetty-adapter-1.6.3.jar:/Users/bhauman/.m2/repository/commons-io/commons-io/2.5/commons-io-2.5.jar:/Users/bhauman/.m2/repository/org/clojure/tools.namespace/0.2.11/tools.namespace-0.2.11.jar:/Users/bhauman/.m2/repository/com/google/jsinterop/jsinterop-annotations/1.0.0/jsinterop-annotations-1.0.0.jar:/Users/bhauman/.m2/repository/org/eclipse/jetty/websocket/websocket-client/9.2.21.v20170120/websocket-client-9.2.21.v20170120.jar:/Users/bhauman/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.8.7/jackson-core-2.8.7.jar:/Users/bhauman/.m2/repository/clj-time/clj-time/0.11.0/clj-time-0.11.0.jar:/Users/bhauman/.m2/repository/org/eclipse/jetty/websocket/websocket-servlet/9.2.21.v20170120/websocket-servlet-9.2.21.v20170120.jar:/Users/bhauman/.m2/repository/org/eclipse/jetty/jetty-security/9.2.21.v20170120/jetty-security-9.2.21.v20170120.jar:/Users/bhauman/.m2/repository/org/mozilla/rhino/1.7R5/rhino-1.7R5.jar:/Users/bhauman/.m2/repository/org/fusesource/jansi/jansi/1.16/jansi-1.16.jar:/Users/bhauman/.m2/repository/ring/ring-headers/0.3.0/ring-headers-0.3.0.jar:/Users/bhauman/.m2/repository/org/clojure/google-closure-library-third-party/0.0-20170809-b9c14c6b/google-closure-library-third-party-0.0-20170809-b9c14c6b.jar:/Users/bhauman/.m2/repository/hiccup/hiccup/1.0.5/hiccup-1.0.5.jar:/Users/bhauman/.m2/repository/binaryage/env-config/0.2.2/env-config-0.2.2.jar:/Users/bhauman/.m2/repository/com/bhauman/rebel-readline-cljs/0.1.4/rebel-readline-cljs-0.1.4.jar:/Users/bhauman/.m2/repository/com/google/javascript/closure-compiler-externs/v20180610/closure-compiler-externs-v20180610.jar:/Users/bhauman/.m2/repository/org/javassist/javassist/3.18.1-GA/javassist-3.18.1-GA.jar:/Users/bhauman/.m2/repository/org/clojure/java.classpath/0.2.3/java.classpath-0.2.3.jar:/Users/bhauman/.m2/repository/ns-tracker/ns-tracker/0.3.1/ns-tracker-0.3.1.jar:/Users/bhauman/.m2/repository/com/google/guava/guava/22.0/guava-22.0.jar:/Users/bhauman/.m2/repository/binaryage/devtools/0.9.10/devtools-0.9.10.jar:/Users/bhauman/.m2/repository/org/msgpack/msgpack/0.6.12/msgpack-0.6.12.jar:/Users/bhauman/.m2/repository/com/google/j2objc/j2objc-annotations/1.1/j2objc-annotations-1.1.jar:/Users/bhauman/.m2/repository/com/cognitect/transit-clj/0.8.309/transit-clj-0.8.309.jar:/Users/bhauman/.m2/repository/args4j/args4j/2.33/args4j-2.33.jar:/Users/bhauman/.m2/repository/crypto-random/crypto-random/1.2.0/crypto-random-1.2.0.jar:/Users/bhauman/.m2/repository/ring/ring-codec/1.0.1/ring-codec-1.0.1.jar:/Users/bhauman/.m2/repository/ring/ring-anti-forgery/1.1.0/ring-anti-forgery-1.1.0.jar:/Users/bhauman/.m2/repository/com/bhauman/spell-spec/0.1.0/spell-spec-0.1.0.jar:/Users/bhauman/.m2/repository/crypto-equality/crypto-equality/1.0.0/crypto-equality-1.0.0.jar:/Users/bhauman/.m2/repository/net/java/dev/jna/jna/3.2.2/jna-3.2.2.jar:/Users/bhauman/.m2/repository/com/bhauman/figwheel-core/0.1.4/figwheel-core-0.1.4.jar:/Users/bhauman/.m2/repository/cljs-tooling/cljs-tooling/0.2.0/cljs-tooling-0.2.0.jar:/Users/bhauman/.m2/repository/org/eclipse/jetty/jetty-io/9.2.21.v20170120/jetty-io-9.2.21.v20170120.jar:/Users/bhauman/.m2/repository/org/clojure/tools.reader/1.3.0-alpha3/tools.reader-1.3.0-alpha3.jar:/Users/bhauman/.m2/repository/compliment/compliment/0.3.6/compliment-0.3.6.jar:/Users/bhauman/.m2/repository/org/jline/jline-terminal/3.5.1/jline-terminal-3.5.1.jar:/Users/bhauman/.m2/repository/javax/servlet/javax.servlet-api/3.1.0/javax.servlet-api-3.1.0.jar:/Users/bhauman/.m2/repository/org/jline/jline-reader/3.5.1/jline-reader-3.5.1.jar:/Users/bhauman/.m2/repository/clj-stacktrace/clj-stacktrace/0.2.8/clj-stacktrace-0.2.8.jar:/Users/bhauman/.m2/repository/com/google/javascript/closure-compiler-unshaded/v20180610/closure-compiler-unshaded-v20180610.jar:/Users/bhauman/.m2/repository/org/eclipse/jetty/websocket/websocket-server/9.2.21.v20170120/websocket-server-9.2.21.v20170120.jar:/Users/bhauman/.m2/repository/org/eclipse/jetty/websocket/websocket-common/9.2.21.v20170120/websocket-common-9.2.21.v20170120.jar:/Users/bhauman/.m2/repository/com/google/protobuf/protobuf-java/3.0.2/protobuf-java-3.0.2.jar:/Users/bhauman/.m2/repository/ring/ring-servlet/1.6.3/ring-servlet-1.6.3.jar:/Users/bhauman/.m2/repository/hawk/hawk/0.2.11/hawk-0.2.11.jar:/Users/bhauman/.m2/repository/ring/ring/1.6.3/ring-1.6.3.jar:/Users/bhauman/.m2/repository/org/eclipse/jetty/jetty-server/9.2.21.v20170120/jetty-server-9.2.21.v20170120.jar:/Users/bhauman/.m2/repository/com/google/code/findbugs/jsr305/3.0.1/jsr305-3.0.1.jar:/Users/bhauman/.m2/repository/ring/ring-core/1.6.3/ring-core-1.6.3.jar:/Users/bhauman/.m2/repository/rewrite-cljs/rewrite-cljs/0.4.3/rewrite-cljs-0.4.3.jar:/Users/bhauman/.m2/repository/com/google/code/gson/gson/2.7/gson-2.7.jar
</div>

### Printing the Classpath with Clojure CLI Tools

Assuming you are in the root directory of your project which has a
`deps.edn` file as detailed in the last chapter. You can examine the
classpath with the following shell command:

```shell
$ clj -Spath
```
And you should get this lengthy result:

<div class="classpath">src:/Users/bhauman/.m2/repository/com/cognitect/transit-java/0.8.332/transit-java-0.8.332.jar:/Users/bhauman/.m2/repository/org/clojure/data.json/0.2.6/data.json-0.2.6.jar:/Users/bhauman/.m2/repository/org/clojure/clojure/1.9.0/clojure-1.9.0.jar:/Users/bhauman/.m2/repository/joda-time/joda-time/2.8.2/joda-time-2.8.2.jar:/Users/bhauman/.m2/repository/commons-codec/commons-codec/1.10/commons-codec-1.10.jar:/Users/bhauman/.m2/repository/com/bhauman/figwheel-repl/0.1.4/figwheel-repl-0.1.4.jar:/Users/bhauman/.m2/repository/org/eclipse/jetty/jetty-servlet/9.2.21.v20170120/jetty-servlet-9.2.21.v20170120.jar:/Users/bhauman/.m2/repository/ring/ring-devel/1.6.3/ring-devel-1.6.3.jar:/Users/bhauman/.m2/repository/com/google/errorprone/error_prone_annotations/2.0.18/error_prone_annotations-2.0.18.jar:/Users/bhauman/.m2/repository/org/clojure/core.specs.alpha/0.1.24/core.specs.alpha-0.1.24.jar:/Users/bhauman/.m2/repository/co/deps/ring-etag-middleware/0.2.0/ring-etag-middleware-0.2.0.jar:/Users/bhauman/.m2/repository/expound/expound/0.7.0/expound-0.7.0.jar:/Users/bhauman/.m2/repository/org/clojure/spec.alpha/0.1.143/spec.alpha-0.1.143.jar:/Users/bhauman/.m2/repository/commons-fileupload/commons-fileupload/1.3.3/commons-fileupload-1.3.3.jar:/Users/bhauman/.m2/repository/com/bhauman/figwheel-main/0.1.4/figwheel-main-0.1.4.jar:/Users/bhauman/.m2/repository/ring/ring-ssl/0.3.0/ring-ssl-0.3.0.jar:/Users/bhauman/.m2/repository/org/codehaus/mojo/animal-sniffer-annotations/1.14/animal-sniffer-annotations-1.14.jar:/Users/bhauman/.m2/repository/org/eclipse/jetty/jetty-http/9.2.21.v20170120/jetty-http-9.2.21.v20170120.jar:/Users/bhauman/.m2/repository/org/eclipse/jetty/jetty-util/9.2.21.v20170120/jetty-util-9.2.21.v20170120.jar:/Users/bhauman/.m2/repository/net/incongru/watchservice/barbary-watchservice/1.0/barbary-watchservice-1.0.jar:/Users/bhauman/.m2/repository/ring-cors/ring-cors/0.1.12/ring-cors-0.1.12.jar:/Users/bhauman/.m2/repository/org/jline/jline-terminal-jansi/3.5.1/jline-terminal-jansi-3.5.1.jar:/Users/bhauman/.m2/repository/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar:/Users/bhauman/.m2/repository/ring/ring-defaults/0.3.1/ring-defaults-0.3.1.jar:/Users/bhauman/.m2/repository/org/clojure/google-closure-library/0.0-20170809-b9c14c6b/google-closure-library-0.0-20170809-b9c14c6b.jar:/Users/bhauman/.m2/repository/com/bhauman/rebel-readline/0.1.4/rebel-readline-0.1.4.jar:/Users/bhauman/.m2/repository/cljfmt/cljfmt/0.5.7/cljfmt-0.5.7.jar:/Users/bhauman/.m2/repository/rewrite-clj/rewrite-clj/0.5.2/rewrite-clj-0.5.2.jar:/Users/bhauman/.m2/repository/org/clojure/clojurescript/1.10.339/clojurescript-1.10.339.jar:/Users/bhauman/.m2/repository/org/eclipse/jetty/websocket/websocket-api/9.2.21.v20170120/websocket-api-9.2.21.v20170120.jar:/Users/bhauman/.m2/repository/ring/ring-jetty-adapter/1.6.3/ring-jetty-adapter-1.6.3.jar:/Users/bhauman/.m2/repository/commons-io/commons-io/2.5/commons-io-2.5.jar:/Users/bhauman/.m2/repository/org/clojure/tools.namespace/0.2.11/tools.namespace-0.2.11.jar:/Users/bhauman/.m2/repository/com/google/jsinterop/jsinterop-annotations/1.0.0/jsinterop-annotations-1.0.0.jar:/Users/bhauman/.m2/repository/org/eclipse/jetty/websocket/websocket-client/9.2.21.v20170120/websocket-client-9.2.21.v20170120.jar:/Users/bhauman/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.8.7/jackson-core-2.8.7.jar:/Users/bhauman/.m2/repository/clj-time/clj-time/0.11.0/clj-time-0.11.0.jar:/Users/bhauman/.m2/repository/org/eclipse/jetty/websocket/websocket-servlet/9.2.21.v20170120/websocket-servlet-9.2.21.v20170120.jar:/Users/bhauman/.m2/repository/org/eclipse/jetty/jetty-security/9.2.21.v20170120/jetty-security-9.2.21.v20170120.jar:/Users/bhauman/.m2/repository/org/mozilla/rhino/1.7R5/rhino-1.7R5.jar:/Users/bhauman/.m2/repository/org/fusesource/jansi/jansi/1.16/jansi-1.16.jar:/Users/bhauman/.m2/repository/ring/ring-headers/0.3.0/ring-headers-0.3.0.jar:/Users/bhauman/.m2/repository/org/clojure/google-closure-library-third-party/0.0-20170809-b9c14c6b/google-closure-library-third-party-0.0-20170809-b9c14c6b.jar:/Users/bhauman/.m2/repository/hiccup/hiccup/1.0.5/hiccup-1.0.5.jar:/Users/bhauman/.m2/repository/binaryage/env-config/0.2.2/env-config-0.2.2.jar:/Users/bhauman/.m2/repository/com/bhauman/rebel-readline-cljs/0.1.4/rebel-readline-cljs-0.1.4.jar:/Users/bhauman/.m2/repository/com/google/javascript/closure-compiler-externs/v20180610/closure-compiler-externs-v20180610.jar:/Users/bhauman/.m2/repository/org/javassist/javassist/3.18.1-GA/javassist-3.18.1-GA.jar:/Users/bhauman/.m2/repository/org/clojure/java.classpath/0.2.3/java.classpath-0.2.3.jar:/Users/bhauman/.m2/repository/ns-tracker/ns-tracker/0.3.1/ns-tracker-0.3.1.jar:/Users/bhauman/.m2/repository/com/google/guava/guava/22.0/guava-22.0.jar:/Users/bhauman/.m2/repository/binaryage/devtools/0.9.10/devtools-0.9.10.jar:/Users/bhauman/.m2/repository/org/msgpack/msgpack/0.6.12/msgpack-0.6.12.jar:/Users/bhauman/.m2/repository/com/google/j2objc/j2objc-annotations/1.1/j2objc-annotations-1.1.jar:/Users/bhauman/.m2/repository/com/cognitect/transit-clj/0.8.309/transit-clj-0.8.309.jar:/Users/bhauman/.m2/repository/args4j/args4j/2.33/args4j-2.33.jar:/Users/bhauman/.m2/repository/crypto-random/crypto-random/1.2.0/crypto-random-1.2.0.jar:/Users/bhauman/.m2/repository/ring/ring-codec/1.0.1/ring-codec-1.0.1.jar:/Users/bhauman/.m2/repository/ring/ring-anti-forgery/1.1.0/ring-anti-forgery-1.1.0.jar:/Users/bhauman/.m2/repository/com/bhauman/spell-spec/0.1.0/spell-spec-0.1.0.jar:/Users/bhauman/.m2/repository/crypto-equality/crypto-equality/1.0.0/crypto-equality-1.0.0.jar:/Users/bhauman/.m2/repository/net/java/dev/jna/jna/3.2.2/jna-3.2.2.jar:/Users/bhauman/.m2/repository/com/bhauman/figwheel-core/0.1.4/figwheel-core-0.1.4.jar:/Users/bhauman/.m2/repository/cljs-tooling/cljs-tooling/0.2.0/cljs-tooling-0.2.0.jar:/Users/bhauman/.m2/repository/org/eclipse/jetty/jetty-io/9.2.21.v20170120/jetty-io-9.2.21.v20170120.jar:/Users/bhauman/.m2/repository/org/clojure/tools.reader/1.3.0-alpha3/tools.reader-1.3.0-alpha3.jar:/Users/bhauman/.m2/repository/compliment/compliment/0.3.6/compliment-0.3.6.jar:/Users/bhauman/.m2/repository/org/jline/jline-terminal/3.5.1/jline-terminal-3.5.1.jar:/Users/bhauman/.m2/repository/javax/servlet/javax.servlet-api/3.1.0/javax.servlet-api-3.1.0.jar:/Users/bhauman/.m2/repository/org/jline/jline-reader/3.5.1/jline-reader-3.5.1.jar:/Users/bhauman/.m2/repository/clj-stacktrace/clj-stacktrace/0.2.8/clj-stacktrace-0.2.8.jar:/Users/bhauman/.m2/repository/com/google/javascript/closure-compiler-unshaded/v20180610/closure-compiler-unshaded-v20180610.jar:/Users/bhauman/.m2/repository/org/eclipse/jetty/websocket/websocket-server/9.2.21.v20170120/websocket-server-9.2.21.v20170120.jar:/Users/bhauman/.m2/repository/org/eclipse/jetty/websocket/websocket-common/9.2.21.v20170120/websocket-common-9.2.21.v20170120.jar:/Users/bhauman/.m2/repository/com/google/protobuf/protobuf-java/3.0.2/protobuf-java-3.0.2.jar:/Users/bhauman/.m2/repository/ring/ring-servlet/1.6.3/ring-servlet-1.6.3.jar:/Users/bhauman/.m2/repository/hawk/hawk/0.2.11/hawk-0.2.11.jar:/Users/bhauman/.m2/repository/ring/ring/1.6.3/ring-1.6.3.jar:/Users/bhauman/.m2/repository/org/eclipse/jetty/jetty-server/9.2.21.v20170120/jetty-server-9.2.21.v20170120.jar:/Users/bhauman/.m2/repository/com/google/code/findbugs/jsr305/3.0.1/jsr305-3.0.1.jar:/Users/bhauman/.m2/repository/ring/ring-core/1.6.3/ring-core-1.6.3.jar:/Users/bhauman/.m2/repository/rewrite-cljs/rewrite-cljs/0.4.3/rewrite-cljs-0.4.3.jar:/Users/bhauman/.m2/repository/com/google/code/gson/gson/2.7/gson-2.7.jar</div>

## The Classpath as a search path

If you look at the classpaths above you you will see a list of paths
separated by a `:` character. If you have ever looked at the `$PATH`
variable in your shell environment this should look familiar.

The classpath is a list of paths that will be used to search for
various artifacts. For instance Clojure uses the classpath to find and
load Clojure namespaces. ClojureScript does the same to find and
compile ClojureScript source files.

Another thing to notice about the classpaths above is that the
classpath starts with several **local** directories and then continues
with a quite of number of `.jar` files that represent our
dependencies.

If we format the classpath that [CLI tools][cli-tools] generated we
can see that it starts with a single local `src` directory.

```shell
$ clj -Spath |  sed -e 's/:/\'$'\n/g' | head
src
/Users/bhauman/.m2/repository/com/cognitect/transit-java/0.8.332/transit-java-0.8.332.jar
/Users/bhauman/.m2/repository/org/clojure/data.json/0.2.6/data.json-0.2.6.jar
/Users/bhauman/.m2/repository/org/clojure/clojure/1.9.0/clojure-1.9.0.jar
/Users/bhauman/.m2/repository/joda-time/joda-time/2.8.2/joda-time-2.8.2.jar
/Users/bhauman/.m2/repository/commons-codec/commons-codec/1.10/commons-codec-1.10.jar
...
```

This local `src` directory is a default that is added to the classpath by
[CLI Tools][cli-tools] if no other paths are configured in the
`deps.edn`.

## Looking up a source file on the Classpath

When the ClojureScript is looking for a namespace to analyze or
compile, it will utilize the classpath to find the file.

For example if a ClojureScript namespace `hello-world.core` is
required the ClojureScript compiler is going to utilize the classpath
to look for a file named `hello_world/core.cljs`. If our only local
path is `src` then `hello_world/core.cljs` is going to have to be in
the `src` directory in order to be found.

```
./
 ├── deps.edn
 └── src
     └── hello_world
         └── core.cljs
```

Here I'm adding an `example.trigger` namespace to show where it would
need to be placed.

```
./
 ├── deps.edn
 └── src
     ├── example
     │   └── trigger.cljs
     └── hello_world
         └── core.cljs
```

All of your ClojureScript and Clojure source files are going to need
to have namespace based paths that are rooted in a path on the
classpath.

## Adding additional paths to the Classpath

When you set up your project you will often have one or more source
directories. Some folks prefer to have a `cljs-src` directory that
only holds files relevant to a local ClojureScript codebase (i.e. a
front-end application).

**Adding the `cljs-src` path with CLI Tools**

Let's add the `cljs-src` path to our classpath with CLI Tools. In your
`deps.edn` file:

```clojure
{:deps {org.clojure/clojure {:mvn/version "1.9.0"}
        org.clojure/clojurescript {:mvn/version "1.10.339"}
        com.bhauman/figwheel-main {:mvn/version "0.1.9"}
        com.bhauman/rebel-readline-cljs {:mvn/version "0.1.4"}}
 :aliases {:fig {:main-opts ["-m" "figwheel.main"]}}
 ;; define paths here
 :paths ["src" "cljs-src"]}
```

Above we added `:paths` key with the value `["src" "cljs-src"]`. We have to
explicitly add `"src"` to the paths key because once it is defined the
`src` is no longer added implicitly.

**Adding the `cljs-src` path with Leiningen**

Let's add the `cljs-src` path to our classpath with CLI Tools. In your
`deps.edn` file:

```clojure
(defproject example-project "0.1.0-SNAPSHOT"
  ...
  :source-paths ["src" "cljs-src"]
  ...
  )
```

Above we added `:source-paths` key with the value `["src" "cljs-src"]`. We have to
explicitly add `"src"` to the paths key because once it is defined the
`src` is no longer added implicitly.

Unlike CLI Tools, Leiningen has several keys to add paths to the
classpath depending on the **type** of path it is. This is because
Leiningen provides jar bundling built-in and needs to know which paths
to bundle. All of the following `project.clj` configuration keys add
paths to the classpath.

* `:source-paths` - is for paths that hold source files
* `:resource-paths` - holds paths to assets that you want
* `:target-paths` - for paths that hold output files that can be safely deleted

#### Inspecting the new classpath

Now that we've added a new path let's verify that its working the way
we want it to.

```shell
$clj -Spath | sed -e 's/:/\'$'\n/g' | head -n 5
src
cljs-dev
/Users/bhauman/.m2/repository/com/cognitect/transit-java/0.8.332/transit-java-0.8.332.jar
/Users/bhauman/.m2/repository/org/clojure/data.json/0.2.6/data.json-0.2.6.jar
/Users/bhauman/.m2/repository/org/clojure/clojure/1.9.0/clojure-1.9.0.jar
```

and there we see `cljs-dev` is now on the classpath.

## Checkpoint: Project layout

We've been trying and learning different things so far. Let's apply
this to a project with a `hello_world.core` ClojureScript namespace
that looks like this:

Place this code in `src/hello_world/core.cljs`:
```clojure
(ns hello_world/core.cljs)

(js/console.log "Hello World!")
```

Ensure that `src` is on the classpath. For this example your project
directory file tree should look like this:

```
./
├── deps.edn # or project.clj
└── src
    └── hello_world
        └── core.cljs
```

We will continue to use this example from here on out.

## Using the classpath to find web assets

Once we start to understand the classpath we can use it to our
advantage. We can bundle our web assets (HTML, CSS, Javascript files)
in a jar and easily access them from inside our web process.

A common pattern in Clojure web development is to serve our public web
assets from a `public` directory that is on the classpath. This is the
default for Jetty server Figwheel provides to deliver compiled
ClojureScript files and other assets.

It is also a Clojure/Java idiom to place a `resources` directory on
the classpath to hold file assets that are not source files that we
will want to bundle and deploy with our application or library.

Concretely, we want to **create a local `resources` directory** and
**add it to the classpath**:

```shell
$ mkdir resources
```

**Add resources to the classpath with CLI Tools**

Edit `deps.edn`:

```clojure
{:deps {org.clojure/clojure {:mvn/version "1.9.0"}
        org.clojure/clojurescript {:mvn/version "1.10.339"}
        com.bhauman/figwheel-main {:mvn/version "0.1.9"}
        com.bhauman/rebel-readline-cljs {:mvn/version "0.1.4"}
 :aliases {:fig {:main-opts ["-m" "figwheel.main"]}
 ;; add "resources" path here
 :paths ["src" "resources"]}
```

> With Leiningen, there is no need to add `resources` to the classpath
> as Leiningen already adds it as a default. Extra Credit: check your
> Leiningen classpath and verify this for yourself.

Now that we have a `resources` directory on the classpath we will need
to place a `public` directory inside of it.

```shell
$ mkdir resources/public
```

Let's add a minimal [`robots.txt` file][robots-info] as an example
static file that we may want to serve from the root of our webserver.

Create a `resources/public/robots.txt` with the following contents:

```
User-agent: *
Disallow: /
```

So now we have a project directory that looks like this:

```
./
├── deps.edn # or project.clj
├── resources
│   └── public
│       └── robots.txt
└── src
    └── hello_world
        └── core.cljs
```

It will be helpful to learn how to get at this file from Clojure.

The following REPL session demonstrates how you can test that files
are accessible from the classpath.

```clojure
# start a REPL in the project root
$ clj
Clojure 1.9.0
user=> (require '[clojure.java.io :as io])
nil
user=> (slurp (io/resource "public/robots.txt"))
"User-agent: *\nDisallow: /\n"
user=> # control-C to exit
```

We were able to find the `public/robots.txt` file on the classpath,
this means our Webserver will be able to find it as well.

So now we have learned how to set the classpath so that the
ClojureScript compiler can find our source files and so that the
Webserver can find our static files. There is one more thing to
consider when setting up our project.

## The Target Path

When we compile our ClojureScript source code the resulting JavaScript
output files will need a place to go and that place will need to be on
the classpath so that the webserver can serve them.

An earlier convention, and one that you can still use, was to place
the output files in the `resources/public` directory alongside all of
your other web assets.

While that works, `figwheel.main` by default places output files in
the `target/public` directory. Using the `target` directory for output
files is a convention that is already used by Leiningen. The major
reason for this is that compiled files are temporary and need to be
deleted from time to time. Leiningen has a `lein clean` task
that deleted the `target` directory and thus cleans out all compiled
and cached assets allowing us to start fresh, and perhaps eliminate
stale files that are causing problems.

Because our output files are going to be placed in the `target/public`
directory by default and we want them to be served as static assets by
the webserver we will need to add the `target` directory (NOT
`target/public`) to our classpath.

**Adding the `target` directory to with Leiningen**

Unfortunately I'm not going to fully explain the changes to your
`project.clj` file as it would require introducing quite a few ideas.



The following config is going to add a path to your classpath in
developement mode so that the files don't get added to your deployed
uberjar. It's going to use the `:resource-paths` because it seems to
be the most appropriate considering how we are using it. We are also
making an adjustment so that we can call `lein clean` without
violating a Leiningen failsafe.

```clojure
(defproject example-project "0.1.0-SNAPSHOT"
  ...
  :profiles {:dev {:resource-paths ["target"]
                   :clean-targets ^{:protect false} ["target"]}}
  ...
  )
```

If you call `lein classpath` you will now see that `target` is indeed
on the classpath.

> Learn more about
> [Leiningen profiles here][lein-profiles] or with the lein command
> `lein help profiles`

**Adding the `target` directory with CLI Tools**

This is just a simple matter as we just need to add `"target"` to the
`:paths` key.

```clojure
{:deps {org.clojure/clojure {:mvn/version "1.9.0"}
        org.clojure/clojurescript {:mvn/version "1.10.339"}
        com.bhauman/figwheel-main {:mvn/version "0.1.9"}
        com.bhauman/rebel-readline-cljs {:mvn/version "0.1.4"}
 :aliases {:fig {:main-opts ["-m" "figwheel.main"]}
 ;; add "target" path here
 :paths ["src" "resources" "target"]}
```

If you now call `clj -Spath` you will see the local `target` direcotry
listed.

When working with CLI Tools we are also going to want to create the
`target` directory because paths on the classpath that we want to
resolve files in, need to exist before the JVM starts or file
resolution will not work.

## Final classpath and project layout

We have completed a rather long explanation of what paths need to be
on the classpath and why.

In summary, we need the following paths on our classpath to work
with `figwheel.main`:

* `src` *and other CLJS source directories* - so the compiler can find source files
* `resources` - so the webserver can serve static assets
* `target` - so the webserver can serve our compiled ClojureScript

Thus, our general project layout will look like this:

```
./
├── deps.edn # or project.clj
├── resources
│   └── public
│       # web assets HTML, CSS, images, etc
├── src
│   ├── hello_world
│   │   └── core.cljs
│   │       # other source files
└── target
    └── public
        # compiled ClojureScript files
```













[lein]: https://leiningen.org/
[lein-profiles]: https://github.com/technomancy/leiningen/blob/stable/doc/PROFILES.md
[cli-tools]: https://clojure.org/guides/getting_started#_installation_on_mac_via_code_brew_code
[classpath]: https://en.wikipedia.org/wiki/Classpath_(Java)
[robots-info]: http://www.robotstxt.org/robotstxt.html
