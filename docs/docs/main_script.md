---
title: Main Script
layout: docs
category: docs
order: 20
---

# Main Scripts

<div class="lead-in">Just like <code>cljs.main</code>, the Figwheel allows you to
run a <code>-main</code> function in a namesapce from the command line. Figwheel
provides additional functionality to facilitate asynchronous execution
and process failure.</div>

If you have a ClojureScript namespace on the classpath with a `-main`
function in it. You can execute that function from the command line.

For example if you have the following namespace:

```clojure
(ns example.hello)

(defn -main [& args]
  (println "ARGS:" (pr-str args)))
```

You can execute it from the command line like this:

```shell
$ clj -m figwheel.main -m example.hello hi there
```

You will see the command will compile and eventually print out:

```
ARGS: ("hi" "there")
```

You may wonder why you might need a feature like this in the first
place. Being able to run one off arbitrary scripts can be a major boon
to your tool chain. This could be very helpful from running tooling
like [Webpack](https://webpack.js.org/api/node/) from Node. However,
**running tests** will probably be the dominant reason why you will
want to use main scripts.

> Being able to accept arbitrary arguments at the end of the command
> line is why CLI option order is important in `clojure.main`,
> `cljs.main` and `figwheel.main`. I know that this can be confusing
> at times, but being able to provide scripts and main scripts with
> arbitrary options including ones that could mistakenly be recognized
> by `figwheel.main` is very important for flexible expression. And is
> the reason why `figwheel.main` can reuse the many of the same CLI
> args as `clojure.main`.

## Figwheel and compile options when using `--main`

Since the `-m` and `-b` options cannot be used at the same time you
may be left wondering how to supply the options that you would
normally supply with the `--build` option to the `--main` option.

You can do this by providing your `[build-id].cljs.edn` file to the `-co`
option. Figwheel will pick up both the metatdata config along with the CLJS
compiler config. 

For example the following will allow you to use the config in your
`dev.cljs.edn` file:

```shell
$ clj -m figwheel.main -co dev.cljs.edn -m example.hello
```

## Asynchronous execution and non-zero exits

This `--main` CLI option behavior is unfortunately hampered in its
ability to be useful because many ClojureScript tasks (including
running tests) are asynchonous. Not only that but we'd prefer that if
a command line task execution fails in ClojureScript we'd like that
failure to be communicated by returning a non-zero exit status from
the `clj` process.

The good news is that currently if you throw a JavaScript Error from
synchronous code in your `-main` function the Clojure process will have
a non-zero exit.

This means that when you are running test code that is synchronous, you
can determine if the tests failed at the end of the run and throw an
error to cause non-zero exit from the Clojure process.

But what do we do when the process is asynchronous?

`figwheel.main` extends the `cljs.main` behavior to provide a means of
waiting for an asynchronus process to complete or throw an error.

The following 3 tools help you do this:

* the Figwheel Clojure process will block and wait when the `-main`
  function returns a vector
  `[:figwheel.main.result/async-wait optional-timeout optional-timeout-value]`
* the `figwheel.main.result/send` ClojureScript function will send
  back a value to the blocked Figwheel process
* the `figwheel.main.result/throw-ex` ClojureScript function will send
  back an exception to the blocked Figwheel process

Figwheel will look at the value returned by the `-main` function and
alter its behavior if it receives something like
`[:figwheel.main.result/async-wait 5000]`. This value will cause the
Clojure process to block and wait for a result or timeout after the
supplied timeout.

For Example:

```clojure
(ns example.hello)

(defn -main [& args]
  [:figwheel.main.async-result/wait 5000])
```

When you run the above namespace with the following:

```shell
$ clj -m figwheel.main -m example.hello
```

The Clojure process will block for 5 seconds while it waits for some
asynchronus result to be sent back. In this example we are not sending
back a result value, so the process will throw a timed out exception
which will cause a non-zero exit.

Now let's modify the above `-main` function to send back an
asynchronous result.

```clojure
(ns example.hello
  (:require [figwheel.main.async-result :as async-result]))

(defn -main [& args]
  (js/setTimeout #(async-result/send args) 3000)
  [:figwheel.main.async-result/wait 5000])
```

Again run the `-main` function with:

```shell
$ clj -m figwheel.main -m example.hello
```

Now the Clojure process will block for 3 seconds and ultimately print
out the args sent by the `figwheel.main.async-result/send` function.

Let's modify the namespace one more time to send and asynchronus
failure:

```clojure
(ns example.hello
  (:require [figwheel.main.async-result :as async-result]))

(defn -main [& args]
  (js/setTimeout #(async-result/throw-ex (ex-info "This failed in ClojureScript!" {}) 3000)
  [:figwheel.main.async-result/wait 5000])
```

After you run the above example the Clojure process will block for 3
seconds and and then fail with an exception.

## More details

The `figwheel.main.async-result/send` and
`figwheel.main.async-result/throw-ex` functions are single use actions
that are only intended to be used in a `--main` script. The first
value or exception returned will end the process.

The returned `:figwheel.main.async-result/wait` message has two
optional parameters.

* `timeout` defaults to 5000 milliseconds
* `timeout-value` defaults to `:figwheel.main.async-result/timed-out`

When Figwheel runs a `-main` script it prints the final result,
`cljs.main` does not do this.











  
