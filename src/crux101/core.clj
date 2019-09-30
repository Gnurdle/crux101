(ns crux101.core
  (:require [crux.api :as crux]
            [clj-time.core :as ct]
            [clj-time.format :as ctf])
 
  (:import (crux.api Crux ICruxAPI)
           (java.net URI)))

(def ^:private opts
  {:dbtype "postgresql"
   :port 5432
   :dbname "crux101"
   :user "replace"
   :password "GnurdleR0x"
   :kv-backend "crux.kv.rocksdb.RocksKv"
   :db-dir "rocks"})

(defonce node (future (Crux/startJDBCNode opts)))

;;
;; the random samples are
;; {:counter <int>
;;  :clj-time-ts <timestamp as clojure time>
;;  :iso-timestamp <iso string timestamp>
;;  :r,g,b <random color component 0-255>
;; }

(def ^:private isofmt (ctf/formatter :basic-date-time-no-ms))

(defn ^:private sample
  "make a sample with some random data"
  [ts counter]
  (let [rnval (partial rand-int 255)]
    {:counter counter
     :clj-time-ts ts
     :iso-timestamp (ctf/unparse isofmt ts)
     :r (rnval) :g (rnval) :b (rnval)}))

(defn ^:private random-samples
  [ts0 n]
  (loop [accum [] counter 1 ts ts0]
    (if (> counter n)
      accum
      (let [tnext (ct/plus ts (ct/minutes 1))]
        (recur (conj accum (sample ts counter)) (inc counter) tnext)))))

(defn ^:private dbid-for
  "make a db-id (uri) for the sample"
  [sample]
  (URI. (format "sampe:id=%d" (:counter sample))))

(defn ^:private stuff-it
  "put bunch of dox into crux"
  []
  (let [nslug 2500
        num-samples 50000
        sys-time-start (System/currentTimeMillis)
        samples (random-samples (ct/date-time 1776 7 4) num-samples)]
    (loop [samples samples num-done 0]
      (let [sys-time-now (System/currentTimeMillis)
            elapsed (- sys-time-now sys-time-start)
            samples-per-sec (if (zero? num-done) 0
                                (quot (* num-done 1000) (- sys-time-now sys-time-start)))
            pct-done (quot (* 100 num-done) num-samples)]
        (println (format "%d/%d, %d seconds: (%d puts/sec) - %d%% complete"
                         num-done num-samples (quot elapsed 1000) samples-per-sec pct-done))
        (if (seq samples)
          (let [slug (take nslug samples)
                txs (as-> slug d
                      (mapv (fn [x] (assoc x :crux.db/id (dbid-for x))) d)
                      (mapv (fn [x] [:crux.tx/put x]) d)
                      (into [] d))]
            (crux/submit-tx @node txs)
            (recur (drop nslug samples) (+ (count slug) num-done))))))))


;; repl stuff to use to test things

(comment

  ;; "Elapsed time: 107874.801973 msecs"
  ;; 50000/50000, 107 seconds: (463 puts/sec) - 100% complete
  (time (stuff-it))

  (def db (crux/db @node))

  ;; so test 1, give them all back to me
  ;; (note that when I create 250K of these, this query times out)
  (def q1 {:find '[e] :where [['e :r]]})
  (time (def r1 (crux/q db q1))) ;; 2231ms
  (count r1) ;; 50000

  ;; inspect at the endpoints
  (crux/entity db (dbid-for {:counter 1})) ;; {:counter 1, :clj-time-ts #object[org.joda.time.DateTime 0x4247ae70 "1776-07-04T00:00:00.000Z"], :iso-timestamp "17760704T000000Z", :r 216, :g 183, :b 196, :crux.db/id #object[java.net.URI 0x5878d0fb "sampe:id=1"]}

  (crux/entity db (dbid-for {:counter 50000})) ;; {:counter 50000, :clj-time-ts #object[org.joda.time.DateTime 0x684b2965 "1776-08-07T17:19:00.000Z"], :iso-timestamp "17760807T171900Z", :r 207, :g 63, :b 86, :crux.db/id #object[java.net.URI 0xe9b121f "sampe:id=50000"]}

  ;; test 2, ':r' should be indexed, so this should be cheap
  (def q2 {:find '[e] :where [['e :r 128]]})
  (time (def r2 (crux/q db q2))) ;; ~ 9.5ms
  (count r2) ;; 197

  ;; test 3 both of these should be indexed too
  (def q3 {:find '[e]
           :where '[[e :r r]
                    [e :g g]]
           :args [{'r 122 'g 122}]})
  (time (def r3 (crux/q db q3))) ;; ~10.5msecs
  (count r3) ;; 0

  ;; test 4 - looking to see index getting used for range
  (def q4 {:find '[e]
           :where '[[e :r r]
                    [(>= r r0)]
                    [(<= r r1)]]
           :args [{'r0 126 'r1 128}]})
  
  (time (def r4 (crux/q db q4))) ;; ~2282 ms  - doesn't seem to be using index
  (count r4) ;; 584

  ;; test 5 - look for stuff on 1776/8/1 using clj-times
  (def q5 {:find '[e]
           :where '[[e :clj-time-ts ts]
                    [(>= ts t0)]
                    [(<= ts t1)]]
           :args [{'t0 (ct/date-time 1776 8 1)
                   't1 (ct/date-time 1776 8 1 23 59 59)}]})
  
  (time (def r5 (crux/q db q5))) ;; ~2200 ms  - doesn't seem to be using index
  (count r5) ;; 1440  - but hey, right answer!

  ;; also note, that I had ZERO luck putting ct/date-times in through the where clause.  They must be supplied as
  ;; args, but hey, I'm new to this, I might have screwed things up!
  
  )
