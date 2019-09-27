(ns recurring-cup.core
  (:require [tea-time.core :as tt])
  (:import (java.time ZoneId Instant ZonedDateTime)
           (tea_time.core Task)))

#_(set! *print-length* 5)                                     ; Only print first 5 items of collections

(defn ^ZonedDateTime now
  ([] (now "UTC"))
  ([tz] (ZonedDateTime/ofInstant
          (Instant/ofEpochSecond (tt/unix-time))
          (ZoneId/of tz))))

(defn- numbers
  ([] (numbers 0))
  ([n] (lazy-seq (cons n (numbers (inc n))))))

(defn skip-past [s]
  (let [start-from (now)]
    (drop-while #(.isBefore % start-from) s)))

(defn daily
  [{:keys [hour minute second timezone]
    :or   {hour     0
           minute   0
           second   0
           timezone "UTC"}}]
  (let [base (now timezone)
        number->zdt #(-> base
                         (.plusDays %)
                         (.withNano 0)
                         (.withSecond second)
                         (.withMinute minute)
                         (.withHour hour))]
    (->> (numbers)
         (map number->zdt)
         (skip-past))))

(defn hourly
  [{:keys [minute second timezone]
    :or   {minute   0
           second   0
           timezone "UTC"}}]
  (let [base (now timezone)
        number->zdt #(-> base
                         (.plusHours %)
                         (.withNano 0)
                         (.withSecond second)
                         (.withMinute minute))]
    (->> (numbers)
         (map number->zdt)
         (skip-past))))

(defn every-n-minute
  [{:keys [n minute second timezone]
    :or   {n        1
           minute   0
           second   0
           timezone "UTC"}}]
  (assert (pos-int? n) "n must be a positive integer")
  (let [base (-> (now timezone)
                 (.withNano 0)
                 (.withSecond second)
                 (.withMinute minute))]
    (->> (numbers)
         (map #(.plusMinutes base (* n %)))
         (skip-past))))

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
