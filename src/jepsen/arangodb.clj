(ns jepsen.arangodb
  (:require [jepsen.arangodb.tests.register :as register]
            [jepsen [cli :as cli]]))

(defn parse-long
  [x]
  (Long/parseLong x))

(def cli-opts
  "Additional command line options."
  [["-r" "--rate HZ" "Approximate number of requests per second, per thread."
    :default  20
    :parse-fn read-string
    :validate [#(and (number? %) (pos? %)) "Must be a positive number"]]
   [nil "--ops-per-key NUM" "Maximum number of operations on any given key."
    :default  200
    :parse-fn parse-long
    :validate [pos? "Must be a positive integer."]]])

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for browsing results"
  [& args]
  (cli/run! (merge
             (cli/single-test-cmd {:test-fn register/register-test
                                   :opt-spec cli-opts})
             (cli/serve-cmd))
            args))
