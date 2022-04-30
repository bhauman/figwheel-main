(ns figwheel.main
  #?(:clj
     (:require
      [cljs.analyzer :as ana]
      [cljs.analyzer.api :as ana-api]
      [cljs.build.api :as bapi]
      [cljs.compiler]
      [cljs.closure]
      [cljs.cli :as cli]
      [cljs.env]
      [cljs.main :as cm]
      [cljs.repl]
      [cljs.repl.figwheel]
      [cljs.util]
      [clojure.data.json :as json]
      [clojure.java.io :as io]
      [clojure.pprint :refer [pprint]]
      [clojure.string :as string]
      [clojure.edn :as edn]
      [clojure.java.shell :as sh]
      [clojure.tools.reader.edn :as redn]
      [clojure.tools.reader.reader-types :as rtypes]
      [clojure.walk :as walk]
      [figwheel.core :as fw-core]
      [figwheel.main.ansi-party :as ansip]
      [figwheel.main.logging :as log]
      [figwheel.main.util :as fw-util]
      [figwheel.main.watching :as fww]
      [figwheel.main.helper :as helper]
      [figwheel.main.npm :as npm]
      [figwheel.main.async-result :as async-result]
      [figwheel.main.react-native :as react-native]
      [figwheel.main.testing :as testing]
      [figwheel.repl :as fw-repl]
      [figwheel.main.compat.ana-api :as ana-compat]
      [figwheel.tools.exceptions :as fig-ex]
      [certifiable.main :as certifiable]
      [certifiable.log]))
  #?(:clj
     (:import
      [java.io StringReader]
      java.net.InetAddress
      java.net.URI
      java.net.URLEncoder
      java.nio.file.Paths))
  #?(:cljs
     (:require-macros [figwheel.main])))

