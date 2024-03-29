(ns ivarref.recurring-cup
  (:require [ivarref.recurring-cup.impl :as impl]
            [clojure.string :as str])
  (:import (java.time ZonedDateTime DayOfWeek)))

(comment
  (set! *print-length* 10))                                 ; only print first 10 items of collections

(defn daily
  "Returns a lazy sequence of daily ZonedDateTime starting from today.

  hour, minute, second and timezone is controlled by the arguments."
  [{:keys [hour minute second timezone]
    :or   {hour     0
           minute   0
           second   0
           timezone "UTC"}}]
  (let [base (impl/now timezone)
        number->zdt #(-> base
                         (.plusDays %)
                         (.withNano 0)
                         (.withSecond second)
                         (.withMinute minute)
                         (.withHour hour))]
    (->> (impl/numbers)
         (map number->zdt)
         (impl/skip-past))))

(defn weekdays
  "Returns a lazy sequence of daily ZonedDateTime.
  Excludes Saturday and Sunday.

  hour, minute, second and timezone is controlled by the arguments."
  [{:keys [hour minute second timezone]
    :or   {hour     0
           minute   0
           second   0
           timezone "UTC"}
    :as   opts}]
  (->> (daily opts)
       (remove #(#{DayOfWeek/SATURDAY DayOfWeek/SUNDAY} (.getDayOfWeek %)))))

(defn every-n-minutes
  ([n]
   (every-n-minutes n 0))
  ([n offset]
   (let [base (impl/now "UTC")
         number->zdt #(-> base
                          (.withNano 0)
                          (.withSecond 0)
                          (.withMinute 0)
                          (.withHour 0)
                          (.plusMinutes (+ offset (* n %))))]
     (->> (impl/numbers)
          (map number->zdt)
          (impl/skip-past)))))


(defn every-n-seconds
  [n]
  (let [base (impl/now "UTC")
        number->zdt #(-> base
                         (.withNano 0)
                         (.withSecond 0)
                         (.withMinute 0)
                         (.withHour 0)
                         (.plusSeconds (* n %)))]
    (->> (impl/numbers)
         (map number->zdt)
         (impl/skip-past))))


(defn immediately [sq]
  (cons (impl/now "UTC") sq))


(def day-kw->DayOfWeek
  (array-map
    :mon DayOfWeek/MONDAY
    :tue DayOfWeek/TUESDAY
    :wed DayOfWeek/WEDNESDAY
    :thur DayOfWeek/THURSDAY
    :fri DayOfWeek/FRIDAY
    :sat DayOfWeek/SATURDAY
    :sun DayOfWeek/SUNDAY))

(defn weekly
  "Returns a lazy sequence of weekly java.time.ZonedDateTime.

  Day is controlled by :day, and may be :mon, :tue, :wed, :thur, :fri, :sat or :sun.
  Hour, minute, second and timezone is also configurable."
  [{:keys [day hour minute second timezone]
    :or   {day      :mon
           hour     0
           minute   0
           second   0
           timezone "UTC"}
    :as   opts}]
  (if-let [java-day (get day-kw->DayOfWeek day)]
    (->> (daily opts)
         (filter #(= java-day (.getDayOfWeek %))))
    (throw (ex-info (str "Unknown :day specified, must be one of "
                         (str/join " " (keys day-kw->DayOfWeek)))
                    {:day day}))))

(defn compose
  ([a] a)
  ([[^ZonedDateTime a & as :as aseq] [^ZonedDateTime b & bs :as bseq]]
   (if aseq
     (if bseq
       (if (.isBefore a b)
         (lazy-seq (cons a (compose as bseq)))
         (lazy-seq (cons b (compose aseq bs))))
       aseq)
     bseq))
  ([a b & xs]
   (reduce compose (compose a b) xs)))

(defn schedule!
  "Schedule a sequence of java.time.ZonedDateTime"
  [id f sq]
  (impl/schedule! id f sq))


(defn dereffable-job!
  [id f sq]
  (impl/dereffable-job! id f sq))

(defn- var->id [v]
  (keyword (str (:ns (meta v))) (str (:name (meta v)))))

(defn dereffable-var!
  [v sq & args]
  (let [f (fn [] (apply (deref v) args))]
    (impl/dereffable-job! (var->id v) f sq)))

(defn deref-var
  ([v not-ready]
   (let [id (var->id v)
         dereffable (get @impl/dereffable-jobs id)]
     (if (nil? dereffable)
       (throw (ex-info "Could not find job" {:id id}))
       (if (realized? dereffable)
         (deref dereffable)
         not-ready))))
  ([v timeout-ms timeout-val]
   (let [id (var->id v)
         dereffable (get @impl/dereffable-jobs id)]
     (if (nil? dereffable)
       (throw (ex-info "Could not find job" {:id id}))
       (deref dereffable timeout-ms timeout-val)))))

(defn stop!
  "Stops the task threadpool. Waits for threads to exit.
  Removes all future tasks. Repeated calls to stop are noops."
  []
  (impl/stop!))

(defn start!
  "Starts the threadpool to execute tasks on the queue automatically. Repeated
  calls to start are noops."
  [& args]
  (apply impl/start! args))
