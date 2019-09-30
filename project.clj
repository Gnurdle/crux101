(defproject crux101 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [juxt/crux-core "19.09-1.4.0-alpha"]
                 [juxt/crux-rocksdb "19.09-1.4.0-alpha"]
                 [juxt/crux-jdbc "19.09-1.4.0-alpha"]
                 [org.postgresql/postgresql "9.4-1200-jdbc41"]
                 [clj-time "0.15.1"]]
  :repl-options {:init-ns crux101.core})
