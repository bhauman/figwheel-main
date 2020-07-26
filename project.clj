(defproject com.bhauman/figwheel-main "0.2.12-SNAPSHOT"
  :description "Figwheel Main - Clojurescript tooling."
  :url "https://github.com/bhauman/figwheel-main"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :scm {:name "git"
        :url "https://github.com/bhauman/figwheel-main"}
  :dependencies
  [[org.clojure/clojure "1.9.0"]
   [org.clojure/clojurescript "1.10.773" :exclusions [commons-codec]]
   [com.bhauman/figwheel-repl "0.2.12-SNAPSHOT"]
   [com.bhauman/figwheel-core "0.2.12-SNAPSHOT"]
   [com.bhauman/spell-spec "0.1.1"]
   [com.bhauman/cljs-test-display "0.1.1"]
   [com.bhauman/certifiable "0.0.7"]
   [ring "1.8.1"]
   [org.eclipse.jetty.websocket/websocket-servlet "9.4.28.v20200408"]
   [org.eclipse.jetty.websocket/websocket-server  "9.4.28.v20200408"]
   [binaryage/devtools "0.9.10"]
   [hawk "0.2.11"]
   [expound "0.7.1"]]
  :resource-paths ["helper-resources"]
  :profiles {:dev {:dependencies [[cider/piggieback "0.3.9"]
                                  #_[com.bhauman/rebel-readline-cljs "0.1.4"]]
                   :source-paths ["src" "devel" "dev"]
                   :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}})
