#!/bin/bash -x

echo "##################### EXECUTE: kurento-java-release #####################"

# Param management

# Release version
[ -n "$1" ] && echo $1 | grep -Pq "^\d+\.\d+\.\d+$" && RELEASE_VERSION=$1 || exit 1

# Next version
[ -n "$2" ] && echo $1 | grep -Pq "^\d+\.\d+\.\d+$" && NEXT_VERSION=$2 || exit 1

# Maven settings
[ -n "$3" ] && MAVEN_SETTINGS=$3 || exit 1

# Release branch
[ -n "$4" ] && RELEASE_BRANCH=$4 || exit 1

[ -n "$5" ] && PUSH_TAG=$5 || exit 1

# If tag already exists terminate silently
git ls-remote --tags|grep -q "v$RELEASE_VERSION" && echo "WARN: Tag already exists" && exit 0

# Checkout release branch
git checkout $RELEASE_BRANCH || git checkout -b $RELEASE_BRANCH origin/$RELEASE_BRANCH

# Prepare release
mvn -B --settings $MAVEN_SETTINGS org.apache.maven.plugins:maven-release-plugin:2.5:prepare -DreleaseVersion=$RELEASE_VERSION -DdevelopmentVersion=$NEXT_VERSION-SNAPSHOT -DpushChanges=false -Darguments='-Dmaven.test.skip=true' || exit 1

# Push release tag to repo
if [ $PUSH_TAG = "y" ]; then
  echo "Push tag v$RELEASE_VERSION to repository"
  git push origin --tags v$RELEASE_VERSION || exit 1
fi
