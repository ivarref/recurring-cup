(ns recurring-cup.core
  (:require [tea-time.core :as tt]
            [tea-time.virtual :as tv])
  (:import (java.time ZoneId Instant ZonedDateTime DayOfWeek LocalDate ZoneOffset)
           (tea_time.core Task)
           (java.time.format DateTimeFormatter)))

(comment
  (set! *print-length* 5))                                  ; Only print first 5 items of collections

(defn ^ZonedDateTime now
  ([] (now "UTC"))
  ([tz] (ZonedDateTime/ofInstant
          (Instant/ofEpochSecond (tt/unix-time))
          (ZoneId/of tz))))

(defn- zdt-seq [zdt increment-fn]
  (lazy-seq (cons zdt (zdt-seq (increment-fn zdt) increment-fn))))

(defn- zdt-seq-skip-past [initial-value increment-fn]
  (let [start-from (now)]
    (->> (zdt-seq initial-value increment-fn)
         (drop-while #(.isBefore % start-from)))))

(defn daily
  [{:keys [hour minute second timezone]
    :or   {hour     0
           minute   0
           second   0
           timezone "UTC"}}]
  (zdt-seq-skip-past (-> (now timezone)
                         (.withNano 0)
                         (.withSecond second)
                         (.withMinute minute)
                         (.withHour hour))
                     #(.plusDays % 1)))

(defn hourly
  [{:keys [minute second timezone]
    :or   {minute   0
           second   0
           timezone "UTC"}}]
  (zdt-seq-skip-past (-> (now timezone)
                         (.withNano 0)
                         (.withSecond second)
                         (.withMinute minute))
                     #(.plusHours % 1)))

(defn every-n-minute
  [{:keys [n minute second timezone]
    :or   {n        1
           minute   0
           second   0
           timezone "UTC"}}]
  (assert (pos-int? n) "n must be a positive integer")
  (zdt-seq-skip-past (-> (now timezone)
                         (.withNano 0)
                         (.withSecond second)
                         (.withMinute minute))
                     #(.plusMinutes % n)))

(defn- seq->fn! [s]
  (let [state (atom s)]
    (fn []
      (let [[head & tail] @state]
        (reset! state tail)
        head))))

(defn- zoned-date-time->linear-micros [^ZonedDateTime zdt]
  (tt/unix-micros->linear-micros (tt/seconds->micros (.toEpochSecond zdt))))

(defrecord ZonedDateTimeFnTask [id f ^long t prev-time next-time-fn! cancelled]
  Task
  (succ [this] (when-not @cancelled
                 (when-let [next-time (next-time-fn!)]
                   (assoc this :t (zoned-date-time->linear-micros next-time)))))
  (run [this] (when-not @cancelled
                (f)))
  (cancel! [this]
    (reset! cancelled true)))

(defn schedule-seq!
  [sq f]
  (let [first-time (first sq)]
    (tt/schedule! (ZonedDateTimeFnTask.
                    (tt/task-id)
                    f
                    (zoned-date-time->linear-micros first-time)
                    first-time
                    (seq->fn! (rest sq))
                    (atom false)))))

(defn- now-str
  ([] (now-str "UTC" "E HH:mm"))
  ([tz fmt] (.format (now tz) (DateTimeFormatter/ofPattern fmt))))

(defn- yyyy-MM-dd->epoch
  [s]
  (-> s
      (LocalDate/parse (DateTimeFormatter/ofPattern "yyyy-MM-dd"))
      (.atStartOfDay ZoneOffset/UTC)
      (.toEpochSecond)))

(comment
  (do
    (set! *print-length* 5)
    (tt/stop!)
    (tv/reset-time!)
    (tv/with-virtual-time!
      (tv/advance! (yyyy-MM-dd->epoch "2019-09-23"))
      (schedule-seq! (->> (daily {:hour 9 :minute 45})
                          (remove #(#{DayOfWeek/SATURDAY DayOfWeek/SUNDAY} (.getDayOfWeek %))))
                     (fn [] (println (now-str) "Get first cup of ☕")))
      (tv/advance! (+ (tt/unix-time) (* 7 24 3600))))))