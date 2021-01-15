# recurring-cup

Schedule lazy sequences in Clojure.

## 10-second example

```clojure
(require '[ivarref.recurring-cup :as cup])

; Start executor thread pool. Repeated calls are no-ops.
(cup/start!) 

; Make a daily reminder to yourself to eat lunch at 12:30 in timezone Europe/Oslo
(cup/schedule-seq! (cup/daily {:hour 12 :minute 30 :timezone "Europe/Oslo"})
                   (bound-fn [] (println "time to eat lunch!")))
```

## Usage

### Coffee work schedule example

```clojure
(require '[ivarref.recurring-cup :as cup])
(import (java.time DayOfWeek))

; Don't blow up the stack if you want to inspect the lazy seq manually:
(set! *print-length* 5)
(cup/start!)

(def coffee-schedule
  (->> (cup/compose 
         (cup/daily {:hour 9 :minute 0 :timezone "Europe/Oslo"})
         (cup/daily {:hour 12 :minute 0 :timezone "Europe/Oslo"})
         (cup/daily {:hour 13 :minute 0 :timezone "Europe/Oslo"}))
       ; cup/daily, cup/compose, etc. returns a lazy seq, so you may tweak
       ; the sequence using standard remove, filter, etc: 
       (remove #(#{DayOfWeek/SATURDAY DayOfWeek/SUNDAY} (.getDayOfWeek %)))))

; inspect...
coffee-schedule
; Example output:
; This was executing on a Friday at 11.40, so the 09 hour is not here
; (#object[java.time.ZonedDateTime 0x4cfa8227 "2021-01-15T12:00+01:00[Europe/Oslo]"] 
;  #object[java.time.ZonedDateTime 0x3f685162 "2021-01-15T13:00+01:00[Europe/Oslo]"] 

; And we skip SATURDAY and SUNDAY, so we go right to Monday 09 after Friday:
;  #object[java.time.ZonedDateTime 0x11f406f8 "2021-01-18T09:00+01:00[Europe/Oslo]"]
;  #object[java.time.ZonedDateTime 0x987455b "2021-01-18T12:00+01:00[Europe/Oslo]"] 
;  #object[java.time.ZonedDateTime 0x1f3165e7 "2021-01-18T13:00+01:00[Europe/Oslo]"]
; ...)

(cup/schedule-seq! coffee-schedule 
                   (bound-fn [] (println "time to get some coffee!")))
```

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
