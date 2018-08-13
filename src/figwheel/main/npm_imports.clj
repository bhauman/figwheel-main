(ns figwheel.main.npm-imports
  (:require
   [figwheel.main.logging :as log]
   [clojure.java.shell :as shell]
   [clojure.java.io :as io]
   [clojure.data.json :as json]
   [clojure.string :as string]))

(def ^:dynamic *features* #{:yarn-init :yarn-add})

(defn package-json []
  (let [pjson (io/file "package.json")]
    (when (.isFile pjson)
      (json/read-json (io/reader pjson) false))))

(defn package-json-dependencies []
  (get (package-json) "dependencies"))

(defn yarn-invoke [cmd]
  (let [{:keys [out err exit]} (shell/sh "sh" "-c" (str "yarn " cmd))]
    (binding [*out* *err*]
      (print err))
    (print out)
    (= exit 0)))

(defn yarn-init []
  (when (and (*features* :yarn-init) (not (.exists (io/file "package.json"))))
    (yarn-invoke "init -y")))

(defn in-package-json? [packages]
  (every? (fn [x]
            (or (get (package-json-dependencies) x)
                ))
          packages))

(defn yarn-add [packages]
  (let [packages (map name packages)]
    (when (*features* :yarn-add) 
      (when (and (not-empty packages) (not (in-package-json? packages)))
        (yarn-invoke (str "add " (string/join " " (map name packages))))))))

#_(yarn-init)
#_(yarn-add ["react" "react-dom" "webpack" "webpack-cli" "lodash"])

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
window.SlateChange = Change;")

(def index-data
  {:react 'react
   :react-dom 'react-dom
   [[:change :as :slate-change]] 'slate})

#_(shell/sh "yarn" "init")

;; stolen
(defn kebab-case
  "Converts CamelCase / camelCase to kebab-case"
  [s]
  (string/join "-" (map string/lower-case (re-seq #"\w[a-z]+" s))))

(defn camel-case
  "Converts kebab-case to CamelCase"
  [s]
  (string/join "" (map string/capitalize (string/split s #"-"))))

(defn handle-js-exports [js-exports]
  )

#_(defn import-statement [[js-export module-name]]
  (let [[js-export-name window-name]
        (if (seq js-export-name)
          (filter #{:as} js-export-name)
          [js-export js-export])
        mod-name (name module-name)]
    (if (seq js-export)
      (format "import " )
      )
    

    )

  )

#_(defn import-data->import-file-js [data]
  

  )


(defn content->window-exports [file-content]
  (->> file-content
       string/split-lines
       (map string/trim)
       (keep #(re-find #"^window\.(\w+)" %))
       (map second)
       distinct))

(defn exports->foreign-libs [exports]
  (let [provides (map kebab-case exports)
        global-exports (into {} (map (juxt kebab-case symbol))
                             exports)]
    {:provides provides
     :global-exports global-exports}))

(defn import-file->foreign-libs [{:keys [webpack-entity-output-path file-content]}]
  (-> (exports->foreign-libs (content->window-exports file-content))
      (assoc :file webpack-entity-output-path)))

#_(import-file->foreign-libs {:file-content index-file})

#_:npm-imports
#_{:index "path/to/index.js"
   :other {:react "react@16.4.2" 
           :react-dom "react-dom@16.4.2"
           [[:change :as :slate-change]] 'slate}}

