---
title: Host Page (index.html)
layout: docs
category: docs
order: 6
---

# Host Page

<div class="lead-in">A <strong>host page</strong> is the HTML page
that includes your ClojureScript program. Figwheel provides a default
host page, but sooner or later you will want to supply your own.</div>

## The Figwheel server

Figwheel starts a server when it launches a build and/or a REPL. The
primary purpose of this server is to provide websocket communication
between the REPL and the client environment. Figwheel not only uses
this connection to evaluate compiled REPL expressions, it also uses it
to communicate hot reloads, compile errors, and other things.

The secondary use of this server is as an initial development HTTP
server. It can serve static assets (HTML, images, CSS, etc.) from the
classpath (this is described in
[Classpaths][classpaths-web-assets]). It also lets you supply a **ring
handler** (via [`:ring-handler` config option][ring-handler])
to embed your own HTTP endpoints in the server.

*The instructions on this page only apply if you are using the Figwheel
server to serve your application.*

> It is important to note that you can and most likely should supply
> your own server to run alongside of Figwheel's server as the needs
> of your project outgrow what the dev server provides. Figwheel is
> designed to handle the cross-origin communication necessary to make
> a connection to a page served by your own server.

## Default Host Page

By default when Figwheel launches it navigates a browser to the root
of the server (usually `http://locahost:9500`). In response, the
Figwheel server provides a **default dev page** if the root route
(`/`) fails and no `public/index.html` file is found on the classpath.

## Including your compiled ClojureScript

When you supply HTML content for a host page, it is important to
correctly include your compiled CLJS code.

By default, Figwheel will direct the compiler to output your compiled
CLJS to `target/public/cljs-out/[build]-main.js`
(substituting your build name for `[build]`).

If you have doubts about where the compiler is placing your output file,
you can examine Figwheel's start-up messages.

<img width="718" alt="showing output file" src="https://user-images.githubusercontent.com/2624/43284699-a17b215c-90ea-11e8-81c8-3f40c9f1e61c.png">

As you can see in the above start-up messages, Figwheel is compiling
the build to `target/public/cljs-out/dev-main.js`.

In order to include this file in our host page, we will
utilize the path to the output file **minus** the classpath based
webroot `target/public`. This leaves us with `/cljs-out/dev-main.js`
as the path to our compiled ClojureScript.

> You should take a moment to look at the content of this file. You
> will see that it is actually a bootstrap script which itself
> requires all of the necessary files for the program.  This is not
> the case when you use a different `:optimizations` setting such
> `:simple`, `:whitespace` or `:advanced`. When these settings are
> used the output file will be a single file. You may be tempted to
> use these settings for development, but for the REPL and Figwheel to
> work the default `:optimizations` setting of `:none` is required.

When you include the output file on your HTML page you will want to place
the `<script>` tag as the last tag in your `<body>` content. This is
the convention for Google Closure compiled projects.

An example host page:

```html
<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link href="css/style.css" rel="stylesheet" type="text/css">
    <link rel="icon" href="https://clojurescript.org/images/cljs-logo-icon-32.png">
  </head>
  <body>
    <div id="app"></div>
    <!-- include your ClojureScript at the bottom of body like this -->
    <script src="/cljs-out/dev-main.js" type="text/javascript"></script>
  </body>
</html>
```

> The `css/style.css` file will need to be available on the
> classpath. In our running example, a good place for this file would
> be at `resources/public/css/style.css`.

## Providing your own page

There are several ways to override the default dev page and provide
your own page:

* supply a static `index.html` file in a `public` directory on the
  classpath
* use a page other than `index.html` and change the [`:open-url` config
  option][open-url] to point to it
* create a Ring handler that handles the root route `/` and supply it
  to the [`:ring-handler` config option][ring-handler]

## Supply a static index.html

The most common way to use your HTML content to host your app is to
place an `index.html` file in a `public` directory on the
classpath. The most common place for this is
`resources/public/index.html`.

## Using a page other than server root

Another way is to provide a static HTML page other than the
`index.html` root page. This pattern is very helpful if you need to
have several different host pages.

As an example, let's say we want our build to use `admin.html` as
the host page. First, we will create a `resources/public/admin.html`
file and ensure that we are requiring our compiled ClojureScript
correctly.

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

> We are about to introduce you to some Clojure code. If you have
> never worked with Clojure up to now, I highly recommend you
> understand how to work with [Clojure at the REPL][clojure-repl]
> before starting this. If you do want to go ahead with this example,
> first start your `hello-world.core` example build running with
> `clojure -m figwheel.main -b dev -r` and keep your the browser page
> open and in view while you code it, as this will give you important
> feedback about the correctness of the Clojure code as you work. Only
> configure the `:ring-handler` option when you are sure the code is
> correct and loadable.

Without further ado here is our fancy handler to serve our HTML host
page.

```clojure
(ns hello-world.server)

;; define index content
(def home
  "<!DOCTYPE html>
<html>
  <head>
    <meta charset=\"UTF-8\">
    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">
    <link href=\"css/style.css\" rel=\"stylesheet\" type=\"text/css\">
    <link rel=\"icon\" href=\"https://clojurescript.org/images/cljs-logo-icon-32.png\">
  </head>
  <body>
    <div id=\"app\"></div>
    <script src=\"/cljs-out/dev-main.js\" type=\"text/javascript\"></script>
  </body>
</html>")

(defn handler [request]
  (if (and (= :get (:request-method request))
           (= "/"  (:uri request)))
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body home}
    {:status 404
     :headers {"Content-Type" "text/plain"}
     :body "Not Found"}))
```

Fancy eh?

Now we'll configure Figwheel to embed the handler with the
[`:ring-handler` config option][ring-handler] in our build file.

Let's configure the ring handler in our example `dev.cljs.edn`:

```clojure
^{:ring-handler hello-world.server/handler}
{:main hello-world.core}
```

Now when you start Figwheel and the browser opens up to display the
root server route it will be served from your handler.

If you are creating an SPA with `pushState` routing you may want every
route or a small subset of routes to return the `home` page. As you can
see it would be pretty straight forward to modify the above handler to
return the `home` page when needed.

[ring-handler]: ../config-options#ring-handler
[open-url]: ../config-options#open-url
[classpaths-web-assets]: classpaths#using-the-classpath-to-find-web-assets
[clojure-repl]: https://clojure.org/guides/repl/introduction
