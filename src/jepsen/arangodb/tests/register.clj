(ns jepsen.arangodb.tests.register
  (:require [clojure.tools.logging :refer :all]
            [jepsen [checker :as checker]
             [client :as client]
             [generator :as gen]
             [independent :as independent]
             [nemesis :as nemesis]]
            [jepsen.arangodb.utils [driver :as driver]
             [support :as s]]
            [jepsen.checker.timeline :as timeline]
            [knossos.model :as model]))

(defn r   [_ _] {:type :invoke, :f :read, :value nil})
(defn w   [_ _] {:type :invoke, :f :write, :value (rand-int 5)})
(defn cas [_ _] {:type :invoke, :f :cas, :value [(rand-int 5) (rand-int 5)]})

(def dbName "register-test")
(def collectionName "register-col")
(def documentName "register-doc")
;; (def attributeName "register-attr")

(defrecord Client [db-created? collection-created? conn node]
  client/Client
  (open! [this test node]
    (assoc this :conn (-> (new com.arangodb.ArangoDB$Builder)
                          (.host node 8529)
                          (.user "root")
                          (.password "")
                          (.timeout (int 10000)) ; 10s timeout for connection and request
                          (.build))
           :node (str node)))

  (setup! [this test]
    (info "sleep 15s to make sure the connection is ready")
    ; you may need to adjust the duration for system discrepancies 
    (Thread/sleep 15000)
    (locking db-created?
      (info "Prepare to create databases")
      (while (false? (compare-and-set! db-created? true false))
        (info "Creating databases")
        (try (Thread/sleep 500)
             ;; create a database with the name of `dbName`
             (-> conn (driver/create-db dbName))
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
             (-> conn (driver/create-collection dbName collectionName))
             (info "Collections created")
             (reset! collection-created? true)
             (catch java.lang.NullPointerException e
               (warn "Collections not created yet")
               (Thread/sleep 2000))
             (catch com.arangodb.ArangoDBException ex
               (warn (.getErrorMessage ex))
               (reset! collection-created? true))))))

  (invoke! [this test op]
    (let [[k v] (:value op)]
      (try
        (case (:f op)
          :read (assoc op :type :ok,
                       :value (-> conn (driver/read-document-attr dbName collectionName documentName (str k))))
          :write (do (-> conn (driver/set-document-attr dbName collectionName documentName (str k) v))
                     (assoc op :type :ok))
          :cas (let [[old new] v]
                 (assoc op :type (if (-> conn (driver/compare-and-set-attr dbName collectionName documentName (str k) old new))
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
        ;; 1200 write-write conflict; key: Anna
        ;; 1465 cluster internal HTTP connection broken
        ;; 1457 timeout in cluster operation
          (let [errorCodeMap
                {1200 :ww-conflict
                 1465 :conn-closed
                 1457 :timeout
                 nil  :timeout}
                errorCode (.getErrorNum ex)]
            (assoc op :type :fail, :error (get errorCodeMap errorCode errorCode)))))))

  (teardown! [this test]
    (try
      (.shutdown conn)
      (info "Connection closed")
      (catch clojure.lang.ExceptionInfo e
        (info "Connection not closed!"))))

  (close! [_ test]))

(defn register-test
  "Given an options map from the command line runner (e.g. :nodes, :ssh,
  :concurrency, ...), constructs a test map."
  [opts]
  (merge s/basic-test
         opts
         {:name            "arangodb-register-test"
          :client          (Client. (atom false) (atom false) nil nil)
          :nemesis         (case (:nemesis-type opts)
                             :partition (nemesis/partition-random-halves)
                             :noop nemesis/noop)
          :checker         (checker/compose
                            {:perf   (checker/perf)
                             :linear (checker/linearizable
                                      {:model     (model/cas-register)
                                       :algorithm :linear})
                             :timeline (timeline/html)})
          :generator       (->> (independent/concurrent-generator
                                 10
                                 (range)
                                 (fn [k]
                                   (->> (gen/mix [r w cas])
                                        (gen/stagger (/ (:rate opts)))
                                        (gen/limit (:ops-per-key opts)))))
                                (gen/nemesis
                                 (cycle [(gen/sleep 5)
                                         {:type :info, :f :start}
                                         (gen/sleep 5)
                                         {:type :info, :f :stop}]))
                                (gen/time-limit (:time-limit opts)))}))