(ns recurring-cup.impl
  (:require [tea-time.core :as tt])
  (:import (java.time ZoneId Instant ZonedDateTime)
           (tea_time.core Task)))

(defn ^ZonedDateTime now
  ([] (now "UTC"))
  ([tz] (ZonedDateTime/ofInstant
          (Instant/ofEpochSecond (tt/unix-time))
          (ZoneId/of tz))))

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

(defn schedule-seq!
  [sq f]
  (tt/schedule! (ZonedDateTimeTask.
                  (tt/task-id)
                  f
                  (zoned-date-time->linear-micros (first sq))
                  (rest sq)
                  (atom false))))

(defn start! []
  (tt/start!))

(defn stop! []
  (tt/stop!))