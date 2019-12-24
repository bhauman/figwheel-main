---
title: Background Builds
layout: docs
category: docs
order: 15
published: true
---

# Background Builds

<div class="lead-in">Background builds allow you to run more than one
Figwheel hot reloading build process at a time.</div>

Background builds can be started with the `--background-build` (`-bb`)
Figwheel Main CLI option.

For instance, let's say you want to run a build that compiles your
main application and a build that compiles an admin backend
application. The build files for both are respectively named
`app.cljs.edn` and `admin.cljs.edn`. You can start both builds with
the following command:

```shell
$ clojure -m figwheel.main -bb admin -b app -r
```

The above command will run both the `admin` and `app` builds and allow
you to work on both of them at the same time.

## Difference from foreground build

Background builds are not full fledged citizens like the build you
supply to the `--build` option.

In the above example we'll call the `app` build the foreground
build. The foreground build is different because its configuration is
what gets applied to the Figwheel server when it starts. Practically
speaking, if both builds have defined their own
[`:ring-handler`][ring-handler], only the `app` build's ring-handler
will be installed. The same goes for all server related options like
`:ring-server-options` etc.

> **Background builds** do not affect the Figwheel server configuration.

Background builds only launch a file watcher/builder and maintain a
REPL connection to the client environment to allow Figwheel to send
reload and error messages. REPL connections are maintained for each
build, but only the foreground build gets an interactive REPL on
terminal.

Background builds should always be builds which initiate a running
watcher/build process.

## Background build requirements

Background builds usually have to define a [`:watch-dirs`][watch-dirs] Figwheel
configuration option.

Background builds also need to define their own [host page][host-page]
as both applications need their own client environment.

## Background build for Testing

Background builds are an ideal solution if you want to run your test
suite in the Browser and you want it to react and run every time you
change a file.

[host-page]: your_own_page
[watch-dirs]: ../config-options#watch-dirs
[ring-handler]: ../config-options#ring-handler
