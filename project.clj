(defproject com.bhauman/figwheel-main "0.2.21-SNAPSHOT"
  :description "Figwheel Main - Clojurescript tooling."
  :url "https://github.com/bhauman/figwheel-main"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :scm {:name "git"
        :url "https://github.com/bhauman/figwheel-main"}
  :dependencies
  [[org.clojure/clojure "1.10.3"]
   [org.clojure/clojurescript "1.10.773" :exclusions [commons-codec]]
   [org.clojure/data.json "2.4.0"]
   [com.bhauman/figwheel-repl "0.2.21-SNAPSHOT"]
   [com.bhauman/figwheel-core "0.2.21-SNAPSHOT"]
   [com.bhauman/spell-spec "0.1.1"]
   [com.bhauman/cljs-test-display "0.1.1"]
   [com.bhauman/certifiable "0.0.7"]
   [ring "1.13.0"]
   [binaryage/devtools "1.0.5"]
   [com.nextjournal/beholder "1.0.0"]
   [expound "0.7.1"]]
  :resource-paths ["helper-resources"]
  :profiles {:dev {:dependencies [[cider/piggieback "0.3.9"]
                                  #_[com.bhauman/rebel-readline-cljs "0.1.4"]]
                   :source-paths ["src" "devel" "dev"]
                   :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}})
