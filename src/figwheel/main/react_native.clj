(ns figwheel.main.react-native
  (:require
   [cljs.compiler]
   [clojure.java.io :as io]
   [figwheel.main.util :as fw-util]
   [figwheel.main.logging :as log]
   [figwheel.main.krell.passes :as krell-passes]
   [figwheel.repl])
  (:import [java.net URI]))

;; ----------------------------------------------------------------------
;; Code generation
;; ----------------------------------------------------------------------

(defn figbridge-setup-code [{:keys [npm-deps-path assets-path npm-requires-path options-url figwheel-bridge-path]}]
  (format "import {npmDeps} from \"%s\";
import {assets} from \"%s\";
import {krellNpmDeps} from \"%s\";
var options = {optionsUrl: \"%s\"};
var figBridge = require(\"%s\");
figBridge.shimRequire({...assets, ...krellNpmDeps, ...npmDeps});"
          npm-deps-path
          assets-path
          npm-requires-path
          options-url
          figwheel-bridge-path))

(defmulti indexjs (fn [{:keys [react-native-tool prod?]}]
                    [react-native-tool prod?]))

(defmethod indexjs [:cli false] [opts]
  (str
   (figbridge-setup-code opts)
   "\nimport {AppRegistry} from 'react-native';
import {name as appName} from './app.json';
AppRegistry.registerComponent(appName,
                              () => figBridge.createBridgeComponent(options));"))

(defmethod indexjs [:cli true] [{:keys [output-to]}]
  (format "import {AppRegistry} from 'react-native';
import {name as appName} from './app.json';
import {renderFn} from './%s';  // output-to
AppRegistry.registerComponent(appName, () => renderFn);"
          output-to))

(defmethod indexjs [:expo false] [opts]
  (str (figbridge-setup-code opts)
       "\nimport { registerRootComponent } from 'expo';
registerRootComponent(figBridge.createBridgeComponent(options));"))

(defmethod indexjs [:expo true] [{:keys [output-to]}]
  (format "import { registerRootComponent } from 'expo';
import {renderFn} from './%s';  // output-to
registerRootComponent(renderFn);"
          output-to))

(defn prod-module-js [main-ns]
  (format "module.exports = { renderFn: %s.figwheel_rn_root };"
          (cljs.compiler/munge (str main-ns))))

;; TODO get expo working

(defn react-native-source-dir [output-dir]
  (str output-dir "_rn"))

(defn tool-name [{:keys [react-native] :as config}]
  (if (true? react-native)
    :cli
    react-native))

(defn setup-react-native [{:keys [options :figwheel.main/config] :as cfg}]
  (assert (not (:auto-bundle config))
          "Should not have :auto-bundle set to true for a React Native build")
  (let [prod? (not= :none (:optimizations options :none))
        output-dir (:output-dir options)
        prod-output-to (str (io/file output-dir "main.js"))
        react-native-src-dir (react-native-source-dir output-dir)
        connect-url (try
                      (URI. (fw-util/setup-connect-url cfg))
                      (catch Throwable t
                        (throw
                         (ex-info ":connect-url doesn't work for React Native"
                                  {:connect-url (:connect-url config)}
                                  t))))
        opts-url-scheme (if (#{"https" "wss"} (.getScheme connect-url))
                          "https"
                          "http")
        opts-url (format "%s://%s:%s%s/cljsc_opts.json"
                         opts-url-scheme
                         (.getHost connect-url)
                         (.getPort connect-url)
                         (:asset-path options))]
    ;; ensure output-dir-rn directory exists
    (io/make-parents (io/file react-native-src-dir "dummy"))
    (when (not prod?)
      ;; copy react-native-figwheel-bridge to react-native-source-dir
      (io/copy (io/input-stream
                (io/resource
                 "com/bhauman/figwheel/react-native-figwheel-bridge/figwheel-bridge.js"))
               (io/file react-native-src-dir "figwheel-bridge.js"))
      (io/copy (io/input-stream
                (io/resource
                 "com/bhauman/figwheel/react-native-figwheel-bridge/clojurescript-bootstrap.js"))
               (io/file react-native-src-dir "clojurescript-bootstrap.js")))

    ;; create an index.js
    (spit (io/file "index.js")
          (indexjs
           {:react-native-tool (tool-name config)
            :prod? prod?
            :output-to prod-output-to
            :options-url opts-url
            :assets-path (str "./" (io/file (:output-dir options) "krell_assets.js"))
            :npm-requires-path (str "./" (io/file (:output-dir options) "krell_npm_deps.js"))
            :npm-deps-path (str "./" (io/file react-native-src-dir "npm_deps.js"))
            :figwheel-bridge-path (str "./" (io/file react-native-src-dir "figwheel-bridge.js"))}))
    (let [post-build-hook
          (fn [{:keys [:figwheel.main/config :figwheel.main/build options] :as cfg}]
            (krell-passes/post-build-hook cfg)
            (let [f (io/file (:output-dir options) "npm_deps.js")
                  scope (:id build)]
              (when (fw-util/file-has-changed? f scope)
                (log/info "Copying npm_deps.js")
                (io/copy f (io/file react-native-src-dir "npm_deps.js")))))]
      (cond->
          (-> cfg
              (assoc-in [:options :target] :bundle)
              (assoc-in [:figwheel.main/config :open-url] false)
              (assoc-in [:figwheel.main/config :cljs-devtools] false)
              (update :figwheel.main/passes (fnil into []) krell-passes/custom-passes))
        (not prod?) (update :figwheel.main/post-build-hooks conj post-build-hook)
        prod? (->
               (assoc-in [:options :output-to] prod-output-to)
               (assoc-in [:options :output-wrapper]
                         (fn [source]
                           (str source
                                (prod-module-js (:main options))))))))))

(defn plugin [{:keys [:figwheel.main/config] :as cfg}]
  (if-not (:react-native config)
    cfg
    (setup-react-native cfg)))
