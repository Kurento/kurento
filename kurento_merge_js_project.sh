#!/usr/bin/env bash

# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="

# Trace all commands
set -o xtrace



# KURENTO_PROJECT string
#   Name of the project to be merged
#
# BOWER_REPO_NAME string
#   Name of the Git repo which contains Bower files

# Path information
BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
PATH="${BASEPATH}:${PATH}"

[ -f package.json ] || {
  log "ERROR: File not found: package.json"
  exit 1
}

kurento_check_version.sh false || {
  log "ERROR: Command failed: kurento_check_version (tagging disabled)"
  exit 1
}

# Deploy to NPM
kurento_npm_publish.sh

# Finish if the project is one of the main client modules:
# "kurento-client-{core,elements,filters}-js" get loaded directly by
# kurento-client via NPM. They should not be available independently in Maven
# or Bower.
case "$KURENTO_PROJECT" in
kurento-client-core-js \
| kurento-client-elements-js \
| kurento-client-filters-js)
    # Only create a tag if the deployment process was successful.
    # Allow errors because the tag might already exist (like if the release
    # is being done again after solving some deployment issue).
    kurento_check_version.sh true || {
      log "WARNING: Command failed: kurento_check_version (tagging enabled)"
    }
    exit 0
    ;;
esac

# Convert into a valid Maven artifact
kurento_mavenize_js_project.sh "$KURENTO_PROJECT" || {
  log "ERROR: Command failed: kurento_mavenize_js_project"
  exit 1
}



# Deploy project
# --------------

kurento_maven_deploy.sh || {
  log "ERROR: Command failed: kurento_maven_deploy"
  exit 1
}

# Deploy to Bower repository
[ -z "$BASE_NAME" ] && BASE_NAME="$KURENTO_PROJECT"
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

export FILES
CREATE_TAG="true" kurento_bower_publish.sh

# Deploy to builds only when it is release
VERSION="$(kurento_get_version.sh)" || {
  log "ERROR: Command failed: kurento_get_version"
  exit 1
}
if [[ $VERSION != *-SNAPSHOT ]]; then
  log "Version is RELEASE, HTTP publish"

  V_DIR="/release/$VERSION"
  S_DIR="/release/stable"

  # Create kws version file
  echo "$VERSION - $(date) - $(date +"%Y%m%d-%H%M%S")" > "${KURENTO_PROJECT}.version"

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
else
  log "Skip HTTP publish: Version is SNAPSHOT"
fi

# Only create a tag if the deployment process was successful
# Allow errors because the tag might already exist (like if the release
# is being done again after solving some deployment issue).
kurento_check_version.sh true || {
  log "WARNING: Command failed: kurento_check_version (tagging enabled)"
}



log "==================== END ===================="
