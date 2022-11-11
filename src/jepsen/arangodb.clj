(ns jepsen.arangodb
  (:require [jepsen.arangodb.tests.register :as register]
            [jepsen [cli :as cli]]))

(defn parse-long
  [x]
  (Long/parseLong x))

(def cli-opts
  "Additional command line options."
  [["-r" "--rate HZ" "Approximate number of requests per second, per thread."
    :default  10
    :parse-fn read-string
    :validate [#(and (number? %) (pos? %)) "Must be a positive number"]]
   [nil "--ops-per-key NUM" "Maximum number of operations on any given key."
    :default  100
    :parse-fn parse-long
    :validate [pos? "Must be a positive integer."]]
   [nil "--threads-per-group NUM" "Number of threads, per group."
    :default  5
    :parse-fn parse-long
    :validate [pos? "Must be a positive integer."]]
   [nil "--nemesis-type partition|noop" "Nemesis used."
    :default :noop
    :parse-fn #(case %
                 ("partition") :partition
                 ("noop") :noop
                 :invalid)
    :validate [#{:partition :noop} "Unsupported nemesis"]]])

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for browsing results"
  [& args]
  (cli/run! (merge
             (cli/single-test-cmd {:test-fn register/register-test
                                   :opt-spec cli-opts})
             (cli/serve-cmd))
            args))
