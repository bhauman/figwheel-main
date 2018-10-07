---
title: Your Own Server
layout: docs
category: docs
order: 11
published: true
---

# Your own server

<div class="lead-in">Injecting a <code class="highlighter-rouge">:ring-handler</code> into the Jetty server
that Figwheel uses primarily for REPL communication is a convenient
way to start building server side functionality. However it is easy to
outgrow this built-in server. This guide will detail how to setup a
separate HTTP server for your application and still benefit from
working with Figwheel.</div>

It normally doesn't take long to outgrow using `:ring-handler` to
embed server side functionality in Figwheel. If there is a server side
component to your application, it is inevitable that you will need to
setup a server to serve your app when you deploy it. So in many cases
you can't get by without it.

## Requirements for working with Figwheel

Normally there is very little that needs to be done so that you can
continue to develop using Figwheel while serving your application from
a different server.

You **will** need to run Figwheel as well as your application
server. It can be helpful to think of Figwheel as a separate tooling
process that has its own Websocket server to handle REPL
communication.

Figwheel's tooling process is not required to be in a separate JVM
process from your application server but it can be. Your application
server can be written in Ruby, Node, Python, C#, Clojure, and Figwheel
will still work.

There are only two things you need to ensure to make Figwheel work
along side your own application server.

The separate server should:

1. serve an HTML host page that loads your CLJS app
   correctly
2. serve the compiled ClojureScript artifacts of your application

In most of the cases that is all that is needed to use your own server.

**No need to proxy**

Many folks worry about Cross Origin problems and believe that they
need to proxy an endpoint on their app server to the Figwheel
server. This is not needed at all. Figwheel has been setup to handle
[CORS][cors] problems.

**How this works**

The reason why Figwheel works as a Cross Origin Websocket is the same
reason that services like Firebase and Pusher work as Cross Origin
services.

In this case, Figwheel is responsible for compiling your ClojureScript
into JavaScript artifacts that get loaded by the browser. When it
compiles these artifacts it inserts a URL that points to the Figwheel
server to establish a Websocket connection. So when your app server
serves these compiled artifacts the Figwheel client will call home to
the Figwheel server to establish a REPL connection. Since the Figwheel
server allows [CORS][cors] requests this works just fine.

Figwheel was designed to work this way to support the inevitable need
to use your own server.

## Connecting Remotely

When you are not developing locally and connecting to localhost you
will need to set the
[`:connect-url` config option](../config-options#connect-url) so that
Figwheel knows which server host to connect back to.

This is something that is required whether you use your own server or
not.

## Example of using your own server

We will start from the
[base `hello-world` example app][base-example-gist].

We will create a Ring server and launch it via a script. Create a
file at `scripts/server.clj` with the following content: 

```clojure
(require '[ring.adapter.jetty :refer [run-jetty]])
(require '[ring.middleware.defaults :refer [wrap-defaults site-defaults]])
(require '[ring.util.response :refer [resource-response content-type]])

(defn handler [req]
  (or
   (when (= "/" (:uri req))
     (some-> (resource-response "index.html" {:root "public"})
             (content-type "text/html; charset=utf-8"))) 
   {:status 404
    :headers {"Content-Type" "text/html"}
    :body "Not found"}))

(run-jetty
 (wrap-defaults handler site-defaults)
 {:port 4000})
```

The above Clojure file defines a Ring handler and runs the server when
it is loaded.

If you haven't defined an `index.html` file yet, create a file like the
following at `resources/public/index.html`:

```html
<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
  </head>
  <body>
    <div id="app"></div>
    <!-- include your ClojureScript at the bottom of body like this -->
    <script src="/cljs-out/dev-main.js" type="text/javascript"></script>
  </body>
</html>
```

If you don't already have a [host page](your_own_page) for your
application and you are not sure how to create one, please see the
[documentation on this subject](your_own_page).

Of course we can generate and serve the above HTML directly from the
server, but I'm choosing this to keep things simple and consistent
with the other guides.

This is all you need to start your own server while you are running a
Figwheel tooling process.

You can start the server along with your Figwheel build with the
following command:

```shell
$ clojure -i scripts/server.clj -m figwheel.main -b dev -r
```

You can also add these CLI options to an alias in your `deps.edn` file
to make things more concise on the command line.

[base-example-gist]: https://gist.github.com/bhauman/a5251390d1b8db09f43c385fb505727d
[cors]: https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS




