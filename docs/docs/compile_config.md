---
title: Compile Configuration
layout: docs
category: docs
order: 8
---

# Compile Configuration

<div class="lead-in">The ClojureScript compiler has a dizzying number
of <a
href="https://clojurescript.org/reference/compiler-options">configuration
options</a>. However, there are really only a few key ones you need to
understand to work effectively.</div>

Figwheel handles setting up several compilation options for you, but
it's very important to understand these options, and how Figwheel uses
them to inject itself into a build. Having this knowledge will empower
you to understand how your build is working and how to adjust the
options so that they serve you better.

Here are the compiler options we'll cover:

* `:output-to`
* `:output-dir`
* `:target`
* `:optimizations`
* `:main`
* `:asset-path`
* `:preloads`
* `:clojure-defines`

## The `--print-config` option

You can use the `figwheel.main` CLI option `--print-conig` (or `-pc`)
to examine the compile options that Figwheel is sending to the
ClojureScript compiler. The `--print-config` CLI flag will only print
out the configuration, it will not run the command.

> It's important to note that order matters for `figwheel.main` CLI
> options. The `-pc` flag is considered an **init option** and as such
> it must always come before a **main option** (`-c`, `-b`, `-r`, `-m`
> are all main options). In the commands that we've been using so far
> in this documentation, this means it has to come **before** the `-b`
> option.

Keeping with our `hello-world.core` example let's print out the
configuration. 

```shell
$ clojure -m figwheel.main -pc -b dev -r
```

The above command should print out something like the following:

```clojure
[Figwheel] :pprint-config true - printing config:
---------------------- Figwheel options ----------------------
{:pprint-config true, :mode :repl, :watch-dirs ("src")}
---------------------- Compiler options ----------------------
{:main hello-world.core,
 :output-to "target/public/cljs-out/dev-main.js",
 :output-dir "target/public/cljs-out/dev",
 :asset-path "cljs-out/dev",
 :preloads
 [figwheel.core
  figwheel.main
  figwheel.repl.preload
  devtools.preload
  figwheel.main.evalback],
 :aot-cache false,
 :closure-defines
 #:figwheel.repl{connect-url
                 "ws://localhost:9500/figwheel-connect?fwprocess=b96800&fwbuild=dev"},
 :repl-requires
 ([figwheel.repl :refer-macros [conns focus]]
  [figwheel.main
   :refer-macros
   [stop-builds start-builds build-once reset clean status]])}
```

As you can see there are two sections one displaying the current
**config options for Figwheel** and one displaying the **final compile
options** to be sent to the ClojureScript compiler.

Looking at the **compiler options** you will see that while we only
supplied `{:main hello-world.core}` in our `dev.cljs.edn`, Figwheel
added a few more options. We will focus on explaining these options.

## The `:output-to` option

The `:output-to` option is "the path to the JavaScript file that will
be output". This is simple enough. You provide the `:output-to` option a path
to a file where the compiled ClojureScript should be output. This file
will always represent your main compiled artifact.

However, the contents of the file and how we load this file will vary
based on the `:optimizations`, `:main`, and `:target` options.

The most important thing to remember when working with the Figwheel
server is that the `:output-to` option needs to point to a path that
is basically the classpath + `public`.

The Figwheel default for this path is in the `target/public` directory
because we want to seperate files that are compiled (and thus
temporary) from files that we edit and keep in version control like
HTML and CSS.

The Figwheel default as metioned before uses the build name and looks
like 

```shell
target/public/cljs-out/[build-name]-main.js
```

If you don't use a build file then the `:output-to` path may look like

```
target/public/cljs-out/main.js
```

This can lead to problems if you are using different command line
options to obtain different compilation results.

## The `:output-dir` option

The `:output-dir` sets the output directory for temporary files used
during compilation.

Regardless of the `:optimizations` level the compiler will output a
file for each ClojureScript "namespace" in your project and all of its
dependencies, as well as the Google Clojure libraries and foreign
libraries that are utilized.

If you have a `hello-world.core` example you can examine the
`:output-dir` after a compile and see the temporary files:

```shell
target/public/cljs-out/dev
├── cljs
│   ├── core.cljs
│   ├── core.js
│   ├── core.js.map
│   ├── pprint.cljs
│   ├── pprint.cljs.cache.json
│   ├── pprint.js
│   ├── pprint.js.map
│   ├── stacktrace.cljc
│   ├── stacktrace.cljc.cache.json
│   ├── stacktrace.js
│   ├── stacktrace.js.map
... a lot more
```

These files also serve as a cache for the compiler which enables
incremental compiles. If your source file has a timestamp newer than
the file in the output directory then the compiler will compile the
sourcefile. Caches can get stale however and you will want to **delete
your output directory on a regular basis**.

When no `:output-dir` is defined Figwheel will provide a default
`:output-dir` which will have the form:

```
target/public/cljs-out/[build-name]
```

For example the default `:output-dir` for our `dev.cljs.edn` build
file will be `target/public/cljs-out/dev`.

You may notice that the `:output-dir` path is placed so that its
contents are available via the classpath. The reason for this is
because when we use the default `:optimizations` level `:none` many of
these "temporary files" are directly loaded into our client
environment. For example, in a web environment when we load our
`:output-to` file the Google Clojure base code will caculate a the
files that need loading and then load them in order. Hence the browser
needs to serve these files and this is why they need to be on
available on the classpath.

So if you choose to customize the `:output-dir` path keep in mind that
they will need to be found by the client environment if you are using
`:optimizations :none`.

But ... you should never put the `:output-dir` directly on the
classpath. This means if `target` is in the classpath, you should
never set `:output-dir` to the `target` directory. If you do this you
are making your compiled artifacts resolvable by the CLJS
compiler. This means that when the CLJS compiler looks for a required
CLJS file it will possibly find it in the `target` directory and this
will cause major problems.

## The `:optimizations` option

The `:optimizations` compiler option designates the optimizations
level that the Google Closure compiler should use. The available
optimizations levels are:

* `:none` - the default level, no code optimizations
* `:whitespace` - basically just removes whitespace and comments
* `:simple` - removes whitespace and shortens local variable names
* `:advanced` - more aggressive renaming, dead code removal, global
  inlining

The Figwheel `--build` option and the ClojureScript REPL both require
`:optimizations` to be set to `:none`. `:none` is the default so it
does not need to be specified in the configuration options map.

As we noted before the `:none` level does not produce a single self
contained compiled artifact, but rather creates an artifact that loads
all of the seperately compiled namespaces.

All the other levels (`:whitespace`, `:simple`, and `:advanced`)
produce a single compiled artifact to the `:output-to` file. These are
often used for producing your final deployable asset. When you use
these optimizations settings.

The `:advanced` level provides absolutely amazing compression and
optimization but it is also the most finicky. This
[guide][advanced-guide] can be helpful.

## The `:main` option

The `:main` option specifies a entry point (root namespace) for your
compiled artifact. When `:optimizations` is `:none` the `:main` option







[advanced-guide]: https://clojurescript.org/reference/advanced-compilation
[cljs-opts]: https://clojurescript.org/reference/compiler-options


