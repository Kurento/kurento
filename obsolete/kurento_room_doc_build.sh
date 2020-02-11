#!/bin/bash

echo "##################### EXECUTE: kurento_room_doc_build.sh #####################"

# Param management
if [ $# -lt 1 ]
then
  echo "Usage: $0 <MAVEN_SETTINGS> [<PUBLISH_FILES>]"
  exit 1
fi

# Maven settings
[ -n "$1" ] && MAVEN_SETTINGS=$1 || exit 1
[ -n "$2" ] && PUBLISH_FILES=$2 || PUBLISH_FILES="no"

mvn --settings $MAVEN_SETTINGS clean package -pl kurento-room-doc

if [ "$PUBLISH_FILES" == "no" ]; then
  exit 0
fi

echo "********** Publishing files *************"

# Extract version
cd kurento-room-doc
VERSION=$(grep -E "^\s*DOC_VERSION\s*=" src/site/sphinx/Makefile | awk -F '=' '{print $2}' | tr -d ' ')
echo "Version built: $VERSION"

# Generate version file
echo "VERSION_DATE=$VERSION - `date` - `date +%Y%m%d-%H%M%S`" > kurento-room-docs.version

# Extract contents
pushd ./target/site/html/
tar -cvzf ../../kurento-room-docs-$VERSION.tgz * || exit 1
popd

# Export files to upload
DATE=$(date +"%Y%m%d")
V_DIR=/dev/$BRANCH/$DATE
S_DIR=/dev/$BRANCH/latest

FILE=""
FILE="$FILE target/kurento-room-docs-$VERSION.tgz:$V_DIR/kurento-room-docs-$VERSION.tgz"
FILE="$FILE kurento-room-docs.version:$S_DIR/kurento-room-docs.version"
FILE="$FILE target/kurento-room-docs-$VERSION.tgz:$S_DIR/kurento-room-docs.tgz"
FILE="$FILE target/kurento-room-docs-$VERSION.tgz:$V_DIR/kurento-room-docs/kurento-room-docs.tgz:1"
FILE="$FILE target/kurento-room-docs-$VERSION.tgz:$S_DIR/kurento-room-docs/kurento-room-docs.tgz:1"


# Path information
BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
PATH="${BASEPATH}:${PATH}"

kurento_builds_publish.sh "$FILE"

echo "FILES=$FILE" > kurento-docs-env.properties
