#!/bin/bash

[ -n $1 -a -n $2 -a -n $3 ] || (echo "Usage: $0 <Level2 repository> <cvs file with release commit per repository> <release version>"; exit 1)

REPOSITORY=$1
CSV_FILE=$2
VERSION=$3

echo "Repository: $REPOSITORY; File: $CSV_FILE; release: $VERSION"

if [ ! -d $REPOSITORY ]; then
  git clone --recursive git@github.com:nubomedia/$REPOSITORY
fi

pushd $REPOSITORY
git fetch --recurse-submodules

LINES=$(cat $CSV_FILE)
for line in $LINES
do
  repo=$(echo $line | cut -d , -f 1)
  commit=$(echo $line | cut -d , -f 2)
  echo "Repo name: $repo, commit: $commit"

  pushd $repo
  git checkout $commit
  popd
done

git add .
git ci -m "Prepare release $VERSION"
git tag $VERSION
#git push
#git push --tags
