#!/bin/bash -x
echo "##################### EXECUTE: kurento_merge_js_project #####################"

# KURENTO_PROJECT string
#   Name of the project to be merged
#
# KURENTO_GIT_REPOSITORY_SERVER string
#   URL of Kurento code repository
#
# MAVEN_KURENTO_SNAPSHOTS url
#   URL of Kurento repository for maven snapshots
#
# MAVEN_KURENTO_RELEASES url
#   URL of Kurento repository for maven releases
#
# MAVEN_SONATYPE_NEXUS_STAGING url
#   URL of Central staging repositories
#
# BOWER_REPOSITORY url
#   URL to bower repository

PATH=$PATH:$(realpath $(dirname "$0"))

# Verify mandatory parameters
[ -z "$KURENTO_PROJECT" ] && exit 1
[ -z "$KURENTO_GIT_REPOSITORY_SERVER" ] && exit 1
[ -z "$KEY" ] && echo "No key specified"
[ -z "$CERT" ] && echo "No cert specified"

# Verify project structure
[ -f package.json ] || exit 1

# Activate ssh-agent and add private key
eval `ssh-agent -s` > /dev/null
ssh-add $KEY

# Deploy to maven repository
kurento_check_version.sh true
kurento_mavenize_js_project.sh $KURENTO_PROJECT
# Deploy to snapshot or kurento release
export KEY
export CERT
export SNAPSHOT_REPOSITORY=$MAVEN_KURENTO_SNAPSHOTS
export RELEASE_REPOSITORY=$MAVEN_KURENTO_RELEASES
kurento_maven_deploy.sh
# Deploy to Maven Central
export SNAPSHOT_REPOSITORY=
export RELEASE_REPOSITORY=$MAVEN_SONATYPE_NEXUS_STAGING
kurento_maven_deploy.sh

# Deploy to NPM
kurento_npm_publish.sh

# Deploy to bower repository
[ -z "$BASE_NAME" ] && BASE_NAME=$KURENTO_PROJECT
# Select files to be moved to bower repository
FILES=""
FILES="$FILES dist/$BASE_NAME.js:js/$BASE_NAME.js"
FILES="$FILES dist/$BASE_NAME.min.js:js/$BASE_NAME.min.js"
FILES="$FILES dist/$BASE_NAME.map:js/$BASE_NAME.map"
# README_bower.md is optional
[ -f README_bower.md ] && FILES="$FILES README_bower.md:README.md"
# bower.json is optional
[ -f bower.json ] && FILES="$FILES bower.json:bower.json"
# LICENSE is optional
[ -f LICENSE ] && FILES="$FILES LICENSE:LICENSE"

export BOWER_REPOSITORY
export FILES
export CREATE_TAG=true
kurento_bower_publish.sh

# Deploy to builds only when it is release
VERSION=$(kurento_get_version.sh)
if [[ $VERSION != *-SNAPSHOT ]]; then
  V_DIR=/release/$VERSION
  S_DIR=/release/stable

  # Create kws version file
  echo "$VERSION - $(date) - $(date +"%Y%m%d-%H%M%S")" > $KURENTO_PROJECT.version

  # Create kws environment file
  FILES=""
  FILES="$FILES dist/$BASE_NAME.js:upload/$V_DIR/js/$BASE_NAME.js"
  FILES="$FILES dist/$BASE_NAME.js:upload/$S_DIR/js/$BASE_NAME.js"
  FILES="$FILES dist/$BASE_NAME.min.js:upload/$V_DIR/js/$BASE_NAME.min.js"
  FILES="$FILES dist/$BASE_NAME.min.js:upload/$S_DIR/js/$BASE_NAME.min.js"
  FILES="$FILES dist/$BASE_NAME.map:upload/$V_DIR/js/$BASE_NAME.map"
  FILES="$FILES dist/$BASE_NAME.map:upload/$S_DIR/js/$BASE_NAME.map"
  FILES="$FILES target/$KURENTO_PROJECT-$VERSION.zip:upload/$V_DIR/$KURENTO_PROJECT-$VERSION.zip"
  FILES="$FILES target/$KURENTO_PROJECT-$VERSION.zip:upload/$S_DIR/$KURENTO_PROJECT.zip"
  FILES="$FILES LICENSE:upload/$V_DIR/LICENSE"
  FILES="$FILES LICENSE:upload/$S_DIR/LICENSE"
  FILES="$FILES $KURENTO_PROJECT.version:upload/$V_DIR/$KURENTO_PROJECT.version"
  FILES="$FILES $KURENTO_PROJECT.version:upload/$S_DIR/$KURENTO_PROJECT.version"

  export FILES
  kurento_http_publish.sh
fi
