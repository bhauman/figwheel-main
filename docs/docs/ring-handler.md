---
title: Embed a Ring Handler
layout: docs
category: docs
order: 10
published: true
---

# Embed a Ring Handler

<div class="lead-in">You can quickly get started creating a back-end
for your application by embedding a <a
href="../config-options#ring-handler"><code>:ring-handler</code></a>
in the Figwheel server.</div>

The Figwheel server is primarily needed to provide a connection for
REPL communication. However, it is a server so it's handy to allow
developers to leverage this server so they can concentrate on the
problems they are actually trying to solve.

Figwheel allows you to specify the name of a function in your Figwheel
configuration options in `figwheel-main.edn` or in the metadata of
your build file under the key [`:ring-handler`][ring-handler].

A quick example.

In `src/hello_world/app_server.clj`

```clojure
(ns hello-world.app-server)

(defn handler [req]
  {:status 404
   :headers {"Content-Type" "text/html"}
   :body "Yep the server failed to find it."})
```

And in your `figwheel-main.edn` file add a [`:ring-handler`][ring-handler] key:

```clojure
{...
 :ring-handler hello-world.app-server/handler
 ...}
```

Now when you start your `figwheel.main` build when you try to access
an endpoint that doesn't exist you will see the `Yep the server failed
to find it.` message.

You can learn more about [Ring](https://github.com/ring-clojure/ring)
by exploring the [Wiki](https://github.com/ring-clojure/ring/wiki) and
[API](http://ring-clojure.github.io/ring/). This
[wiki document on Ring concepts](https://github.com/ring-clojure/ring/wiki/Concepts)
provides a good summary of how Ring works.

## The last handler

The Figwheel server has a chain of middleware that helps it do its
job, this chain of middleware is composed on top of the popular
[`ring-defaults`](https://github.com/ring-clojure/ring-defaults)
middleware stack.

The function you specify in [`:ring-handler`][ring-handler] will be
the last handler in the Figwheel middleware chain. This means it will
only be called if the request isn't handled by other middleware.

This can cause confusion if you have an `index.html` file present and
you are trying to respond to a request for the root path `/` in your
handler. In this case the Figwheel middleware will handle the root
path returning the `index.html` file before the request has a chance
to make it to your handler.

## Returning `index.html` at specific routes for an SPA

If you are doing `pushState` history based routing, which is common in
Single Page Applications, you will probably want to return the
`index.html` for a given set of routes.

Here's an example Ring handler that does this.

In `src/hello_world/app_server.clj`:

```clojure
(ns hello-world.app-server
  (:require 
    [ring.util.response :refer [resource-response content-type not-found]]))

;; the routes that we want to be resolved to index.html
(def route-set #{"/" "/contact" "/menu" "/about"})

(defn handler [req]
  (or
   (when (route-set (:uri req))
     (some-> (resource-response "index.html" {:root "public"})
             (content-type "text/html; charset=utf-8")))
   (not-found "Not found")))
```

## A more advanced Ring Handler

A quick example of a advanced Ring handler that uses higher level
Clojure web libraries.

You will need to add
[compojure](https://github.com/weavejester/compojure) and
[hiccup](https://github.com/weavejester/hiccup) to your dependencies
for the following `:ring-handler` to work.

```clojure
(ns hello-world.app-server
  (:require 
    [compojure.core :refer [defroutes GET]]
    [compojure.route :as route]
    [hiccup.page :refer [html5 include-js include-css]]))

(defn index-html []
  (html5
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1"}]
    (include-css "/css/style.css")]
   [:body
    [:div {:id "app"}]
    (include-js "/cljs-out/dev-main.js")]))

(defroutes handler
  (GET "/" [] (index-html))
  (route/not-found "<h1>Page not found</h1>"))
```

The `defroutes` macro above creates a ring handler function that will
work as a `:ring-handler` parameter.

## ring-defaults

It's also important to remember that the Figwheel's server already
uses [ring-defaults](https://github.com/ring-clojure/ring-defaults)
middleware. 

When you create [your own server](your-own-server),
this middleware will not be present and you will have to supply it by
wrapping your handler via `(ring.middleware.defaults/wrap-defaults
your-app-handler ring.middleware.defaults/site-defaults)`

[ring-handler]: ../config-options#ring-handler
