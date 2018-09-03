---
title: Build Inputs
layout: docs
category: docs
order: 20
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
`src/example/core.cljs` only that file will in the intial sources and
all it's depedencies will be resovled via the classpath.

This behavior has various trade-offs that need to be understood in the
context of compiling for development and production.

One could not be blamed for coming to the conclusion that it is always
better to supply the compiler with a single build input that
represents the root namespace of your application. This way the final
code base will only include code that is needed by the application.

Its a temptingly simple heuristic that works for many cases. But let
me make an argument why it is better to pass a directory or set of
directories to the compiler during developement when you are using a
hot reloading workflow.

The biggest reason to have all you files under watch while you are
developing them is so that you receive feedback on files that are not
currently included in the application.  I.E you can work and prototype
them and Figwheel will give you feedback on how well they are
compiling. This feedback is not to be underestimated as it parallels
the feedback that every decent IDE gives you about your code's syntax.

Also, since when you are developing with `:optimizations` level
`:none` any extra source files that are being worked on but are not
required by the running application will not be loaded in the running
application. The cost of supplying a directory of source files to the
compiler will only be high if it is expensive when the unrequired
source files have a long initial compile time, which is a very rare
situation. Even if that situation occurs the cost is paid only
once during the initial compile.

It is for the above reason that both `figwheel.main` and `cljs.main`
supply the watched directories to the compiler. They are being watched
so there is an expectation of feedback, when there is no feedback it
is normally assumed that the code you are writing is just fine :)

## Production

Now when we leave development mode the priority changes. We normally
do not want to include extra source files in a deployed artifact and
we have no need for feedback on files that are not getting
deployed. When we compile one big artifact with in `:optimizations`
level `:whitespace` we don't want it contain unneeded files.

This is why `figwheel.main` uses the file containing your `:main`
namespace as the single build input when you are not in
`:optimizations` level `:none`.








