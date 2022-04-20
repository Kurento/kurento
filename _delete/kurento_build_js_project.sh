#!/bin/bash -x
#
echo "##################### EXECUTE: kurento_build_js_project #####################"

# Build JS artifacts and deploy to download service
#
# Following variables will be read from script
# BASE_NAME
#   Mandatory
#   This is the basename of the project. It can be found in package.json
#
# GERRIT_REFSPEC
#   Optional
#   Name of the branch where artifacts are build

[ -z "$BASE_NAME" ] && exit 1
# If no checkout reference, use the repo default branch.
[ -z "$GERRIT_REFSPEC" ] && GERRIT_REFSPEC="$(kurento_git_default_branch.sh)"
export PROJECT_NAME=$BASE_NAME-js
kurento_mavenize_js_project.sh

# Build project
MAVEN_GOALS="clean package assembly:single" \
MAVEN_OPTIONS="-Dmaven.test.skip=true" \
kurento_mvn.sh

# Upload artifacts to builds
VERSION=$(xmlstarlet sel -N x=http://maven.apache.org/POM/4.0.0 -t -v "/x:project/x:version" pom.xml) || exit 1
DATE=$(date +"%Y%m%d")
V_DIR=/dev/$GERRIT_REFSPEC/$DATE
S_DIR=/dev/$GERRIT_REFSPEC/latest

echo "$VERSION - $(date) - $(date +"%Y%m%d-%H%M%S")" > $PROJECT_NAME.version

FILES=""
FILES="$FILES dist/$BASE_NAME.js:upload/$V_DIR/js/$BASE_NAME.js"
FILES="$FILES dist/$BASE_NAME.js:upload/$S_DIR/js/$BASE_NAME.js"
FILES="$FILES dist/$BASE_NAME.min.js:upload/$V_DIR/js/$BASE_NAME.min.js"
FILES="$FILES dist/$BASE_NAME.min.js:upload/$S_DIR/js/$BASE_NAME.min.js"
FILES="$FILES dist/$BASE_NAME.map:upload/$V_DIR/js/$BASE_NAME.map"
FILES="$FILES dist/$BASE_NAME.map:upload/$S_DIR/js/$BASE_NAME.map"
FILES="$FILES target/$PROJECT_NAME-$VERSION.zip:upload/$V_DIR/$PROJECT_NAME-$VERSION.zip"
FILES="$FILES target/$PROJECT_NAME-$VERSION.zip:upload/$S_DIR/$PROJECT_NAME.zip"
FILES="$FILES LICENSE:upload/$V_DIR/LICENSE"
FILES="$FILES LICENSE:upload/$S_DIR/LICENSE"
FILES="$FILES $PROJECT_NAME.version:upload/$V_DIR/$PROJECT_NAME.version"
FILES="$FILES $PROJECT_NAME.version:upload/$S_DIR/$PROJECT_NAME.version"

FILES=$FILES kurento_http_publish.sh
