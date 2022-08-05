#!/bin/bash

set -ex

clojure -Spom
clojure -X:test
clojure -M:jar
VERSION=$(clojure -X:release ivarref.pom-patch/set-patch-version! :patch :commit-count+1)

git add pom.xml
git commit -m "Release $VERSION"
git tag -a v$VERSION -m "Release v$VERSION"
git push --follow-tags

clojure -T:deploy

rm *.pom.asc || true

echo "Released $VERSION"
