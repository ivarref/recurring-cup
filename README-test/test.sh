#!/bin/bash

set -ex

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

RAW_VERSION="$(grep "^  <version>" $DIR/../pom.xml | sed 's|^.*<version>\(.*\)</version>.*$|\1|')"

rm "$DIR/temp"* || true
sed $DIR/../README.md -ne '/```clojure/,/```/p' | \
sed 's/^```$//g' | \
awk -v RS='```clojure' '{ print $0 > "'$DIR'/temp" NR }'

for entry in "$DIR"/temp*
do
  clojure -Sdeps '{:deps {ivarref/recurring-cup {:mvn/version "'$RAW_VERSION'"}}}' \
  -M --report stderr \
  -e "$(cat $entry)"
done

