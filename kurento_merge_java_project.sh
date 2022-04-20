#!/usr/bin/env bash

#/ Merge script for Java projects.
#/
#/ Generates Java modules for Maven:
#/ - Uploads Snapshots + Releases to Kurento Archiva.
#/ - Uploads Releases to Sonatype Nexus (for deployment to Maven Central).
#/
#/
#/ Arguments
#/ ---------
#/
#/ --release
#/
#/   Build artifacts intended for Release.
#/   If this option is not given, artifacts are built as nightly snapshots.
#/
#/   Optional. Default: Disabled.
#/
#/ --version-file <File>
#/
#/   Indicates the Maven project file that stores the project's versions.
#/
#/   Optional. Default: "pom.xml".
#/
#/
#/ Environment variables
#/ ---------------------
#/
#/ None.



# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="



# Parse call arguments
# --------------------

CFG_RELEASE="false"
CFG_VERSION_FILE="pom.xml"

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --release)
            CFG_RELEASE="true"
            ;;
        --version-file)
            if [[ -n "${2-}" ]]; then
                CFG_VERSION_FILE="$2"
                shift
            else
                log "ERROR: --version-file expects <File>"
                log "Run with '--help' to read usage details"
                exit 1
            fi
            ;;
        *)
            log "ERROR: Unknown argument '${1-}'"
            log "Run with '--help' to read usage details"
            exit 1
            ;;
    esac
    shift
done

log "CFG_RELEASE=${CFG_RELEASE}"
log "CFG_VERSION_FILE=${CFG_VERSION_FILE}"



# Verify project structure
# ------------------------

[[ -f pom.xml ]] || {
    log "ERROR: File not found: pom.xml"
    exit 1
}

kurento_check_version.sh false || {
    log "ERROR: Command failed: kurento_check_version (tagging disabled)"
    exit 1
}



# Release
# -------

# TODO FIXME This won't work for all Java projects!
# The commands for tutorial are different...
# Review: Maybe it's better to do locally

# if [[ "$CFG_RELEASE" == "true" ]]; then
#     # Drop the SNAPSHOT suffix
#     mvn --batch-mode versions:set -DgenerateBackupPoms=false \
#         -DremoveSnapshot=true \
#         --file "$CFG_VERSION_FILE"
#
#     # Release data
#     RELEASE_VERSION="$(kurento_get_version.sh)"
#     RELEASE_COMMIT_MSG="Prepare release $RELEASE_VERSION"
#
#     # Commit version change
#     git ls-files --modified | grep 'pom.xml' | xargs -r git add
#     git commit -m "$RELEASE_COMMIT_MSG"
# fi



# Deploy project
# --------------

kurento_maven_deploy.sh || {
  log "ERROR: Command failed: kurento_maven_deploy"
  exit 1
}

# Deploy to Kurento Builds only when it is release
VERSION="$(kurento_get_version.sh)"
if [[ $VERSION != *-SNAPSHOT ]]; then
  log "Version is RELEASE, HTTP publish"

  if [[ -n "$FILES" ]]; then
    log "$VERSION - $(date) - $(date +"%Y%m%d-%H%M%S")" > project.version
    log "Command: kurento_http_publish"
    FILES=$FILES kurento_http_publish.sh
  else
    log "Skip HTTP publish: No FILES provided"
  fi
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
