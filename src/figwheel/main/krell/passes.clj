;; ------------------------------------------------------------------------
;; Code taken directly from Krell https://github.com/vouch-opensource/krell
;; ------------------------------------------------------------------------

(ns figwheel.main.krell.passes
  (:require [cljs.analyzer :as ana]
            [figwheel.main.krell.ana-api :as ana-api]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [figwheel.main.krell.assets :as assets]
            [figwheel.main.krell.util :as util])
  (:import [java.io File]))

(def ^:dynamic *nses-with-requires* nil)

(defn normalize [s]
  (cond-> s (string/starts-with? s "./") (subs 2)))

(defn asset? [s]
  (and (not (nil? (util/file-ext s)))
       (not (assets/js? s))))

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

(defn rewrite-asset-requires [env ast opts]
  (if (js-require-asset? ast)
    (let [referenced-path (-> ast :args first :val)
          new-path
          (str
           (.normalize
            (.toPath
             (util/relativize
              (.getAbsoluteFile (io/file (:output-dir opts)))
              (.getAbsoluteFile
               (io/file
                (.getParentFile (io/file (ana-api/current-file)))
                (normalize referenced-path)))))))
          cur-ns (ana-api/current-ns)]
      (when *nses-with-requires*
        (swap! *nses-with-requires* conj cur-ns))
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
      (when *nses-with-requires*
        (swap! *nses-with-requires* conj cur-ns))
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
    (util/mkdirs out-file)
    (spit out-file (pr-str nses'))
    nses'))

(defn load-analysis [nses opts]
  (reduce
    (fn [ret ns]
      (assoc-in ret [::ana/namespaces ns]
        (ana-api/read-analysis-cache (util/ns->cache-file ns opts))))
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
    (util/mkdirs out-file)
    (write-if-different out-file
                        (assets/assets-js assets))))

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
    (util/mkdirs out-file)
    (write-if-different out-file (krell-npm-deps-js node-requires))))

(defn post-build-hook [{:keys [options]}]
  (let [nses (cache-krell-requires @*nses-with-requires* options)
        analysis (load-analysis nses options)]
    #_(binding [*out* *err*]
        (prn :HERER (all-assets analysis)))
    (write-assets-js (all-assets analysis)
                     options)
    (write-krell-npm-deps-js (all-requires analysis) options)))
