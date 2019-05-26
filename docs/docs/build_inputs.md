---
title: Build Inputs
layout: docs
category: docs
order: 16
---

# Build inputs

<div class="lead-in"><p>Build inputs to the ClojureScript compiler
determine what ClojureScript source files get compiled. Figwheel
follows a simple heuristic for determining the build inputs based on
your configuration.</p>
You can determine the build inputs explicitly with the <a href="../config-options#build-inputs"><code>:build-inputs</code> config option.</a>
</div>

Understanding how build inputs change the ClojureScript compiler's
behavior can be very helpful depending on your compilation goals.

## Development

When you supply a directory like `src` to the compiler, it will find
all the ClojureScript files (`.cljs` and `.cljc`) and use them
directly as the initial **sources** for compilation. All of these
initial sources will be compiled regardless of their relationship to
the `:main`. When a namespaces is required but not present in these
initial sources, the compiler will then attempt to find it on the
classpath.

If you provide a single file to the compiler like
`src/example/core.cljs` only that file will in the initial sources and
all it's dependencies will be resolved via the classpath.

This behavior has various trade-offs that need to be understood in the
context of compiling for development and production.

One could not be blamed for coming to the conclusion that it is always
better to supply the compiler with a single build input that
represents the root namespace of your application. This way the final
code base will only include code that is needed by the application.

It's a temptingly simple heuristic that works for many cases. But let
me make an argument why it is better to pass a directory or set of
directories to the compiler during development when you are using a
hot reloading workflow.

The biggest reason to have all you files under watch while you are
developing them is so that you receive feedback on files that are not
currently included in the application.  I.E you can work and prototype
them and Figwheel will give you feedback on how well they are
compiling. This feedback is not to be underestimated as it parallels
the feedback that every decent IDE gives you about your code's syntax.

Another major reason to pass your source directories as your build
inputs is to support the [extra mains](extra_mains) feature. If you
are simply compiling from a single root namespace, files that you need
for your extra main entry point will possibly not be compiled.

When you are developing with `:optimizations` level `:none`, any extra
source files that are being worked on but are not required by the
running application will not be loaded in the running application. The
cost of supplying a directory of source files to the compiler will
only be high if the unrequired source files have
a long initial compile time. This is a very rare situation. Even if
that situation does occur the cost of compiling is paid only once 
during the initial compile.

It is for the above reasons that both `figwheel.main` and `cljs.main`
supply the watched directories to the compiler. They are being watched
so that there is an expectation of feedback.  When there is no feedback,
one can mistakenly assume that the code one is writing has no syntax
errors.

## Production

Now when we leave development mode the priority changes. We normally
do not want to include extra source files in a deployed artifact and
we have no need for feedback on files that are not getting
deployed. When we compile one big artifact with in `:optimizations`
level `:whitespace` we don't want it contain unneeded namespaces.

`figwheel.main` uses the file containing your `:main` namespace as the
single build input when you are not in `:optimizations` level
`:none`. This will ensure that the deployed bundle of JavaScript only
contains code that is being depended on.

This is less of a problem when you use the `:advanced` level as it
will perform dead code elimination from the final artifact.

## Overriding build inputs

Now that you understand the trade-offs you can safely use the
[`:build-inputs` config option] to override the default sources passed
to the compile by Figwheel.

#### Only sending the main namespace

This will probably be the most common build inputs override.

You can accomplish this with the following config:

```clojure
:build-inputs [:main]
```

The `:main` keyword will be replaced by the main namespaces.

#### Only sending the watched directories

```clojure
:build-inputs [:watch-dirs]
```

The `:watch-dirs` keyword will be replaced by the watched directories.

#### Compiling a directory that you don't want to watch

Assuming that `./src-no-watch` is a source directory that you want to
compile but don't want to watch.

```clojure
:build-inputs [:watch-dirs "src-no-watch"]
```

A string is assumed to be a file or a directory.

#### Watched directories and a specific namespace

```clojure
:build-inputs [:watch-dirs example.tool.extra]
```

A symbol is assume to be a ClojureScript namespace.



[build-inputs]: ../config-options#build-inputs













