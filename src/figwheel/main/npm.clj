(ns figwheel.main.npm
  (:require
   [clojure.java.io :as io]
   [clojure.data.json :as json]
   [clojure.string :as string]))

;; ------------------------------------------------------------
;; utils
;; ------------------------------------------------------------

(defn kebab-case
  "Converts CamelCase / camelCase to kebab-case"
  [s]
  (-> s
      (string/replace #"[a-z\d][A-Z]"
                      (fn [x]
                        (str (first x) \- (second x))))
      (string/replace #"\s+" "-")
      string/lower-case))

(defn camel-case
  "Converts kebab-case to CamelCase"
  [s]
  (string/join "" (map string/capitalize (string/split s #"-"))))

;; ------------------------------------------------------------
;; Gen index file
;; ------------------------------------------------------------
;; experimental not currently used

(defn emit-js-export [js-export]
  (if (coll? js-export)
    (let [[a _ b] js-export]
      (format "%s as %s" (camel-case (name a)) (camel-case (name b))))
    (camel-case (name js-export))))

(defn handle-js-exports [js-exports]
  (str "{ " (string/join ", " (map emit-js-export js-exports)) " }"))

(defn import-statement [[js-export module-name]]
  (let [export-content
        (if (coll? js-export)
          (handle-js-exports js-export)
          (camel-case (name js-export)))
        mod-name (name module-name)]
    (format "import %s from '%s';" export-content (name module-name))))

(defn window-statements [[js-exports _]]
  (let [export-names
        (map (comp camel-case name)
             (if (coll? js-exports)
               (map #(if (coll? %) (last %) %) js-exports)
               [js-exports]))]
    (map #(format "window.%s = %s;" % %) export-names)))

(defn index-js-from-data [data]
  (string/join
   "\n"
   (concat
    (map import-statement data)
    (mapcat window-statements data))))

;; ------------------------------------------------------------
;; foreign-libs-entry
;; ------------------------------------------------------------

(defn content->window-exports [file-content]
  (->> file-content
       string/split-lines
       (map string/trim)
       (keep #(re-find #"^(window|goog.global)(?:\.(\w+)|\[['\"]([^'\"\s]+)['\"]\])" %))
       (mapcat rest)
       (keep identity)
       distinct))

(defn exports->foreign-libs [exports]
  (let [provides (mapv kebab-case
                       exports)
        global-exports (into {} (map (juxt (comp symbol kebab-case)
                                           symbol))
                             exports)]
    {:provides provides
     :global-exports global-exports}))

(defn import-file->foreign-lib [[webpack-entity-output-path file-content]]
  (-> (exports->foreign-libs (content->window-exports file-content))
      (assoc :file webpack-entity-output-path)))

#_(import-file->foreign-lib [nil index-file])

(defn bundle->foreign-lib [[bundle-file js-index-file]]
  (import-file->foreign-lib [bundle-file (slurp (io/file js-index-file))]))

(defn check-that-files-exist [[bundle-file js-index-file]]
  (when (not (.isFile (io/file bundle-file)))
    (throw (ex-info
            (format "Webpack: bundle file '%s' doesn't exist. Make sure you have generated the bundle." bundle-file)
            {:figwheel.main/error true})))
  (when (not (.isFile (io/file js-index-file)))
    (throw (ex-info
            (format "Webpack: index file '%s' containing imports does not exist." js-index-file)
            {:figwheel.main/error true})))
  [bundle-file js-index-file])

(defn bundles->foreign-libs [bundles]
  (mapv
   (comp bundle->foreign-lib check-that-files-exist)
   bundles))

(defn config [{:keys [:figwheel.main/config] :as cfg}]
  (if-let [{:keys [bundles]} (:npm config)]
    (if (not-empty bundles)
      (let [libs (bundles->foreign-libs bundles)]
        (-> cfg
            (update-in [:options :infer-externs] #(if (nil? %) true %))
            (update-in [:options :npm-deps] #(if (nil? %) false %))
            (update-in [:options :foreign-libs]
                       (comp vec concat)
                       libs)))
      cfg)
    cfg))

#_(config {::config {:npm
                     {:bundles
                      {"target-webpack/dev/index.bundle.js" "devel/webpack/index.js"}}}})

(comment
  ;; example data

  (def index-file
    "import React from 'react';
import ReactDom from 'react-dom';
import CreateReactClass from 'create-react-class';
import SlatePlainSerializer from 'slate-plain-serializer';
import SlateReact from 'slate-react';
import {Change} from 'slate';
window.React = React;
window.ReactDom = ReactDom;
window.CreateReactClass = CreateReactClass;
window.SlateReact = SlateReact;
window.SlatePlainSerializer = SlatePlainSerializer;
window.SlateChange = Change;
window[\"ouchy-ouch\"] = Change;
window['ouchy-oucher'] = Change;
")

  (def index-data
    {:react 'react
     :react-dom 'react-dom
     [[:change :as :slate-change]] 'slate}))
