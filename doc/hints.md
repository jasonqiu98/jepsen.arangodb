# Hints

Some errors seen during the development of this project, and some (possible/failed) solutions.

## 21 June

- Added all the five server ips to "known_hosts" by `ssh-keyscan <host_ip> >> ~/.ssh/known_hosts`

- ERROR [2022-06-21 02:36:36,239] main - jepsen.cli Oh jeez, I'm sorry, Jepsen broke. Here's why:
  clojure.lang.ExceptionInfo: Command exited with non-zero status 127 on node 192.168.56.104:
  cd /;  start-stop-daemon --start --background --no-close --make-pidfile --exec arangodb  --pidfile /home/vagrant/arangodb.pid --chdir /opt/arangodb --startas arangodb -- --server.storage-engine=rocksdb --auth.jwt-secret=/home/vagrant/arangodb.secret --starter.data-dir=./data --starter.join 192.168.56.101,192.168.56.102,192.168.56.103,192.168.56.104,192.168.56.105 >> /home/vagrant/arangodb.log 2>&1
- jepsen.os.debian Installing #{curl netcat tcpdump apt-transport-https libzip4 psmisc ntpdate faketime vim unzip dirmngr}
- To install gcc and curses. This is optional though. I installed this in case there would be anything wrong with `start-stop-daemon` but eventually it didn't help anything :(

```clojure
;; (info node "installing gcc and curses")
;; (c/exec :apt-get :update)
;; (c/exec :apt-get :install :libncurses5-dev :libncursesw5-dev :build-essential :--yes)
;; (centos/install-start-stop-daemon!)
```

## 22 June

- ArangoDB Architecture
- ArangoDB Isolation Levels
- Slack Channel - Bang them ArangoDB developers
- Implementation
  - Jepsen
  - Benchmarks (with ArangoDB)
    - Can borrow from DGraph
    - Can find some more inspirations from ArangoDB Github Repo / Issues
- Write what I have in Overleaf
  - Start with short and simple paragraphs
  - But Keep all the sentences clear!

## 6 July

1. other command-line args for ArangoDB Builder

```clojure
(.jwt (slurp (io/resource "jwtSecret")))
(.connectionTtl 180000)
```

2. GNU Plot (Installation)

```
WARN [2022-07-06 01:31:35,050] clojure-agent-send-off-pool-10 - jepsen.checker Error while checking history:
java.lang.IllegalStateException: Error rendering plot, verify gnuplot is installed and reachable
        at jepsen.checker.perf$plot_BANG_.invokeStatic(perf.clj:480)
        at jepsen.checker.perf$plot_BANG_.invoke(perf.clj:417)
        at jepsen.checker.perf$rate_graph_BANG_.invokeStatic(perf.clj:599)
        at jepsen.checker.perf$rate_graph_BANG_.invoke(perf.clj:559)
        at jepsen.checker$rate_graph$reify__9939.check(checker.clj:819)
        at jepsen.checker$check_safe.invokeStatic(checker.clj:81)
        at jepsen.checker$check_safe.invoke(checker.clj:74)
        at jepsen.checker$compose$reify__9724$fn__9726.invoke(checker.clj:97)
        at clojure.core$pmap$fn__8485$fn__8486.invoke(core.clj:7024)
        at clojure.core$binding_conveyor_fn$fn__5772.invoke(core.clj:2034)
        at clojure.lang.AFn.call(AFn.java:18)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:264)
        at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
        at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
        at java.base/java.lang.Thread.run(Thread.java:829)
```

## 1 Sep

ERROR [2022-09-01 15:50:17,458] sshj-Reader-/192.168.56.103:22 - net.schmizz.sshj.transport.TransportImpl Dying because - Too many authentication failures
net.schmizz.sshj.transport.TransportException: Too many authentication failures
        at net.schmizz.sshj.transport.TransportImpl.gotDisconnect(TransportImpl.java:533)
        at net.schmizz.sshj.transport.TransportImpl.handle(TransportImpl.java:489)
        at net.schmizz.sshj.transport.Decoder.decode(Decoder.java:113)
        at net.schmizz.sshj.transport.Decoder.received(Decoder.java:200)
        at net.schmizz.sshj.transport.Reader.run(Reader.java:60)

clojure.lang.ExceptionInfo: throw+: {:private-key-path nil, :password "--ssh-private-key", :username "vagrant", :type :jepsen.control/session-error, :port 22, :strict-host-key-checking false, :host "192.168.56.101", :dummy false, :message "Error opening SSH session. Verify username, password, and node hostnames are correct."}

# 12 Sep

To set up a cluster, use the following command.
```
/opt/arangodb/bin/arangodb --server.storage-engine rocksdb --auth.jwt-secret /home/vagrant/arangodb.secret --starter.data-dir ./data --starter.join 192.168.56.101,192.168.56.102,192.168.56.103,192.168.56.104,192.168.56.105
```




# 19 Sep

- read and write on random values
- transaction id (group a bunch of reads and writes on the operations of a worker. Check how Jepsen deals with this. MUST!! Check serializability.) event id => transactional id
- Dgraph ? operational? transactional?

