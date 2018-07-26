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
ClojureScript. Here we'll explore how to supply your own host
page.</div>

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
of the server (normally `http://locahost:9500`) In response the
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
    <!-- include your ClojureScript at the bootom of body like this -->
    <script src="/cljs-out/dev-main.js"></script>
  <body>
</html>
```

## Using a page other than server root

A very simple solution is to provide a static HTML page other than the
root path.

By default Figwheel opens up `http://localhost:9500`




[ring-handler]: ../config-options#ring-handler
[open-url]: ../config-options#ring-handler
[classpaths-web-assets]: classpaths#using-the-classpath-to-find-web-assets


