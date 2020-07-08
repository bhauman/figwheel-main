;; ------------------------------------------------------------------------
;; Code modified from Krell https://github.com/vouch-opensource/krell
;; ------------------------------------------------------------------------

(ns figwheel.main.react-native.krell-passes
  (:require [cljs.analyzer :as ana]
            [figwheel.main.compat.ana-api :as ana-api]
            [figwheel.main.util :as fw-util]
            [cljs.build.api :as build-api]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [java.io File]
           [java.nio.file Path]))

;; util

(defn relativize ^File [^File source ^File target]
  (.toFile (.relativize (.toPath source) (.toPath target))))

(defn file-ext [f]
  (let [path (if (instance? File f) (.getPath ^File f) f)]
    (let [idx (.lastIndexOf path ".")]
      (when (pos? idx) (subs path idx)))))

(defn mkdirs
  "Create all parent directories for the passed file."
  [^File f]
  (.mkdirs (.getParentFile (.getCanonicalFile f))))

(defn ns->cache-file [ns {:keys [output-dir] :as opts}]
  (let [f (build-api/target-file-for-cljs-ns ns output-dir)]
    (io/file (str (string/replace (.getPath f) #".js$" "") ".cljs.cache.json"))))

;; assets

(defn js? [f]
  (#{".js"} (file-ext f)))

(defn asset-require [path]
  (str "\"" path "\": require('" path "')" ))

(defn assets-js [assets]
  (str
    "module.exports = {\n"
    "  assets: {\n"
    (string/join ",\n" (map (comp #(str "    " %) asset-require) assets))
    "  }\n"
    "};\n"))

;; passes

(defn normalize [s]
  (cond-> s (string/starts-with? s "./") (subs 2)))

(defn asset? [s]
  (and (not (nil? (file-ext s)))
       (not (js? s))))

(defn lib? [s]
  (not (asset? s)))

(defn js-require? [ast]
  (and (= :invoke (:op ast))
       (= 'js/require (-> ast :fn :name))
       (= :const (-> ast :args first :op))))

(defn js-require-asset? [ast]
  (and (js-require? ast)
       (asset? (-> ast :args first :val))))

(defn update-require-path [ast new-path]
  (update-in ast [:args 0] merge
    {:val new-path :form new-path}))

(defn collect-ns! [ns-sym]
  (when fw-util/*compile-collector*
    (swap! fw-util/*compile-collector* update ::nses-with-requires (fnil conj #{}) ns-sym)))

(defn rewrite-asset-requires [env ast opts]
  (if (js-require-asset? ast)
    (let [referenced-path (-> ast :args first :val)
          new-path
          (str
           (.normalize
            (.toPath
             (relativize
              (.getAbsoluteFile (io/file (:output-dir opts)))
              (.getAbsoluteFile
               (io/file
                (.getParentFile (io/file (ana-api/current-file)))
                (normalize referenced-path)))))))
          cur-ns (ana-api/current-ns)]
      (collect-ns! cur-ns)
      (swap! (ana-api/current-state) update-in
             [::ana/namespaces cur-ns ::assets] (fnil conj #{}) new-path)
      (update-require-path ast new-path))
    ast))

(defn js-require-lib? [ast]
  (and (js-require? ast)
       (lib? (-> ast :args first :val))))

(defn collect-lib-requires [env ast opts]
  (when (js-require-lib? ast)
    (let [lib (-> ast :args first :val)
          cur-ns (ana-api/current-ns)]
      (collect-ns! cur-ns)
      (swap! (ana-api/current-state) update-in
        [::ana/namespaces cur-ns ::requires] (fnil conj #{}) lib)))
  ast)

(defn cache-krell-requires [nses opts]
  ;; NOTE: just additive for now, can revisit later if someone finds a
  ;; performance issue
  (let [out-file (io/file (:output-dir opts) "krell_requires.edn")
        nses'    (cond-> nses
                   (.exists out-file)
                   (into (edn/read-string (slurp out-file))))]
    (mkdirs out-file)
    (spit out-file (pr-str nses'))
    nses'))

(defn load-analysis [nses opts]
  (reduce
    (fn [ret ns]
      (assoc-in ret [::ana/namespaces ns]
        (ana-api/read-analysis-cache (ns->cache-file ns opts))))
    {} nses))

(defn all-assets [analysis]
  (into #{} (mapcat (comp ::assets val) (get-in analysis [::ana/namespaces]))))

(defn all-fig-assets [analysis]
  (apply merge (map (comp ::figrn-assets val) (get-in analysis [::ana/namespaces]))))

(defn all-requires [analysis]
  (into #{} (mapcat (comp ::requires val) (get-in analysis [::ana/namespaces]))))

(def custom-passes [rewrite-asset-requires collect-lib-requires])

;; ----------------------------------------------------------------------
;; Adding extra cribbed code here
;; ----------------------------------------------------------------------

(defn contents-equal? [f content]
  (= (slurp f) content))

(defn write-if-different [^File f content]
  (when-not (and (.exists f)
                 (contents-equal? f content))
    (spit f content)))

(defn write-assets-js
  "Write out the REPL asset support code."
  [assets opts]
  (let [out-file (io/file (:output-dir opts) "krell_assets.js")]
    (mkdirs out-file)
    (write-if-different out-file
                        (assets-js assets))))

(defn export-dep [dep]
  (str "\""dep "\": require('" dep "')" ))

(defn krell-npm-deps-js
  "Returns the JavaScript code to support runtime require of bundled modules."
  [node-requires]
  (str
    "module.exports = {\n"
    "  krellNpmDeps: {\n"
    (string/join ",\n" (map (comp #(str "    " %) export-dep) node-requires))
    "  }\n"
    "};\n"))

(defn write-krell-npm-deps-js
  [node-requires opts]
  (let [out-file (io/file (:output-dir opts) "krell_npm_deps.js")]
    (mkdirs out-file)
    (write-if-different out-file (krell-npm-deps-js node-requires))))

(defn post-build-hook [{:keys [options]}]
  (let [nses (cache-krell-requires (get @fw-util/*compile-collector* ::nses-with-requires) options)
        analysis (load-analysis nses options)]
    (write-assets-js (all-assets analysis)
                     options)
    (write-krell-npm-deps-js (all-requires analysis) options)))
