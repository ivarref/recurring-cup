(ns recurring-cup.core-test
  (:require [clojure.test :refer :all]
            [recurring-cup.core :as cup]
            [tea-time.virtual :as tv]
            [tea-time.core :as tt])
  (:import (java.time ZonedDateTime)
           (java.time.format DateTimeFormatter)))

(defn parse [s]
  (ZonedDateTime/parse s (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss z")))

(defn advance! [s]
  (-> s
      (parse)
      (.toEpochSecond)
      (tv/advance!)))

(deftest no-timezone-change-drift
  (set! *print-length* nil)
  (tt/stop!)
  (tv/reset-time!)
  (tv/with-virtual-time!
    (advance! "2020-03-29 00:00:00 Europe/Oslo")
    (let [[a b] (vec (take 2 (cup/daily {:hour 2 :timezone "Europe/Oslo"})))]
      (is (= a (parse "2020-03-29 03:00:00 Europe/Oslo")))
      (is (= b (parse "2020-03-30 02:00:00 Europe/Oslo")))))
  (tt/stop!)
  (tv/reset-time!)
  (tv/with-virtual-time!
    (advance! "2020-03-28 00:00:00 Europe/Oslo")
    (let [[a b c] (vec (take 3 (cup/daily {:hour 2 :timezone "Europe/Oslo"})))]
      (is (= a (parse "2020-03-28 02:00:00 Europe/Oslo")))
      (is (= b (parse "2020-03-29 03:00:00 Europe/Oslo")))
      (is (= c (parse "2020-03-30 02:00:00 Europe/Oslo"))))
    (let [[a b c] (vec (take 3 (cup/daily {:hour 1 :timezone "Europe/Oslo"})))]
       (is (= a (parse "2020-03-28 01:00:00 Europe/Oslo")))
       (is (= b (parse "2020-03-29 01:00:00 Europe/Oslo")))
       (is (= c (parse "2020-03-30 01:00:00 Europe/Oslo"))))))