(ns recurring-cup.demo
  (:require [clojure.test :refer :all]
            [recurring-cup.core :as cup]
            [tea-time.core :as tt]
            [tea-time.virtual :as tv])
  (:import (java.time.format DateTimeFormatter)
           (java.time LocalDate ZoneOffset DayOfWeek ZoneId)))

(defn- now-str
  ([] (now-str "UTC" "E HH:mm"))
  ([tz fmt] (.format (cup/now tz) (DateTimeFormatter/ofPattern fmt))))

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
      (cup/schedule-seq! (->> (cup/daily {:hour 9 :minute 45})
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
      (cup/daily {:hour 2 :timezone "Europe/Oslo"}))))

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
      (cup/daily {:hour 3 :timezone "Europe/Oslo"}))))

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

(comment
  (->> (daily {:hour 9 :minute 45 :timezone "Europe/Oslo"})
       (compose (daily {:hour 11 :minute 0 :timezone "Europe/Oslo"}))
       (compose (daily {:hour 13 :minute 30 :timezone "Europe/Oslo"}))
       (remove #(#{DayOfWeek/SATURDAY DayOfWeek/SUNDAY} (.getDayOfWeek %)))))
