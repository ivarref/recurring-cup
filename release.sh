#!/bin/bash

if [[ $# -ne 1 ]]; then
    echo "Illegal number of parameters" >&2
    exit 2
fi

set -ex

clojure -Spom
clojure -X:test
clojure -M:jar

LAST_TAG="$(git rev-list --tags --no-walk --max-count=1)"
COMMITS_SINCE_LAST_TAG="$(git rev-list "$LAST_TAG"..HEAD --count)"
echo "Squashing $COMMITS_SINCE_LAST_TAG commits ..."
git reset --soft HEAD~"$COMMITS_SINCE_LAST_TAG"
git commit -m"..."

VERSION="$(clojure -X:release ivarref.pom-patch/set-patch-version! :patch :commit-count)"
echo "Releasing $VERSION: $1"
git add pom.xml
git commit -m "Release $VERSION"
git reset --soft HEAD~2
git commit -m"Release $VERSION: $1"

git tag -a v"$VERSION" -m "Release v$VERSION: $1"
git push --follow-tags --force

clojure -T:deploy

rm *.pom.asc || true

echo "Released $VERSION"
