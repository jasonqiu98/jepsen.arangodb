(ns jepsen.arangodb.tests.register
  (:require [clojure.tools.logging :refer :all]
            [clojure.string :as str]
            [jepsen [checker :as checker]
             [client :as client]
             [control :as c]
             [generator :as gen]
             [tests :as tests]]
            [jepsen.os.debian :as debian]
            [jepsen.arangodb.utils [driver :as driver]
             [support :as s]]
            [jepsen.checker.timeline :as timeline]
            [knossos.model :as model]))

(defn r   [_ _] {:type :invoke, :f :read, :value nil})
(defn w   [_ _] {:type :invoke, :f :write, :value (rand-int 5)})
(defn cas [_ _] {:type :invoke, :f :cas, :value [(rand-int 5) (rand-int 5)]})

(defrecord Client [db-created? collection-created? conn node]
  client/Client
  (open! [this test node]
    (assoc this :conn (-> (new com.arangodb.ArangoDB$Builder)
                          (.host node 8529)
                          (.user "root")
                          (.password "")
                          (.build))
           :node (str node)))

  (setup! [this test]
    ;; sleep 15s to make sure the connection is ready
    (Thread/sleep 15000)
    (locking db-created?
      (info "Prepare to create databases")
      (while (false? (compare-and-set! db-created? true false))
        (info "Creating databases")
        (try (Thread/sleep 500)
             ;; create a database called "socialNetwork"
             (-> conn (driver/create-db "socialNetwork"))
             (info "Databases created")
             (reset! db-created? true)
             ;; database not created yet
             (catch java.lang.NullPointerException e
               (warn "Databases not created yet")
               (Thread/sleep 2000))
             ;; database already created
             (catch com.arangodb.ArangoDBException ex
               (warn (.getErrorMessage ex))
               (reset! db-created? true)))))

    (locking collection-created?
      (while (false? (compare-and-set! collection-created? true false))
        (info "Creating collections")
        (try (Thread/sleep 500)
             (-> conn (driver/create-collection "socialNetwork" "people"))
             (info "Collections created")
             (reset! collection-created? true)
             (catch java.lang.NullPointerException e
               (warn "Collections not created yet")
               (Thread/sleep 2000))
             (catch com.arangodb.ArangoDBException ex
               (warn (.getErrorMessage ex))
               (reset! collection-created? true))))))

  (invoke! [this test op]
    (try
      (case (:f op)
        :read (assoc op :type :ok,
                     :value (-> conn (driver/read-document-attr "socialNetwork" "people" "Anna" "total")))
        :write (do (-> conn (driver/set-document-attr "socialNetwork" "people" "Anna" "total" (:value op)))
                   (assoc op :type :ok))
        :cas (let [[old new] (:value op)]
               (assoc op :type (if (-> conn (driver/compare-and-set-attr "socialNetwork" "people" "Anna" "total" old new))
                                 :ok
                                 :fail))))
      (catch java.net.SocketTimeoutException ex
        (assoc op :type :fail, :error :timeout))
      (catch java.lang.NullPointerException ex
        (error "Connection error")
        (assoc op
               :type  (if (= :read (:f op)) :fail :info)
               :error :connection-lost))
      (catch com.arangodb.ArangoDBException ex
        (warn (.getErrorMessage ex))
        (assoc op :type :fail, :error :write-conflict))))

  (teardown! [this test]
    (try
      (let [arango-procs (str/split (c/exec "pgrep" "arango") #"\n")]
        (c/su (c/exec "kill" "-9" arango-procs))
        (info "Processes" arango-procs " were killed"))
      (catch clojure.lang.ExceptionInfo e
        (info "No arangodb processes were killed"))))

  (close! [_ test]))

(defn register-test
  "Given an options map from the command line runner (e.g. :nodes, :ssh,
  :concurrency, ...), constructs a test map."
  [opts]
  (merge tests/noop-test
         opts
         {:pure-generators true
          :name            "arangodb"
          :os              debian/os
          :db              (s/db-setup)
          :client          (Client. (atom false) (atom false) nil nil)
          :checker         (checker/compose
                            {:perf   (checker/perf)
                             :linear (checker/linearizable
                                      {:model     (model/cas-register)
                                       :algorithm :linear})
                             :timeline (timeline/html)})
          :generator       (->> (gen/mix [r w cas])
                                (gen/stagger (/ (:rate opts)))
                                (gen/nemesis nil)
                                (gen/time-limit (:time-limit opts)))}))