#?(:clj
   (do

     (def ^:dynamic *base-config*)
     (def ^:dynamic *config*)

     (def default-target-dir "target")

     (defonce process-unique (subs (str (java.util.UUID/randomUUID)) 0 6))

     (defn- time-elapsed [started-at]
       (let [elapsed-us (- (System/currentTimeMillis) started-at)]
         (with-precision 2
           (str (/ (double elapsed-us) 1000) " seconds"))))

     (defn- extract-bundle-cmd-cli [opts]
       (get-in opts [:bundle-cmd (or (#{:none} (:optimizations opts :none)) :default)]))

     (defn bundle-once? [config]
       (if (not (contains? config :bundle-freq))
         (get config :bundle-once true)
         (= :once (get config :bundle-freq :once))))

     (defn bundle-always? [config]
       (if (not (contains? config :bundle-freq))
         (not (get config :bundle-once true))
         (= :always (get config :bundle-freq :once))))

     (defn bundle-smart? [config]
       (= :smart (get config :bundle-freq :once)))

     (defn bundle-once-opts [config opts]
       (if (bundle-once? config)
         (dissoc opts :bundle-cmd)
         opts))

     (def NPM-DEPS-FILE "npm_deps.js")

     (defn bundle-smart-opts [opts & [scope]]
       (if (and (bundle-smart? (::config *config*))
                (let [{:keys [output-to output-dir]} opts
                      output-to? (fw-util/file-has-changed? output-to scope)
                      npm-deps? (fw-util/file-has-changed? (io/file output-dir NPM-DEPS-FILE) scope)]
                  (and (not output-to?) (not npm-deps?))))
         (dissoc opts :bundle-cmd)
         opts))

     ;; filling in a bundle-cmd template at the last moment
     (let [npx-cmd (fw-util/npx-executable)]
       (defn- fill-in-bundle-cmd-template [opts final-output-to]
         (let [final-output-to-file (io/file final-output-to)
               file-path (try (.getParent final-output-to-file) (catch Throwable t nil))
               file-name (try (.getName final-output-to-file) (catch Throwable t nil))
               fill-in (cond-> {:output-to (str (fw-util/dot-slash (:output-to opts)))
                                :final-output-to (str (fw-util/dot-slash final-output-to))
                                :none :none
                                :default :default
                                :npx-cmd npx-cmd}
                         file-path (assoc :final-output-dir (str (fw-util/dot-slash file-path)))
                         file-name (assoc :final-output-filename file-name))]
           (if (:bundle-cmd opts)
             (update opts :bundle-cmd
                     #(walk/postwalk
                       (fn [x]
                         (if (keyword? x)
                           (if-let [replace (fill-in x)]
                             replace
                             (throw (ex-info (format "No %s available to fill :bundle-cmd template" x)
                                             {})))
                           x))
                       %))
             opts))))

     ;; taken and modified from cljs.closure/run-bundle-cmd
     (defn run-bundle-cmd* [opts]
       (when-let [cmd (extract-bundle-cmd-cli opts)]
         (let [{:keys [exit out err]}
               (try
                 (log/info (str "Bundling: " (string/join " " cmd)))
                 (apply sh/sh cmd)
                 (catch Throwable t
                   (throw
                    (ex-info (str "Bundling command failed: " (.getMessage t))
                             {::error true :cmd cmd} t))))]
           (when-not (== 0 exit)
             (throw
              (ex-info (cond-> (str "Bundling command failed")
                         (not (string/blank? out)) (str "\n" out)
                         (not (string/blank? err)) (str "\n" err))
                       {::error true :cmd cmd :exit-code exit :stdout out :stderr err}))))))

     (defn run-bundle-cmd
       ([opts]
        (run-bundle-cmd opts (:final-output-to (::config *config*))))
       ([opts final-output-to]
        (let [opts (fill-in-bundle-cmd-template opts final-output-to)]
          (run-bundle-cmd* opts))))

     (defn- wrap-with-bundling [build-fn]
       (fn [id build-inputs opts & args]
         (let [bundling? (and (= :bundle (:target opts))
                              (:bundle-cmd opts))]
           (when bundling?
             (fw-util/file-has-changed? (:output-to opts) id)
             (fw-util/file-has-changed? (io/file (:output-dir opts) NPM-DEPS-FILE) id))
           (apply build-fn id build-inputs (dissoc opts :bundle-cmd) args)
           (when bundling?
             (run-bundle-cmd (bundle-smart-opts opts id))))))

     (defn- wrap-with-build-logging [build-fn]
       (fn [id? build-inputs opts & args]
         (let [started-at (System/currentTimeMillis)
               {:keys [output-to output-dir]} opts]
           ;; print start message
           (log/info (str "Compiling build"
                          (when id? (str " " id?))
                          " to \""
                          (or output-to output-dir)
                          "\""))
           (try
             (let [warnings (volatile! [])
                   out *out*
                   warning-fn (fn [warning-type env extra]
                                (when (get cljs.analyzer/*cljs-warnings* warning-type)
                                  (let [warn {:warning-type warning-type
                                              :env env
                                              :extra extra
                                              :path ana/*cljs-file*}]
                                    (binding [*out* out]
                                      (if (<= (count @warnings) 2)
                                        (log/cljs-syntax-warning warn)
                                        (binding [log/*syntax-error-style* :concise]
                                          (log/cljs-syntax-warning warn))))
                                    (vswap! warnings conj warn))))]
               (binding [cljs.analyzer/*cljs-warning-handlers*
                         (conj (remove #{cljs.analyzer/default-warning-handler}
                                       cljs.analyzer/*cljs-warning-handlers*)
                               warning-fn)]
                 (apply build-fn build-inputs opts args)))
             (log/succeed (str "Successfully compiled build"
                               (when id? (str " " id?))
                               " to \""
                               (or output-to output-dir)
                               "\" in " (time-elapsed started-at) "."))
             (catch Throwable e
               (log/failure (str
                             "Failed to compile build" (when id? (str " " id?))
                             " in " (time-elapsed started-at) "."))
               (log/syntax-exception e)
               (throw e))))))

     (declare resolve-fn-var)

     (defn run-hooks [hooks & args]
       (when (not-empty hooks)
         (doseq [h hooks]
           (apply h args))))

     (defn- wrap-with-build-hooks [build-fn]
       (fn [& args]
         (run-hooks (::pre-build-hooks *config*) *config*)
         (apply build-fn args)
         (run-hooks (::post-build-hooks *config*) *config*)))

     (defn wrap-with-compiler-passes [build-fn]
       (fn [& args]
         (if (::passes *config*)
           (binding [ana/*passes* (into ana-compat/default-passes (::passes *config*))
                     fw-util/*compile-collector* (atom {})]
             (apply build-fn args))
           (apply build-fn args))))

     (def build-cljs
       (-> bapi/build
           wrap-with-build-logging
           wrap-with-bundling
           wrap-with-build-hooks
           wrap-with-compiler-passes))

     (def fig-core-build
       (-> figwheel.core/build
           wrap-with-build-logging
           wrap-with-bundling
           wrap-with-build-hooks
           wrap-with-compiler-passes))

;; TODO the word config is soo abused in this namespace that it's hard to
;; know what and argument is supposed to be
     (defn config->reload-config [config]
       (select-keys config [:reload-clj-files :wait-time-ms :bundle-once :bundle-freq]))

     (defn watch-build [id paths inputs opts cenv & [reload-config]]
       (when-let [inputs (not-empty (if (coll? inputs) inputs [inputs]))]
         (let [build-inputs (if (coll? inputs) (apply bapi/inputs inputs) inputs)
               ;; watch is always called after in initial build
               opts (bundle-once-opts reload-config opts)
               ;; the build-fn needs to be passed in before here?
               build-fn (if (some #{'figwheel.core} (:preloads opts))
                          #(fig-core-build id build-inputs opts cenv %)
                          (fn [files] (build-cljs id build-inputs opts cenv)))]
           (log/info "Watching paths:" (pr-str paths) "to compile build -" id)
           (log/debug "Build Inputs:" (pr-str inputs))
           (fww/add-watch!
            [::autobuild id]
            (merge
              {::watch-info (merge
                             (:extra-info reload-config)
                             {:id id
                              :paths paths
                              :inputs inputs
                              :options opts
                              :compiler-env cenv
                              :reload-config reload-config})}
              {:paths paths
               :filter (fww/suffix-filter #{"cljc" "cljs" "js" "clj"})
               :handler (fww/throttle
                         (:wait-time-ms reload-config 50)
                         (bound-fn [evts]
                           (binding [cljs.env/*compiler* cenv]
                             (let [files (mapv (comp #(.getCanonicalPath %) :file) evts)]
                               (try
                                 (when-let [clj-files
                                            (->> evts
                                                 (filter
                                                  (partial
                                                   (fww/suffix-filter
                                                    (set
                                                     (cond
                                                       (coll? (:reload-clj-files reload-config))
                                                       (mapv name (:reload-clj-files reload-config))
                                                       (false? (:reload-clj-files reload-config)) []
                                                       :else ["clj" "cljc"]))) nil))
                                                 (mapv (comp #(.getCanonicalPath %) :file))
                                                 not-empty)]
                                   (log/debug "Reloading clj files: " (pr-str (map str clj-files)))
                                   (try
                                     (figwheel.core/reload-clj-files clj-files)
                                     (catch Throwable t
                                       (if (-> t ex-data :figwheel.core/internal)
                                         (do
                                           (log/error (.getMessage t) t)
                                           (log/debug (with-out-str (clojure.pprint/pprint (Throwable->map t)))))
                                         (do
                                           (log/syntax-exception t)
                                           (figwheel.core/notify-on-exception cenv t {})))
                                  ;; skip cljs reloading in this case
                                       (throw t))))
                                 (log/debug "Detected changed cljs files: " (pr-str (map str files)))
                                 (build-fn files)
                                 (catch Throwable t
                                   (log/error t)
                                   (log/debug (with-out-str (clojure.pprint/pprint (Throwable->map t))))
                                   false))))))})))))

     (declare read-edn-file)

     (defn get-edn-file-key
       ([edn-file key] (get-edn-file-key edn-file key nil))
       ([edn-file key default]
        (try (get (read-string (slurp edn-file)) key default)
             (catch Throwable t default))))

     (def validate-config!*
       (when (try
               (require 'clojure.spec.alpha)
               (require 'expound.alpha)
               (require 'expound.ansi)
               (require 'figwheel.main.schema.config)
               (require 'figwheel.main.schema.cljs-options)
               (require 'figwheel.main.schema.cli)
               true
               (catch Throwable t false))
         (resolve 'figwheel.main.schema.core/validate-config!)))

     (defn validate-config! [spec edn fail-msg & [succ-msg]]
       (when (and validate-config!*
                  (not
                   (false?
                    (:validate-config
                     edn
                     (get-edn-file-key "figwheel-main.edn" :validate-config)))))
         (binding [expound.ansi/*enable-color* (:ansi-color-output edn true)]
           (validate-config!* spec edn fail-msg))
         (when succ-msg
           (log/succeed succ-msg))))

     (def validate-cli!*
       (when validate-config!*
         (resolve 'figwheel.main.schema.cli/validate-cli!)))

     (defn validate-cli! [cli-args & [succ-msg]]
       (when (and validate-cli!*
                  (get-edn-file-key "figwheel-main.edn" :validate-cli true))
         (binding [expound.ansi/*enable-color*
                   (get-edn-file-key "figwheel-main.edn" :ansi-color-output true)]
           (validate-cli!* cli-args "Error in command line args"))
         (when succ-msg
           (log/succeed succ-msg))))

;; ----------------------------------------------------------------------------
;; Additional cli options
;; ----------------------------------------------------------------------------

;; Help


     (def help-template
       "Usage: clojure -m figwheel.main [init-opt*] [main-opt] [arg*]

Common usage:
  clj -m figwheel.main -b dev -r
Which is equivalient to:
  clj -m figwheel.main -co dev.cljs.edn -c example.core -r

In the above example, dev.cljs.edn is a file in the current directory
that holds a build configuration which is a Map of ClojureScript
compile options. In the above command example.core is ClojureScript
namespace on your classpath that you want to compile.

A minimal dev.cljs.edn will look similar to:
{:main example.core}

The above command will start a watch process that will compile your
source files when one of them changes, it will also facilitate
communication between this watch process and your JavaScript
environment (normally a browser window) so that it can hot reload
changed code into the environment. After the initial compile, it
will then launch a browser to host your compiled ClojureScript code,
and finally a CLJS REPL will launch.

Configuration:

In the above example, besides looking for a dev.cljs.edn file,
figwheel.main will also look for a figwheel-main.edn file in the
current directory as well.

A list of all the config options can be found here:
https://github.com/bhauman/figwheel-main/blob/master/doc/figwheel-main-options.md

A list of ClojureScript compile options can be found here:
https://clojurescript.org/reference/compiler-options

You can add build specific figwheel.main configuration in the
*.cljs.edn file by adding metadata to the build config file like
so:

^{:watch-dirs [\"dev\" \"cljs-src\"]}
{:main example.core}

Command Line Options:

With no options or args, figwheel.main runs a ClojureScript REPL

%s
For --main and --repl:

  - Enters the cljs.user namespace
  - Binds *command-line-args* to a seq of strings containing command line
    args that appear after any main option
  - Runs all init options in order
  - Calls a -main function or runs a repl or script if requested

The init options may be repeated and mixed freely, but must appear before
any main option.

In the case of --compile and --build you may supply --repl or --serve
options afterwards.

Paths may be absolute or relative in the filesystem or relative to
classpath. Classpath-relative paths have prefix of @ or @/")

     (defn adjust-option-docs [commands]
       (-> commands
           (update-in [:groups :cljs.cli/main&compile :pseudos]
                      dissoc ["-re" "--repl-env"])
           (assoc-in [:init ["-d" "--output-dir"] :doc]
                     "Set the output directory to use")
           (update-in [:init ["-w" "--watch"] :doc] str
                      ". This option can be supplied multiple times.")))

     (defn help-str [repl-env]
       (format
        help-template
        (#'cljs.cli/options-str
         (adjust-option-docs
          (#'cljs.cli/merged-commands repl-env)))))

     (defn help-opt
       [repl-env _ _]
       (println (help-str repl-env)))

;; safer option reading from files which prints out syntax errors

     (defn read-edn-file [f]
       (try (redn/read
             (rtypes/source-logging-push-back-reader (io/reader f) 1 f))
            (catch Throwable t
              (log/syntax-exception t)
              (throw
               (ex-info (str "Couldn't read the file:" f)
                        {::error true} t)))))

     (defn read-edn-string [s & [fail-msg]]
       (try
         (redn/read
          (rtypes/source-logging-push-back-reader (io/reader (.getBytes s)) 1))
         (catch Throwable t
           (let [except-data (fig-ex/add-excerpt (fig-ex/parse-exception t) s)]
             (log/info (ansip/format-str (log/format-ex except-data)))
             (throw (ex-info (str (or fail-msg "Failed to read EDN string: ")
                                  (.getMessage t))
                             {::error true}
                             t))))))

     (defn read-edn-opts [str]
       (letfn [(read-rsrc [rsrc-str orig-str]
                 (if-let [rsrc (io/resource rsrc-str)]
                   (read-edn-string (slurp rsrc))
                   (cljs.cli/missing-resource orig-str)))]
         (cond
           (string/starts-with? str "@/") (read-rsrc (subs str 2) str)
           (string/starts-with? str "@") (read-rsrc (subs str 1) str)
           :else
           (let [f (io/file str)]
             (if (.isFile f)
               (read-edn-file f)
               (cljs.cli/missing-file str))))))

     (defn merge-meta [m m1] (with-meta (merge m m1) (merge (meta m) (meta m1))))

     (defn load-edn-opts [str]
       (reduce merge-meta {} (map read-edn-opts (cljs.util/split-paths str))))

     (defn fallback-id [edn]
       (let [m (meta edn)]
         (cond
           (and (:id m) (not (string/blank? (str (:id m)))))
           (:id m)
      ;;(:main edn)      (munge (str (:main edn)))
           :else
           (str "build-"
                (.getValue (doto (java.util.zip.CRC32.)
                             (.update (.getBytes (pr-str (into (sorted-map) edn))))))))))

     (defn compile-opts-opt
       [cfg copts]
       (let [copts (string/trim copts)
             edn   (if (or (string/starts-with? copts "{")
                           (string/starts-with? copts "^"))
                     (read-edn-string copts "Error reading EDN from command line flag: -co ")
                     (load-edn-opts copts))
             config  (meta edn)
             id
             (and edn
                  (if (or (string/starts-with? copts "{")
                          (string/starts-with? copts "^"))
                    (and (map? edn) (fallback-id edn))
                    (->>
                     (cljs.util/split-paths copts)
                     (filter (complement string/blank?))
                     (filter #(not (.startsWith % "@")))
                     (map io/file)
                     (map (comp first #(string/split % #"\.") #(.getName %)))
                     (string/join ""))))]
         (log/debug "Validating options passed to --compile-opts")
         (validate-config!
          :figwheel.main.schema.cljs-options/cljs-options
          edn
          (str "Configuration error in options passed to --compile-opts"))
         (cond-> cfg
           edn (update :options merge edn)
           id  (update-in [::build :id] #(if-not % id %))
           config (update-in [::build :config] merge config))))

     (defn repl-env-opts-opt
       [cfg ropts]
       (let [ropts (string/trim ropts)
             edn   (if (string/starts-with? ropts "{")
                     (read-edn-string ropts "Error reading EDN from command line flag: --repl-opts ")
                     (load-edn-opts ropts))]
         (update cfg :repl-env-options merge edn)))

     (defn figwheel-opts-opt
       [cfg ropts]
       (let [ropts (string/trim ropts)
             edn   (if (string/starts-with? ropts "{")
                     (read-edn-string ropts "Error reading EDN from command line flag: -fw-opts ")
                     (load-edn-opts ropts))]
         (validate-config!
          :figwheel.main.schema.config/edn
          edn "Error validating figwheel options EDN provided to -fwo CLI flag")
         (update cfg ::config merge edn)))

     (defn print-config-opt [cfg opt]
       (assoc-in cfg [::config :pprint-config] (not= "false" opt)))

     (defn clean-outputs-opt [cfg opt]
       (assoc-in cfg [::config :clean-outputs] (not= "false" opt)))

     (defn- watch-opt
       [cfg path]
       (when-not (.exists (io/file path))
         (if (or (string/starts-with? path "-")
                 (string/blank? path))
           (throw
            (ex-info
             (str "Missing watch path")
             {:cljs.main/error :invalid-arg}))
           (throw
            (ex-info
             (str "Watch path \"" path "\" does not exist")
             {:cljs.main/error :invalid-arg}))))
       (update-in cfg [::extra-config :watch-dirs] (fnil conj []) path))

     (defn figwheel-opt [cfg bl]
       (assoc-in cfg [::config :figwheel-core] (not= bl "false")))

     (defn get-build [bn]
       (let [fname (if (.contains bn (System/getProperty "path.separator"))
                     bn
                     (str bn ".cljs.edn"))
             build (->> (cljs.util/split-paths bn)
                        (map #(str % ".cljs.edn"))
                        (string/join (System/getProperty "path.separator"))
                        load-edn-opts)]
         (when build
           (when-not (false? (:validate-config (meta build)))
             (when (meta build)
               (log/debug "Validating metadata in build: " fname)
               (validate-config!
                :figwheel.main.schema.config/edn
                (meta build)
                (str "Configuration error in build options meta data: " fname)))
             (log/debug "Validating CLJS compile options for build:" fname)
             (validate-config!
              :figwheel.main.schema.cljs-options/cljs-options
              build
              (str "Configuration error in CLJS compile options: " fname))))
         build))

     (defn watch-dir-from-ns [main-ns]
       (let [source (fw-util/ns->location main-ns)]
         (when-let [f (:uri source)]
           (when (= "file" (.getScheme (.toURI f)))
             (let [res (fw-util/relativized-path-parts (.getPath f))
                   end-parts (fw-util/path-parts (:relative-path source))]
               (when (= end-parts (take-last (count end-parts) res))
                 (str (apply io/file (drop-last (count end-parts) res)))))))))

     (def default-main-repl-index-body
       (str
        "<p>Welcome to the Figwheel REPL page.</p>"
        "<p>This page is served when you launch <code>figwheel.main</code> without any command line arguments.</p>"
        "<p>This page is currently hosting your REPL and application evaluation environment. "
        "Validate the connection by typing <code>(js/alert&nbsp;\"Hello&nbsp;Figwheel!\")</code> in the REPL.</p>"))

     (defn get-build-with-error [bn]
       (when-not (.exists (io/file (str bn ".cljs.edn")))
         (if (or (string/starts-with? bn "-")
                 (string/blank? bn))
           (throw
            (ex-info
             (str "Missing build name")
             {:cljs.main/error :invalid-arg}))
           (throw
            (ex-info
             (str "Build " (str bn ".cljs.edn") " does not exist")
             {:cljs.main/error :invalid-arg}))))
       (get-build bn))

     (defn build-opt [cfg bn]
       (let [bns (string/split bn #":")
             id  (string/join "" bns)
             options (->> bns
                          (map get-build-with-error)
                          (reduce merge-meta))]
         (-> cfg
             (update :options merge options)
             (assoc  ::build (cond-> {:id id}
                               (meta options)
                               (assoc :config (meta options)))))))

     (defn build-once-opt [cfg bn]
       (let [cfg (build-opt cfg bn)]
         (assoc-in cfg [::config ::build-once] true)))

     (defn background-build-opt [cfg bn]
       (let [{:keys [options ::build]} (build-opt {} bn)]
         (update cfg ::background-builds
                 (fnil conj [])
                 (assoc build :options options))))

;; TODO move these down to main action section

     (declare default-compile)

     (defn build-main-opt [repl-env-fn [_ build-name & args] cfg]
  ;; serve if no other args
       (let [args (if-not (#{"-s" "-r" "--repl" "--serve"} (first args))
                    (cons "-s" args)
                    args)]
         (default-compile repl-env-fn
           (merge (build-opt cfg build-name)
                  {:args args
                   ::build-main-opt true}))))

     (defn build-once-main-opt [repl-env-fn [_ build-name & args] cfg]
       (default-compile repl-env-fn
         (merge (build-once-opt cfg build-name)
                {:args args})))

     (declare default-output-dir default-output-to)

     (defn make-temp-dir []
       (let [tempf (java.io.File/createTempFile "figwheel" "repl")]
         (.delete tempf)
         (.mkdirs tempf)
         (.deleteOnExit (io/file tempf))
         (fw-util/add-classpath! (.toURL tempf))
         tempf))

     (defn add-temp-dir [cfg]
       (let [temp-dir (make-temp-dir)
             config-with-target (assoc-in cfg [::config :target-dir] temp-dir)
             output-dir (default-output-dir config-with-target)
             output-to  (default-output-to config-with-target)]
         (-> cfg
             (assoc-in [:options :output-dir] output-dir)
             (assoc-in [:options :output-to]  output-to)
             (assoc-in [:options :asset-path]
                       (str "/cljs-out"
                            (when-let [id (-> cfg ::build :id)]
                              (str "/" id)))))))

     (defn pwd-likely-project-root-dir? []
       (or (some #(.isFile (clojure.java.io/file %))
                 ["project.clj" "deps.edn" "figwheel-main.edn"])
           (->> (seq (.listFiles (clojure.java.io/file ".")))
                (map #(.getName %))
                (some #(.endsWith % ".cljs.edn")))))

     (defn should-add-temp-dir? [cfg]
       (not (pwd-likely-project-root-dir?)))

     (defn helper-ring-app [handler html-body output-to & [force-index?]]
       (figwheel.server.ring/default-index-html
         handler
         (figwheel.server.ring/index-html (cond-> {}
                                            html-body (assoc :body html-body)
                                            output-to (assoc :output-to output-to)))
         force-index?))

     (defn repl-main-opt [repl-env-fn args cfg]
       (let [cfg (if (should-add-temp-dir? cfg)
                   (add-temp-dir cfg)
                   cfg)
             cfg (if (get-in cfg [::build :id])
                   cfg
                   (assoc-in cfg [::build :id] "figwheel-default-repl-build"))
             output-to (get-in cfg [:options :output-to]
                               (default-output-to cfg))]
         (default-compile
           repl-env-fn
           (-> cfg
               (assoc :args args)
               (update :options (fn [opt] (merge {:main 'figwheel.repl.preload} opt)))
               (assoc-in [:options :aot-cache] true)
               (assoc-in [::config
                          :ring-stack-options
                          :figwheel.server.ring/dev
                          :figwheel.server.ring/system-app-handler]
                         #(helper/middleware
                           %
                           {:header "REPL Host page"
                            :body (slurp (io/resource "public/com/bhauman/figwheel/helper/content/repl_welcome.html"))
                            :output-to output-to}))
               (assoc-in [::config :mode] :repl)))))

     (declare serve update-config)

     (defn print-conf [cfg]
       (println "---------------------- Figwheel options ----------------------")
       (pprint (::config cfg))
       (println "---------------------- Compiler options ----------------------")
       (pprint (:options cfg)))

     (defn serve-main-opt [repl-env-fn args b-cfg]
       (let [{:keys [repl-env-options repl-options options] :as cfg}
             (-> b-cfg
                 (assoc :args args)
                 update-config)
             repl-env-options
             (update-in
              repl-env-options
              [:ring-stack-options
               :figwheel.server.ring/dev
               :figwheel.server.ring/system-app-handler]
              (fn [sah]
                (if sah
                  sah
                  #(helper/serve-only-middleware % {}))))
             {:keys [pprint-config]} (::config cfg)
             repl-env (apply repl-env-fn (mapcat identity repl-env-options))]
         (log/trace "Verbose config:" (with-out-str (pprint cfg)))
         (if pprint-config
           (do
             (log/info ":pprint-config true - printing config:")
             (print-conf cfg))
           (serve {:repl-env repl-env
                   :repl-options repl-options
                   :join? true}))))

     (def figwheel-commands
       {:init {["-w" "--watch"]
               {:group :cljs.cli/compile :fn watch-opt
                :arg "path"
                :doc "Continuously build, only effective with the --compile and --build main options"}
               ["-fwo" "--fw-opts"]
               {:group :cljs.cli/compile :fn figwheel-opts-opt
                :arg "edn"
                :doc (str "Options to configure figwheel.main, can be an EDN string or "
                          "system-dependent path-separated list of EDN files / classpath resources. Options "
                          "will be merged left to right.")}
               ["-ro" "--repl-opts"]
               {:group ::main&compile :fn repl-env-opts-opt
                :arg "edn"
                :doc (str "Options to configure the repl-env, can be an EDN string or "
                          "system-dependent path-separated list of EDN files / classpath resources. Options "
                          "will be merged left to right.")}
               ["-co" "--compile-opts"]
               {:group :cljs.cli/main&compile :fn compile-opts-opt
                :arg "edn"
                :doc (str "Options to configure the build, can be an EDN string or "
                          "system-dependent path-separated list of EDN files / classpath resources. Options "
                          "will be merged left to right. Any meta data will be merged with the figwheel-options.")}
          ;; TODO uncertain about this
               ["-fw" "--figwheel"]
               {:group :cljs.cli/compile :fn figwheel-opt
                :arg "bool"
                :doc (str "Use Figwheel to auto reload and report compile info. "
                          "Only takes effect when watching is happening and the "
                          "optimizations level is :none or nil."
                          "Defaults to true.")}
               ["-bb" "--background-build"]
               {:group :cljs.cli/compile :fn background-build-opt
                :arg "str"
                :doc "The name of a build config to watch and build in the background."}
               ["-pc" "--print-config"]
               {:group :cljs.cli/main&compile :fn print-config-opt
                :doc "Instead of running the command print out the configuration built up by the command. Useful for debugging."}
               ["--clean"]
               {:group :cljs.cli/main&compile :fn clean-outputs-opt
                :doc (str "Delete the compile artifacts for this build before compiling. "
                          "Deletes :output-dir, :output-to, :final-output-to and any extra-main files."
                          "This option can be supplied by itself to clean all builds."
                          "Or you can supply a single build name to clean only that build.")}}
        :main {["-b" "--build"]
               {:fn build-main-opt
                :arg "string"
                :doc (str "Run a compile process. The supplied build name or a list of build names "
                          "(seperated by \":\") refer to "
                          "EDN files of compile options "
                          "IE. If you use \"dev\" as a build name it will indicate "
                          "that a \"dev.cljs.edn\" will be read for "
                          "compile options. "
                          "Multiple build names will merged left to right along with their metadata. "
                          "The --build option will make an "
                          "extra attempt to "
                          "initialize a figwheel live reloading workflow. "
                          "May be followed buy either --repl or --serve. "
                          "If --repl follows, "
                          "will launch a REPL (along with a server) after the compile completes. "
                          "If --serve follows, will only start a web server according to "
                          "current configuration after the compile "
                          "completes.")}
               ["-bo" "--build-once"]
               {:fn build-once-main-opt
                :arg "string"
                :doc (str "Compile for the build name one time. "
                          "Looks for build EDN files just like the --build command. "
                          "This will not inject Figwheel or REPL functionality into your build. "
                          "It will still inject devtools if you are using :optimizations :none. "
                          "If --serve follows, will start a web server according to "
                          "current configuration after the compile "
                          "completes.")}
               ["-r" "--repl"]
               {:fn repl-main-opt
                :doc "Run a REPL"}
               ["-s" "--serve"]
               {:fn serve-main-opt
                :arg "host:port"
                :doc "Run a server based on the figwheel-main configuration options."}
               ["-h" "--help" "-?"]
               {:fn help-opt
                :doc "Print this help message and exit"}}})

;; ----------------------------------------------------------------------------
;; Config
;; ----------------------------------------------------------------------------

     (defn browser-target? [target]
       (or (nil? target)
           (= :bundle target)))

     (defn default-output-dir* [target & [scope]]
       (->> (cond-> [(or target default-target-dir) "public" "cljs-out"]
              scope (conj scope))
            (apply io/file)
            (.getPath)))

     (defn config-auto-bundle [{:keys [options ::config] :as cfg}]
       ;; we only support webpack right now
       (if (:auto-bundle config)
         (cond-> cfg
           true (assoc-in [:options :target] :bundle)
           (= :webpack (:auto-bundle config))
           (update-in [:options :bundle-cmd]
                      #(merge
                        {:none [:npx-cmd "webpack" "--mode=development" "--entry" :output-to
                                "--output-path" :final-output-dir
                                "--output-filename" :final-output-filename]
                         :default [:npx-cmd "webpack" "--mode=production" "--entry" :output-to
                                   "--output-path" :final-output-dir
                                   "--output-filename" :final-output-filename]}
                        %))
           (= :parcel (:auto-bundle config))
           (update-in [:options :bundle-cmd]
                      #(merge
                        {:none [:npx-cmd "parcel" "build" :output-to
                                "--out-dir" :final-output-dir
                                "--out-file" :final-output-filename
                                "--no-minify"]
                         :default [:npx-cmd "parcel" "build" :output-to
                                   "--out-dir" :final-output-dir
                                   "--out-file" :final-output-filename]}
                        %))
           (#{:advanced :simple :whitespace} (:optimizations options))
           (assoc-in [:options :closure-defines 'cljs.core/*global*] "window"))
         cfg))

     (defmulti default-output-dir (fn [{:keys [options]}]
                                    (get options :target :browser)))

     (defmethod default-output-dir :default [{:keys [::config ::build]}]
       (default-output-dir* (:target-dir config) (:id build)))

     (defmethod default-output-dir :nodejs [{:keys [::config ::build]}]
       (let [target (:target-dir config default-target-dir)
             scope (:id build)]
         (->> (cond-> [target "node"]
                scope (conj scope))
              (apply io/file)
              (.getPath))))

     (defn default-output-to* [target & [scope]]
       (.getPath (io/file (or target default-target-dir) "public" "cljs-out"
                          (cond->> "main.js"
                            scope (str scope "-")))))

     (defn default-bundle-output-to* [target & [scope]]
       (->> (cond-> [(or target default-target-dir) "public" "cljs-out"]
              scope (conj scope)
              true (conj "main.js"))
            (apply io/file)
            (.getPath)))

     (defmulti default-output-to (fn [{:keys [options]}]
                                   (get options :target :browser)))

     (defmethod default-output-to :default [{:keys [options ::config ::build]}]
       (if-let [out-dir (:output-dir options)]
         (.getPath (io/file out-dir "main.js"))
         (default-output-to* (:target-dir config) (:id build))))

     (defmethod default-output-to :bundle [{:keys [options ::config ::build]}]
       (if-let [out-dir (:output-dir options)]
         (.getPath (io/file out-dir "main.js"))
         (default-bundle-output-to* (:target-dir config) (:id build))))

     (defmethod default-output-to :nodejs [{:keys [::build] :as cfg}]
       (let [scope (:id build)]
         (.getPath (io/file (default-output-dir cfg)
                            (cond->> "main.js"
                              scope (str scope "-"))))))

     (defn extra-config-merge [a' b']
       (merge-with (fn [a b]
                     (cond
                       (and (map? a) (map? b)) (merge a b)
                       (and (sequential? a)
                            (sequential? b))
                       (distinct (concat a b))
                       (nil? b) a
                       :else b))
                   a' b'))

     (defn resolve-fn-var [prefix handler]
       (if (or (nil? handler) (var? handler))
         handler
         (let [prefix (when prefix (str prefix ": "))]
           (when (and handler (nil? (namespace (symbol handler))))
             (throw
              (ex-info
               (format "%sThe var '%s has the wrong form it must be a namespaced symbol" prefix
                       (pr-str handler))
               {::error true :handler handler})))
           (let [handler-res (fw-util/require-resolve-handler-or-error handler)]
             (when (and handler (not handler-res))
               (throw (ex-info
                       (format "%sWas able to load namespace '%s but unable to resolve the specific var: '%s"
                               prefix
                               (namespace (symbol handler))
                               (str handler))
                       {::error true
                        :handler handler})))
             (when (map? handler-res)
               (letfn [(error [s]
                         (throw (ex-info s {::error true :handler handler})))]
                 (condp = (:stage handler-res)
                   :bad-namespaced-symbol
                   (do (log/syntax-exception (:exception handler-res))
                       (error (format "%sThere was an error while trying to resolve '%s"
                                      prefix
                                      (pr-str handler))))
                   :unable-to-resolve-handler-fn
                   (error (format "%sWas able to load namespace '%s but unable to resolve the specific var: '%s"
                                  prefix
                                  (namespace (symbol handler))
                                  (str handler)))
                   :unable-to-load-handler-namespace
                   (do
                     (log/syntax-exception (:exception handler-res))
                     (error (format "%sThere was an exception while requiring the namespace '%s while trying to load the var '%s"
                                    prefix
                                    (namespace (symbol handler))
                                    (str handler)))))))
             handler-res))))

     (defn resolve-ring-handler [ring-handler]
       (resolve-fn-var "ring-handler" ring-handler))

     (defn process-main-config [{:keys [ring-handler] :as main-config}]
       (let [handler (resolve-ring-handler ring-handler)]
         (cond-> main-config
           handler (assoc :ring-handler handler))))

     (defn process-figwheel-main-edn [main-edn]
       (when main-edn
         (when-not (false? (:validate-config main-edn))
           (log/info "Validating figwheel-main.edn")
           (validate-config!
            :figwheel.main.schema.config/edn
            main-edn "Configuration error in figwheel-main.edn"
            "figwheel-main.edn is valid \\(ツ)/"))
         (process-main-config main-edn)))

     (defn config-use-ssl [{:keys [::config] :as cfg}]
       (if-not (:use-ssl config)
         cfg
         (cond->
             (let [ssl-port (get-in config [:ring-server-options :ssl-port] figwheel.repl/default-ssl-port)
                   cfg (-> cfg
                           (assoc-in  [::config :ring-server-options :ssl-port] ssl-port)
                           (update-in [::config :ring-server-options :ssl?] (fnil identity true))
                           (update-in [::config :connect-url]
                                      (fnil identity (format "wss://[[config-hostname]]:%d/figwheel-connect" ssl-port)))
                           (update-in [::config :open-url]
                                      (fnil identity (format "https://[[server-hostname]]:%d" ssl-port))))]
               (if (or (get-in cfg [::config :ring-server-options :keystore])
                       (get-in cfg [::config :ring-server-options :truststore]))
                 cfg
                 (do (log/info "Attempting to get an SSL certificate for localhost")
                     (if-let [{:keys [server-keystore-path password]}
                              (try
                                (binding [certifiable.log/*log-fn*
                                          (fn [level & args]
                                            (when (log/levels-map level)
                                              (log/fwlog! log/*logger* level (string/join " " (map str args)) nil)))]
                                  (certifiable/create-dev-certificate-jks
                                   (merge {:print-instructions? false}
                                          (when-let [hosts (not-empty (:ssl-valid-hosts config))]
                                            (certifiable/parse-domain-ip-arguments hosts)))))
                                (catch Throwable t
                                  (log/debug (with-out-str (clojure.pprint/pprint (Throwable->map t))))
                                  (log/error (.getMessage t) t)))]
                       (cond-> cfg
                           server-keystore-path (assoc-in [::config :ring-server-options :keystore] (str server-keystore-path))
                           password (assoc-in [::config :ring-server-options :key-password] password))
                       cfg)))))))

;; use tools reader read-string for better error messages
     #_(redn/read-string)
     (defn fetch-figwheel-main-edn [cfg]
       (when (.isFile (io/file "figwheel-main.edn"))
         (read-edn-file "figwheel-main.edn")))

     (defn- config-figwheel-main-edn [cfg]
       (let [config-edn (process-figwheel-main-edn
                         (or (::start-figwheel-options cfg)
                             (fetch-figwheel-main-edn cfg)))]
         (cond-> cfg
           config-edn (update ::config #(merge config-edn %)))))

     (defn- config-merge-current-build-conf [{:keys [::extra-config ::build] :as cfg}]
       (update cfg
               ::config #(extra-config-merge
                          (merge-with (fn [a b] (if b b a)) %
                                      (process-main-config (:config build)))
                          extra-config)))

     (defn host-port-arg? [arg]
       (and arg (re-matches #"(.*):(\d*)" arg)))

     (defn update-server-host-port [config [f address-port & args]]
       (if (and (#{"-s" "--serve"} f) address-port)
         (let [[_ host port] (host-port-arg? address-port)]
           (cond-> config
             (not (string/blank? host)) (assoc-in [:ring-server-options :host] host)
             (not (string/blank? port)) (assoc-in [:ring-server-options :port] (Integer/parseInt port))))
         config))

;; targets options
     (defn- config-main-ns [{:keys [ns options] :as cfg}]
       (let [main-ns (if (and ns (not (#{"-r" "--repl" "-s" "--serve"} ns)))
                       (symbol ns)
                       (:main options))]
         (cond-> cfg
           main-ns (assoc :ns main-ns)       ;; TODO not needed?
           main-ns (assoc-in [:options :main] main-ns))))

     (defn warn-that-dir-not-on-classpath [typ dir]
       (let [[n k] (condp = typ
                     :source ["Source directory" :source-paths]
                     :target ["Target directory" :resource-paths])]
         (log/warn (ansip/format-str
                    [:yellow n " "
                     (pr-str (str dir))
                     " is not on the classpath"]))
         (log/warn "Please fix this by adding" (pr-str (str dir))
                   "to your classpath\n"
                   "I.E.\n"
                   "For Clojure CLI Tools in your deps.edn file:\n"
                   "   ensure" (pr-str (str dir))
                   "is in your :paths key\n\n"
                   (when k
                     (format
                      (str "For Leiningen in your project.clj:\n"
                           "   add it to the %s key\n")
                      (pr-str k))))))

;; takes a string or file representation of a directory
     (defn- add-classpath! [dir]
       (when-not (fw-util/dir-on-classpath? dir)
         (log/warn (ansip/format-str [:yellow
                                      (format "Attempting to dynamically add %s to classpath!"
                                              (pr-str (str dir)))]))
         (fw-util/add-classpath! (->> dir
                                      io/file
                                      .getCanonicalPath
                                      io/file
                                      .toURL))))

     (defn- config-main-source-path-on-classpath [{:keys [options] :as cfg}]
       (when-let [main (:ns cfg)]
         (when-not (fw-util/safe-ns->location main)
           (when-let [src-dir (fw-util/find-source-dir-for-cljs-ns main)]
             (when-not (fw-util/dir-on-classpath? src-dir)
               (if (get-in cfg [::config :helpful-classpaths] true)
                 (do
                   (add-classpath! src-dir)
                   (warn-that-dir-not-on-classpath :source src-dir))
                 (log/warn (ansip/format-str
                            [:yellow
                             "The source directory for the main ns "
                             (pr-str (str src-dir))
                             " is not the classpath!"])))))))
       cfg)

;; targets local config
     (defn- config-repl-serve? [{:keys [ns args] :as cfg}]
       (let [rfs      #{"-r" "--repl"}
             sfs      #{"-s" "--serve"}]
         (cond-> cfg
           (boolean (or (rfs ns) (rfs (first args))))
           (assoc-in [::config :mode] :repl)
           (boolean (or (sfs ns) (sfs (first args))))
           (->
            (assoc-in [::config :mode] :serve)
            (update ::config update-server-host-port args))
           (rfs (first args))
           (update :args rest)
           (sfs (first args))
           (update :args rest)
           (and (sfs (first args)) (host-port-arg? (second args)))
           (update :args rest))))

;; targets local config
     (defn- config-update-watch-dirs [{:keys [options ::config] :as cfg}]
  ;; remember we have to fix this for the repl-opt fn as well
  ;; so that it understands multiple watch directories
       (update-in cfg [::config :watch-dirs]
                  #(not-empty
                    (distinct
                     (let [ns-watch-dir (and
                                         (#{:repl :serve} (:mode config))
                                         (not (::build-once config))
                                         (not (:watch options))
                                         (empty? %)
                                         (:main options)
                                         (watch-dir-from-ns (:main options)))]
                       (cond-> %
                         (:watch options) (conj (:watch options))
                         ns-watch-dir (conj ns-watch-dir)))))))

     (defn- config-ensure-watch-dirs-on-classpath [{:keys [::config] :as cfg}]
       (doseq [src-dir (:watch-dirs config)]
         (when-not (fw-util/dir-on-current-classpath? src-dir)
           (log/warn (ansip/format-str
                      [:yellow
                       "The watch directory "
                       (pr-str (str src-dir))
                       " is not on the classpath! A watch directory must be "
                       "on the classpath and point to the root directory of your namespace "
                       "source tree. A general all encompassing watch directory will not work."]))
           (when (get config :helpful-classpaths true)
             (add-classpath! src-dir)
             (warn-that-dir-not-on-classpath :source src-dir)))) cfg)

;; needs local config
     (defn figwheel-mode? [{:keys [::config options]}]
       (and (:figwheel-core config true)
            (and (#{:repl :serve} (:mode config))
                 (not (::build-once config))
                 (not-empty (:watch-dirs config)))
            (= :none (:optimizations options :none))))

     (defn repl-connection? [{:keys [::config options] :as cfg}]
       (or (and (#{:repl :serve} (:mode config))
                (not (::build-once config))
                (= :none (:optimizations options :none)))
           (figwheel-mode? cfg)))

;; TODO this is a no-op right now
     (defn prep-client-config [config]
       (let [cl-config (select-keys config [])]
         cl-config))

;; targets options needs local config
     (defn- config-figwheel-mode? [{:keys [::config options] :as cfg}]
       (cond-> cfg
    ;; check for a main??
         (figwheel-mode? cfg)
         (->
          (update ::initializers (fnil conj []) #(figwheel.core/start*))
          (update-in [:options :preloads]
                     (fn [p]
                       (vec (distinct
                             (concat p '[figwheel.core figwheel.main]))))))
         (false? (:heads-up-display config))
         (update-in [:options :closure-defines] assoc 'figwheel.core/heads-up-display false)
         (true? (:load-warninged-code config))
         (update-in [:options :closure-defines] assoc 'figwheel.core/load-warninged-code true)))

     (defn- modules-output-to [{:keys [options ::config ::build] :as cfg}]
       (if (not-empty (:modules options))
         (update-in
          cfg [:options :modules]
          #(->> %
                (map (fn [[k v]]
                       (if-not (:output-to v)
                         [k (assoc v :output-to
                                   (string/replace
                                    (default-output-to cfg)
                                    "main.js" ;; brittle
                                    (str (name k) ".js")))]
                         [k v])))
                (into {})))
         cfg))

     (defn output-to-inside-output-dir? [{:keys [output-to output-dir]}]
       (= (.getCanonicalPath (.getParentFile (io/file output-to)))
          (.getCanonicalPath (io/file output-dir))))

     (defn validate-output-paths-relationship! [{:keys [options] :as cfg}]
       (if (= :bundle (:target options))
         (do (when-not (output-to-inside-output-dir? options)
               (throw (ex-info (str "[Config Error] When using the :bundle target the :output-to file needs to be inside\nthe directory specified by :output-dir"
                                    "\n :output-to "  (:output-to options)
                                    "\n :output-dir " (:output-dir options)) {::error true})))
             cfg)
         cfg))

     (defn closure-defines-has-key-value? [{:keys [closure-defines] :as options} k v]
       (= (str v)
          (str (or (get closure-defines (symbol k))
                   (get closure-defines (str k))))))

     (defn validate-bundle-advanced! [{:keys [options] :as cfg}]
       (if (and (= :bundle (:target options))
                (#{:simple :advanced} (:optimizations options)))
         (do (when-not (closure-defines-has-key-value? options
                                                       'cljs.core/*global* 'window)
               (log/warn "When using :bundle target with simple or advanced optimizations,
you should add a cljs.core/*global* key with a value of \"window\" to :closure-defines.
I.E. {:closure-defines {cljs.core/*global* \"window\" ...}}"))
             cfg)
         cfg))

     ;; targets options
     ;; TODO needs to consider case where one or the other is specified???
     (defn- config-default-dirs [{:keys [options ::config ::build] :as cfg}]
       (cond-> cfg
         (and (nil? (:output-to options)) (not (:modules options)))
         (assoc-in [:options :output-to] (default-output-to cfg))
         (:modules options)
         modules-output-to
         (nil? (:output-dir options))
         (assoc-in [:options :output-dir] (default-output-dir cfg))))

     (defn- file-extension [f]
       (apply str "" (reverse (take-while #(not= \. %) (reverse (str f))))))

     (defn- append-to-filename-before-ext [f to-append]
       (let [f (io/file f)
             fname (.getName ^java.io.File f)
             ext (file-extension fname)
             fname (subs fname 0 (- (count fname) (inc (count ext))))]
         (.getPath (io/file (.getParent ^java.io.File f) (str fname to-append "." ext)))))

     (defn- config-default-final-output-to [{:keys [options ::config] :as cfg}]
       (cond-> cfg
         (nil? (:final-output-to config))
         (assoc-in [::config :final-output-to]
                   (if (= :bundle (:target options))
                     (append-to-filename-before-ext (:output-to options) "_bundle")
                     (:output-to options)))))

     (defn figure-default-asset-path [{:keys [figwheel-options options ::config ::build] :as cfg}]
       (if (= :nodejs (:target options))
         (:output-dir options)
         (let [{:keys [output-dir]} options]
      ;; TODO could discover the resource root if there is only one
      ;; or if ONLY static file serving can probably do something with that
      ;; as well
      ;; UNTIL THEN if you have configured your static resources no default asset-path
           (when-not (contains? (:ring-stack-options figwheel-options) :static)
             (let [parts (fw-util/relativized-path-parts (or output-dir
                                                             (default-output-dir cfg)))]
               (when-let [asset-path
                          (->> parts
                               (split-with (complement #{"public"}))
                               last
                               rest
                               not-empty)]
                 (str "/" (string/join "/" asset-path))))))))

;; targets options
     (defn- config-default-asset-path [{:keys [options] :as cfg}]
       (cond-> cfg
         (nil? (:asset-path options))
         (assoc-in [:options :asset-path] (figure-default-asset-path cfg))))

;; targets options
     (defn- config-default-aot-cache-false [{:keys [options] :as cfg}]
       (cond-> cfg
         (not (contains? options :aot-cache))
         (assoc-in [:options :aot-cache] false)))

     (defn config-clean [cfg]
       (update cfg :options dissoc :watch))

     ;; find and clean all builds

     (defn clean-outputs* [& args]
       (doseq [f (keep identity (distinct args))]
         (when (.exists (io/file f))
           (log/info (str "Deleting: " (str f)))
           (fw-util/delete-file-or-directory f))))

     (declare extra-main-altered-output-filenames)

     (defn config-clean-outputs! [{:keys [options ::config ::build] :as cfg}]
       (when (:clean-outputs config)
         (let [files-for-mains
               (->> (keys (:extra-main-files config))
                    (mapcat
                     #(vals (extra-main-altered-output-filenames % options config)))
                    (keep identity))]
           (log/info (str "Cleaning compiler output for build " (:id build)))
           (apply clean-outputs*
                  (concat
                   files-for-mains
                   [(:final-output-to config)
                    (:output-to options)
                    (:output-dir options)
                    (react-native/react-native-source-dir (:output-dir options))]))))
       cfg)

     (defn clean-build-outputs! [build-id]
       (when-let [build-edn (and build-id (get-build build-id))]
         (-> {:options build-edn
              ::config (cond-> (assoc (meta build-edn) :clean-outputs true)
                         (:auto-testing (meta build-edn))
                         (update :extra-main-files assoc :auto-testing true)) }
             (merge {::build {:id build-id}})
             config-auto-bundle
             config-default-dirs
             config-default-final-output-to
             config-clean-outputs!)))

     (defn add-to-query [uri query-map]
       (let [[pre query] (string/split uri #"\?")]
         (str pre
              (when (or query (not-empty query-map))
                (str "?"
                     (string/join "&"
                                  (map (fn [[k v]]
                                         (str (name k)
                                              "="
                                              (java.net.URLEncoder/encode (str v) "UTF-8")))
                                       query-map))
                     (when (not (string/blank? query))
                       (str "&" query)))))))

     (defn config-repl-connect [{:keys [::config options ::build] :as cfg}]
       (let [connect-id (:connect-id config
                                     (cond-> {:fwprocess process-unique}
                                       (:id build) (assoc :fwbuild (:id build))))
             conn-url (-> cfg
                          fw-util/setup-connect-url
                          (add-to-query connect-id))
             conn? (repl-connection? cfg)]
         (cond-> cfg
           conn?
           (update-in [:options :closure-defines] assoc 'figwheel.repl/connect-url conn-url)
           conn?
           (update-in [:options :preloads]
                      (fn [p]
                        (vec (distinct
                              (concat p '[figwheel.repl.preload])))))
           conn?
           (update-in [:options :repl-requires] into '[[cljs.repl :refer-macros [source doc find-doc apropos dir pst]]
                                                       [cljs.pprint :refer [pprint] :refer-macros [pp]]
                                                       [figwheel.main :refer-macros [stop-builds start-builds build-once reset clean status]]
                                                       [figwheel.repl :refer-macros [conns focus]]])
           (and conn? (:client-print-to config))
           (update-in [:options :closure-defines] assoc
                      'figwheel.repl/print-output
                      (string/join "," (distinct (map name (:client-print-to config)))))
           (and conn? (:client-log-level config))
           (update-in [:options :closure-defines] assoc
                      'figwheel.repl/client-log-level
                      (name (:client-log-level config)))
           (and conn? (not-empty (:watch-dirs config)))
           (update-in [:repl-options :analyze-path] (comp vec concat) (:watch-dirs config))
           (and conn? (not-empty connect-id))
           (assoc-in [:repl-env-options :connection-filter]
                     (let [kys (keys connect-id)]
                       (fn [{:keys [query]}]
                         (if (not (:fwprocess query))
                           (= (:fwbuild query)
                              (:fwbuild connect-id))
                           (= (select-keys query kys)
                              connect-id))))))))

     (defn config-cljs-devtools [{:keys [::config options] :as cfg}]
       (if (and
            (browser-target? (:target options))
            (= :none (:optimizations options :none))
            (:cljs-devtools config true)
            (try (bapi/ns->location 'devtools.preload) (catch Throwable t false)))
         (update-in cfg
                    [:options :preloads]
                    (fn [p]
                      (vec (distinct
                            (concat p '[devtools.preload])))))
         cfg))

     (defn config-open-file-command [{:keys [::config options] :as cfg}]
       (if-let [setup (and (:open-file-command config)
                           (repl-connection? cfg)
                           (fw-util/require-resolve-var 'figwheel.main.editor/setup))]
         (-> cfg
             (update ::initializers (fnil conj []) #(setup (:open-file-command config)))
             (update-in [:options :preloads]
                        (fn [p] (vec (distinct (conj p 'figwheel.main.editor))))))
         cfg))

     (defn config-eval-back [{:keys [::config options] :as cfg}]
       (if-let [setup (and (repl-connection? cfg)
                           (fw-util/require-resolve-var 'figwheel.main.evalback/setup))]
         (-> cfg
             (update ::initializers (fnil conj []) #(setup))
             (update-in [:options :preloads]
                        (fn [p] (vec (distinct (concat p '[figwheel.main.evalback #_figwheel.main.testing]))))))
         cfg))

     (defn config-system-exit [{:keys [::config options] :as cfg}]
       (if-let [setup (and (repl-connection? cfg)
                           (fw-util/require-resolve-var 'figwheel.main.system-exit/setup))]
         (-> cfg
             (update ::initializers (fnil conj []) #(setup))
             (update-in [:options :preloads]
                        (fn [p] (vec (distinct (concat p '[figwheel.main.system-exit]))))))
         cfg))

     (defn watch-css [css-dirs & [reload-config]]
       (when-let [css-dirs (not-empty css-dirs)]
         (when-let [start-css (fw-util/require-resolve-var 'figwheel.main.css-reload/start*)]
           (start-css css-dirs reload-config))))

     (defn config-watch-css [{:keys [::config options] :as cfg}]
       (cond-> cfg
         (and (not-empty (:css-dirs config))
              (repl-connection? cfg))
         (->
          (update ::initializers (fnil conj [])
                  #(watch-css (:css-dirs config) (config->reload-config config)))
          (update-in [:options :preloads]
                     (fn [p] (vec (distinct (conj p 'figwheel.main.css-reload))))))))

     (defn get-repl-options [{:keys [options args inits repl-options] :as cfg}]
       (assoc (merge (dissoc options :main)
                     repl-options)
              :inits
              (into
               [{:type :init-forms
                 :forms (when-not (empty? args)
                          [`(set! *command-line-args* (list ~@args))])}]
               inits)))

     (defn get-repl-env-options [{:keys [repl-env-options ::config options] :as cfg}]
       (let [repl-options (get-repl-options cfg)]
         (merge
          (select-keys config
                       [:ring-server
                        :ring-server-options
                        :ring-stack
                        :ring-stack-options
                        :ring-handler
                        :cljsjs-resources
                        :launch-node
                        :inspect-node
                        :node-command
                        :broadcast
                        :open-url
                        :open-url-wait-ms
                        :launch-js
                        :repl-eval-timeout])
          repl-env-options ;; from command line
          (select-keys options [:output-dir :target])
          {:output-to (:final-output-to config)})))

     (defn config-finalize-repl-options [cfg]
       (let [repl-options (get-repl-options cfg)
             repl-env-options (get-repl-env-options cfg)]
         (assoc cfg
                :repl-options repl-options
                :repl-env-options repl-env-options)))

     (defn config-set-log-level! [{:keys [::config] :as cfg}]
       (when-let [log-level (:log-level config)]
         (log/set-level log-level))
       cfg)

     (defn config-ansi-color-output! [{:keys [::config] :as cfg}]
       (when (some? (:ansi-color-output config))
         (alter-var-root #'ansip/*use-color* (fn [_] (:ansi-color-output config))))
       cfg)

     (defn config-log-syntax-error-style! [{:keys [::config] :as cfg}]
       (when (some? (:log-syntax-error-style config))
         (alter-var-root #'log/*syntax-error-style* (fn [_] (:log-syntax-error-style config))))
       cfg)

     (defn- config-warn-resource-directory-not-on-classpath [{:keys [::config options] :as cfg}]
  ;; this could check for other directories than resources
  ;; but this is mainly to help newcomers
       (when (and (browser-target? (:target options))
                  (or (and (::build-once config)
                           (#{:serve} (:mode config)))
                      (#{:repl :serve} (:mode config)))
                  (.isFile (io/file "resources/public/index.html"))
                  (not (fw-util/dir-on-classpath? "resources")))
         (log/warn (ansip/format-str
                    [:yellow "A \"resources/public/index.html\" exists but the \"resources\" directory is not on the classpath\n"
                     "    the default server will not be able to find your index.html"])))
       cfg)

     (defn config-pre-post-hooks [{:keys [::config] :as cfg}]
       (cond-> cfg
         (not-empty (:pre-build-hooks config))
         (update ::pre-build-hooks concat
                 (doall
                  (keep (partial resolve-fn-var "pre-build-hook-fn")
                        (:pre-build-hooks config))))
         (not-empty (:post-build-hooks config))
         (update ::post-build-hooks concat
                 (doall
                  (keep (partial resolve-fn-var "post-build-hook-fn")
                        (:post-build-hooks config))))))

     (defn output-to-for-extra-main [nm {:keys [output-to output-dir]}]
       (cond
         output-to
         (append-to-filename-before-ext output-to (str "-" nm))
         output-dir
         (str (io/file output-dir (str "main-" nm ".js")))
         :else nil))

     (defn merge-extra-key-with [m extra-m f k]
       (if-let [value (get extra-m k)]
         (let [act-key (keyword (string/replace (name k) #"^extra-" ""))]
           (update m act-key f value))
         m))

     (defn merge-extra-cljs-options
       "Merges ClojureScript options that are collections if they are
  prepended with :extra."
       [opts extra-opts]
       (let [coll-keys [:extra-foreign-libs
                        :extra-externs
                        :extra-preloads
                        :extra-closure-extra-annotations]
             map-keys [:extra-modules
                       :extra-npm-deps
                       :extra-closure-defines
                       :extra-closure-warnings]]
         (as-> (merge opts extra-opts) opts
           (apply dissoc opts
                  (concat coll-keys map-keys
                          [:extra-warnings]))
           (merge-extra-key-with opts extra-opts (fn [x y]
                                                   (merge (if (boolean? x) {} x) y)) :extra-warnings)
           (reduce #(merge-extra-key-with %1 extra-opts (comp vec concat) %2) opts coll-keys)
           (reduce #(merge-extra-key-with %1 extra-opts merge %2) opts map-keys))))

     (defn extra-main-options [nm em-options options]
  ;; need to remove modules so that we get a funcitioning independent main endpoint
       (merge-extra-cljs-options
        (dissoc options :modules)
        em-options))

     (defn extra-main-altered-output-filenames [nm options config]
         {:output-to (output-to-for-extra-main (name nm) options)
          :final-output-to (append-to-filename-before-ext
                            (:final-output-to config)
                            (str "-" (name nm)))})

     (defn- compile-resource-helper [res opts]
       (let [parts (-> res
                       (string/replace #"\.cljs$" ".js")
                       (string/split #"/"))]
         (when-not (.exists (apply io/file (:output-dir opts) parts))
           (cljs.closure/-compile (io/resource res)
                                  (assoc opts
                                         :output-file
                                         (str (apply io/file parts)))))))

     (defn extra-main-fn [nm em-options options]
       ;; TODO modules??
       (let [{:keys [output-to final-output-to]}
             (extra-main-altered-output-filenames nm options (::config *config*))
             opts (-> (extra-main-options nm em-options options)
                      (assoc :output-to output-to))
             bundled-already (atom false)]
         (fn [_]
           (log/info (format "Outputting main file: %s" (:output-to opts "main.js")))
           (let [switch-to-node? (and (= :nodejs (:target em-options)) (not= :nodejs (:target options)))
            ;; fix asset-path for nodejs to output-dir if the original options are not nodejs
            ;; and if the asset path isn't set in the extra-main options
                 opts (if (and switch-to-node? (not (:asset-path em-options)))
                        (assoc opts :asset-path (:output-dir opts))
                        opts)]
             (cljs.closure/output-main-file
              (cljs.closure/add-implicit-options
               opts))
             ;; have to run the bundle command as well
             (when (and (= :bundle (:target opts))
                        (:bundle-cmd opts))
               (if-not @bundled-already
                 (do
                   (fw-util/file-has-changed? (:output-to opts) nm)
                   (fw-util/file-has-changed? (io/file (:output-dir opts) NPM-DEPS-FILE) nm)
                   (run-bundle-cmd opts final-output-to)
                   (reset! bundled-already true))
                 (when (not (bundle-once? (::config *config*)))
                   (run-bundle-cmd (bundle-smart-opts opts nm) final-output-to))))
             (when switch-to-node?
               (compile-resource-helper "cljs/nodejs.cljs" opts)
               (compile-resource-helper "cljs/nodejscli.cljs" opts)
               (spit (io/file (:output-dir opts) "cljs_deps.js")
                     (str "goog.addDependency(\"../cljs/nodejs.js\", ['cljs.nodejs'], []);\n"
                          "goog.addDependency(\"../cljs/nodejscli.js\", ['cljs.nodejscli'], ['goog.object', 'cljs.nodejs']);\n")
                     :append true)
               (cljs.closure/output-bootstrap opts))))))

     (defn config-extra-mains [{:keys [::config options] :as cfg}]
       (let [{:keys [extra-main-files]} config]
         (if (and (not-empty extra-main-files)
                  (= :none (:optimizations options :none)))
           (update cfg ::post-build-hooks
                   concat (map (fn [[k v]]
                                 (extra-main-fn k v options))
                               extra-main-files))
           cfg)))

     (defn config-cljsc-opts-json [cfg]
       (letfn [(munge-nses [opts]
                 (cond-> opts
                   (:closure-defines opts)
                   (update :closure-defines cljs.closure/normalize-closure-defines)
                   (:preloads opts)
                   (update :preloads #(map cljs.compiler/munge %))
                   (:main opts)
                   (update :main cljs.compiler/munge)))]
         (update cfg ::post-build-hooks conj
                 (fn [{:keys [::config options] :as cfg}]
                   (let [f (io/file (:output-dir options) "cljsc_opts.edn")]
                     (when (.isFile f)
                       (some->>
                        (slurp f)
                        (#(try (read-string %) (catch Throwable t nil)))
                        munge-nses
                        json/write-str
                        (spit (io/file (:output-dir options) "cljsc_opts.json")))))))))

     (defn expand-build-inputs [{:keys [watch-dirs build-inputs] :as config} {:keys [main] :as options}]
       (doall
        (distinct
         (mapcat
          (fn [x]
            (cond
              (= x :main)
              (when-let [{:keys [uri]} (and main (fw-util/ns->location (symbol main)))]
                [uri])
              (symbol? x)
              (when-let [{:keys [uri]} (fw-util/ns->location x)]
                [uri])
              (= x :watch-dirs)
              watch-dirs
              :else [x]))
          build-inputs))))

     (defn config->inputs [{:keys [watch-dirs mode build-inputs ::build-once] :as config} options]
       (if (not-empty build-inputs)
         (expand-build-inputs config options)
         (if-let [inputs (and (not build-once)
                              (= :none (:optimizations options :none))
                              (not-empty watch-dirs))]
           inputs
           (let [source (when (:main options)
                          (:uri (fw-util/ns->location (symbol (:main options)))))]
             (cond
               source [source]
               (not-empty watch-dirs) watch-dirs)))))

     (defn config-build-inputs [{:keys [options ::config] :as cfg}]
       (if-let [inputs (not-empty (config->inputs config options))]
         (update-in cfg [::config ::build-inputs] (comp vec distinct concat) inputs)
         cfg))

     (defn config-compile-is-build-once [{:keys [args] :as cfg}]
       (cond-> cfg
         (and (= (set (keys cfg)) #{:args :ns})
              (not (#{"-r" "--repl" "-s" "--serve"} (first args))))
         (assoc-in [::config ::build-once] true)))

     (defn config-run-pre-start-hooks [{:keys [::pre-start-hooks] :as cfg}]
       (doseq [init-fn pre-start-hooks]
         (init-fn cfg))
       cfg)

     (defn update-config [cfg]
       (->> cfg
            config-compile-is-build-once
            config-figwheel-main-edn
            config-merge-current-build-conf
            config-ansi-color-output!
            config-set-log-level!
            config-log-syntax-error-style!
            config-repl-serve?
            config-main-ns
            config-main-source-path-on-classpath
            config-update-watch-dirs
            config-ensure-watch-dirs-on-classpath
            config-figwheel-mode?
            config-auto-bundle
            config-use-ssl
            config-default-dirs
            config-default-final-output-to
            validate-output-paths-relationship!
            validate-bundle-advanced!

            config-default-asset-path
            react-native/plugin
            config-default-aot-cache-false
            npm/config
            testing/plugin
            config-pre-post-hooks
            config-cljsc-opts-json
            config-repl-connect
            config-cljs-devtools
            config-open-file-command
            config-system-exit
            #_config-eval-back
            config-watch-css
            config-finalize-repl-options
            config-extra-mains
            config-build-inputs
            config-clean
            config-clean-outputs!
            config-warn-resource-directory-not-on-classpath
            config-run-pre-start-hooks))

;; ----------------------------------------------------------------------------
;; Main action
;; ----------------------------------------------------------------------------

     (defn build [{:keys [watch-dirs mode ::build-once ::build-inputs] :as config}
                  options cenv]
       (let [id (:id (::build *config*) "unknown")]
         (assert (not-empty build-inputs) "Should have at least one build input!")
         (build-cljs id (apply bapi/inputs build-inputs) options cenv)
    ;; are we watching?
         (when-let [paths (and (not build-once)
                               (= :none (:optimizations options :none))
                               (not-empty watch-dirs))]
           (watch-build id paths build-inputs options cenv (config->reload-config config)))))

     (defn log-server-start [repl-env]
       (let [host (get-in repl-env [:ring-server-options :host] "localhost")
             port (get-in repl-env [:ring-server-options :port] figwheel.repl/default-port)
             scheme (if (get-in repl-env [:ring-server-options :ssl?])
                      "https" "http")]
         (log/info (str "Starting Server at " scheme "://" host ":" port))))

     (defn start-file-logger []
       (when-let [log-fname (and (bound? #'*config*) (get-in *config* [::config :log-file]))]
         (log/info "Redirecting log ouput to file:" log-fname)
         (io/make-parents log-fname)
         (log/switch-to-file-handler! log-fname)))

;; ------------------------------
;; REPL
;; ------------------------------

     (defn repl-api-docs []
       (let [dvars (filter (comp :cljs-repl-api meta) (vals (ns-publics 'figwheel.main)))]
         (string/join
          "\n"
          (map (fn [{:keys [ns name arglists doc]}]
                 (str "--------------------------------------------------------------------------------\n"
                      "(" ns "/" name
                      (when-let [args (not-empty (first arglists))]
                        (str " " (pr-str args)))
                      ")\n   " doc))
               (map meta dvars)))))

     #_(println (repl-api-docs))

     (defn bound-var? [sym]
       (when-let [v (resolve sym)]
         (thread-bound? v)))

     (defn in-nrepl? []
       (or
        (bound-var? 'nrepl.middleware.interruptible-eval/*msg*)
        (bound-var? 'clojure.tools.nrepl.middleware.interruptible-eval/*msg*)))

     (defn nrepl-repl [repl-env repl-options]
       (if-let [piggie-repl (or (and (bound-var? 'cider.piggieback/*cljs-repl-env*)
                                     (resolve 'cider.piggieback/cljs-repl))
                                (and (bound-var? 'cemerick.piggieback/*cljs-repl-env*)
                                     (resolve 'cemerick.piggieback/cljs-repl)))]
         (apply piggie-repl repl-env (mapcat identity repl-options))
         (throw (ex-info "Failed to launch Figwheel CLJS REPL: nREPL connection found but unable to load piggieback.
This is commonly caused by
 A) not providing piggieback as a dependency and/or
 B) not adding piggieback middleware into your nrepl middleware chain.
Please see the documentation for piggieback here https://github.com/clojure-emacs/piggieback#installation

Note: Cider will inject this config into your project.clj.
This can cause confusion when your are not using Cider."
                         {::error :no-cljs-nrepl-middleware}))))

     (defn repl-caught [err repl-env repl-options]
       (let [root-source-info (some-> err ex-data :root-source-info)]
         (if (and (instance? clojure.lang.IExceptionInfo err)
                  (#{:js-eval-error :js-eval-exception} (:type (ex-data err))))
           (try
             (cljs.repl/repl-caught err repl-env repl-options)
             (catch Throwable e
               (let [{:keys [value stacktrace] :as data} (ex-data err)]
                 (when value
                   (println value))
                 (when stacktrace
                   (println stacktrace))
                 (log/debug (with-out-str (pprint data))))))
           (let [except-data (fig-ex/add-excerpt (fig-ex/parse-exception err))]
        ;; TODO strange ANSI color error when printing this inside rebel-readline
             (println (binding [ansip/*use-color* (if (resolve 'rebel-readline.cljs.repl/repl*)
                                                    false
                                                    ansip/*use-color*)]
                        (ansip/format-str (log/format-ex except-data))))
             (log/debug (with-out-str (clojure.pprint/pprint (Throwable->map err))))
             (flush)))))

     (def repl-header
       (str "Figwheel Main Controls:
          (figwheel.main/stop-builds id ...)  ;; stops Figwheel autobuilder for ids
          (figwheel.main/start-builds id ...) ;; starts autobuilder focused on ids
          (figwheel.main/reset)               ;; stops, cleans, reloads config, and starts autobuilder
          (figwheel.main/build-once id ...)   ;; builds source one time
          (figwheel.main/clean id ...)        ;; deletes compiled cljs target files
          (figwheel.main/status)              ;; displays current state of system
Figwheel REPL Controls:
          (figwheel.repl/conns)               ;; displays the current connections
          (figwheel.repl/focus session-name)  ;; choose which session name to focus on
In the cljs.user ns, controls can be called without ns ie. (conns) instead of (figwheel.repl/conns)
    Docs: (doc function-name-here)
    Exit: :cljs/quit
 Results: Stored in vars *1, *2, *3, *e holds last exception object"))

     (defn repl [repl-env repl-options]
       (binding [cljs.analyzer/*cljs-warning-handlers*
                 (conj (remove #{cljs.analyzer/default-warning-handler}
                               cljs.analyzer/*cljs-warning-handlers*)
                       (fn [warning-type env extra]
                         (when (get cljs.analyzer/*cljs-warnings* warning-type)
                      ;; warnings happen during compile so we must
                      ;; output to *err* but when rebel readline is
                      ;; available we will use the the root value of
                      ;; out which is bound to a special printer this
                      ;; is a bit tricky, its best just to handle
                      ;; *err* correctly in rebel-readline
                           (binding [*out*
                                     (if (some-> (resolve 'rebel-readline.jline-api/*line-reader*)
                                                 deref)
                                       (.getRawRoot #'*out*)
                                       *err*)]
                             (->> {:warning-type warning-type
                                   :env env
                                   :extra extra
                                   :path ana/*cljs-file*}
                                  figwheel.core/warning-info
                                  (fig-ex/root-source->file-excerpt (:root-source-info env))
                                  log/format-ex
                                  ansip/format-str
                                  string/trim-newline
                                  println)
                             (flush)))))]
         (let [repl-options (-> repl-options
                                (assoc :caught (:caught repl-options repl-caught)))]
           (println (ansip/format-str
                     [:bright (format "Prompt will show when REPL connects to evaluation environment (i.e. %s)"
                                      (if (= :nodejs (:target repl-options))
                                        "Node"
                                        "a REPL hosting webpage"))]))
           (println repl-header)

           (if (in-nrepl?)
             (nrepl-repl repl-env repl-options)
             (let [repl-fn (or (when-not (false? (:rebel-readline (::config *config*)))
                                 (fw-util/require-resolve-var 'rebel-readline.cljs.repl/repl*))
                               cljs.repl/repl*)]
               (try
                 (repl-fn repl-env repl-options)
                 (catch clojure.lang.ExceptionInfo e
                   (if (-> e ex-data :type (= :rebel-readline.jline-api/bad-terminal))
                     (do (println (.getMessage e))
                         (cljs.repl/repl* repl-env repl-options))
                     (throw e)))))))))

     (defn repl-action [repl-env repl-options]
       (log-server-start repl-env)
       (log/info "Starting REPL")
  ;; when we have a logging file start log here
       (start-file-logger)
       (repl repl-env repl-options))

     (defn serve [{:keys [repl-env repl-options eval-str join?]}]
       (cljs.repl/-setup repl-env repl-options)
       (when eval-str
         (cljs.repl/evaluate-form repl-env
                                  (assoc (ana/empty-env)
                                         :ns (ana/get-namespace ana/*cljs-ns*))
                                  "<cljs repl>"
                             ;; todo allow opts to be added here
                                  (first (ana-api/forms-seq (StringReader. eval-str)))))
       (when-let [server (and join? @(:server repl-env))]
         (.join server)))

     (defn serve-action [{:keys [repl-env] :as options}]
       (log-server-start repl-env)
       (start-file-logger)
       (serve options))

     (defonce build-registry (atom {}))

     (defn register-build! [id build-info]
       (swap! build-registry assoc id (assoc build-info :id id)))

     (defn background-build [cfg {:keys [id config options]}]
       (let [{:keys [::build ::config repl-env-options repl-options] :as cfg}
             (-> (select-keys cfg [::start-figwheel-options])
                 (assoc :options options
                        ::build {:id id :config (assoc config :mode :serve)})
                 update-config)
             repl-env (figwheel.repl/repl-env*
                       (merge
                        repl-env-options
                        (select-keys cljs.repl/*repl-env* [:server])))
             cenv (cljs.env/default-compiler-env)
             repl-options (assoc repl-options :compiler-env cenv)
             build-inputs (::build-inputs config)]
         (when (empty? (:watch-dirs config))
           (log/failure (format "Can not watch build \"%s\" with no :watch-dirs" id)))
         (when (not-empty (:watch-dirs config))
           (log/info "Starting background autobuild - " (:id build))
           (binding [*config* cfg
                     cljs.env/*compiler* cenv]
             (build-cljs (:id build) (apply bapi/inputs build-inputs) (:options cfg) cenv)
             (watch-build (:id build)
                          (:watch-dirs config)
                          build-inputs
                          (:options cfg)
                          cenv
                          (config->reload-config config))
             (register-build!
              (get-in *config* [::build :id])
              {:repl-env repl-env
               :repl-options repl-options
               :config *config*
               :background true})

             (when (first (filter #{'figwheel.core} (:preloads (:options cfg))))
          ;; let's take parent repl-env and change the connection filter
               (binding [cljs.repl/*repl-env* repl-env
                         figwheel.core/*config*
                         (select-keys config [:hot-reload-cljs
                                              :broadcast-reload
                                              :reload-dependents
                                              :watch-dirs
                                              :build-inputs])]
                 (doseq [init-fn (::initializers cfg)] (init-fn))))))))

     (defn start-background-builds [{:keys [::background-builds] :as cfg}]
       (doseq [build background-builds]
         (background-build cfg build)))

     (defn validate-fix-target-classpath! [{:keys [::config ::build options]}]
       (when (and (browser-target? (:target options))
                  ;; if build-once and not :serve -> don't validate fix classpath
                  (not (and (::build-once config)
                            (not (= (:mode config) :serve)))))
         ;; browsers need the target classpath to load files
         (when-not (contains? (:ring-stack-options config) :static)
           (when-let [output-to (:output-dir options (:output-to options))]
             (when-not (.isAbsolute (io/file output-to))
               (let [parts (fw-util/path-parts output-to)
                     target-dir (first (split-with (complement #{"public"}) parts))]
                 (when (some #{"public"} parts)
                   (when-not (empty? target-dir)
                     (let [target-dir (apply io/file target-dir)]
                       (if (and (fw-util/dir-on-classpath? target-dir)
                                (not (.exists target-dir)))
                         (if (get config :helpful-classpaths true)
                           (do
                             (log/warn
                              (ansip/format-str
                               [:yellow
                                "Making target directory "
                                (pr-str (str target-dir))
                                " and re-adding it to the classpath"
                                " (this only needs to be done when the target directory doesn't exist)"]))
                             (.mkdirs target-dir)
                             (fw-util/add-classpath! (.toURL target-dir)))
                           (log/warn (ansip/format-str
                                      [:yellow
                                       "Target directory "
                                       (pr-str (str target-dir))
                                       " is on the classpath but doesn't exist. The figwheel "
                                       "server will not be able to find compiled assets."])))
                    ;; quietly fix this situation??

                         (when-not (fw-util/dir-on-classpath? target-dir)
                           (if (get config :helpful-classpaths true)
                             (do
                               (.mkdirs target-dir)
                               (add-classpath! target-dir)
                               (warn-that-dir-not-on-classpath :target target-dir))
                             (log/warn (ansip/format-str
                                        [:yellow
                                         "Target directory "
                                         (pr-str (str target-dir))
                                         " is not on the classpath! The figwheel "
                                         "server will not be able to find compiled assets."]))))))))))))))

;; build-id situations
;; - temp-dir build id doesn't matter
;; - target directory build id
;;   (if unique would ensure clean main compile and run)
;;   when don't you want it to be unique
;;   when do you not want a clean compile when running main or repl?
;;     (when you are running it over and over again)
;;     for main we can use the main-ns for a build id
;;   - repl only
;;   - main only

     (defn default-main [repl-env-fn cfg]
       (let [cfg (if (should-add-temp-dir? cfg)
                   (add-temp-dir cfg)
                   cfg)
             cfg (if (get-in cfg [::build :id])
                   cfg
                   (assoc-in cfg [::build :id] "figwheel-main-option-build"))
             final-output-to (get-in (update-config cfg) [::config :final-output-to])
             main (:main cfg)
             cfg (-> cfg
                     (assoc-in [:options :aot-cache] false)
                     (update :options #(assoc % :main
                                              (or (some-> (:main cfg) symbol)
                                                  'figwheel.repl.preload)))
                     (assoc-in [::config
                                :ring-stack-options
                                :figwheel.server.ring/dev
                                :figwheel.server.ring/system-app-handler]
                               ;; executing a function is slightly different as well
                               #(helper/middleware
                                 %
                                 {:header "Main fn exec page"
                                  :body (str
                                         (format "<blockquote class=\"action-box\">Invoked main function: <code>%s/-main</code></blockquote>" (str main))
                                         (slurp
                                          (io/resource "public/com/bhauman/figwheel/helper/content/welcome_main_exec.html")))
                                  :output-to final-output-to}))
                     (assoc-in [::config :mode] :repl))
             source (:uri (fw-util/ns->location (get-in cfg [:options :main])))]
         (let [{:keys [options repl-options repl-env-options ::config] :as b-cfg}
               (update-config cfg)
               {:keys [pprint-config]} config]
           (binding [*config* b-cfg]
             (validate-fix-target-classpath! b-cfg)
             (if pprint-config
               (do
                 (log/info ":pprint-config true - printing config:")
                 (print-conf b-cfg))
               (cljs.env/ensure
                (build-cljs (get-in b-cfg [::build :id] "figwheel-main-option-build")
                            source
                            (:options b-cfg) cljs.env/*compiler*)
                ;; all the complexity below is to handle async results from -main
                (let [stolen-repl-env (promise)
                      result-prom (promise)]
                  (async-result/listen result-prom)
                  (try
                    (with-redefs [cljs.repl/tear-down (partial deliver stolen-repl-env)]
                      (let [res (cljs.cli/default-main repl-env-fn b-cfg)
                            parsed-result (try
                                            (read-string res)
                                            (catch Throwable t
                                              ::read-error))]
                        (if (= parsed-result ::read-error)
                          (deliver result-prom res)
                          (if (and (coll? parsed-result)
                                   (= (first parsed-result) :figwheel.main.async-result/wait))
                            (let [[_ arg1 arg2] parsed-result
                                  timeout-val (or arg2 :figwheel.main.async-result/timed-out)]
                              (when (= timeout-val (deref result-prom (or arg1 5000) timeout-val))
                                (println (pr-str timeout-val))
                                (throw (ex-info "Main script timed out" {:value timeout-val}))))
                            (deliver result-prom parsed-result)))
                        (let [final-result @result-prom]
                          (if (and (map? final-result)
                                   (= (get final-result :type)
                                      :figwheel.main.async-result/exception))
                            (throw (ex-info
                                    (:value final-result "System error exit from ClojureScript")
                                    {:type :js-eval-exception
                                     :error final-result
                                     :repl-env @stolen-repl-env}))
                            (if (string? final-result)
                              (println final-result)
                              (println (pr-str final-result)))))))
                    (finally
                      (cljs.repl/tear-down @stolen-repl-env))))))))))

     (defn add-default-system-app-handler [{:keys [options ::config] :as cfg}]
       (let [final-output-to (:final-output-to config)
             extra-mains-name->output-to
             (into {}
                   (keep (fn [[nm em-options]]
                           [(name nm)
                            (append-to-filename-before-ext final-output-to (str "-" (name nm)))])
                         (:extra-main-files config)))]
         (update-in
          cfg
          [:repl-env-options
           :ring-stack-options
           :figwheel.server.ring/dev
           :figwheel.server.ring/system-app-handler]
          (fn [sah]
            (if sah
              sah
              #(-> %
                   (helper/extra-main-hosting extra-mains-name->output-to)
                   (helper/missing-index
                    (if (and (:modules options)
                             (:output-dir options))
                      {:output-to (str (io/file (:output-dir options) "cljs_base.js"))}
                      {:output-to (:final-output-to config)}))))))))

     (defn validate-basic-assumptions! [{:keys [options ::config] :as cfg}]
       (when (and (= (:mode config) :repl)
                  (not= :none (:optimizations options :none)))
         (throw (ex-info "Can only start a REPL and inject hot-reloading when the :optimizations level is set to :none" {::error true}))))

     (defn default-compile [repl-env-fn cfg]
       (let [{:keys [options repl-options repl-env-options ::config] :as b-cfg}
             (add-default-system-app-handler (update-config cfg))
             {:keys [mode pprint-config ::build-once]} config
             repl-env (apply repl-env-fn (mapcat identity repl-env-options))
             ;; prevent cljs compiler from running bundle-cmd as figwheel handles it already
             cenv (cljs.env/default-compiler-env (dissoc options :bundle-cmd))
             repl-options (assoc repl-options :compiler-env cenv)]
         (validate-basic-assumptions! b-cfg)
         (validate-fix-target-classpath! b-cfg)
         (binding [*base-config* cfg
                   *config* b-cfg]
           (cljs.env/with-compiler-env cenv
             (log/trace "Verbose config:" (with-out-str (pprint b-cfg)))
             (if pprint-config
               (do
                 (log/info ":pprint-config true - printing config:")
                 (print-conf b-cfg))
               (binding [cljs.repl/*repl-env* repl-env
                         figwheel.core/*config* (select-keys config [:hot-reload-cljs
                                                                     :broadcast-reload
                                                                     :reload-dependents
                                                                     :build-inputs
                                                                     :watch-dirs])]
                 (try
                   (let [fw-mode? (figwheel-mode? b-cfg)]
                     (try
                       (build config options cenv)
                       (catch Throwable t
                         (log/error (ansip/format-str [:red (.getMessage t)]))
                         (log/debug (with-out-str (clojure.pprint/pprint (Throwable->map t))))
                    ;; when not watching throw build errors
                         (when-not (and (not build-once)
                                        (= :none (:optimizations options :none))
                                        (not-empty (:watch-dirs config)))
                           (throw t))))
                     (log/trace "Figwheel.core config:" (pr-str figwheel.core/*config*))
                     (when-not build-once
                       (register-build!
                        (get-in *config* [::build :id])
                        {:repl-env repl-env
                         :repl-options repl-options
                         :config *config*})
                       (start-background-builds (select-keys cfg [::background-builds
                                                                  ::start-figwheel-options]))
                       (doseq [init-fn (::initializers b-cfg)] (init-fn)))
                     (cond
                       (and (= mode :repl) (not build-once))
                  ;; this forwards command line args
                       (repl-action repl-env repl-options)
                       (= mode :serve)
                  ;; we need to get the server host:port args
                       (serve-action
                        {:repl-env repl-env
                         :repl-options repl-options
                         :join? (get b-cfg ::join-server? true)})
                  ;; the final case is compiling without a repl or a server
                  ;; if there is a watcher running join it
                       (and (not build-once)
                            (fww/running?)
                            (get b-cfg ::join-server? true))
                       (fww/join)))
                   (finally
                ;; these are the blocking states that we want to clean up after
                     (when (or (get b-cfg ::join-server? true)
                               (and
                                (not (in-nrepl?))
                                (= mode :repl)))
                       (fww/stop!)
                       (remove-watch cenv :figwheel.core/watch-hook)
                       (swap! build-registry dissoc (get-in *config* [::build :id])))))))))))

;; ------------------------------------------------------------
;; REPL API
;; ------------------------------------------------------------

     (defn- start-build-arg->build-options [build]
       (let [[build-id build-options config]
             (if (map? build)
               [(:id build) (:options build)
                (:config build)]
               [build])
             build-id (name build-id)
             options  (or (and (not build-options)
                               (get-build build-id))
                          build-options
                          {})
             config  (or config (meta options))]
         (cond-> {:id build-id
                  :options options}
           config (assoc :config config))))

     (defn build-option-arg? [a]
       (or
        (string? a)
        (keyword? a)
        (symbol? a)
        (and (map? a)
             (:id a)
             (:options a))))

     (defn start*
       ([join-server? build]
        (assert (build-option-arg? build) "Figwheel Start: build argument required")
        (start* join-server? nil build))
       ([join-server? figwheel-options-or-build bbuild & builds]
        (let [[figwheel-options build & background-builds]
              (if-not (build-option-arg? figwheel-options-or-build)
                (concat [figwheel-options-or-build bbuild] builds)
                (concat [nil figwheel-options-or-build bbuild] builds))
              {:keys [id] :as build} (start-build-arg->build-options build)
              cfg
              (cond-> {:options (:options build)
                       ::join-server? (if (true? join-server?) true false)}
                figwheel-options (assoc ::start-figwheel-options figwheel-options)
                id    (assoc ::build (dissoc build :options))
                (and (not (get figwheel-options :mode))
                     (= :none (get-in build [:options :optimizations] :none)))
                (assoc-in [::config :mode] :repl)
                (not-empty background-builds)
                (assoc ::background-builds (mapv
                                            start-build-arg->build-options
                                            background-builds)))]
          (if (and id (get @build-registry id))
            (throw (ex-info (format "A build with id \"%s\" is already running." id) {}))
            (default-compile cljs.repl.figwheel/repl-env cfg)))))

     (defn ^{:deprecated "0.1.6"} start
       "Starts a Figwheel build process.

   Deprecated see the documentation for figwheel.main.api/start"
       [& args]
       (apply start* false args))

     (defn ^{:deprecated "0.1.6"} start-join
       "Deprecated see the documentation for figwheel.main.api/start-join"
       [& args]
       (apply start* true args))

;; ----------------------------------------------------------------------------
;; CLJS REPL api
;; ----------------------------------------------------------------------------

;; Unfortunately this is rather hacky way to allow some control over builds
;; this works fine for allowing folks to reset builds that are running
;; but not super helpful otherwise

;; this does not allow starting up new builds or truely dropping a
;; build it only manages stopping and starting the the watcher
;; process, reloading the config, and stopping the watcher process
;; it doesn't stop and restart the server as that would interrupt the repl
;; session

     (defn autobuilding-ids []
       (->> (:watches @fww/*watcher*)
            keys
            (filter #(-> % first (= ::autobuild)))
            (map second)
            set))

     (defn currently-available-ids [] (set (keys @build-registry)))

;; doesn't respect ::start-figwheel-options
     (defn config-for-id [id]
       (when-let [{:keys [config]} (get @build-registry id)]
         config))

     (defn clean-build [{:keys [output-to output-dir]}]
       (when (and output-to output-dir)
         (doseq [file (cons (io/file output-to)
                            (reverse (file-seq (io/file output-dir))))]
           (when (.exists file) (.delete file)))))

     (defn warn-on-bad-id [ids]
       (when-let [bad-ids (not-empty (remove (currently-available-ids) ids))]
         (doseq [bad-id bad-ids]
           (println "No available build currently has id:" bad-id))))

;; TODO this should clean ids that are not currently running as well
;; TODO should this default to cleaning all builds??
;; I think yes
     (defn clean* [ids]
       (let [ids (->> ids (map name) distinct)]
         (warn-on-bad-id ids)
         (doseq [id ids]
           (when-let [options (-> build-registry deref (get id) :config :options)]
             (println "Cleaning build id:" id)
             (clean-build options)))))

     (defmacro ^:cljs-repl-api clean
       "Takes one or more builds ids and deletes their compiled artifacts."
       [& build-ids]
       (clean* (map name build-ids))
       nil)

     (defn status* []
       (println "------- Figwheel Main Status -------")
       (println "Currently available:" (string/join ", " (currently-available-ids)))
       (if-let [ids (not-empty (autobuilding-ids))]
         (println "Currently building:" (string/join ", " ids))
         (println "No builds are currently being built.")))

     (defmacro ^:cljs-repl-api status
       "Displays the build ids of the builds are currently being watched and compiled."
       []
       (status*) nil)

     (defn stop-builds* [ids]
       (let [ids (->> ids (map name) distinct)]
         (warn-on-bad-id ids)
         (doseq [k (map #(vector ::autobuild %) ids)]
           (when (-> fww/*watcher* deref :watches (get k))
             (println "Stopped building id:" (last k))
             (fww/remove-watch! k)))))

;; TODO should this default to stopping all builds??
;; I think yes
     (defmacro ^:cljs-repl-api stop-builds
       "Takes one or more build ids and stops watching and compiling them."
       [& build-ids]
       (stop-builds*
        (or (not-empty build-ids)
            (autobuilding-ids)))
       nil)

     (defn start-build*
       "This starts a build that has previously been stopped."
       [id]
       (when-let [{:keys [repl-env repl-options] :as build-info} (get @build-registry (name id))]
         (let [{:keys [options ::config]} (:config build-info)
               {:keys [watch-dirs ::build-inputs]} config
               compiler-env (:compiler-env repl-options)]
           (println "Starting build id:" id)
      ;; XXX should this have syntax error feedback?
           (build-cljs id (apply bapi/inputs build-inputs) options compiler-env)
           (watch-build id
                        watch-dirs
                        build-inputs
                        options
                        compiler-env
                        (config->reload-config config))
      ;; this might not even need to be done
           #_(when (first (filter #{'figwheel.core} (:preloads options)))
               (binding [cljs.repl/*repl-env* repl-env]
                 (figwheel.core/start*))))))

     (defn start-builds* [ids]
       (let [ids (->> ids (map name) distinct)
             already-building (not-empty (filter (autobuilding-ids) ids))
             ids (filter (complement (autobuilding-ids)) ids)]
         (when (not-empty already-building)
           (doseq [i already-building]
             (println "Already building id: " i)))
         (doseq [id ids]
           (start-build* id))))

     (defmacro ^:cljs-repl-api start-builds
       "Takes one or more build names and starts them building."
       [& build-ids]
       (start-builds*
        (or (not-empty build-ids)
            (currently-available-ids)))
       nil)

     (defn reload-config* [id]
       (println "Reloading config for id:" id)
  ;; update the config in the registry
       (when-let [{:keys [build-info]} (get (currently-available-ids) (name id))]
         (swap! build-registry update-in [id :config]
                #(update-config
                  (-> (when (::start-figwheel-options %)
                        (select-keys % ::start-figwheel-options))
                      (build-opt id)
                      (assoc-in [::config :mode] :repl))))))

     (defn reset* [ids]
       (let [ids (->> ids (map name) distinct)
             ids (or (not-empty ids) (autobuilding-ids))]
         (clean* ids)
         (stop-builds* ids)
         (mapv reload-config* ids)
         (start-builds* ids)
         nil))

     (defmacro ^:cljs-repl-api reset
       "If no args are provided, all current builds will be cleaned and restarted.
   Otherwise, this will clean and restart the provided build ids."
       [& build-ids]
       (reset* build-ids))

;; TODO build inputs will affect this
     (defn build-once* [ids]
       (let [ids (->> ids (map name) distinct)
             bad-ids (filter (complement (currently-available-ids)) ids)
             good-ids (filter (currently-available-ids) ids)]
         (when (not-empty bad-ids)
           (doseq [i bad-ids]
             (println "Build id not found:" i)))
         (when (not-empty good-ids)
      ;; clean?
           (doseq [i good-ids]
             (let [{:keys [options ::config]} (config-for-id i)
                   input (if-let [paths (not-empty (:watch-dirs config))]
                           (apply bapi/inputs paths)
                           (when-let [source (when (:main options)
                                               (:uri (fw-util/ns->location (symbol (:main options)))))]
                             source))]
               (when input
                 (build-cljs i input options
                             (cljs.env/default-compiler-env options))))))))

     (defmacro ^:cljs-repl-api build-once
       "Forces a single compile of the provided build ids."
       [& build-ids]
       (build-once* build-ids)
       nil)

;; ----------------------------------------------------------------------------
;; Main
;; ----------------------------------------------------------------------------

     (defn fix-simple-bool-arg* [flags args]
       (let [[pre post] (split-with (complement flags) args)]
         (if (empty? post)
           pre
           (concat pre [(first post) "true"] (rest post)))))

     (defn fix-simple-bool-args [flags args]
       (reverse
        (reduce (fn [accum arg]
                  (if (and (flags (first accum))
                           (not (#{"true" "false"} arg)))
                    (-> accum
                        (conj "true")
                        (conj arg))
                    (conj accum arg)))
                (list)
                args)))

     (defn available-build-ids []
       (->> (seq (.listFiles (clojure.java.io/file ".")))
            (filter #(.endsWith (.getName %) ".cljs.edn"))
            (mapv #(string/replace (.getName %) ".cljs.edn" ""))))

     (defn clean-all-builds! []
       (doseq [build-id (available-build-ids)]
         (clean-build-outputs! build-id)))

     (defn build-cleaning! [args]
       (cond
         (= args ["--clean"])
         (do (clean-all-builds!)
             (System/exit 0))
         (and (= 3 (count args))
              (= ["--clean" "true"]
                 (take 2 args)))
         (let [build-ids (available-build-ids)
               build-id (last args)]
           (if ((set build-ids) build-id)
             (clean-build-outputs! build-id)
             (println (str "Cannot find " build-id ".cljs.edn build file to clean build.")))
           (System/exit 0))))

     (defn -main [& orig-args]
  ;; todo make this local with-redefs?
       (alter-var-root #'cli/default-commands cli/add-commands figwheel-commands)
  ;; set log level early
       (when-let [level (get-edn-file-key "figwheel-main.edn" :log-level)]
         (log/set-level level))
       (try
         (let [args       (fix-simple-bool-args #{"-pc" "--print-config" "--clean"} orig-args)
               [pre post] (split-with (complement #{"-re" "--repl-env"}) args)
               _          (when (not-empty post)
                            (throw
                             (ex-info (str "figwheel.main does not support the --repl-env option\n"
                                           "The figwheel REPL is implicitly used.\n"
                                           "Perhaps you were intending to use the --target option?")
                                      {::error true})))
               _          (build-cleaning! args)
               _          (validate-cli! (vec orig-args))
               args'      (concat ["-re" "figwheel"] args)
               args'      (if (or (empty? args)
                                  (= args ["-pc" "true"])
                                  (= args ["--print-config" "true"]))
                            (concat args' ["-r"]) args')]
           (with-redefs [cljs.cli/default-compile default-compile
                         cljs.cli/load-edn-opts load-edn-opts]
             (apply cljs.main/-main args')))
         (catch Throwable e
           (let [d (ex-data e)
                 build-once? (some (set orig-args) ["--build-once" "-bo"])]
             (cond
               (or
                (:figwheel.main.schema.core/error d)
                (:figwheel.main.schema.cli/error d)
                (:cljs.main/error d)
                (::error d))
               (binding [*out* *err*]
                 (if build-once?
                   (throw e)
                   (println (.getMessage e))))
               (and (#{:js-eval-exception :js-eval-error} (:type d))
                    (:error d))
               (let [{:keys [repl-env error form]} d]
                 (#'cljs.repl/display-error repl-env error form {})
                 (throw e))
               :else (throw e))))))))

#_(def test-args
    (concat ["-co" "{:aot-cache false :asset-path \"out\"}" "-b" "dev" "-e" "(figwheel.core/start-from-repl)"]
            (string/split "-w src -d target/public/out -o target/public/out/mainer.js -c exproj.core -r" #"\s")))

#_(handle-build-opt (concat (first (split-at-main-opt args)) ["-h"]))

#_(apply -main args)
#_(.stop @server)
