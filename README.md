# recurring-cup

Schedule daily or weekly occurrences in a given timezone in Clojure (JVM only).
Tweak them using standard `clojure.core/filter`, `clojure.core/remove` and so on.

## Installation

[![Clojars Project](https://img.shields.io/clojars/v/ivarref/recurring-cup.svg)](https://clojars.org/ivarref/recurring-cup)

## 10-second example

```clojure
(require '[ivarref.recurring-cup :as cup])

; Start executor thread pool. Repeated calls are no-ops.
(cup/start!) 

; Make a daily reminder to yourself to eat lunch at 12:30 in timezone Europe/Oslo:
(cup/schedule! ; Identifier of schedule:
               ::lunch-reminder
               ; The function to execute:
               (bound-fn [] (println "Time to eat lunch!"))
               ; The schedule, i.e. daily at 12:30 in timezone Europe/Oslo:
               ; If :timezone is left out, UTC is used.
               (cup/daily {:hour 12 :minute 30 :timezone "Europe/Oslo"}))

; Make a weekly reminder on Mondays:
(cup/schedule! ::another-week
               (bound-fn [] (println "Another week begins... 😱"))
               (cup/weekly {; :day should be one of
                            ; :mon, :tue, :wed, :thur, :fri, :sat or :sun
                            :day :mon
                            :hour 7 :minute 0 :timezone "Europe/Oslo"}))

; Replace an existing schedule by using the same identifier:
(cup/schedule! ::another-week
               (bound-fn [] (println "Another week begins! 😻"))
               (cup/weekly {:day :mon
                            :hour 8 :minute 0 :timezone "Europe/Oslo"}))
```

## Advanced usage

`cup/daily` and `cup/weekly` returns lazy sequences of [java.time.ZonedDateTime](https://docs.oracle.com/javase/8/docs/api/java/time/ZonedDateTime.html).
Thus you can use standard `clojure.core/filter`, `clojure.core/remove`, etc.
to build up your preferred schedule.

`cup/compose` joins two or more sequences together and 
returns a single sorted lazy sequence.

### Example coffee schedule using cup/compose and clojure.core/remove 

```clojure
(require '[ivarref.recurring-cup :as cup])
(import (java.time DayOfWeek))

; Don't blow up the stack if you want to inspect the seq manually:
(set! *print-length* 10)
(cup/start!)

(def coffee-schedule
  (->> (cup/compose 
         (cup/daily {:hour 9 :minute 0 :timezone "Europe/Oslo"})
         (cup/daily {:hour 12 :minute 0 :timezone "Europe/Oslo"})
         (cup/daily {:hour 13 :minute 0 :timezone "Europe/Oslo"}))
       ; cup/daily, cup/compose, etc. returns a lazy seq, so you may tweak
       ; the sequence using standard remove, filter, etc: 
       (remove #(#{DayOfWeek/SATURDAY DayOfWeek/SUNDAY} (.getDayOfWeek %)))))

; Example output:
coffee-schedule
; This was executing on a Friday at 11.40, so the 09 hour is not here
; (#object[ZonedDateTime "2021-01-15T12:00+01:00[Europe/Oslo]"] 
;  #object[ZonedDateTime "2021-01-15T13:00+01:00[Europe/Oslo]"] 

; We skip SATURDAY and SUNDAY, so we go right to Monday 09 after Friday.
; Notice thus that we go from 2021-01-15 to 2021-01-18:
;  #object[ZonedDateTime "2021-01-18T09:00+01:00[Europe/Oslo]"]
;  #object[ZonedDateTime "2021-01-18T12:00+01:00[Europe/Oslo]"] 
;  #object[ZonedDateTime "2021-01-18T13:00+01:00[Europe/Oslo]"]
; ...)

(cup/schedule! ::coffee-reminder
               (bound-fn [] (println "Time to get some coffee ☕"))
               coffee-schedule)
```

## Error handling

If a scheduled function throws an exception, it will be logged using `ERROR` level.
Note: the default log level for errors in tea-time is `WARN`.

## Other

[List of available timezones](timezones.md).

## Credits

Built on top of the excellent [Tea-Time](https://github.com/aphyr/tea-time).

## Change log

#### 2022-11-08 v1.0.90

`deref-var` should not require timeout-ms.

#### 2022-11-08 v1.0.89

Add `dereffable-var!` and `deref-var`.

## License

Copyright © 2019 Ivar Refsdal

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
