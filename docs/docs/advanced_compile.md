---
title: Advanced Compile
layout: docs
category: docs
order: 10
---

# Advanced Compile

<div class="lead-in">If you are going to deploy your compiled
ClojureScript to the web you will want to use <strong>advanced
compilation</strong> as it will shrink your compiled code
significantly.</div>

The Google Closure Compiler's advanced compilation may be the best
possible way to compress your compiled ClojureScript. It shrinks names
globally, it in-lines code, it removes code that is not reachable and
just does a fantastic job of compressing your code.

However, there is a downside. It needs to know the variable and
function names that it shouldn't compress. It can be very frustrating
when you are finally getting ready to deploy your project and then
discover your code has some subtle incompatibilities with advanced
compilation.

The way this plays out is that your code will compile without failure
or warnings and then it just won't run. And you will see in the
developers console that some randomly compressed function name like
`zB` does not exist.

It is for this reason that you should get in the habit of compiling
your project in advanced mode fairly often, at least once a day, to
verify that things are working.

## How to advanced compile

Given our `hello-world.core` example project we can advanced compile
it very simply by executing the following shell command:

```shell
$ clj -m figwheel.main -O advanced -bo dev
```

The `-bo` is the short flag for `--build-once` and it takes a build
name just like the `--build` flag. The `-O` flag overrides the
`:optimizations` configuration option in the build options. It is
important that the `-O` flag and its options is before the final `-bo` option.

> The `-O` flag is a capital letter and overrides the `:optimizations`
> build option. Not to be confused with the `-o` flag which overrides
> the `:output-to` config option.

The above command should have advanced compiled your code one time. If
you examine the `target/public/cljs-out/dev-main.js` file you will the
compressed output of your program.

Now something to notice here is that the `:output-to` path
`target/public/cljs-out/dev-main.js` has not changed. This means that
the HTML that is hosting this file can still be used to verify that it
all works.

You can verify that your advanced compiled code is working two
different ways.

The first is to just build it once and start the server like so:

```shell
$ clojure -m figwheel.main -O advanced -bo dev -s
```

or alternatively you can watch and compile the advanced build so that
you can have it recompiled after you change it.

```shell
$ clojure -m figwheel.main -O advanced -b dev -s
```

The above method can be very helpful when diagnosing advanced
compile problems.

Keep in mind the above command will not hot-load your changes and you
will need to manually refresh the browser to see if your fixes have
taken. Also, remember that advanced compilation is significantly
slower than `:optimizations :none` compiles.

## Troubleshooting

If and when you run into problems please make sure that you have read
and understood these guides on the ClojureScript site:

* [Dependencies guide][dependencies-guide] is written with a focus on advanced compilation
* [Advanced compilation guide][advanced-guide] provides some extra info

[dependencies-guide]: https://clojurescript.org/reference/dependencies
[advanced-guide]: https://clojurescript.org/reference/advanced-compilation
