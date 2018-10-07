---
title: Jetty Conflicts (Datomic Cloud)
layout: docs
category: docs
order: 25
published: true
---

# Jetty Conflicts

<div class="lead-in"> Figwheel currently uses the same Jetty webserver
version that the <a href="https://github.com/ring-clojure/ring/tree/master/ring-jetty-adapter">Ring Jetty Adapter</a> uses. Unfortunately Jetty is
packaged in a way that is prone to version conflicts.</div>

Moving to Jetty means that Figwheel is using the same server that many
Clojure applications are already using. While this can be helpful in
terms of consistent behavior, supporting HTTPS and miniming
dependencies, this also leads to inevitable version conflicts because
of the way Jetty is packeged. The most common conflict that folks
experience is when they include the
[Datomic Cloud](https://docs.datomic.com/cloud/index.html) client.

The ultimate solution to this problem may be turning away from Jetty
entirely and writing a simpler server based on
[Netty](https://netty.io), or perhaps using
[Undertow](https://github.com/undertow-io/undertow).

## Fixing the Datomic Cloud Conflict

Currently if I add `com.datomic/client-cloud {:mvn/version "0.8.63"}`
to a `deps.edn` file along with `com.bhauman/figwheel-main`, trying to
start a build with `figwheel.main` with fail with 

```
java.lang.NoClassDefFoundError: org/eclipse/jetty/http/HttpParser$ProxyHandler
```

This is due to a dependency missmatch.

You can fix this by excluding the conflicting dependencies from the
`com.datomic/client-cloud` dependenciy in your `deps.edn` file:

```
com.datomic/client-cloud {:mvn/version "0.8.63"
                          :exclusions [org.eclipse.jetty/jetty-http
                                       org.eclipse.jetty/jetty-util
                                       org.eclipse.jetty/jetty-io]}
```

The next time you start Figwheel the error should be gone.

## Conflicts in general

You can detect these conflicts by looking at the dependency tree of
your application.

Leinigen has excellent support for pointing out these conflicts.

If you are using Leiningen then you can run the `lein deps :tree`
command.

Take some time and carefully read the output. It will print out
dependency conflicts and make suggestions as to how you can fix them.

It also prints out the dependency tree that it is using, so take some
time to examine that as well.

### with Clojure CLI tools

Clojure CLI tools doesn't have the helpful output that Leinigen
currently supplies. It can print out the dependency tree:

```
$ clj -S:tree
```





