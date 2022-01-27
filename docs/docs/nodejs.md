---
title: Using Nodejs
layout: docs
category: docs
order: 16
---

<div class="lead-in">Normally we are targeting the front end when we
write ClojureScript are but you can also target the backend and write
app servers with Nodejs.</div>

# Setting up a Nodejs application

First we will set up a simple hello world application to ensure
everything is up and running properly.

This is going to be very similar to a regular Figwheel project that
targets the web.

First off we'll create a project directory and change into it:

```shell
$ mkdir hello-node
$ cd hello-node
```

Let's add a `src/example/core.cljs` file in our new project directory
with the following contents:

```clojure
(ns example.core)

(console.log "hello NodeJS!!")
```

And then we'll add a `dev.cljs.edn` file with the following configuration:

```clojure
{:main example.core
 :target :nodejs}
```

And a `deps.edn` as well.

```clojure
{:deps {org.clojure/clojurescript {:mvn/version "1.10.773"}
        com.bhauman/rebel-readline-cljs {:mvn/version "0.1.4"}
        com.bhauman/figwheel-main {:mvn/version "0.2.16"}}}
```

Now that we've created our three basic files let's start up a
Node/CLJS repl with Figwheel.

Let's give it a whirl:

```shell
$ clojure -m figwheel.main -b dev -r
```

When you run that you should see:

![Figwheel node repl image](/assets/images/node-run-repl.png)

In this case Figwheel ran the `node` executable for you taking care to
run it with the correct parameters etc.

Now stop the figwheel build/repl with `Control-D`.

> You can turn off launching `node` in the background by setting the
> [`:launch-node`](/config-options#launch-node) to `false`.

## Adding moment.js as a dependency

Let's add some depdencies with `npm` and use them.

First we'll initialize `npm` running `npm init -y` in the root of the project directory:

```shell
$ npm init -y
```

This should create a `package.json` file.

Next let's add the `moment` date library.

```shell
$ npm add moment
```

We'll alter the `example/core.cljs` to use the `moment` library.

```clojure
(ns example.core
  (:require [moment]))
  
(defn one-day-from-now []
  (->
   (moment)
   (.add 1 "days")
   (.calendar)))

(println (one-day-from-now) " <<<---")
```

Now we can run Figwheel again:

```shell
$ clojure -m figwheel.main -b dev -r
```

This will boot you into a REPL you won't see the output of `(println
(str (one-day-from-now) " <<<---"))` in the REPL as it happens before
the REPL starts.

If we look at the contents of `target/node/dev/node.log` we will see
the output of the first run of the program.

```shell
$ cat target/node/dev/node.log
Tomorrow at 1:35 PM  <<<---
 [Figwheel REPL] Connected: http-long-polling
 [Figwheel REPL] Session ID: 2d024034-415d-4431-817f-a2cb196a618c
 [Figwheel REPL] Session Name: Roselle
n found! Falling back to http-long-polling:
 For a more efficient connection ensure that "ws" is installed :: do -> 'npm install ws'
```

> The note about `http-long-polling` can be corrected by adding the `ws`
> library to your dev dependencies via `npm install ws
> --save-dev`. This is desirable as it allows Figwheel to use a
> Websocket for its REPL communication instead of long-polling.

Since Figwheel is running you can now change the file to subtract one day.

```clojure
(ns example.core
  (:require [moment]))
  
(defn one-day-ago []
  (->
   (moment)
   (.subtract 1 "days")
   (.calendar)))

(println (one-day-ago) "<<<---")
```

Now save the file.  If you look in the REPL you should now see the
printed output of `(println (one-day-from-now) "<<<---")`.

You can now interatively work on the program and experiment with the
`moment` API. Look at the examples on
[https://momentjs.com/](https://momentjs.com/).

When you are finished kill the REPL with `Control-D`.

## Starting a `express` webservice

Now lets create a webserver that shows the current time minus one day.

Let's add the `express` library.

```shell
$ npm add express
```

Now we can setup an web application that serves our `moment` output.

Edit the `src/example/core.cljs` to look like this:

```clojure
(ns example.core
  (:require [moment]
            [express]
            [http]))

(defn one-day-ago []
  (->
   (moment)
   (.subtract 1 "days")
   (.calendar)))

;; app gets redefined on reload
(def app (express))

;; routes get redefined on each reload
(.get app "/"
      (fn [requst response]
        (.send response (one-day-ago))))

;; This is called once on start and dispatches requests to
;; the current "app"
(defn -main []
	;; This is the secret sauce. you want to capture a reference to 
    ;; the app function (don't use it directly) this allows it to be redefined on each reload
    ;; this allows you to change routes and have them hot loaded as you
    ;; code.
  (doto (.createServer http #(app %1 %2))
    (.listen 3000)))

;; *main-cli-fn* only gets called once on startup
(set! *main-cli-fn* -main)
```

You'll find that if you start figwheel up again and go to
`localhost:3000` in your browser you will see something like
`Yesterday at 2:42 PM` on that page. You have sucessfully created a
`express` app.

There are some subtleties about the way this is written to allow hot
reloading to redefine the application and thus allow you to edit and
add new routes.

The main thing is that when the file is reloaded you are creating a
new `app` that gets new routes assigned to it on each reload.

The `-main` function only get's called once and we capture a reference
to `app` which will always point to the currently defined `app`.

## Using the chrome inspector

Every time you start a Figwheel node build there are two very helpful
lines printed out.

```shell
For a better development experience:
  1. Open chrome://inspect/#devices ... (in Chrome)
  2. Click "Open dedicated DevTools for Node"
```

If you follow those instructions you will get a Chrome Dev Tools
console that you can use.


