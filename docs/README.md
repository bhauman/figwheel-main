# Figwheel Main

[![Clojars Project](https://img.shields.io/clojars/v/com.bhauman/figwheel-main.svg)](https://clojars.org/com.bhauman/figwheel-main)

Figwheel Main builds your ClojureScript code and hot loads it as you are coding!

![Figwheel heads up example](https://s3.amazonaws.com/bhauman-blog-images/figwheel-main/figwheel-main-demo-image.png)

Get a quick idea of what figwheel does by watching the 6 minute
[flappy bird demo of figwheel][figwheel-demo-video].

Learn even more by watching a 45 minute
[talk on Figwheel][clojure-west-figwheel-video] given at ClojureWest
2015.

Read the [introductory blog post][flappy-bird-blog-post].

## Support Work on Figwheel and other Clojure tools

I contribute a significant amount of time writing tools and libraries
for Clojure and ClojureScript. If you enjoy using figwheel,
[rebel-readline](https://github.com/bhauman/rebel-readline),
[spell-spec](https://github.com/bhauman/spell-spec),
[cljs-test-display](https://github.com/bhauman/cljs-test-display) or
[piggieback](https://github.com/nrepl/piggieback) please consider
making a contribution.

<a href="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=B8B3LKTXKV69C">
<img src="https://s3.amazonaws.com/bhauman-blog-images/Smaller%2BDonate%2BButton%402x.png" width="200">
</a>

## Features

#### Live code reloading

If you write
[**reloadable code**](https://github.com/bhauman/lein-figwheel#writing-reloadable-code),
figwheel can facilitate automated live interactive programming. Every
time you save your ClojureScript source file, the changes are sent to
the browser so that you can see the effects of modifying your code in
real time.

#### Supports Node.js

#### Static file server

The inclusion of a **static file server** allows you to get a decent
ClojureScript development environment up and running quickly. For
convenience there is a `:ring-handler` option so you can load a ring
handler into the figwheel server.

#### Live CSS reloading

Figwheel will reload your CSS live as well.

#### Live JavaScript reloading

Figwheel can live reload your JavaScript source files.

#### Heads up display

Figwheel has a non-intrusive heads up display that gives you feedback
on how well your project is compiling. By writing a shell script you
can click on files in the heads up display and they will open in your
editor!

#### Descriptive Errors with Code Context

Figwheel provides descriptive compiler errors that point to where
the error is in your code.  These errors appear in the REPL as well
as the heads up display.

#### First Class Configuration Error Reporting

It can be quite daunting, when you are configuring a tool for the
first time.  Figwheel currently offers excellent configuration
error reporting that will help you if you happen to misconfigure
something.

#### Built-in ClojureScript REPL

When you launch Figwheel it not only starts a live building/reloading
process but it also optionally launches a CLJS REPL into your running
application. This REPL shares compilation information with the
ClojureScript compiler, allowing the REPL is to be aware of the code
changes as well. The REPL also has some special built-in control functions
that allow you to control the auto-building process and execute
various build tasks without having to stop and rerun `figwheel.main`.

#### Robust connection

Figwheel's connection is fairly robust. I have experienced figwheel
sessions that have lasted for days through multiple OS sleeps.

#### Message broadcast

Figwheel **broadcasts** changes to all connected clients. This means you
can see code and CSS changes take place in real time on your phone and
in your laptop browser simultaneously.

#### Respects dependencies

Figwheel will not load a file that has not been required. It will also
respond well to new requirements and dependency tree changes.

#### Won't load code that is generating warnings

If your ClojureScript code is generating compiler warnings Figwheel
won't load it. This, again, is very helpful in keeping the client
environment stable. This behavior is optional and can be turned off.

## Try Figwheel with Flappy Bird

#### Via Leiningen

Make sure you have the [latest version of leiningen installed](https://github.com/technomancy/leiningen#installation).

Clone this repo:

```shell
$ git clone https://github.com/bhauman/flappy-bird-demo-new.git
```

Change into the flappy-bird-demo-new directory and run:

```shell
$ lein fig:build
```

### Via Clojure Tools

First we will want to [install][CLI tools] the `clj` and `clojure` [command line
tools][CLI tools].

Clone this repo:

```shell
$ git clone https://github.com/bhauman/flappy-bird-demo-new.git
```

Change into the flappy-bird-demo-new directory and run:

```shell
$ clj -A:build
```

## Get started quickly with the template!

You can get a quick greenfield project with the [Figwheel Template][figwheel-main-template]

## Learning ClojureScript

If you are brand new to ClojureScript it is highly recommended that
you do the [ClojureScript Quick
Start](https://clojurescript.org/guides/quick-start)
first. If you skip this you will probably suffer.

There is a **lot to learn** when you are first learning ClojureScript,
I recommend that you bite off very small pieces at first. Smaller bites than
you would take when learning other languages like JavaScript and Ruby.

Please don't invest too much time trying to set up a sweet development
environment, there is a diverse set of tools that is constantly in
flux and it's very difficult to suss out which ones will actually help
you. If you spend a lot of time evaluating all these options it can
become very frustrating. If you wait a while, and use simple
tools you will have much more fun actually using the language itself.

## Read the Tutorial

[tutorial button here]

There is an [extensive getting started tutorial][tutorial] I highly
reccomend reading it if you are new to Clojure, ClojureScript and or
the new Clojure CLI tools.

## Getting Help

You can get help at both the
[ClojureScript Google Group](https://groups.google.com/forum/#!forum/clojurescript)
and on the **#clojurescript**, **#figwheel-main** and **#beginners**
[Clojurians Slack Channels](http://clojurians.net)

## Quick Usage

This is abbreviated usage documentation intended for experienced
Clojure/Script developers. I highly reccomend the [tutorial][tutorial]
if you are new to Figwheel and ClojureScript.

#### Clojure CLI Tools

First, make sure you have the [Clojure CLI Tools][CLI Tools]
installed.

On Mac OSX with brew:

    brew install clojure

Now launch a ClojureScript REPL with:

```
clj -Sdeps "{:deps {com.bhauman/figwheel-main {:mvn/version \"0.1.4\"}}}}"  -m figwheel.main
```

This will first compile browser REPL code to a temp directory, and
then a browser will open and a `cljs.user=>` prompt will appear.

From here you can do REPL driven development of ClojureScript.

#### With Leiningen

You can also use `leiningen` by adding it to `:dependencies` in your
`project.clj` and launching it like so:

```
lein run -m figwheel.main
```

**With Rebel Readline for much better REPL experience**

Figwheel main will automatically use `rebel-readline-cljs` if it is
available. So, you can get Rebel Readline behavior by simply adding it
to your dependencies.

```
clojure -Sdeps "{:deps {com.bhauman/figwheel-main {:mvn/version \"0.1.4\"} com.bhauman/rebel-readline-cljs {:mvn/version \"0.1.4\"}}}}"  -m figwheel.main
```

As of right now using Rebel readline does create some startup overhead
(hoping to correct this in the near future), so you may want to choose
use it only when you are going to interact at the REPL.

**Creating a build**

To define a build which will allow you work on a set of files and hot
reload them.

Ensure your `deps.edn` file has `figwheel.main` dependencies:

```clojure
{:deps {com.bhauman/figwheel-main {:mvn/version "0.1.4"}
        com.bhauman/rebel-readline-cljs {:mvn/version "0.1.4"}}
 ;; setup common development paths that you may be used to 
 ;; from lein
 :paths ["src" "target" "resources"]}
```

Create a file `dev.cljs.edn` build file:

```clojure
{:main example.core}
```

And in `src/example/core.cljs`

```clojure
(ns example.core)
(enable-console-print!)
(prn "hello world!")
```

and run the command:

```
clojure -m figwheel.main -b dev -r
```

This will launch a REPL and start autobuilding and reloading the `src`
directory so that any files you add or change in that directory will
be automatically hot reloaded into the browser.

The `-b` or `--build` flag is indicating that we should read
`dev.cljs.edn` for configuration.

The `-r` or `--repl` flag indicates that a repl should be launched.

Interesting to note that the above command is equivalent to:

```
clojure -m figwheel.main -co dev.cljs.edn -c -r
```

If would prefer to use your own HTML page to host your application
instead of the default page served by `figwheel.main`, you will first
need to ensure that you have added `resources` to the `:paths` key in
`deps.edn`, as demonstrated above. After that, you can place the
`index.html` in `resources/public/index.html` so that it will mask the
one served by the `figwheel.main` helper application.

The following is some example HTML to help you get started. The
trickly part is the path to the ClojureScript bootstrap file. The
default output path is available at `cljs-out/[build-id]-main.js`. So
in this case it will be: `cljs-out/dev-main.js`

```html
<!DOCTYPE html>
<html>
  <head>
  </head>
  <body>
    <div id="app"></div>
    <script src="cljs-out/dev-main.js"></script>
  </body>
</html>
```

You can place CSS and other static assets in the `resources/public` directory.

## Configuring Figwheel Main

If you need to configure `figwheel.main`, you will use a
`figwheel-main.edn` file in the root of your project directory.

For example let's explicitly set our watch directory.

Create a `figwheel-main.edn` file in the root of your project
folder with these contents:

```clojure
{:watch-dirs ["cljs-src"]
 :css-dirs ["resources/public/css"]}
```

`:watch-dirs` instructs `figwheel.main` to watch and compile the
sources in the `cljs-src` directory.

`:css-dirs` instructs `figwheel.main` to watch and reload the CSS
files in the `resources/public/css` directory.

If you need to override some of the figwheel configuration options for a
particular build, simply add those options as metadata on the build edn.

For example if you want to have `:watch-dirs` that are specific to the
**dev** build then in your `dev.cljs.edn` file:

```clojure
^{:watch-dirs ["cljs-src" "dev"]}
{:main example.core}
```

All the available configuration options are documented here: [https://github.com/bhauman/figwheel-main/blob/master/doc/figwheel-main-options.md](https://github.com/bhauman/figwheel-main/blob/master/doc/figwheel-main-options.md)

All the available configuration options specs are here:
[https://github.com/bhauman/figwheel-main/blob/master/src/figwheel/main/schema/config.clj](https://github.com/bhauman/figwheel-main/blob/master/src/figwheel/main/schema/config.clj)

## Classpaths, Classpaths, Classpaths

Understanding of the Java Classpath can be very helpful when working
with ClojureScript. 

ClojureScript searches for source files on the Classpath. When you add
a `re-frame` dependency like so:

```clojure
{:deps {com.bhauman/figwheel-main {:mvn/version "0.1.4"}
        com.bhauman/rebel-readline-cljs {:mvn/version "0.1.4"}
        ;; adding re-frame
        re-frame {:mvn/version "1.10.5"}}
 :paths ["src" "target" "resources"]}
```

The source files in `re-frame` are on the Classpath and the
ClojureScript compiler can find `re-frame.core` when you require it.

Your sources will need to be on the Classpath so that the Compiler can
find them. For example, if you have a file
`cljs-src/example/core.cljs` you should add `cljs-src` to the `:paths`
key so that the ClojureScript compiler can find your `example.core`
namespace. It is important to note that the `src` directory is on your
Classpath by default.

In Figwheel, the embedded HTTP server serves its files from the Java
Classpath.

It actually serves any file it finds on the Classpath in a `public`
sub-directory. This is why we added `target` and `resources` to the
`:paths` key in the `deps.edn` file above. With `target` and
`resources` both on the Classpath the server will be able to serve
anyfile in `target/public` and `resources/public`.

The compiler by default compiles artifacts to `target` for easy cleaning.

It is custmary to put your `index.html`, CSS files, and other
web artifacts in the `resources/public` directory.

## Working with Node.js

Unlike `cljs.main`, with `figwheel.main` you will not specify a
`--repl-env node` because the `figwheel.repl` handles Node.js REPL
connections in addition to others.

You can launch a Node REPL like so:

    clojure -m figwheel.main -t node -r
    
You can quickly get a hot reloading CLJS node build up an running using the
`deps.edn`, `example.core` and `dev.cljs.edn` above. Simply add a `--target node`
or `-t node` to the compile command.

    clojure -m figwheel.main -t node -b dev -r

This will launch a CLJS Node REPL initialized with `example.core` you
can now edit `example/core.cljs` and it will be hot reloaded.
    
Of course if you add `:target :nodejs` to `dev.cljs.edn` like so:

```clojure
{:main example.core
 :target :nodejs}
```

You be able to run the build more simply:

    clojure -m figwheel.main -b dev -r

## Reload hooks

It is common to want to provide callbacks to do some housekeeping
before or after a hot reload has occurred.

You can conveniently configure hot reload callbacks at runtime with
metadata. You can see and example of providing callbacks below:

```clojure
;; first notify figwheel that this ns has callback defined in it
(ns ^:figwheel-hooks example.core)

;; mark the hook functions with ^:before-load and ^:after-load 
;; metadata

(defn ^:before-load my-before-reload-callback []
    (println "BEFORE reload!!!"))

(defn ^:after-load my-after-reload-callback []
    (println "AFTER reload!!!"))
```

The reload hooks will be called before and after every hot code reload.

## Quick way for experienced devs to understand the command line options

You can supply a `-pc` or `--pprint-config` flag to `figwheel.main`
and it will print out the computed configuration instead of running
the command.

For example:

```
clojure -m figwheel.main -pc -b dev -r
```

Will output:

```clojure
---------------------- Figwheel options ----------------------
{:ring-server-options {:port 9550},
 :client-print-to [:repl :console],
 :pprint-config true,
 :watch-dirs ("src"),
 :mode :repl}
---------------------- Compiler options ----------------------
{:main exproj.core,
 :preloads [figwheel.core figwheel.main figwheel.repl.preload],
 :output-to "target/public/cljs-out/dev-main.js",
 :output-dir "target/public/cljs-out/dev",
 :asset-path "cljs-out/dev",
 :aot-cache false,
 :closure-defines
 #:figwheel.repl{connect-url
                 "ws://localhost:9550/figwheel-connect?fwprocess=c8712b&fwbuild=dev",
                 print-output "repl,console"}}
```


{::comment}

[Under Construction]

## Figwheel Innovations

When it was first released, Figwheel represented an relatively new
front end development experience. It introduced a hot code reloading
workflow that was very fast and worked well, and combined that with a
heads up display which presented compile time messages like exceptions
and warnings in the same browser window where you were working. By
eliminating the code and reload cycle, figwheel had greatly increased
the amount of time that I spent in a productive coding state.

It also introduced a more complete configuration validation that
attempted to go the extra mile and really help the person who made the
mistake. It offered visual feedback on where the mistake was make and
did its best to offer a solution along with relative documentation. I
feel like this was very helpful because the number of questions that I
received about how to use figwheel dropped significantly.

{:/comment}

[tutorial]: http://rigsomelight.com/figwheel-main/tutorial
[figwheel-main-template]: http://rigsomelight.com/figwheel-main-template
[clojure-west-figwheel-video]: https://www.youtube.com/watch?v=j-kj2qwJa_E
[figwheel-demo-video]: https://www.youtube.com/watch?v=KZjFVdU8VLI 
[flappy-bird-blog-post]: http://rigsomelight.com/2014/05/01/interactive-programming-flappy-bird-clojurescript.html
[figwheel-main]: https://github.com/bhauman/figwheel-main
[install-lein]: https://github.com/technomancy/leiningen#installation
[CLI Tools]: https://clojure.org/guides/getting_started#_installation_on_mac_via_code_brew_code

