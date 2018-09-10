---
title: Testing
layout: docs
category: docs
order: 20
---

# Testing

<div class="lead-in"><p>Testing is an important part of any
programming workflow. Having tests that run after every code change is
essential and should not be hard to set up.</p> <p>Figwheel now
provides <a href="#auto-testing">automatic test discovery and display</a>, faciliates custom
testing with <a href="extra_mains">Extra Main</a> entry points, complements build
processes and CI with non-zero test run failures, and provides
a flexible way to configure an alternative JS environment to run your
tests in.</p></div>

> Only available in `figwheel.main` >= 0.1.9-SNAPSHOT

## Testing ClojureScript

If you haven't written tests in ClojureScript yet, see the
[testing guide over at clojurescript.org][cljs-testing].

## Auto testing

The easiest way to start working with tests in Figwheel is to write
some tests in a namespace that is on your classpath, and set the
[`:auto-testing` config option][auto-testing] to `true`. Figwheel will
automatically discover your tests and display them with
[cljs-test-display][cljs-test-display] at the HTTP endpoint
`/figwheel-extra-main/auto-testing`. No test runner is required and
the tests will be re-run every time you save a watched source code
file.

The [`:auto-testing` config option][auto-testing] can also take a map
with the following keys:

* `:cljs-test-display` can have a boolean value indicating if you want
  to display the tests
* `:namespaces` takes a vector of namespaces that you want to test
  
## Custom testing with `:extra-main-files`

If the testing setup provided by [`:auto-testing`][auto-testing]
doesn't work for you. The [extra mains](extra_mains) feature has you
covered. We'll quickly walk through an example of using
[`:extra-main-files`][extra-main-files] to provide a custom testing
setup up with relatively little effort.

First you would add a test runner namespace of some sort. I'm going to
assume we are using [`cljs.test`][cljs-testing] and [cljs-test-display][cljs-test-display].

```clojure
(ns example.test-runner
  (:require 
    [cljs-test-display.core]
    [cljs.test :refer-macros [run-tests]]
    ;; require all the namespaces that have tests in them
    [example.core-test]
    [example.other-test]))
	
(run-tests (cljs-test-display.core/init! "app-testing")
           'example.core-test
           'example.other-test)
```

Now we'll configure this as an extra main entry point in our `dev.cljs.edn` file:

```clojure
^{:extra-main-files {:testing {:main example.test-runner}}}
{:main example.core}
```

Now when you start your build with the standard `clojure -m
figwheel.main -b dev -r` command you will now be able to see your
custom testing at the `/figwheel-extra-main/testing` endpoint.

Please note that the `"app-testing"` above refers to the DOM `id` of
the `DIV` that is available by default on the page served by the extra
main endpoint.  This `id` needs to correspond to the name of your
[extra main](extra_mains). In this case our extra main is named `:testing`. If your
extra main was named `:tests` you would need to use `"app-tests"` in
the `cljs-test-display/init!` call.

You can also utilize the `figwheel.main.testing/run-tests` macro that
will automatically find all the testing namespaces in your source files.

```clojure
(ns example.test-runner
  (:require 
    [cljs-test-display.core]
    [figwheel.main.testing :refer-macros [run-tests]]
    ;; require all the namespaces that have tests in them
    [example.core-test]
    [example.other-test]))
	
(run-tests (cljs-test-display.core/init! "app-testing"))
```

And as usual you can always create [your own HTML host page](your_own_page) for your
extra main. In this case you would have to require the
`target/public/cljs-out/dev-main-testing.js` bootstrap script.

## Running tests from the command line

Running tests from the command line is an important feature that
allows you to confirm that your tests are passing without having to
boot up and kill a REPL. Being able to run tests from the command line
in a process that will non-zero exit on test failure is an essential
feature that allows us to integrate testing into modern CI and Cloud
testing services.

Up until now most CLJS developers have had to rely on using a Node
process to drive a headless JavaScript environment to run their
ClojureScript tests. This is a shame considering that we are already
using a mediating process when we run Clojure to start our
ClojureScript REPLs and run our scripts.

Figwheel can now run an asynchronous [main script](main_script) and
report failures with a non-zero exit status.

Here is an example test runner that uses the new asynchronous
[main script](main_script) functionality.

