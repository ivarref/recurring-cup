#!/bin/bash

set -ex

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

RAW_VERSION="$(grep "^  <version>" $DIR/pom.xml | sed 's|^.*<version>\(.*\)</version>.*$|\1|')"

rm -rv "$DIR/temp" || true
mkdir -p $DIR/temp

sed $DIR/README.md -ne '/```clojure/,/```/p' | \
sed 's/^```$//g' | \
awk -v RS='```clojure' '{ print $0 > "'$DIR'/temp/temp" NR }'

for entry in "$DIR/temp"/temp*
do
  echo $entry
  clojure  \
  -M --report stderr \
  -e "$(cat $entry)\n(require '[ivarref.recurring-cup :as cup])\n(cup/stop!)\n(shutdown-agents)"
done
