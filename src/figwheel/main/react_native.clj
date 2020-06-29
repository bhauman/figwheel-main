(ns figwheel.main.react-native
  (:require [clojure.java.io :as io]
            [figwheel.main.util :as fw-util]
            [figwheel.main.logging :as log]
            [figwheel.repl]))

(defn cli-indexjs [{:keys [npm-deps-path options-url figwheel-bridge-path]}]
  (format "import {AppRegistry} from 'react-native';
import {name as appName} from './app.json';
import {npmDeps} from \"%s\";

var options = {optionsUrl: \"%s\"};

var figBridge = require(\"%s\");
figBridge.shimRequire(npmDeps);
AppRegistry.registerComponent(appName,
                              () => figBridge.createBridgeComponent(options));"
          npm-deps-path
          options-url
          figwheel-bridge-path))

(defn expo-indexjs [{:keys [npm-deps-path options-url figwheel-bridge-path]}]
  (format "import { registerRootComponent } from 'expo';
import {npmDeps} from '%s';

var options = {optionsUrl: '%s'};

var figBridge = require('%s');
figBridge.shimRequire(npmDeps);
registerRootComponent(figBridge.createBridgeComponent(options));"
          npm-deps-path
          options-url
          figwheel-bridge-path))

;; need to assert some basic things about the config here
;; DONE :auto-bundle should not be true as its only for web targets
;; DONE create output-dir-rn
;; DONE add in react-native-figwheel-bridge javascript to output-dir-rn
;; TODO add post-build-hook that copies npm_deps.js to
;;      output-dir-rn on change
;; DONE output index.js once
;; TODO create image compiler pass based on krell
;; TODO output assets-images.js to output-dir and output-dir.rn (only if changed)
;; TODO look at how to handle non dev builds for production

(defn react-native-source-dir [output-dir]
  (str output-dir "_rn"))

(defn setup-react-native [{:keys [options :figwheel.main/config] :as cfg}]
  (assert (not (:auto-bundle config))
          "Should not have :auto-bundle set to true for a React Native build")
  (let [output-dir (:output-dir options)
        react-native-src-dir (react-native-source-dir output-dir)
        opts-url (let [port (get-in config [:ring-server-options :port] figwheel.repl/default-port)
                       host (get-in config [:ring-server-options :host] "localhost")]
                      (format "http://%s:%s%s/cljsc_opts.json" host port (:asset-path options))
                      )]
    ;; ensure output-dir-rn directory exists
    (io/make-parents (io/file react-native-src-dir "dummy"))
    ;; copy react-native-figwheel-bridge to react-native-source-dir
    (io/copy (io/input-stream
              (io/resource
              "com/bhauman/figwheel/react-native-figwheel-bridge/figwheel-bridge.js"))
             (io/file react-native-src-dir "figwheel-bridge.js"))
    (io/copy (io/input-stream
              (io/resource
               "com/bhauman/figwheel/react-native-figwheel-bridge/clojurescript-bootstrap.js"))
             (io/file react-native-src-dir "clojurescript-bootstrap.js"))

    ;; create an index.js
    (spit (io/file "index.js")
          (cli-indexjs {:options-url opts-url
                        ;; TODO use figwheel connect url here as its a url we know
                        #_(format "http://localhost:8081/%s/cljsc_opts.json" output-dir)
                        :npm-deps-path
                        (str "./" (io/file react-native-src-dir "npm_deps.js"))
                        :figwheel-bridge-path
                        (str "./" (io/file react-native-src-dir "figwheel-bridge.js"))}))
    (let [post-build-hook
          (fn [{:keys [:figwheel.main/config :figwheel.main/build options] :as cfg}]
            (let [f (io/file (:output-dir options) "npm_deps.js")
                  scope (:id build)]
              (when (fw-util/file-has-changed? f scope)
                (log/info "Copying npm_deps.js")
                (io/copy f (io/file react-native-src-dir "npm_deps.js")))))
          cfg (-> cfg
                  (update :figwheel.main/post-build-hooks conj post-build-hook)
                  (assoc-in [:options :target] :bundle)
                  (assoc-in [:figwheel.main/config :open-url] false)
                  (assoc-in [:figwheel.main/config :cljs-devtools] false))]
      cfg)))

(defn plugin [{:keys [:figwheel.main/config] :as cfg}]
  (if-not (:react-native config)
    cfg
    (setup-react-native cfg)))