```clojure
(ns example.test-runner
  (:require 
    [cljs.test :refer-macros [run-tests] :refer [report]]
    [figwheel.main.async-result :as async-result]
    ;; require all the namespaces that have tests in them
    [example.core-test]
    [example.other-test]))
	
;; tests can be asynchronous, we must hook test end
(defmethod report [:cljs.test/default :end-run-tests] [test-data]
  (if (cljs.test/successful? test-data)
    (async-result/send "Tests passed!!")
    (async-result/throw-ex (ex-info "Tests Failed" test-data))))
	
(defn -main [& args]
  (run-tests 'example.core-test 'example.other-test)
  ;; return a message to the figwheel process that tells it to wait
  [:figwheel.main.async-result/wait 5000])
```

This is just an example but given that the test namespaces are on the
classpath, you can execute a test runner main script like the one
above on the command line as follows:

```clojure
clj -m figwheel.main -m example.test-runner
```

The above example is a bit verbose so Figwheel has provided a
helper that will find all your testing namespaces and execute them
taking advantage of the new asynchronous `-main` support.

Let's repeat the above example using the
`figwheel.main.testing/run-tests-async` helper.

```clojure
(ns example.test-runner
  (:require 
    [figwheel.main.testing :refer-macros [run-tests-async]]
    ;; require all the namespaces that have tests in them
    [example.core-test]
    [example.other-test]))
	
(defn -main [& args]
  ;; this needs to be the last statement in the main function so that it can
  ;; return the value `[:figwheel.main.async-result/wait 10000]`
  (run-tests-async 10000))
```

If by any chance your tests are synchronous please feel free to use
the `figwheel.main.testing/run-tests` helper which will throw an
exception if tests fail.

## Running tests in a headless environment

CI and testing environments almost always require being able to run
tests in a headless JavaScript environment.

Figwheel now has the
[`:launch-js` configuration option][launch-js] which should
allow you to launch an arbitrary JavaScript environment.

[`:launch-js`][launch-js] allows three different ways to
launch a JavaScript environment.

* a **string** representing a shell script that will be supplied a URL or
  a path to your compiled ClojureScript. Example: `"headless-chrome"`
* a **vector** representing a shell command. Example: `["headless-chrome" :open-url]`
* a **namespaced symbol** that represents a Clojure function that will be
  supplied a map. Example: `user/run-test-env`
  
I'm going to show how you can use the [`:launch-js` config option][launch-js]
to launch a headless Chrome environment to run your tests using all 3
different methods.

#### Shell Script method

On my Mac in my `~bin` directory I have an executable shell script named
`headless-chrome` that has the following content:

```shell
#!/bin/sh
/Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome --headless --disable-gpu --repl $1
```

The above script takes a single URL argument.

Now we just have to add `:launch-js "headless-chrome"` to our
Figwheel options. We can do this on the command line while we run our
tests like so:

```shell
clojure -m figwheel.main -fwo '{:launch-js "headless-chrome"}'  -m example.test-runner
```

We can also add the option in a `tests.cljs.edn` build file if we are using it:

```clojure
^{:launch-js "headless-chrome"}
{:main example.normal-test-runner}
```

And then use it like this:

```shell
clojure -m figwheel.main -co tests.cljs.edn  -m example.test-runner
```

#### The command vector method

The command vector method is pretty much the same as the shell script
method but it allows you to execute arbitrary shell commands without
needing to write a script.

The following will launch headless Chrome on my system:

```clojure
:launch-js ["/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" 
            "--headless" "--disable-gpu" "--repl" :open-url]
```

#### Using a Clojure function

This time let's use a Clojure function to launch headless Chrome. First
we'll place a `headless-js-env` function in our `user.clj` file.

```clojure
(ns user
   (:require [clojure.java.shell :as shell]))
   
(defn headless-js-env [{:keys [open-url]}]
  (shell/sh "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" 
            "--headless" "--disable-gpu" "--repl" open-url))
```

Now we will configure `:launch-js` to take a `user/headless-js-env` symbol:


```shell
clojure -m figwheel.main -fwo '{:launch-js user/headless-js-env}'  -m example.test-runner
```

[cljs-test-display]: https://github.com/bhauman/cljs-test-display
[auto-testing]: ../config-options#auto-testing
[extra-main-files]: ../config-options#extra-main-files
[launch-js]: ../config-options#launch-js
[cljs-testing]: https://clojurescript.org/tools/testing
