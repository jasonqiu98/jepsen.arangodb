(ns jepsen.arangodb.utils.support
  (:require [clojure.tools.logging :refer :all]
            [clojure.string :as str]
            [jepsen [control :as c]
             [db :as db]]
            [jepsen.control.util :as cu]
            [jepsen.control :as c]))

(def dir "/opt/arangodb")
(def bin-dir (str dir "/bin"))
(def binary "arangodb")
(def logfile "/home/vagrant/arangodb.log")
(def pidfile "/home/vagrant/arangodb.pid")

;; (def cluster-nodes-path (io/file (io/resource "nodes-vagrant.txt")))
;; (def arangodb-properties-path (io/file (io/resource "arangodb.properties")))
;; (def arangodb-properties-in (io/input-stream arangodb-properties-path))
(def jwt-secret-path "/home/vagrant/arangodb.secret")

(defn cli-arg
  "command line argument of arangodb"
  [flag value]
  (str flag "=" value))

(defn initial-cluster
  "Constructs an initial cluster string for a test, like
  \"192.168.56.101,192.168.56.102,192.168.56.103,192.168.56.104,192.168.56.105\""
  [test]
  (str/join "," (:nodes test)))

(defn parse-int [s]
  (Integer/parseInt (re-find #"\A-?\d+" s)))

(defn db-setup
  "ArangoDB Version v3.9.1"
  []
  (reify db/DB
    (setup! [_ test node]
      (info node "installing arangodb v3.9.1")
      (c/su
       (let [url (str "https://download.arangodb.com/arangodb39/Community/Linux/arangodb3-linux-3.9.1.tar.gz")]
         (cu/install-archive! url dir)))

      (c/su
       (cu/start-daemon!
        {:logfile logfile
         :pidfile pidfile
         :chdir bin-dir}
        binary
        (cli-arg "--server.storage-engine" "rocksdb")
        (cli-arg "--auth.jwt-secret" jwt-secret-path)
        (cli-arg "--starter.data-dir" "./data")
        :--starter.join (initial-cluster test))))

    (teardown! [_ test node]
      (info node "tearing down arangodb")
      (cu/stop-daemon! binary pidfile)
      (try
        ; can use cu/grepkill! instead
        ; https://github.com/jepsen-io/jepsen/blob/40b24800122433ea260bd188c05033059329d3a0/jepsen/src/jepsen/control/util.clj#L286
        (let [arango-procs (str/split (c/exec "pgrep" "arango") #"\n")]
          (c/su (c/exec "kill" "-9" arango-procs))
          (info "processes" arango-procs "killed"))
        (catch clojure.lang.ExceptionInfo e
          (info "no arangodb processes killed"))))

    db/LogFiles
    (log-files [_ test node]
      [logfile])))