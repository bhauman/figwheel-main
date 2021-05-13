---
title: Using NPM
layout: docs
category: docs
order: 12
---

# Using NPM

<div class="lead-in"><a href="https://www.npmjs.com/">NPM</a> is the
defacto package repository for the JavaScript ecosystem. It holds a
tremendous amount of valuable functionality. This guide will show you
how to include and consume NPM packages in your ClojureScript
codebase.</div>

> Npm usage in Figwheel has changed significantly. For reference purposes the
> original version of this document can be found
> [here](/docs/npm_archived)

> These instructions require `org.clojure/clojurescript` >= `1.10.773`
> and `com.bhauman/figwheel-main` >= `0.2.6`.

<hr/>
**Quick Reference**

*Read the rest of this document and come back here for a quick reference.*

Set the [`:target` compiler
option](https://clojurescript.org/reference/compiler-options#target)
to `:bundle`.  This will cause the compiler to emit an output file
file that can be bundled a JavaScript bundler like `webpack`.

Optionally set the [`:bundle-cmd` compiler
option](https://clojurescript.org/reference/compiler-options#target)
to 
```clojure
{:none ["npx" "webpack" "--mode=development" "--entry" :output-to 
       "--output-path" :final-output-dir 
	   "--output-filename" :final-output-filename]}
``` 
to ensure the output file is bundled after a compile. Figwheel will
fill in `:output-to`, `:final-output-dir` and `:final-output-filename`.

Your host page will need to load the final bundled asset.

> You will want to make sure that the `:output-to` file is in the
> `:output-dir` directory so the bundler can resolve the assets it
> requires.

> As a rule, if you are using NPM libraries along with the `:bundle`
> target, don't use `:aot-cache true`.

> If your are porting a project over to use the
> `:bundle` target and NPM libs, use the
> [`:clean-outputs`](/config-options#clean-outputs) Figwheel
> option.

**Relevant Figwheel Options**

* [:auto-bundle](/config-options#auto-bundle)
* [:bundle-freq](/config-options#bundle-freq)
* [:final-output-to](/config-options#final-output-to)

<hr/>

## What?

[NPM][npm] is a package repository for the JavaScript
ecosystem. Almost all available JavaScript libraries are packaged,
stored, and retrieved via NPM.

We want to use these libraries inside our ClojureScript codebase, but
there is some friction because ClojureScript embraced the Google
Closure Compiler and its method of declaring libraries, which is quite
different than NPM's.

{::comment}
We could get into a debate about why ClojureScript designers decided
to embrace the less popular ecosystem, but that is largely academic at
this point. I will say that the advantages of effortless interactive
development via hot-reloading and the amazing capabilities of the
Google Closure Compiler's advanced mode are direct benefits of using
the GCC's (Google Closure Compiler's) method of defining modules via
simple JavaScript object literals. In other words, without the GCC
there would most likely not be a Figwheel.

Nevertheless, experiencing friction while importing libraries from the
dominant JavaScript ecosystem is a very unfortunate trade-off.
{:/comment}

However, with recent changes in the ClojureScript compiler (along with
changes in Figwheel) it is now becoming much more straightforward to
include NPM modules in your codebase.

## The Overview

We are going to utilize a JavaScript bundler like [Webpack][webpack]
to bundle up the output of our ClojureScript compiled code to produce
a final bundled output file which will contain all the NPM libraries
we have required in our ClojureScript code.

So the code is going to go through two steps:

1. compilation by the ClojureScript compiler to an intermediate file
2. bundled together with its NPM dependencies to a final output
   file that we will load into the browser

It's important to remember that the under the `:bundle` target the
output of the ClojureScript compiler is not loadable by the browser it
has to be bundled first.

During development under optimizations `:none` the bundled output file
will only contain the NPM libraries and small amount of ClojureScript
boot code that will in turn load our compiled ClojureScript code. So
there exists a bundle file and then a bunch of individually compiled
ClojureScript namespaces that the bundled boot script will need to
load. 

> It's important to remember that during development the bundled file
> still depends on the other ClojureScript output files and will not
> work on its own.

When we deploy to production we will be compiling a single
ClojureScript artifact using `:simple` or `:advanced`
optimizations. This single ClojureScript artifact will then be bundled
along with its NPM dependencies into a single bundle that can be
deployed on its own.

## Getting started with NPM libraries into your project

We are going to assume you are starting from the [base example][base-example-gist].

I'm going to use `npm` for this example but if you prefer `yarn` go
ahead and use that. It doesn't really matter for this.

There are four steps that we are going to follow to add some libraries to
our `hello-world.core` project.

1. Initialize NPM in our project by adding a `package.json` file
2. Install Webpack to bundle the needed dependencies into a single JS file.
3. Add the needed libraries as NPM dependencies.
4. Configure our ClojureScript build to use the bundle generated by Webpack.

## Initialize NPM

We will need to initialize `npm` for our `hello-world` project.

One way to do this is to use the `npm init` command. You can of course
just create the file from scratch but `npm init` is faster.

Make sure you are in the root directory of the project and execute:

```sh
$ npm init -y
```

This will create a `package.json` file in the root directory of your
project along side your `dev.cljs.edn` file.

## Install Webpack

We will need webpack to bundle our application along with our `moment`
dependency to make it available to our ClojureScript code.

Install `webpack` and `webpack-cli`:

```sh
$ npm add --save-dev webpack webpack-cli
```

This add `webpack` and `webpack-cli` as development dependencies in
your `package.json`. It will also download them to a `node_modules`
directory in your project.

## Add the needed libraries

Let's say we want to use the `moment` library in our
application.

We'll use `npm` to add a `moment` dependency to our `package.json`
file in the usual manner:

```sh
$ npm add moment
```

This should download and install the `moment` library along with its
dependencies if it has any. 

## Configure our ClojureScript build

Everything we have done up until this point is very similar to what we
would normally do if we were using NPM and Webpack for a simple
JavaScript project.

In the `dev.cljs.edn` file we'll add the following config:

```clojure
{:main hello-world.core
 :target :bundle
 :bundle-cmd {:none ["npx" "webpack" "--mode=development" "--entry" :output-to 
                     "--output-path" :final-output-dir
					 "--output-filename" :final-output-filename]}}	
```

Understanding the above configuration is important, so I'm going to
explain each part.

The [`:target` compiler
option](https://clojurescript.org/reference/compiler-options#target)
is set to `:bundle` to instruct the ClojureScript compiler to produce
an output file that can be bundled by a JavaScript bundler like Webpack.

The [`:bundle-cmd` compiler
option](https://clojurescript.org/reference/compiler-options#bundle-cmd)
is set to 

```clojure
{:none ["npx" "webpack" "--mode=development" "--entry" :output-to 
        "--output-path" :final-output-dir
		"--output-filename" :final-output-filename]}}
```

This provides the ClojureScript compiler with a command that it can
use to bundle the intermediate output of the compiler into its final
bundled form.

Figwheel adds some additional functionality to the `:bundle-cmd`. It
interpolates the keywords `:output-to`, `:final-ouitput-dir` and
`:final-output-filename` into the command. In this case the
`:output-to` is going to be replaced by the default `:output-to` path
`./target/public/cljs-out/dev/main.js`. The `:final-output-dir` is
replaced by the path part of `:output-to` which is
`./target/public/cljs-out/dev/`. The `:final-output-filename` defaults
to the filename of of `:output-to` with a `_bundle` added before the
extension or `main_bundle.js`.

Or stated more simply in this case:

* `:output-to` is replaced with `./target/public/cljs-out/dev/main.js`
* `:final-output-dir` is replaced with `./target/public/cljs-out/dev`
* `:final-output-filename` is replaced with `main_bundle.js`

If you supply your own `:output-to` cljs compiler option, it will be
used instead of the default.

After the ClojureScript compiler is finished compiling it will call
the `:bundle-cmd` to bundle up the output.

In this case it will call:

```sh
$ npx webpack --mode=development --entry ./target/public/cljs-out/dev/main.js --output-path ./target/public/cljs-out/dev --output-filename main_bundle.js
```

This will bundle up the `main.js` file and pull in the `moment`
dependency along with it.

Now let's modify our source file to use `moment` so that we can make
sure that things are working.

Edit the `src/hello_world/core.cljs` to look like:

```clojure
(ns hello-world.core
  (:require [moment]))

(js/console.log moment)
(println (str "Hello there it's "
              (.format (moment) "dddd")))
```

## Run the build

OK now that we've setup everything up we can run the build.

```sh
$ clojure -m figwheel.main -b dev -r
```

The browser and REPL should launch as usual:

![Repl](/assets/images/npm_run_build.png)

Except now you can see one additional line in the output which
notifies us that the bundle command was called.

And if you look at the dev tools console of the browser window that
just popped open you should see similar output printed as below
verifying that we were able to use `moment` successfully.

![Repl](/assets/images/npm_run_devtools_output.png)

Well we successfully used an [NPM][npm] package from our ClojureScript
code. Now you can `npm add` other JavaScript NPM packages and use them
from ClojureScript 

## Simplifying with `:auto-bundle`

If you just need to get up and running quickly the `:auto-bundle`
Figwheel option will set up all the default options that we configured above.

So this configuration is equivalent to the above configuration:

```clojure
^{:auto-bundle :webpack}
{:main hello-world.core}
```

When enabled `:auto-bundle` will set `:target` to `:bundle`.

When choosing `:webpack` it will set `:bundle-cmd` to:

```clojure
{:none ["npx" "webpack" "--mode=development" "--entry ":output-to 
        "--output-path" :final-output-dir
		"--output-filename" :final-output-filename]
 :default ["npx" "webpack" "--mode=production" "--entry" :output-to 
           "--output-path" :final-output-dir
		   "--output-filename" :final-output-filename]}
``` 
  
and when choosing `:parcel` it will set `:bundle-cmd` to:

```clojure
{:none ["npx" "parcel" "build" :output-to
        "--out-dir" :final-output-dir
        "--out-file" :final-output-filename
        "--no-minify"]
 :default ["npx" "parcel" "build" :output-to
           "--out-dir" :final-output-dir
           "--out-file" :final-output-filename]}
```

These `:bundle-cmd` configurations are merged with any `:bundle-cmd`
configurations in your `.cljs.edn` file so that you can override
either the default ones.

`:auto-bundle` also adds an important config to `:closure-defines` in
your compiler options when not using `:optimizations` `:none`.

It adds:

```clojure
{:closure-defines {...
                   cljs.core/*global "window"}}
```

## Configuration Tips

### Don't specify `:output-to`

Don't specify the `:output-to` compiler option. Figwheel will take
care of this for you and normally it's just an intermediate file. You are
probably much more interested in the output of the bundler.

If you must specify where the output of the compiler is sent use
`:output-dir` and Figwheel will specify an `[:output-dir]/main.js`
file for you.

It's important to remember that all the output-files still have to be
accessible from the browser (i.e. served by the web-server) when
developing, as these files are not included in the output bundle.

### Using `:final-output-to`

> `:final-output-to` is a Figwheel option, NOT a ClojureScript
> compiler option. Place it in the metadata section of your build
> config.

If you don't want to use the default location and need to specify a
specific location for your build's final bundled asset its probably
best to supply a `:final-output-to` config option.

You don't need to specify `:final-output-to` if you are **not** using the
built in Figwheel REPL host page, [Extra-Mains](/docs/extra_mains.html), or
[Auto-testing](/docs/testing.html#auto-testing).

However, its helpful for Figwheel to know where the final bundled
asset of your build is located. If the location is known then Figwheel
can provide you a REPL without having to create and `index.html` page.
Figwheel can also munge the name of the `:final-output-to` to create
bundles for [Extra-Mains](/docs/extra_mains.html) and
[Auto-testing](/docs/testing.html#auto-testing). Both
`:final-output-dir` and `:final-output-filename` are derived from
`:final-output-to`.

### Using `:bundle-cmd`

The `:bundle-cmd` compiler option is not required. It specifies what
to do with the a compiled ClojureScript output file.

You can skip using `:bundle-cmd` entirely and this would require that
you run the bundler manually **AFTER** your files have been initially
compiled.

This can be unpleasant because it's rather nice to run a Figwheel build
command and have the browser window pop open along with a running REPL.

When you omit the `:bundle-cmd` you will need to launch figwheel
first. The browser will pop open and display your custom `index.html`
application page but it will be broken because the bundled JavaScript
isn't available yet. Next, you will need to run your bundler and
reload the browser. At this point, everything should be up and running
fine. However, this is not the best experience and why using the
`:bundle-cmd` is helpful.

Another reason the `:bundle-cmd` is helpful is to provide Figwheel a
template of a command that can create bundles for your build. If you
create a `:bundle-cmd` with the keywords `:output-to` and
`:final-output-to`, Figwheel will be able to reuse that command with
different parameters to create slightly different bundles for things
like [Extra-Mains](/docs/extra_mains.html) and
[Auto-testing](/docs/testing.html#auto-testing).

When filling in the `:bundle-cmd` template Figwheel also replaces the
`:final-output-dir` and `:final-output-filename` template keywords
(this helps with commands that require them). It obtains these values
from the `:final-output-to` value.

Figwheel by default only runs the `:bundle-cmd` after the first
compile, this avoids the incurring the latency of bundling on every
single file change. This can significantly slow down hot-reloading
depending on your set up. You can change this with the `:bundle-freq`
Figwheel option.


## Using `:bundle-freq`

The `:bundle-freq` Figwheel option controls how often a the
`:bundle-cmd` is called.

It has three settings `:once`, `:always`, and `:smart`:

* `:once` - bundles only once after the initial build compile. This
  helpful if you want to launch a `webpack` watch command on the
  command line after you start your build. 
* `:always` - bundles after every ClojureScript compile. This is fine
  when you are starting out and bundle times are short. This however
  increases the total compile time and can slow down your hot-reload
  time.
* `:smart` - re-bundles only when your `:output-to` or `npm_deps.js`
  changes thus trying to only bundle when you have included a new node
  dependency in your CLJS code. This will cover most cases where you
  need to rebundle but is no where near a complete as launching a
  `webpack` watcher.
  
The default is `:once` which is the most conservative setting. 

I think that using `:smart` is probably the best choice if you are not
using a `webpack` watcher.

> Keep in mind that a hot-reload will NOT pick up changes from a
> re-bundling. You have to refresh the browser to get those changes.
> So if you add a node dependency and it gets re-bundled into your JS
> bundle you won't see it until you reload the browser.

## Using a `webpack.config.js`

It's important to note that you can still use a `webpack.config.js` to
specify various configurations for your bundle. The CLI options simply
override the configuration in the `webpack.config.js` file. If an
input file is specified on the command line it will simply override
any `entry` supplied in the Webpack config. If an `-o` is supplied it will
any `output` supplied in the Webpack config as well.

This frees you to configure your Webpack bundle as you need.

## How NPM support works

The motivation is that we want to have a JavaScript bundler process
our ClojureScript output so that it can bring in and bundle all of our
NPM dependencies. So we have two output files the output file from
ClojureScript and the final bundled output from a JavaScript
bundler like [Webpack][webpack] or [Parcel][parcel].

When you use the `:bundle` target, the ClojureScript compiler does
just this. We're going to examine how this is accomplished.

Let's look at an example default output file when you don't supply a
`:target` option and thus get the default browser target output.

```javascript
window.CLOSURE_UNCOMPILED_DEFINES = {"figwheel.repl.connect_url":"ws:\/\/localhost:9500\/figwheel-connect?fwprocess=29ee5f&fwbuild=devy"};
window.CLOSURE_NO_DEPS = true;
if(typeof goog == "undefined") document.write('<script src="/cljs-out/devy/goog/base.js"></script>');
document.write('<script src="/cljs-out/devy/goog/deps.js"></script>');
document.write('<script src="/cljs-out/devy/cljs_deps.js"></script>');
document.write('<script>if (typeof goog == "undefined") console.warn("ClojureScript could not load :main, did you forget to specify :asset-path?");</script>');
document.write('<script>goog.require("figwheel.core");</script>');
document.write('<script>goog.require("figwheel.main");</script>');
document.write('<script>goog.require("figwheel.repl.preload");</script>');
document.write('<script>goog.require("devtools.preload");</script>');
document.write('<script>goog.require("figwheel.main.system_exit");</script>');
document.write('<script>goog.require("process.env");</script>');
document.write('<script>goog.require("hello_world.core");</script>');
```

In the code above you can see the standard ClojureScript bootstrap
code that loads your application into the browser (along with the
figwheel preloads).

When we are working with NPM, we want the ClojureScript compiler to
resolve all of our references to NPM libraries in our `ns`
declarations and then add them into this file somehow so we can call
`webpack` on it to import them.

For example when we have a namespace like above:

```clojure
(ns hello-world.core
  (:require [moment]))
```

We want ClojureScript to make a note of that and then emit something
that can bundled and that will process and include the `moment` NPM
library.

With this in mind let's look at the main output-to file when we use
the `:bundle` target.

```javascript
import {npmDeps} from "./npm_deps.js";
window.CLOSURE_UNCOMPILED_DEFINES = {"figwheel.repl.connect_url":"ws:\/\/localhost:9500\/figwheel-connect?fwprocess=87057f&fwbuild=dev","cljs.core._STAR_target_STAR_":"bundle"};
window.CLOSURE_NO_DEPS = true;
if(typeof goog == "undefined") document.write('<script src="/cljs-out/dev/goog/base.js"></script>');
document.write('<script src="/cljs-out/dev/goog/deps.js"></script>');
document.write('<script src="/cljs-out/dev/cljs_deps.js"></script>');
document.write('<script>if (typeof goog == "undefined") console.warn("ClojureScript could not load :main, did you forget to specify :asset-path?");</script>');
document.write('<script>goog.require("figwheel.core");</script>');
document.write('<script>goog.require("figwheel.main");</script>');
document.write('<script>goog.require("figwheel.repl.preload");</script>');
document.write('<script>goog.require("devtools.preload");</script>');
document.write('<script>goog.require("figwheel.main.system_exit");</script>');
document.write('<script>goog.require("process.env");</script>');
document.write('<script>goog.require("hello_world.core");</script>');
window.require = function(lib) {
   return npmDeps[lib];
}
```

The first line is new. Let's look at it:

```javascript
import {npmDeps} from "./npm_deps.js";
```

OK so we are importing an `npm_deps.js` file. Let's look at its contents as well:

```javascript
module.exports = {
  npmDeps: {
    "moment": require('moment')  }
};
```

So, the `npm_deps.js` file is a JavaScript bundler ready file that was
generated by the CLJS compiler. The compiler looked at all of our `ns`
declarations and resolved the NPM libraries by looking at the
`node_modules` directory.

Now when we run a bundler on our `main.js` output file the bundler will
resolve the `npm_deps.js` file and then will resolve and include all
NPM libs we are using.

It's important to note that the `main.js` output file is importing the
`./npm_deps.js` file *relatively* so both these files need to be in
the same directory. ClojureScript will let you break this easily if
you supply an `:output-to` option that isn't in the output directory.

Now let's look at the last lines of our `main.js` output file.

```javascript
window.require = function(lib) {
   return npmDeps[lib];
}
```

These lines shim `require` so that it requiring a NPM library in
ClojureScript works correctly.

Further is we look at the output of compiling the
`hello_world/core.cljs` file you will see this:

```javascript
// Compiled by ClojureScript 1.10.775 {:target :nodejs}
goog.provide('hello_world.core');
goog.require('cljs.core');
hello_world.core.node$module$moment = require('moment');
console.log(hello_world.core.node$module$moment);
cljs.core.println.call(null,["Hello there it's ",cljs.core.str.cljs$core$IFn$_invoke$arity$1(hello_world.core.node$module$moment.call(null).format("dddd"))].join(''));

//# sourceMappingURL=core.js.map
```

The interesting line is where a scoped local reference of the `moment`
is created.

```javascript
hello_world.core.node$module$moment = require('moment');
```

The `:bundle` target uses the current functionality of the `:nodejs`
target which emits these requires. And while this works fine for
`Node` we need to shim `require` to support it in the browser.

Keep in mind this is all only true when we are developing in
`:optimizations :none`. When we want to create a single compiled
artifact to deploy for production, using `:simple` or `:advanced`
optimization mode ClojureScript will put together a single output file
with the requires in it. Then the bundler will resolve and replace
these top `require`s as it normally does.

## Problems with cached compiled assets

Looking at the previous example if we hadn't been using the `:bundle`
target had been using the Cljsjs `cljsjs/moment` library instead of
the NPM based `moment` library the output of compiling
`hello_world/core.cljs` would have instead been the following:

```javascript
// Compiled by ClojureScript 1.10.775 {}
goog.provide('hello_world.core');
goog.require('cljs.core');
goog.require('moment');
hello_world.core.global$module$moment = goog.global["moment"];
console.log(hello_world.core.global$module$moment);
cljs.core.println.call(null,["Hello there it's ",cljs.core.str.cljs$core$IFn$_invoke$arity$1(hello_world.core.global$module$moment.call(null).format("dddd"))].join(''));

//# sourceMappingURL=core.js.map
```

The major difference is one the line:

```clojure
hello_world.core.global$module$moment = goog.global["moment"];
```

which when compiled with an NPM `moment` library looks like this:

```clojure
hello_world.core.node$module$moment = require('moment');
```

When transitioning from Cljsjs libraries to NPM based libraries the
way that `moment` is required changes. This can become a problem while
we are actively converting our code to use NPM libraries.

The caching of compiled code can wreak havoc on this process. The AOT
caching that happens when you set `:aot-cache true` caches compiled
libraries *globally*.

If you compile a version of `reagent` the depends on Cljsjs based
`react` and it gets cached to the global AOT-cache, then the compiled
`reagent` lib in the cache will have been specialized to use a Cljsjs
based `react`.

If you then create a new project that has a `reagent` dependency that
tries to meet its `react` dependency from NPM it will fail if you are
using `:aot-cache true`. It will fail because you aren't providing the
library via Cljsjs but the cached version of `reagent` is expecting
it.

This same caching problem also occurs with code that's been compiled
to your local target directory. A compiled `reagent` library could
be specialized to either NPM or Cljsjs.

This becomes even more confusing if you are accidentally providing
both the Cljsjs lib and the NPM lib. It's possible that your code will
appear to work but may potentially be fetching the JavaScript library
from the wrong source.

*Bottom line*

As a rule, if you are using NPM libraries along with the `:bundle`
target don't use `:aot-cache true`. The default value of `:aot-cache`
is `false` in Figwheel. However the default value of `:aot-cache` is
`true` when you use `cljs.main`.

Also, if your are porting a project over to use the `:bundle` target
and NPM libs, use the
[`:clean-outputs`](/config-options#clean-outputs) Figwheel option. The
clean outputs option will delete all of your compiled artifacts
everytime you start a Figwheel build.

If you run into a situation where your Cljsjs lib or your NPM lib
doesn't resolve you should look at the compiled output file and verify
that the file is being required correctly as you may have a caching
problem.

## Compiling for production

When you want to compile your final output file you will use an
`:optimizations` mode other than `:none`. 

The thing to remember is that the ClojureScript compiler is going to
outputing a single JavaScript file and then the bundler is going to
resolve all of the JavaScript `require`s present in that file in order
to ultimately create a JavaScript bundle that you can deploy.

This is different process to what happens during development (under
`:optimizations :none`). During developement bundle only resolves the
`require`s in the `:output-to` file and the `npm_deps.js` file.

This is an important difference because if there is an errant
`require` in your code that is called conditionally it won't cause a
problem during development because require is shimmed, however the
bundle process is resolving `require`s statically. So if there is a
`(js/require ...)` for a library that isn't in `node_modules` or is
only available in the `Nodejs` runtime (but not in the browser) your
bundler will throw an error about a missing library.

Keeping this in mind we should remember to not dynamically `require`
NPM libraries that are only available on the backend, or are not
currently stored in `node_modules`.

When compiling for production we should also, make sure that you
aren't including Cljsjs libraries that are being provided for by NPM
libraries. We don't want to be including libraries twice.

For example, we need to exclude `react` dependencies from `reagent` to
prevent bundling to copies of `react` into your final application.

An example of excluding `react` from `reagent`:

```clojure
{:deps {org.clojure/clojurescript {:mvn/version "1.10.773"}
        reagent {:mvn/version "0.10.0" :exclusions [cljsjs/react cljsjs/react-dom]}}}
```

We also need to make sure we include `cljs.core/*global* "window"` in
our `:closure-defines` (I.e.`:closure-defines {cljs.core/*global*
"window" ...}`). This is required when we aren't compiling in
`:optimizations :none`.  Figwheel adds this automatically when using the
`:auto-bundle` option.

## Troubleshooting

### Bad bundle command

The most likely thing that will happen is you will have a bad
`:bundle-cmd`.

* Please check that the command that is logged is the
command that you expect.
* comment out the `:bundle-cmd` option, run the figwheel build and then run the
webpack command from your terminal/shell environment to see what
errors are showing up

### The relationship between the `:output-to` file and `npm_deps.js`

The `:output-to` file emitted by the `:bundle` target imports the
`npm_deps.js` file. This can only work when the `:output-to` file is
in the same directory as the `npm_deps.js` file. I.E. the `:output-to`
file has to be in the `:output-dir` for the build.

{::comment}
TODO
- advanced 
- talk about the combination of bundling :once and the launching a watcher
- maybe discouraging the :output-to is a bad idea, perhaps just encourage the
"_bundle" convention.
{/:comment}

[webpack]: https://webpack.js.org/
[parcel]: https://parceljs.org/
[base-example-gist]: https://gist.github.com/bhauman/a5251390d1b8db09f43c385fb505727d
[npm]: https://www.npmjs.com/






