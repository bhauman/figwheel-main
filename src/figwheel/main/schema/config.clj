(ns figwheel.main.schema.config
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.spec.alpha :as s]
   [clojure.set]
   [figwheel.main.util :as util]
   [figwheel.main.schema.core
    :as schema
    :refer [def-spec-meta non-blank-string? directory-exists? file-exists?
            ensure-all-registered-keys-included]]
   [expound.alpha :as exp]
   [spell-spec.alpha :as spell]
   [spell-spec.expound]))

(s/def ::watch-dirs
  (s/coll-of
   (s/and non-blank-string?
          directory-exists?
          ::schema/has-cljs-source-files)))

#_(exp/expound ::watch-dirs ["/Users/bhauman/workspace/temp/figtest/ouchy"])

(def-spec-meta ::watch-dirs
  :doc
  "A list of ClojureScript source directories to be watched and compiled on change.

The directories in `:watch-dirs` are passed to the compiler as source
directories. For this reason, any entry in the watch directories must
be on the classpath and must point to the root directory of a
ClojureScript namespace source tree.

I.E. If your `example.core` namespace is located at
`src/cljs/example/core.cljs` you cannot use `src` as an element of
`:watch-dirs`, you must use the path to the root directory of the
namespace tree `src/cljs`.

    :watch-dirs [\"cljs-src\"]"
  :group :common)

(s/def ::css-dirs (s/coll-of (s/and non-blank-string?
                                    directory-exists?)))
(def-spec-meta ::css-dirs
  :doc
  "A list of CSS source directories to be watched and reloaded into the browser.

    :css-dirs [\"resources/public/css\"]"
  :group :common)

(s/def ::ring-handler (s/or :non-blank-string non-blank-string?
                            :symbol ::schema/unquoted-symbol))
(def-spec-meta ::ring-handler
  :doc
  "A symbol or string indicating a ring-handler to embed in the
figwheel.repl server. This aids in quickly getting a dev server up and
running. If the figwheel server doesn't meet your needs you can simply
start your own server. The figwheel.client will still be able to
connect to its websocket endpoint.
Default: none

    :ring-handler my-project.server/handler"
  :group :common)

(s/def ::ring-server-options (s/keys :opt-un
                                     [::schema/integer-port
                                      ::schema/host]))

(def-spec-meta ::ring-server-options
  :doc
  "All the options to forward to the `ring-jetty-adapter/run-jetty` function
which figwheel.main uses to run its ring server.

All the available options are documented here:
https://github.com/ring-clojure/ring/blob/master/ring-jetty-adapter/src/ring/adapter/jetty.clj#L127

This will normally be used to set the `:port` and `:host` of the server.

Most uses of these options are considered advanced. If you find
yourself using many of these options you problably need to run your
own server outside of figwheel.main."
  :group :common)

(s/def ::rebel-readline boolean?)
(def-spec-meta ::rebel-readline
  :doc
  "By default Figwheel engauges a Rebel readline editor when it starts
the ClojureScript REPL in the terminal that it is launched in.

This will only work if you have `com.bhauman/rebel-readline-cljs` in
your dependencies.

More about Rebel readline:
https://github.com/bhauman/rebel-readline

Default: true

    :rebel-readline false"
  :group :common)

(s/def ::pprint-config boolean?)
(def-spec-meta ::pprint-config
  :doc
  "When `:pprint-config` is set to true. The `figwheel.main` will print the
computed config information and will terminate the process. Useful for
understanding what figwheel.main adds to your configuration before it
compiles your build.

Default: false

    :pprint-config true"
  :group :common)

(s/def ::open-file-command non-blank-string?)
(def-spec-meta ::open-file-command
  :doc
  "A path to an executable shell script that will be passed a file and
line information for a particular compilation error or warning.

A script like this would work
ie. in  ~/bin/myfile-opener

    #! /bin/sh
    emacsclient -n +$2:$3 $1

Then add this script in your config:

    :open-file-command \"myfile-opener\"

But that's not the best example because Figwheel handles `emacsclient`
as a special case. So as long as `emacsclient` is on the shell path you can
simply do:

    :open-file-command \"emacsclient\"

and Figwheel will call `emacsclient` with the correct args."
  :group :common)

(s/def ::figwheel-core boolean?)
(def-spec-meta ::figwheel-core
  :doc
  "Whether to include the figwheel.core library in the build. This
 enables hot reloading and client notification of compile time errors.
 Default: true

    :figwheel-core false"
  :group :common)

(s/def ::hot-reload-cljs boolean?)
(def-spec-meta ::hot-reload-cljs
  :doc
  "Whether or not figwheel.core should hot reload compiled
ClojureScript. Only has meaning when :figwheel-core is true.
Default: true

    :hot-reload-cljs false"
  :group :common)

(s/def ::reload-dependents boolean?)
(def-spec-meta ::reload-dependents
  :doc
  "Whether or not figwheel.core should reload the namespaces that
`depend` on the changed namespaces in addition to the changed
namespaces themselves. Only has meaning when :figwheel-core is true.
Default:true

    :reload-dependents false"
  :group :common)

(s/def ::connect-url non-blank-string?)
(def-spec-meta ::connect-url
  :doc
  "The url that the figwheel REPL client will use to connect back to
the server.

This url is actually a template that will be filled in.  For example
the default `:connect-url` is:

    \"ws://[[config-hostname]]:[[server-port]]/figwheel-connect\"

The available template variables are:

For the server side:

    [[config-hostname]]  the host supplied in :ring-server-options > :host or \"localhost\"
    [[server-hostname]]  the java.InetAddress localhost name - \"Bruces-MacBook-Pro.local\" on my machine
    [[server-ip]]        the java.InetAddress localhost ip interface - normally 192.168.x.x
    [[server-port]]      the port supplied in :ring-server-options > :port or the default port 9500

On the client side:

    [[client-hostname]]  the js/location.hostname on the client
    [[client-port]]      the js/location.port on the client

If the url starts with a Websocket scheme \"ws://\" a websocket
connection will be established. If the url starts with an http scheme
\"http\" an http long polling connection will be established."
  :group :common)

(s/def ::open-url (s/or :non-blank-string non-blank-string?
                        :false false?))
(def-spec-meta ::open-url
  :doc
  "Either a boolean value `false` or a string that indicates the url
that the figwheel REPL will open in the browser after the source code
has been compiled. A `false` value will disable this behavior.

The string value is actually a template that can provide optional
template variables. For example the default `:open-url` is:

    \"http://[[server-hostname]]:[[server-port]]\"

The available template variables are:

For the server side:

    [[server-hostname]]  the host supplied in :ring-server-options > :host or \"localhost\"
    [[server-port]]      the port supplied in :ring-server-options > :port or the default port 9500"
  :group :common)

(s/def ::open-url-wait-ms integer?)
(def-spec-meta ::open-url-wait-ms
  :doc
  "The number of milliseconds to wait before launching the browser.

Default: none

    :open-url-wait-ms 1000"
  :group :un-common)

(s/def ::reload-clj-files (s/or :bool boolean?
                                :extension-coll (s/coll-of #{:clj :cljc})))
(def-spec-meta ::reload-clj-files
  :doc
  "Figwheel naively reloads `clj` and `cljc` files on the `:source-paths`.
It doesn't reload clj dependent files like tools.namspace.

Figwheel does note if there is a macro in the changed `clj` or `cljc` file
and then marks any cljs namespaces that depend on the `clj` file for
recompilation and then notifies the figwheel client that these
namespaces have changed.

If you want to disable this behavior:

    :reload-clj-files false

Or you can specify which suffixes will cause the reloading

    :reload-clj-files #{:clj :cljc}"
  :group :common)

(s/def ::log-file non-blank-string?)
(def-spec-meta ::log-file
  :doc
  "The name of a file to redirect the figwheel.main logging to. This
will only take effect when a REPL has been started.

    :log-file \"figwheel-main.log\""
  :group :common)

(s/def ::log-level #{:error :info :debug :trace :all :off})
(def-spec-meta ::log-level
  :doc
  "The level to set figwheel.main java.util.logger to.
Can be one of: `:error` `:info` `:debug` `:trace` `:all` `:off`

    :log-level :error"
  :group :common)

(s/def ::client-log-level  #{:severe :warning :info :config :fine :finer :finest})
(def-spec-meta ::client-log-level
  :doc
  "The log level to set the client side goog.log.Logger to for
figwheel.repl and figwheel.core. Can be one of:
`:severe` `:warning` `:info` `:config` `:fine` `:finer` `:finest`

    :client-log-level :warning"
  :group :common)

(s/def ::log-syntax-error-style #{:verbose :concise})
(def-spec-meta ::log-syntax-error-style
  :doc
  "figwheel.main logging prints out compile time syntax errors which
includes displaying the erroneous code.
Setting `:log-syntax-error-style` to `:concise` will cause the logging to
not display the erroneous code.
Available options: `:verbose`, `:concise`
Default: `:verbose`

    :log-syntax-error-style :concise"
  :group :common)

(s/def ::load-warninged-code boolean?)
(def-spec-meta ::load-warninged-code
  :doc
  "If there are warnings in your code emitted from the compiler, figwheel
does not refresh. If you would like Figwheel to load code even if
there are warnings generated set this to true.
Default: false

    :load-warninged-code true"
  :group :common)

(s/def ::ansi-color-output boolean?)
(def-spec-meta ::ansi-color-output
  :doc
  "Figwheel makes an effort to provide colorful text output. If you need
to prevent ANSI color codes in figwheel output set `:ansi-color-output`
to false.  Default: true

    :ansi-color-output false"
  :group :common)

(s/def ::validate-config boolean?)
(def-spec-meta ::validate-config
  :doc
  "Whether to validate the figwheel-main.edn and build config (i.e.\".cljs.edn\") files.
Default: true

    :validate-config false"
  :group :common)

(s/def ::validate-cli boolean?)
(def-spec-meta ::validate-cli
  :doc
  "Whether to validate the figwheel-main command line options
Default: true

    :validate-cli false"
  :group :common)

(s/def ::target-dir non-blank-string?)
(def-spec-meta ::target-dir
  :doc
  "A String that specifies the target directory component of the path
where figwheel.main outputs compiled ClojureScript

The default [`:output-dir`](https://figwheel.org/docs/compile_config) is composed of:

    [[:target-dir]]/public/cljs-out/[[build-id]]

The default `:output-to` is composed of:

    [[:target-dir]]/public/cljs-out/[[build-id]]-main.js

If you are using the default figwheel.repl server to serve compiled
assets, it is very important that the :target-dir be on the classpath.

The default value of `:target-dir` is \"target\"

    :target-dir \"cljs-target\""
  :group :common)

(s/def ::launch-node boolean?)
(def-spec-meta ::launch-node
  :doc
  "A boolean that indicates whether you want figwheel to automatically
launch Node. Defaults to true."
  :group :common)

(s/def ::inspect-node boolean?)
(def-spec-meta ::inspect-node
  :doc
  "A boolean that indicates whether you want figwheel to enable remote
inspection by adding \"--inspect\" when it launches Node.
Defaults to true."
  :group :common)

(s/def ::node-command non-blank-string?)
(def-spec-meta ::node-command
  :doc
  "A String indicating the Node.js executable to launch Node with.
Defaults to \"node\""
  :group :common)

(s/def ::launch-js (s/or :script-path non-blank-string?
                         :namespaced-symbol ::schema/unquoted-symbol
                         :shell-command-vector (s/coll-of (s/or :script-part non-blank-string?
                                                                :replace #{:open-url :output-to}))))

(def-spec-meta ::launch-js
  :doc
  "Figwheel optionally launches a JavaScript host environment when it
starts a REPL or runs a script. You see this behavior when it opens a
browser or starts Nodejs. This behavior can be overridden with the
`:launch-js` option.

Can take the name of an executable script on your system and will
pass it either the path to the compiled JavaScript (when the target is
Nodejs) or the URL to the JavaScript (when the target is the browser).

Script example:

    #! /bin/sh
    chrome --headless --disable-gpu --repl --remote-debugging-port=9222 $1

If the above script is named `headless-chrome-launcher` and is on your
path, then you would add this to your config:

    :launch-js `headless-chrome-launcher`

Can also take a vector that represents a shell command to invoke. The
vector can contain the keywords `:output-to` and `:open-url` which
will be replaced with the the path or the URL to the compiled
JavaScript.

Shell command vector example:

    :launch-js [\"chrome\" \"--headless\" \"--repl\" \"--disable-gpu\" :open-url]

The `:launch-js` option can also take a namespaced symbol
representing a function to invoke. The function will be passed a map
containing the keys `:open-url` and `:output-to`.

Symbol example:

    :launch-js user/start-js-environment

and in your user.clj file:

    (defn start-js-environment [{:keys [output-to open-url]}]
       (clojure.java.shell/sh \"headless-chome\" open-url))

The `:launch-js` option will take precedence over any node
configurations like `:node-command` or `:launch-node`."
  :group :common)

(s/def ::cljs-devtools boolean?)

(def-spec-meta ::cljs-devtools
  :doc
  "A boolean that indicates whether to include binaryage/devtools into
the clojurescript build. Defaults to true when the target is a browser
and the :optimizations level is :none, otherwise it is false.

    :cljs-devtools false"
  :group :common)

(s/def ::helpful-classpaths boolean?)

(def-spec-meta ::helpful-classpaths
  :doc
  "A boolean that indicates whether figwheel should try and be helpful
by adding classpaths to help you get started, or whether you want to
have complete control over classpaths. Advanced users will want to
disable this option.

    :helpful-classpaths false"
  :group :common)

(s/def ::bundles (s/map-of non-blank-string? non-blank-string?))

(s/def ::npm (spell/strict-keys :req-un [::bundles]))

(def-spec-meta ::npm
  :doc
  "Support for importing webpack bundles.

> Experimental feature! This feature may change or be removed entirely.
> Only available in `0.1.8-SNAPSHOT` or higher

This also works best with ClojureScript >= `1.10.339`.

Currently takes a map with only one valid key `:bundles`. The value of
the `:bundles` key must be a map of bundled JavaScript files to the
index JavaScript files that they are compiled from.

    :npm {:bundles {\"dist/index.bundle.js\" \"src/webpack/index.js\"}}

This feature will read an index.js file like:

    import React from 'react';
    import ReactDom from 'react-dom';
    window.React = React;
    window.ReactDom = ReactDom;

and will then generate a `:foreign-libs` entry for it. For example the
above index.js would cause the following to be added to your compiler
options:

    :foreign-libs [{:file \"dist/index.bundle.js\"
                    :provides [\"react\" \"react-dom\"]
                    :global-exports {react React
                                     react-dom ReactDom}}]

This will set `:npm-deps` to `false` if it hasn't been previously set.

This will set `:infer-externs` to `true` if it hasn't been
previously set.

You can learn more about ClojureScript and Webpack here:
https://clojurescript.org/guides/webpack"
  :group :common)

(s/def ::pre-build-hooks (s/coll-of (s/or :non-blank-string non-blank-string?
                                          :symbol ::schema/unquoted-symbol)))
(def-spec-meta ::pre-build-hooks
  :doc
  "A collection of symbol or strings that represent Clojure functions
to call just before your ClojureScript sources get built.

These functions will be called before **every** build.

These functions will be passed the current configuration of the
system. This is a fairly complex data-structure and contains the
`:options` for the current build among other things.

    :pre-build-hooks [user/gen-testfile]"
  :group :common)

(s/def ::post-build-hooks (s/coll-of (s/or :non-blank-string non-blank-string?
                                           :symbol ::schema/unquoted-symbol)))
(def-spec-meta ::post-build-hooks
  :doc
  "A collection of symbol or strings that represent clojure functions
to call just after your ClojureScript sources have been built.

These functions will be called after **every** build.

These functions will be passed the current configuration of the
system. This is a fairly complex data-structure and contains the
`:options` for the current build among other things.

    :post-build-hooks [user/gen-alternate-main-js]"
  :group :common)

(s/def ::extra-main-files (s/map-of keyword? map?))

(def-spec-meta ::extra-main-files
  :doc
  "A map of keyword ids to Clojurescript option maps.

`:extra-main-files` will output *extra* main files besides the one that was
configured in your ClojureScript options.

For example this will output a dev-main-test.js file for your tests:

    ::extra-main-files {:tests {:main example.tests.test-runner}}

This file will be created in addition to the `dev-main.js` file.

The options will be merged with the ClojureScript options for the
current build. Keep in mind that this merge supports keywords prefixed
with `extra-` when you want the values of these keys to be merged.  If
you supply `:extra-preloads` in the options map they will be
concatenated with the existing `:preloads`. Since
Figwheel works by injecting itself into your config with `:preloads`
and `:closure-defines` it is recommended that you always use
`:extra-preloads` and `:extra-closure-defines` if you want to change
these values.

This extra main will have all the same configured Figwheel options as
the main build. In other words, the extra main will connect to the
Figwheel REPL and get reloads just like the main build.

This feature will only output the ClojureScript bootstrap file that
you will require on your host page, it will not cause any files to be
compiled. So you will need to make sure that you have added all the
needed source directories to your `:watch-dirs` and your classpath.

This will only work under `:optimizations` level `:none`.

Figwheel provides a default host page for extra mains so that you do
not have to configure one. The default host page can be found at
`/figwheel-extra-main/[id]` where id is the id you supplied as a key
in the config you passed to the `:extra-main-files`. For example the
config above you would be able to find the `:tests` main at
`/figwheel-extra-main/tests`. Keep in mind that the `id` of the app
div on the default host page will be `app-[id]`.

If you don't want to use the default host page you will need to create
a your own host page for it. See
https://figwheel.org/docs/your_own_page for help.

This feature is perfect for adding [cljs-test-display](https://github.com/bhauman/cljs-test-display)
and [devcards](https://github.com/bhauman/devcards) to your workflow.

Also keep in mind that you can insert extra behavior with `:preloads`
and you can even change the `:target` to `:nodejs` if you want to work
on a Nodejs app in parallel with your main build.

    ::extra-main-files {:devcards {:main example.devcards}}"
  :group :common)

(s/def ::build-inputs
  (s/coll-of (s/or :keyword (s/and keyword? #{:main :watch-dirs})
                   :non-blank-string (s/and non-blank-string?
                                            (s/or
                                             :file file-exists?
                                             :directory
                                             (s/and directory-exists?
                                                    ::schema/has-cljs-source-files)))
                   :namespace ::schema/unquoted-symbol)))

(def-spec-meta ::build-inputs
  :doc
  "Build inputs are passed as the first argument to the CLJS compiler.

Build inputs are normally a list of sources (files and directories)
for the compiler to compile.

Figwheel attempts to provide build inputs to the ClojureScript
compiler based on your current configuration. The logic is roughly: if
you are using `:optimizations` level `:none` and not only building
once, use the `:watch-dirs` as the build inputs, otherwise use the
`:main` namespace as the build input.

Using the `:watch-dirs` as a build input has the advantage that
Figwheel will watch and compile all the source files in the
`:watch-dirs` even if they are not required in your application
yet. This allows Figwheel to provide compiler feedback while you are
working on files that are not in your require tree.

When you provide a `:build-inputs` in your config you will be
overriding the default Figwheel behavior and be specifing which
specific inputs you want to send to the compiler.

`:build-inputs` is a collection of:

* strings representing paths to source files and directories
* namespace symbols that are on the classpath
* the keyword `:main` which will be replaced with the namespace in your `:main` CLJS option
* the keyword `:watch-dirs` which will be replaced with your configured `:watch-dirs`


    :build-inputs [:watch-dirs example.core-tests \"extra-src\"]"
  :group :common)

(s/def :figwheel.main.schema.config.testing/namespaces (s/coll-of ::schema/unquoted-symbol))
(s/def :figwheel.main.schema.config.testing/cljs-test-display boolean?)

(s/def ::auto-testing (s/or
                       :bool boolean?
                       :map (spell/strict-keys
                             :opt-un
                             [:figwheel.main.schema.config.testing/namespaces
                              :figwheel.main.schema.config.testing/cljs-test-display])))

(def-spec-meta ::auto-testing
  :doc
  "Figwheel will automatically discover all the cljs.test based tests
that you have defined and will provide an endpoint to display them
with `cljs-test-display`. It will only provide this by default when
the tests are present in your watched directories and a build is using
`:optimizations` level `:none`.

You can find these tests at the `/figwheel-extra-main/auto-testing`
HTTP endpoint on the Figwheel server.

Figwheel will automatically find all the namespaces with tests in
them.

You can enable this feature by specifying:

    :auto-testing true

You can specify which namespaces to test:

    :auto-testing {:namespaces [example.core-tests example.logic-tests]}

You can also disable `cljs-test-display` with:

    :auto-testing {:cljs-test-display false}"
  :group :common)

(s/def ::cljsjs-resources boolean?)
(def-spec-meta ::cljsjs-resources
  :doc
  "When you use libraries from http://cljsjs.github.io they sometimes
come bundled with static resources (like CSS files) that you would
like to be served from the Figwheel server.

If you set `:cljsjs-resources` to `true` the Figwheel server will
serve the resources with the `cljsjs` root.

For example: if there is a CSS file in the `react-vis` jar at
`cljsjs/react-vis/common/react-vis.inc.css` you will be able to access
it via the Figwheel server at the path
`/react-vis/common/react-vis.inc.css`.

    :cljsjs-resources true"
  :group :un-common)

;; ------ Uncommon options ----------------------------------

(s/def ::client-print-to (s/coll-of #{:console :repl}))
(def-spec-meta ::client-print-to
  :doc
  "The `figwheel.repl` client can direct printed (via pr) output to the
REPL and/or the console. `:client-print-to` is a list of where you
want print output directed. The output choices are `:console` and `:repl`
Default: [:console :repl]

    :client-print-to [:console]"
  :group :un-common)

(s/def ::ring-stack (s/or :non-blank-string non-blank-string?
                          :symbol ::schema/unquoted-symbol))

(def-spec-meta ::ring-stack
  :doc
  "The figwheel server has a notion of a `:ring-stack`. The
`:ring-stack` is a composition of basic ring-middleware (think
sessions) to wrap around a supplied `:ring-handler`.

The default `:ring-stack` is a slightly modified
`ring.middleware.defaults/wrap-defaults`"
  :group :un-common)

(s/def ::ring-stack-options map?)
(def-spec-meta ::ring-stack-options
  :doc
  (str "The figwheel.repl server has a notion of a `:ring-stack`. The
`:ring-stack` is a composition of basic ring-middleware to wrap around
a supplied `:ring-handler`.

The default `:ring-stack` is a slightly modified
ring.middleware.defaults/wrap-defaults.

`:ring-stack-options` are the options that figwheel.repl supplies to
`ring.middleware.defaults/wrap-defaults`.

The default options are slightly modified from `ring.middleware.defaults/site-defaults`:

```
" (when-let [opt (resolve 'figwheel.server.ring/default-options)]
    (with-out-str (clojure.pprint/pprint (deref opt))))
       "```

You can override these options by suppling your own to `:ring-stack-options`

If these options are changed significantly don't be suprised if the
figwheel server stops behaving correctly :)")
  :group :un-common)

(s/def ::wait-time-ms integer?)
(def-spec-meta ::wait-time-ms
  :doc
  "The number of milliseconds to wait before issuing reloads. Set this
higher to wait longer for changes. This is the interval from when the first
file change occurs until we finally issue a reload event.

Default: 50

    :wait-time-ms 50"
  :group :un-common)

(s/def ::mode #{:build-once :repl :serve})
(def-spec-meta ::mode
  :doc
  "The `:mode` indicates the behavior that occurs after a compile.
Options: `:repl` `:serve` or `:build-once`

* `:repl` indicates that a REPL will be started
* `:serve` indicates that a server will be started
* `:build-once` indicates that a compile will not be follwed by any action

This is mainly intended for use when you are launching figwheel.main from a script.

Normally defaults to `:repl`"
  :group :un-common)

(s/def ::broadcast-reload boolean?)
(def-spec-meta ::broadcast-reload
  :doc
  "Figwheel broadcasts hot reloads to all clients that have connected
since the figwheel process has started. Set `:broadcast-reload` to
`false` if you want to only send hot-reloads to the client where the
REPL eval occurs.
Default: true

    :broadcast-reload false"

  :group :un-common)

(s/def ::broadcast boolean?)
(def-spec-meta ::broadcast
  :doc
  "In the past figwheel would broadcast REPL evaluations to all
connected clients and then print the first result received in the
REPL. Setting `:broadcast` to `true` will give you back this legacy
behavior. Default: false

    :broadcast true"
  :group :un-common)

(s/def ::repl-eval-timeout integer?)
(def-spec-meta ::repl-eval-timeout
  :doc
  "The time (in milliseconds) it takes for the REPL to timeout.
Evaluating any given expression in cljs can take some time.
The REPL is configured to throw a timeout exception as to not hang forever.

This config option will determine how long the REPL waits for the result of an eval
before throwing.

Default: 8000

    :repl-eval-timeout 10000 ;;waits for 10 seconds instead of 8"
  :group :un-common)

(s/def ::hawk-options (s/map-of #{:watcher} #{:barbary :java :polling}))

(def-spec-meta ::hawk-options
  :doc
  "If you need to watch files with polling instead of FS events. This can
be useful for certain docker environments.

    :hawk-options {:watcher :polling}"
  :group :un-common)

(s/def ::edn
  (ensure-all-registered-keys-included
   #{::edn ::bundles}
   (spell/strict-keys
    :opt-un
    [::watch-dirs
     ::css-dirs
     ::ring-handler
     ::ring-server-options
     ::rebel-readline
     ::pprint-config
     ::open-file-command
     ::figwheel-core
     ::hot-reload-cljs
     ::reload-dependents
     ::connect-url
     ::open-url
     ::open-url-wait-ms
     ::reload-clj-files
     ::log-file
     ::log-level
     ::client-log-level
     ::log-syntax-error-style
     ::load-warninged-code
     ::ansi-color-output
     ::validate-config
     ::validate-cli
     ::target-dir

     ::npm
     ::pre-build-hooks
     ::post-build-hooks
     ::extra-main-files
     ::build-inputs
     ::auto-testing
     ::launch-js
     ::cljsjs-resources

     ::helpful-classpaths

     ::launch-node
     ::inspect-node
     ::node-command
     ::cljs-devtools

     ::client-print-to
     ::ring-stack
     ::ring-stack-options
     ::wait-time-ms
     ::mode
     ::ring-server
     ::broadcast
     ::broadcast-reload
     ::repl-eval-timeout
     ::hawk-options])))
