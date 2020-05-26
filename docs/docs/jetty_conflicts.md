---
title: Jetty Conflicts (Datomic Cloud)
layout: docs
category: docs
order: 25
published: true
---

# Jetty Conflicts

> This should be fixed if you upgrade to `figwheel-main 0.2.1-SNAPSHOT`
> or later.

<div class="lead-in"> Figwheel currently uses the same Jetty webserver
version that the <a href="https://github.com/ring-clojure/ring/tree/master/ring-jetty-adapter">Ring Jetty Adapter</a> uses. Unfortunately Jetty is
packaged in a way that is prone to version conflicts.</div>

Moving to Jetty means that Figwheel is using the same server that many
Clojure applications are already using. While this can be helpful in
terms of consistent behavior, supporting HTTPS and minimizing
dependencies, it unfortunately also leads to version conflicts
because of the way Jetty is packaged. The most common conflict that
folks seem to experience is when they include the [Datomic
Cloud](https://docs.datomic.com/cloud/index.html) client.

This also can happen if you update your version of Ring to `1.7.1`.

## Fixing the conflict by specifying explicit Jetty deps

Currently if I add `com.datomic/client-cloud {:mvn/version "0.8.71"}`
to a `deps.edn` file along with `com.bhauman/figwheel-main`, trying to
start a build with `figwheel.main` with fail with:

```
java.lang.NoClassDefFoundError: org/eclipse/jetty/http/HttpParser$ProxyHandler
```

This is due to a dependency mismatch between Jetty deps.

The most sure fire way to fix this error is to explicitly specify all
the Jetty dependencies in your project and ensure they are all the
same version.

In your `deps.edn` this would look like this:

```clojure
{:deps {org.clojure/clojurescript {:mvn/version "1.10.339"}
        com.bhauman/figwheel-main {:mvn/version "0.2.6"}
		com.datomic/client-cloud {:mvn/version "0.8.71"}
	    
		;; directly specify all jetty dependencies
		;; ensure all the dependencies have the same version
		org.eclipse.jetty/jetty-server                {:mvn/version "9.4.12.v20180830"}
		org.eclipse.jetty.websocket/websocket-servlet {:mvn/version "9.4.12.v20180830"}
		org.eclipse.jetty.websocket/websocket-server  {:mvn/version "9.4.12.v20180830"}
		}}
```

The next time you start Figwheel the error should be gone.

## Conflicts in general

You can detect these conflicts by looking at the dependency tree of
your application.

Leiningen has excellent support for pointing out these conflicts.

If you are using Leiningen then you can run the `lein deps :tree`
command.

Take some time and carefully read the output. It will print out
dependency conflicts and make suggestions as to how you can fix them.

It also prints out the dependency tree that it is using, so take some
time to examine that as well.

### with Clojure CLI tools

Clojure CLI tools doesn't have the helpful output that Leiningen
currently supplies. It can print out the dependency tree:

```
$ clj -S:tree
```





