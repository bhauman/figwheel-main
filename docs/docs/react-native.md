---
title: React Native
layout: docs
category: docs
order: 13
---

# React Native

<div class="lead-in">React Native let's you build cross platform
mobile and desktop Native applications using good old React. Figwheel
now makes setting up a <a href="https://reactnative.dev">React
Native</a> and ClojureScript project a breeze.</div>

## Creating a React Native project

We're going to walk through setting up a Figwheel build for a React
Native project.

> When working with React Native you will be using NPM packages. This
> guide assumes that you have read and understood the 
> [Using NPM](/docs/npm.html) docs page.

> The [React Native](https://reactnative.dev/docs/getting-started)
> docs are great and well worth your time.

## Initial setup

First you will need to make sure you have React Native and its
dependencies installed.

On the [environment setup page](https://reactnative.dev/docs/environment-setup) page you will
want to choose either the `React Native CLI` or the `Expo CLI`.  I
prefer to start with the React Native CLI as there is less complexity
in the tooling to deal with making it easier to figure out what is
going on when you use it. However, `Expo` has its benefits and is very
popular.

Install your CLI of choice according to the instructions on that page.

Once things are installed you can then follow the instructions below
to get an ClojureScript project setup for Figwheel development.

## Create React Native project

Initialize a project:

For `React Native CLI` do:

```shell
$ npx react-native init MyAwesomeProject
```

For `Expo CLI` do:

```shell
$ npx expo init MyAwesomeProject
```

> In Expo choose either the `blank` or `minimal` template

This will create an initial React Native project. Before you go any
further you will want to ensure that everything is setup so that you can
launch and run your application in a simulator.

Change into the `MyAwesomeProject` directory and launch a simulator like so:

```shell
$ npx react-native run-ios # or run-android
```

If everything is set up correctly this should launch a phone simulator
with the RN application defined in `App.js`.

> This may be a good point to familiarize yourself a bit with React
> Native development.

## Troubleshooting

If you have any problems with setting up an application please consult
the React Native documentation. I really recommend reading all of the
[React Native documentation](https://reactnative.dev/docs/getting-started) as it is well written and will more than
likely save you lots of headaches.

If everything is up and running go ahead an close everything so that
we can setup a ClojureScript application that uses `figwheel-main` to
support hot reloading and a REPL.

## Integrating the ClojureScript and Figwheel 

Now we'll start setting up a basic Figwheel project.

Create a `deps.edn` file in the `MyAwesomeProject` directory:

```clojure
{:deps {org.clojure/clojurescript {:mvn/version "1.10.773"}
        com.bhauman/figwheel-main {:mvn/version "0.2.19"}}
 :paths ["src" "target"]}
```

Create a `ios.cljs.edn` file in the `MyAwesomeProject` directory:

```clojure
^{:react-native :cli} ;; use :expo if you are using Expo
{:main awesome.main}
```

Remember to use `:react-native :expo` if you are using Expo.


Create a `src/awesome/main.cljs` file in the `MyAwesomeProject` directory:

```clojure
(ns awesome.main
  (:require [react]
            [react-native :as rn]))

(def <> react/createElement)

(defn renderfn [props]
  (<> rn/View
      #js {:style #js {:backgroundColor "#FFFFFF"
                       :flex 1
                       :justifyContent "center"}}
      (<> rn/Text
          #js {:style #js {:color "black"
                           :textAlign "center"}}
          (str "HELLO"))))

;; the function figwheel-rn-root MUST be provided. It will be called by 
;; by the react-native-figwheel-bridge to render your application. 
(defn figwheel-rn-root []
  (renderfn {}))
```

We are almost ready to launch our ClojureScript application, however
if you are using Expo we'll need to make a few adjustments first.

> If you are using Expo **edit** `package.json` and change `"main":
> "node_modules/expo/AppEntry.js"` to `"main": "index.js"`. Also
> **delete** the original `App.js` file at the root of the project as
> it leads to compilation problems in certain Expo templates.

Now we are ready to launch our ClojureScript application:

First we will start the `figwheel-main` process to compile our project
and create a Websocket for REPL communication.

```shell
$ clj -m figwheel.main -b ios -r
```

**Wait for the compile to complete** up until the REPL is trying to
start and connect. At this point your ClojureScript code has been
compiled and you are ready to open your application. In another
terminal window change into the `MyAwesomeProject` directory and start
`react-native` using the correct command for your chosen CLI.

Currently for both CLIs you can run:

```shell
$ npm run ios
```

For Android:
```
$ adb reverse tcp:9500 tcp:9500
$ npm run android
```

When using `figwheel-main`, it will take care of auto refreshing the
application for you when figwheel reloads code.

You can see this behavior by editing the `src/awesome/main.cljs`
file. Try changing the `"HELLO"` to `"HELLO THERE"`. You should see
the application change when you save `src/awesome/main.cljs`.

## Auto launching React Native tooling with :launch-js

Using the [`:launch-js`](/config-options#launch-js) Figwheel option
you can set it up so that when you run Figwheel it will launch your
React Native tooling.

For example:

```clojure
^{:react-native :cli
  :launch-js ["npm" "run" "ios"]}
{:main awesome.main}
```

Now when you launch Figwheel it will take care of launching React
Native for you.

This may or may not work for you and is highly dependent on the
behavior of React Native tooling. Currently this set up works for me
but your mileage may vary.

## Compiling for production

You will want to compile for production before you create a *release*
build for your native project.

You can advance compile the above sample project using the command:

```shell
$ clj -m figwheel.main -O advanced -bo ios
```

Compiling for production carries the same caveates as usual. Please
see the [Advanced compile docs](/docs/advanced_compile.html).

## Controlling Reload

The React Native Figwheel bridge code automatically refreshes the
application by forcing an update on the root element of the
application. You may want to control the code refreshes yourself.

After application is loaded a `figwheelBridgeRefresh` function is
registered on `goog`.  You can call this function to force the root
element to reload.

So for the above example you could set the
`:react-native-auto-refresh` option to `false`.

In `ios.cljs.edn` this looks like:

```clj
^{:react-native :cli
  :react-native-auto-refresh false}
{:main awesome.main}
```

and you can then control reloading via `figwheel.main`'s [reload
hooks](https://figwheel.org/docs/hot_reloading.html#reload-hooks)

Using our `src/awesome/main.cljs` an example of this looks like:

```clojure
(ns ^:figwheel-hooks awesome.main
  (:require [react]
            [react-native :as rn]))

(def <> react/createElement)

(defn renderfn [props]
  (<> rn/View
      #js {:style #js {:backgroundColor "#FFFFFF"
                       :flex 1
                       :justifyContent "center"}}
      (<> rn/Text
          #js {:style #js {:color "black"
                           :textAlign "center"}}
          (str "HELLO"))))

(defn figwheel-rn-root []
  (renderfn {}))

;; adding the reload hook here
(defn ^:after-load on-reload [] (goog/figwheelBridgeRefresh))
```

## React Native on MacOS and Windows

Thanks to Microsoft publishing
[react-native-macos](https://github.com/Microsoft/react-native-macos)
and
[react-native-windows](https://github.com/Microsoft/react-native-windows)
we can use React Native along with ClojureScript to create desktop
applications.

The setup for MacOS is very similar to the above.

You will need to setup a RN MacOS [project according to the
instructions](https://microsoft.github.io/react-native-windows/docs/rnm-getting-started).

Then just follow the instructions above for the React Native CLI.

The only difference is that you need to launch the React Native MacOs tooling  with:

```shell
$ npx react-native run-macos
```

And there you go.

I haven't tried this for
[react-native-windows](https://github.com/Microsoft/react-native-windows)
but it should work in the same manner.

## Details

This uses
[`react-native-figwheel-bridge`](https://github.com/bhauman/react-native-figwheel-bridge)
to bridge the gap from React Native to figwheel, feel free to read the
code and learn more about the details of how this is all managed.

This also introduces a compiler pass taken from
[Krell](https://github.com/vouch-opensource/krell) to support
`js/require` of images and assets.

