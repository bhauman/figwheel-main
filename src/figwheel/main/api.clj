(ns figwheel.main.api
  (:require
   [clojure.java.io :as io]
   [figwheel.main :as fig]
   [figwheel.main.logging :as log]
   [figwheel.main.watching :as fww]
   [figwheel.repl]))

(defn start
  "Starts a Figwheel build process.

  Has two arities:

  (start build)
  (start figwheel-config-or-build build & backgound-builds)

  You can call `start` with any number of `build` arguments. The first
  one will be the foreground build and any builds that follow will be
  background builds. When you provide more than one argument to `start`
  the first argument can optionally be a map of Figwheel Main options.

  A `build` arg can be either:
  * the name of a build like \"dev\" (described in a .cljs.edn file)
  * a map describing a build with the following form

  ```clojure
  {
       :id      \"dev\"                   ; a required string build id
       :options {:main 'hello-world.core} ; a required map of cljs compile options
       :config  {:watch-dirs [\"src\"]}   ; an options map of figwheel.main config options
  }
  ```

  If the `:options` map has Figwheel options metadata, it will be used
  unless there is a non-nil `:config` option. The presence of a non-nil
  `:config` option map will cause any metadata on the `:options` map
  to be ignored.

  The `figwheel-config-or-build` arg can be a build or a map of
  Figwheel options that will be used in place of the options found in
  a `figwheel-main.edn` file if present.

  The `background-builds` is a collection of `build` args that will be
  run in the background.

  Examples:

  ```clojure
  ; The simplest and most common case. This will start figwheel just like
  ; `clojure -m figwheel.main -b dev -r`
  (start \"dev\")

  ; With inline build config
  (start {:id \"dev\"
          :options {:main 'example.core}
          :config {:watch-dirs [\"src\"]}})

  ; With inline figwheel config
  (start {:css-dirs [\"resources/public/css\"]} \"dev\")

  ; With inline figwheel and build config:
  (start {:css-dirs [\"resources/public/css\"]}
         {:id \"dev\" :options {:main 'example.core}})
  ```

  ### REPL API Usage

  Starting a Figwheel build stores important build-info in a build
  registry. This build data will be used by the other REPL API
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
  (start {:mode :serve} \"dev\")

  (start {:id \"dev\"
          :options {:main 'example.core}
          :config {:watch-dirs [\"src\"]
                   :mode :serve}})
  ```

  The above commands will leave you free to call the `cljs-repl`,
  `repl-env` and `stop` functions without interrupting the server and
  build process.

  However once you call `start` you cannot call it again until you
  have stopped all of the running builds."
  ([build] (fig/start* false build))
  ([figwheel-options-or-build build & background-builds]
   (apply fig/start* false figwheel-options-or-build build background-builds)))

(defn start-join
  "Takes the same arguments as `start`.

  Starts figwheel and blocks. Useful when you want Figwheel to block
  on the server it starts when using `:mode :serve`. You would
  normally use this in a script that would otherwise exit
  prematurely."
  [& args]
  (apply fig/start* true args))

(defn stop
  "Takes a `build-id` and stops the given build from running. This
  will not work if you have not started the build with `start`."
  [build-id]
  ;; This is all ad-hoc need to move to a notion of starting and stopping
  (if-let [build-info (get @fig/build-registry build-id)]
    (let [{:keys [repl-options] {:keys [server] :as repl-env} :repl-env} build-info]

      (log/info (format "Stopping the watcher for build - %s" build-id))
      (fww/remove-watch! [::autobuild build-id])

      (when-let [compiler-env (:compiler-env repl-options)]
        (log/info "Removing Figwheel Core watch hook")
        (remove-watch compiler-env :figwheel-core/watch-hook))

      (swap! fig/build-registry dissoc build-id)

      (when (->> @fig/build-registry
                 (vals)
                 (filter (comp #{server} :server :repl-env))
                 (empty?))
        (log/info "Doing final clean up")
        (log/info (format "Stopping the Figwheel server" build-id))
        (figwheel.repl/tear-down-server repl-env)
        (log/info "Remove all repl listeners")
        (figwheel.repl/clear-listeners)
        (log/info "Remove all watchers")
        (fww/reset-watch!))
      true)
    (throw (ex-info (format "Build \"%s\" isn't registered. Did you start it?" build-id) {}))))

(defn stop-all "Stops all of the running builds." []
  (doseq [build-id (keys @fig/build-registry)]
    (stop build-id)))

(defn repl-env
  "Once you have already started a build in the background with a call
  to `start`, you can supply the `build-id` of the running build to
  this function to fetch the repl-env for the running build. This is
  helpful in environments like **vim-fireplace** that need the repl-env.

  Example:

  ```clojure
  (figwheel.main.api/repl-env \"dev\")
  ```

  The repl-env returned by this function will not open urls when you
  start a ClojureScript REPL with it. If you want to change that
  behavior:

  ```clojure
  (dissoc (figwheel.main.api/repl-env \"dev\") :open-url-fn)
  ```

  The REPL started with the above repl-env will be inferior to the
  REPL that is started by either `figwheel.main.api/start` and
  `figwheel.main.api/cljs-repl` as these will listen for and print out
  well formatted compiler warnings."
  [build-id]
  (when-let [repl-env (get-in @fig/build-registry [build-id :repl-env])]
    (assoc repl-env
           :prevent-server-tear-down true
           :open-url-fn
           (fn [open-url]
             (when open-url
               (println (str "Open URL " open-url)))))))

(defn cljs-repl
  "Once you have already started Figwheel in the background with a
  call to `figwheel.main.api/start`, you can supply a build name of a
  running build to this function to start a ClojureScript REPL for the
  running build.

  Example:

  ```clojure
  (figwheel.main.api/cljs-repl \"dev\")
  ```"
  [build-id]
  (if-let [{:keys [repl-options config]} (get @fig/build-registry build-id)]
    (binding [fig/*config* config]
      (fig/repl (repl-env build-id) repl-options))
    (throw (ex-info (format "Build %s isn't registered. Did you start it?" build-id) {}))))

(defn read-build
  "A helper function that takes one or more build-ids and merges them into a single build
  ready to be passed to `figwheel.main.api/start`

  Example:

  ```clojure
  (figwheel.main.api/start (read-build \"dev\" \"admin\") \"tests\")
  ```"
  [build-id & build-ids]
  (let [b (->> (cons build-id build-ids)
               (map name)
               (map #(io/file (str % ".cljs.edn")))
               (map #(do (assert (.isFile %) "should be a file.")
                         %))
               (map (comp read-string slurp))
               (reduce figwheel.main/merge-meta {}))]
    {:id (apply str (map name build-ids))
     :options b
     :config (meta b)}))
