## `figwheel.main.api/start`

Args: `([build] [figwheel-options-o-build build & background-builds])`

Starts a Figwheel build process.

Has two arities:

(start build)
(start figwheel-config-o-build build & backgound-builds)

You can call `start` with any number of `build` arguments. The first
one will be the foreground build and any builds that follow will be
background builds. When you provide more than one argument to `start`
the first argument can optionally be a map of Figwheel Main options.

A `build` arg can be either:
* the name of a build like "dev" (described in a .cljs.edn file) 
* a map describing a build with the following form

```
{
     :id      "dev"                  ; a required string build id   
     :options {:main hello-world.core} ; a required map of cljs compile options
     :config  {:watch-dirs ["src"]}  ; an options map of figwheel.main config options
}
```

If the `:options` map has Figwheel options metadata, it will be used
unless there is non-nil `:config` option. The presence of a non-nil
`:config` option map will cause any metadata on the `:options` map
to be ignored.

The `figwheel-config-o-build` arg can be a build or a map of
Figwheel options that will be used in place of the options found in
a `figwheel-main.edn` file if present.

The `background-builds` is collection of `build` args that will be
run in the background. 

Examples:

```clojure
; The simplest and most common case. This will start figwheel just like
; `clojure -m figwheel.main -b dev -r`
(start "dev") 

; With inline build config
(start {:id "dev" 
        :options {:main 'example.core} 
        :config {:watch-dirs ["src"]}})

; With inline figwheel config
(start {:css-dirs ["resources/public/css"]} "dev")

; With inline figwheel and build config:
(start {:css-dirs ["resources/public/css"]}
       {:id "dev" :options {:main 'example.core}})
```

### REPL Api Usage

Starting a Figwheel build stores important build-info in a build
registry. This build data will be used by the other REPL Api
functions:

* `figwheel.main.api/cljs-repl`
* `figwheel.main.api/repl-env`
* `figwheel.main.api/stop`

If you are in a REPL session the only way you can use the above
functions is if you start Figwheel in a non-blocking manner. You can
make `start` not launch a REPL by providing a `:mode :serve` entry in
the Figwheel options.

For example neither of the following will start a REPL:

```clojure
(start {:mode :serve} "dev")

(start {:id "dev" 
        :options {:main 'example.core} 
        :config {:watch-dirs ["src"]
                 :mode :serve}})
```  

The above commands will leave you free to call the `cljs-repl`,
`repl-env` and `stop` functions without interrupting the server and
build process.

However once you call `start` you cannot call it again until you
have stopped all of the running builds.


## `figwheel.main.api/cljs-repl`

Args: `([build-id])`

Once you have already started Figwheel in the background with a
call to `figwheel.main.api/start`

You can supply a build name of a running build to this function to
start a ClojureScript REPL for the running build.

Example:

```clojure
(figwheel.main.api/cljs-repl "dev")
```


## `figwheel.main.api/repl-env`

Args: `([build-id])`

Once you have already started a build in the background with a
call to `start`

You can supply the `build-id` of the running build to this function
to fetch the repl-env for the running build. This is helpful in
environments like **vim-fireplace** that need the repl-env.

Example:

```clojure
(figwheel.main.api/repl-env "dev")
```

The repl-env returned by this function will not open urls when you
start a ClojureScript REPL with it. If you want to change that
behavior:

```clojure
(dissoc (figwheel.main.api/repl-env "dev") :open-url-fn)
```

The REPL started with the above repl-env will be inferior to the
REPL that is started by either `figwheel.main.api/start` and
`figwheel.main.api/cljs-repl` as these will listen for and print out
well formatted compiler warnings.


## `figwheel.main.api/stop`

Args: `([build-id])`

Takes a `build-id` and stops the given build from running. This
will not work if you have not started the build with `start`


## `figwheel.main.api/stop-all`

Args: `([])`

Stops all of the running builds.


## `figwheel.main.api/start-join`

Args: `([& args])`

Takes the same arguments as `start`.

Starts figwheel and blocks, useful when you want Figwheel to block
on the server it starts when using `:mode :serve`. You would
normally use this in a script that would otherwise exit
prematurely.

