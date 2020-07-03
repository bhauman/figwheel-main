(ns ^{:doc "Backwards compatibility for earlier versions of CLJS"}
    figwheel.main.krell.ana-api
  (:require [cljs.analyzer :as ana]
            [cljs.env :as env]
            [cljs.util :as util]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn current-state
  "Return the current compiler state atom."
  []
  env/*compiler*)

(defn current-file
  "Return the current file under analysis or compilation."
  []
  ana/*cljs-file*)

(defn current-ns
  "Return the current ns under analysis or compilation."
  []
  ana/*cljs-ns*)

 (defn read-analysis-cache
   "Read an analysis cache."
   [cache-file]
   (when (.exists (io/file cache-file))
     (case (util/ext cache-file)
       "edn" (edn/read-string (slurp cache-file))
       "json" (let [{:keys [reader read]} @ana/transit]
                (with-open [is (io/input-stream cache-file)]
                  (read (reader is :json ana/transit-read-opts)))))))

(def
  ^{:doc "ClojureScript's default analysis passes."}
  default-passes [ana/infer-type ana/check-invoke-arg-types ana/ns-side-effects])
