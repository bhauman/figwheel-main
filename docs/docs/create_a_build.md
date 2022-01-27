---
title: Create a Build
layout: docs
category: docs
order: 5
---

# Create a Build


<div class="lead-in">In order to start using Figwheel's hot reloading
based workflow we will need to create a build. A
<strong>build</strong> defines a compilation process, and is going to
be your main unit of configuration.</div>

`figwheel.main` has a CLI that is fairly expressive. However, most
folks who work with it are going to want to define a watch/compile
process with a hot-reloading workflow to get the bulk of their work
done.

Now that we know how to add our dependencies and set up our project
and classpath, let's start using Figwheel to compile and reload our
ClojureScript code.

We'll start with a project setup as described in the previous
chapters.

Your project layout should look like this:

```shell
./
├── deps.edn # or project.clj
├── resources
│   └── public
├── src
│   └── hello_world
│       └── core.cljs
└── target
```

The contents of the `deps.edn` file should be:

```clojure
{:deps {org.clojure/clojure {:mvn/version "1.9.0"}
        org.clojure/clojurescript {:mvn/version "1.10.773"}
        com.bhauman/figwheel-main {:mvn/version "0.2.16"}
        com.bhauman/rebel-readline-cljs {:mvn/version "0.1.4"}}
 :aliases {:fig {:main-opts ["-m" "figwheel.main"]}}
 :paths ["src" "resources" "target"]}
```

If you're using Leiningen your `project.clj` should be:

```clojure
(defproject example-project "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]]
  :profiles
    {:dev
      {:dependencies [[org.clojure/clojurescript "1.10.773"]
                      [com.bhauman/figwheel-main "0.2.16"]
                      ;; optional but recommended
                      [com.bhauman/rebel-readline-cljs "0.1.4"]]
       :resource-paths ["target"]
       :clean-targets ^{:protect false} ["target"]}}
  :aliases {"fig" ["trampoline" "run" "-m" "figwheel.main"]})
```

The contents of the `src/hello_world/core.cljs` file should be:

```clojure
(ns hello-world.core)

(js/console.log "Hello there world!")
```

## Configuring a build

The ClojureScript compiler can take a fairly extensive set of
[configuration options][cljs-options]. Figwheel provides sane defaults
for several important compiler options. This will allow us to
configure a compile process by simply defining the
[`:main` option][cljs-main-opt].

We are going to pass this option to Figwheel via a **build file**. A
build file is a *named* set of compiler configuration options. Figwheel
utilizes this name to isolate a particular build's REPL connection and
output files from all the other builds.

The build file will sit in our project root directory and the build
file's name will take the form `[build-name].cljs.edn` where you
will substitute `[build-name]` with a name of your choosing.

Let's create a build called `dev`. Create a `dev.cljs.edn` file with
the following content:

```clojure
{:main hello-world.core}
```

The above `:main` option defines a root namespace for our build.
When we include the compiled artifact on a webpage it will pull in
all the code that our `:main` namespace depends on.

> Figwheel will only be able to start a REPL and hot reload if
> `:optimizations` level is at its default setting of `:none` as it
> is in the above configuration. The other `:optimization` levels are
> intended to be used for deployment.

## Running a build

At this point we have everything we need to start compiling and
editing our code with a hot reloading workflow.

#### Start a build with CLI Tools

Run the following in the root directory of the project:

```shell
$ clojure -m figwheel.main --build dev --repl
```

We can also use the shorter `-b` and `-r` flags

```shell
$ clojure -m figwheel.main -b dev -r
```

or with the defined alias:

```shell
$ clojure -A:fig -b dev -r
```

#### Start a build with Leiningen

Let's use our defined alias:

```shell
$ lein fig -- --build dev --repl
```

We can also use the shorter `-b` and `-r` flags

```shell
$ lein fig -- -b dev -r
```

**Once you've started Figwheel**

When you start Figwheel you should see a browser pop open:

<img width="1045" alt="default dev page"  class="white-img-border" src="https://user-images.githubusercontent.com/2624/43164421-1cc23342-8f5f-11e8-9c50-65aae3b2ed8f.png">

The green **Connected** animation that appears next to the CLJS logo
indicates the browser environment has successfully connected back to
the Figwheel server. If you open your browser's devtools for the
current page you will see `Hello there world!` (remember that the code
that prints this is in our `hello_world.core` namespace).

<img width="1032" class="white-img-border" alt="devtools open" src="https://user-images.githubusercontent.com/2624/43166008-6047dfa0-8f63-11e8-9f63-6997228e003f.png">

You should now be able to return to the terminal where you launched
Figwheel from to see a working REPL that is ready for you to evaluate
some ClojureScript Code.

<img width="773" alt="screen shot 2018-07-24 at 4 44 25 pm" src="https://user-images.githubusercontent.com/2624/43165069-05d5b4fe-8f61-11e8-9943-4fb546150415.png">

You can now try out some ClojureScript at the REPL.

As an example try entering `(js/alert "Crocodile Rock!")` at the
`cljs.user=>` prompt.

You can also return to the `src/hello_world/core.cljs` file and edit
the `"Hello there world!"` string so that it reads `"Live edit!!!!!"`
and then save the file.

You should notice that Figwheel reports loading the new file in the
console and you should see `Live edit!!!!!` printed in the console as
well.

## Congrats!

You have successfully set up a Figwheel hot-reloading compile process
for a ClojureScript project.

If you have never built an application with ClojureScript this is a
good place to start building from.

If you are building an application that works with the DOM there is a
`<div id="app"></div>` already on the Figwheel Default Dev Page that
you can override. If you clear it's contents all of the CSS and HTML
for the content of the Dev Page will removed.

## Adding Figwheel configuration to the build

Figwheel Main has [several options][fig-main-config] to configure how
your build process works.

You can add these configuration options straight to your
`dev.cljs.edn` build file. As an example you can turn off hot
reloading by adding `:hot-reload-cljs false` to your build file like
this:

```clojure
^{:hot-reload-cljs false}
{:main hello-world.core}
```

The `^` character is not a typo, it's a
[reader macro][reader-macro-meta] that in this case is adding metadata
to the map that follows it. Figwheel examines the metadata of your
build config for its own configuration options.

You can also specify a map of Figwheel configuration in a
`figwheel-main.edn` file in your project root directory. See the
[configuration][fig-main-config] for more details.


[fig-main-config]: ../config-options
[cljs-options]: https://clojurescript.org/reference/compiler-options
[cljs-main-opt]: https://clojurescript.org/reference/compiler-options#main
[reader-macro-meta]: https://clojure.org/reference/reader#macrochars
