(ns recurring-cup.core
  (:require [tea-time.core :as tt]
            [tea-time.virtual :as tv])
  (:import (java.time ZoneId Instant ZonedDateTime DayOfWeek LocalDate ZoneOffset)
           (tea_time.core Task)
           (java.time.format DateTimeFormatter)))

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

; Sommertid starter: Søndag 29. mar 2020
; Klokka 02.00 natt til søndag, 29. mars 2020, stiller vi klokka en time fram til kl. 03.00 når vi går fra normaltid til sommertid.

(comment
  (do
    (set! *print-length* 5)
    (tt/stop!)
    (tv/reset-time!)
    (tv/with-virtual-time!
      (-> "2020-03-28"
          (LocalDate/parse (DateTimeFormatter/ofPattern "yyyy-MM-dd"))
          (.atStartOfDay (ZoneId/of "Europe/Oslo"))
          (.toEpochSecond)
          (tv/advance!))
      (daily {:hour 2 :timezone "Europe/Oslo"}))))

(comment
  (do
    (set! *print-length* 5)
    (tt/stop!)
    (tv/reset-time!)
    (tv/with-virtual-time!
      (-> "2019-10-26"
          (LocalDate/parse (DateTimeFormatter/ofPattern "yyyy-MM-dd"))
          (.atStartOfDay (ZoneId/of "Europe/Oslo"))
          (.toEpochSecond)
          (tv/advance!))
      (daily {:hour 3 :timezone "Europe/Oslo"}))))

; Sommertid slutter: Søndag, 27. oktober, 2019
; Natt til søndag 27. okt 2019, kl 03.00 stiller vi klokka en time tilbake fra sommertid til normaltid.

(comment
  (ZonedDateTime/parse "2020-03-29 02:00:00 Europe/Oslo" (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss z")))

(comment
  (let [b (ZonedDateTime/parse "2020-03-28 02:00 Europe/Oslo" (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm z"))]
    (= (-> b (.plusDays 1) (.plusDays 1))
       (-> b (.plusDays 2)))))

(comment
  (ZonedDateTime/parse "2019-10-27 03:00:00 Europe/Oslo" (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss z")))

(comment
  (ZonedDateTime/parse "2019-10-27 02:59:59 Europe/Oslo" (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss z")))
