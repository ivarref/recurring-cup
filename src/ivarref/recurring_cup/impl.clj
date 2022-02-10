(ns ivarref.recurring-cup.impl
  (:require [tea-time.core :as tt]
            [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:import (java.time ZoneId Instant ZonedDateTime)
           (tea_time.core Task)
           (java.io Writer)
           (clojure.lang IDeref IPending IBlockingDeref)))

(def available-zones
  (into (sorted-set) (ZoneId/getAvailableZoneIds)))

(defn ^ZonedDateTime now
  ([] (now "UTC"))
  ([tz]
   (if (contains? available-zones tz)
     (ZonedDateTime/ofInstant
       (Instant/ofEpochSecond (tt/unix-time))
       (ZoneId/of tz))
     (do
       (log/error "invalid timezone specified:" tz)
       (log/error "must use one of:" (str/join ", " available-zones))
       (throw (ex-info "invalid timezone specified" {:timezone tz}))))))

(defn numbers
  ([] (numbers 0))
  ([n] (lazy-seq (cons n (numbers (inc n))))))

(defn skip-past [s]
  (let [start-from (now)]
    (drop-while #(.isBefore % start-from) s)))

(defn- zoned-date-time->linear-micros [^ZonedDateTime zdt]
  (tt/unix-micros->linear-micros (tt/seconds->micros (.toEpochSecond zdt))))

(defrecord ZonedDateTimeTask [id f ^long t sq cancelled]
  Task
  (succ [this] (when-not @cancelled
                 (when-let [next-time (first sq)]
                   (assoc this :t (zoned-date-time->linear-micros next-time)
                               :sq (rest sq)))))
  (run [this] (when-not @cancelled
                (f)))
  (cancel! [this]
    (reset! cancelled true)))

(defmethod clojure.core/print-method ZonedDateTimeTask
  [task ^Writer writer]
  (.write writer "#<ZonedDateTimeTask>")
  (.write writer "[")
  (.write writer (str/join ", " (mapv str (take 10 (:sq task)))))
  (.write writer ", ...]"))

(defonce tasks (atom {}))

(defn schedule!
  [id f sq]
  (let [cancelled (atom false)
        new-task (ZonedDateTimeTask.
                   (tt/task-id)
                   (fn []
                     (let [old-name (.getName (Thread/currentThread))]
                       (try
                         (.setName (Thread/currentThread) (str "cup-" (name id)))
                         (f)
                         (catch Throwable t
                           (log/error t "Recurring-cup task" id "threw uncaught exception"))
                         (finally
                           (.setName (Thread/currentThread) old-name)))))
                   (zoned-date-time->linear-micros (first sq))
                   (rest sq)
                   cancelled)]
    (tt/schedule! new-task)
    (swap! tasks update id (fn [old-cancelled]
                             (when old-cancelled
                               (reset! old-cancelled true))
                             cancelled))
    new-task))


(defn scheduled-dereffable-job! [id f sq]
  (let [p (promise)
        a (atom nil)]
    (schedule! id
               (fn [] (let [r (f)]
                        (reset! a r)
                        (deliver p a)))
               sq)
    (reify
      IDeref
      (deref [_]
        @@p)
      IBlockingDeref
      (deref [_ timeout-ms timeout-val]
        (let [v (deref p timeout-ms timeout-val)]
          (if (= v a)
            @a
            v)))
      IPending
      (isRealized [_]
        (realized? p)))))


(defn dereffable-job!
  [id f sq]
  ; cancel any old job
  (swap! tasks update id (fn [old-cancelled]
                           (when old-cancelled
                             (reset! old-cancelled true))
                           nil))
  (if @tt/running
    (scheduled-dereffable-job! id f sq)
    (reify
      IDeref
      (deref [_]
        (f))
      IBlockingDeref
      (deref [_ _ _]
        (f))
      IPending
      (isRealized [_]
        true))))


(defn start! [& {:keys [thread-count]
                 :or   {thread-count 4}}]
  (alter-var-root
    (var tt/thread-count)
    (constantly thread-count))
  (tt/start!))

(defn stop! []
  (tt/stop!))

(comment
  (do
    (println "starting...")
    (start!)
    (let [begin (-> (now "UTC"))
          number->zdt #(-> begin
                           (.plusSeconds %))
          schedule (map number->zdt (numbers))]
      (let [job (dereffable-job! ::say-hi
                                 (let [cnt (atom 0)]
                                   (bound-fn []
                                     #_(when (> (Math/random) 0.5))
                                     (throw (ex-info "janei..." {}))
                                     (swap! cnt inc)))
                                 (skip-past schedule))]
        (def j job)))))


(comment
  (do
    (println "starting...")
    (start!)
    (let [begin (-> (now "UTC"))
          number->zdt #(-> begin
                           (.plusSeconds %))
          schedule (map number->zdt (numbers))]
      (schedule! ::say-hi
                 (let [cnt (atom 0)]
                   (bound-fn []
                     (println "hello" (swap! cnt inc))
                     #_(Thread/sleep 10000)))
                 (skip-past schedule))
      #_(Thread/sleep 3000)
      #_(schedule! ::say-hi
                   (let [cnt (atom 0)]
                     (bound-fn []
                       (println "hi" (.getName (Thread/currentThread)) (swap! cnt inc))))
                   (skip-past schedule)))))
