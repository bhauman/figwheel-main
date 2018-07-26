---
title: Host Page (index.html)
layout: docs
category: docs
order: 6
---

# Host Page

<div class="lead-in">A <strong>host page</strong> is the HTML page
that you ClojureScript program will run in. Figwheel provides a
default host page to make easy to start working with
ClojureScript. Let's explore how to supply your own.</div>

## The Figwheel server

Figwheel starts a server when it launches a build and or a REPL. The
primary purpose of this server is to provide websocket communication
between the REPL and the client environment. Figwheel not only uses
this connection to evalute compiled REPL expressions, it also uses it
to communicate hot reloads, compile errors, and other things.

The secondary use of this server is as an initial development HTTP
server. It can serve static assets (HTML, images, CSS, etc.) from the
classpath (this is described in
[Classpaths][classpaths-web-assets]). It also lets you supply a **ring
handler** (via [`:ring-handler` config option][ring-handler])
to embed your own HTTP endpoints in the server.

> Its important to note that you can and most likely should supply
> your own server at some point during the development of your
> project. Figwheel will still work well when you use your own
> server. Figwheel is designed to handle the cross-origin
> communication, and you do not have to proxy to the Figwheel server.

## Default Host Page

By default when Figwheel launches it navigates a browser to the root
of the server (normally `http://locahost:9500`). In response, the
Figwheel server provides a **default index page** if the root route
`/` fails and no static `index.html` is found. 

While this page is helpful there will quickly be a point when will
want to supply your own page.

## Providing your own page

There are several ways to do this:

* use a page other than `index.html` and change the [`:open-url` config
  option][open-url] to point to it
* supply a static `index.html` file in a `public` directory on the
  classpath
* create a Ring handler that handles the root route `/` and supply it
  to the [`:ring-handler` config option][ring-handler]

## Including your compiled ClojureScript

When you supply HTML content for a host page the most important thing
you will need to do is include your compiled CLJS code.

When you are working with the defaults the Figwheel will direct the
compiler to ouput put your compiled CLJS to
`target/public/cljs-out/[build]-main.js`. Where you will substitute
the name of your build (i.e `dev`).

Figwheel will also print the output file when it starts up.

<img width="718" alt="showing output file" src="https://user-images.githubusercontent.com/2624/43284699-a17b215c-90ea-11e8-81c8-3f40c9f1e61c.png">

As you can see from the above Figwheel is compiling our project to
`target/public/cljs-out/dev-main.js`.

In order to include this file on our host page we will include use the
above path **minus** the classpath based webroot `target/public`. This
leaves us with `/cljs-out/dev-main.js` as the path to our compiled
ClojureScript file.

> You should take a moment to look at the content of this file. You
> will see that it is a bootstrap script which itself requires all of
> the necessary files for program.  This is not the case when you use
> a different `:optimizations` setting such `:simple`, `:whitespace`
> or `:advanced`. In that case the output file will be a single file.
> You may be tempted to use these settings for development, but for
> the REPL and Figwheel to work the default `:optimizations` setting
> of `:none` is required.

When you include the output file on your HTML page you will want place
the `<script>` tag as the last tag in your `<body>` content. This is
the convention for Google Closure compiled projects.

For example:

```html
<html>
  <head></head>
  <body>
    <div id="app">
    </div>
    <!-- include your ClojureScript at the bottom of body like this -->
    <script src="/cljs-out/dev-main.js"></script>
  <body>
</html>
```

## Supply a static index.html 

The most common way to use your HTML content to host your app is to
place an `index.html` file in a `public` directory on the
classpath. The most common place for this is
`resources/public/index.html`.

## Using a page other than server root

A very simple solution is to provide a static HTML page other than the
`index.html` root page. This is pattern is very helpful if you need to
have several different host pages.

For example let's we want our build to use an `admin.html` as the host
page. First we will create a `resources/public/admin.html` file and
ensure that we are requiring our compiled ClojureScript correctly.

Once that is done we will want to make our build pop open the
`admin.html` url in our browser. We can use the
[`:open-url` config option][open-url] to do this.

In our build file we'll set `:open-url` like so:

```clojure
^{:open-url "http://localhost:[[server-port]]/admin.html"}
{:main example.core}
```

## Create a Ring handler that handles the root route

It's surprisingly easy to start building out server side actions in
Figwheel. Let's create a handler that responds to the root route and
delivers the HTML content to host our app.

You can create a `server.clj` file right next to CLJS source
files. In our example `hello-world` app the handler would be at
`src/hello_world/server.clj`.

This file will be a Clojure source file rather than a ClojureScript
source file and it will be running in the Clojure server process.

Without further ado here is our fancy handler to serve our HTML host
page.

```clojure
(ns hello-world.server)

;; define index content
(def home
  "<html>
  <head></head>
  <body>
    <h2>Hi from the handler</h2>
    <div id=\"app\"></div>
    <script src=\"/cljs-out/dev-main.js\"></script>
  </body>
</html>")

(defn handler [request]
  (if (and (= :get (:method request))
           (= "/"  (:uri request)))
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body home}}
    {:status 404
     :headers {"Content-Type" "text/plain"}
     :body "Not Found"}))
```

Fancy eh?

Now we'll configure figwheel to embed the handler with the
[`:ring-handler` config option][ring-handler] in our build file.

Let's configure the ring handler in our example `dev.cljs.edn`:

```clojure
^{:ring-handler hello-world.server/handler}
{:main hello-world.core}
```

Now when you start Figwheel and the browser opens up to display the
root server route it will be served from your handler.

If you are creating an SPA wish `pushState` routing you may want every
route or a small subset of routes to return the `home` page. As you can
see it would be pretty straight forward to modify the above handler to
return the `home` page when needed.




[ring-handler]: ../config-options#ring-handler
[open-url]: ../config-options#open-url
[classpaths-web-assets]: classpaths#using-the-classpath-to-find-web-assets


