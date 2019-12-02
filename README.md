# recurring-cup

Schedule lazy sequences in Clojure.



## Usage

### My work coffee schedule

```clojure
(set! *print-length* 10)
(use 'recurring-cup.core)
(import (java.time DayOfWeek))

(->> (daily {:hour 9 :minute 45 :timezone "Europe/Oslo"})
     (compose (daily {:hour 12 :minute 45 :timezone "Europe/Oslo"}))
     (remove #(#{DayOfWeek/SATURDAY DayOfWeek/SUNDAY} (.getDayOfWeek %))))
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
