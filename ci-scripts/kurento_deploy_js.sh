#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ Deploy JavaScript packages.



# Configure shell
# ===============

SELF_DIR="$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null && pwd -P)"
source "$SELF_DIR/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="
trap_add 'log "==================== END ===================="' EXIT

# Trace all commands (to stderr).
set -o xtrace



# Parse call arguments
# ====================

CFG_MAVEN_SETTINGS_PATH=""

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --maven-settings)
            if [[ -n "${2-}" ]]; then
                CFG_MAVEN_SETTINGS_PATH="$(realpath "$2")"
                shift
            else
                log "ERROR: --maven-settings expects <Path>"
                exit 1
            fi
            ;;
        *)
            log "ERROR: Unknown argument '${1-}'"
            exit 1
            ;;
    esac
    shift
done



# Validate config
# ===============

log "CFG_MAVEN_SETTINGS_PATH=$CFG_MAVEN_SETTINGS_PATH"



# Verify project
# ==============

[[ -f package.json ]] || {
    log "ERROR: File not found: package.json"
    exit 1
}

kurento_check_version.sh || {
    log "ERROR: Command failed: kurento_check_version"
    exit 1
}



# Deploy for NPM
# ==============

kurento_deploy_js_npm.sh

# Finish if the project is one of the main client modules, which get loaded by
# kurento-client directly as NPM dependencies:
# kurento-client-(core|elements|filters)
# They should not be available independently in Maven or Bower.
{
    PROJECT_NAME="$(kurento_get_name.sh)" || {
        echo "ERROR: Command failed: kurento_get_name"
        exit 1
    }

    case "$PROJECT_NAME" in
        kurento-client-core|kurento-client-elements|kurento-client-filters)
            log "Skip deploying: Project is internal client dependency"
            exit 0
            ;;
    esac
}



# Deploy for Maven
# ================

kurento_mavenize_js_project.sh
kurento_maven_deploy.sh --maven-settings "$CFG_MAVEN_SETTINGS_PATH"



# TODO: Publish for Bower and HTTP.
exit 0



# Deploy for Bower
# ================

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



# Deploy for HTTP linking
# =======================

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
