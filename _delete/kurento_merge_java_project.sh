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
#/ None.
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

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        *)
            log "ERROR: Unknown argument '${1-}'"
            log "Run with '--help' to read usage details"
            exit 1
            ;;
    esac
    shift
done



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



# Deploy project
# --------------

kurento_maven_deploy.sh || {
  log "ERROR: Command failed: kurento_maven_deploy"
  exit 1
}



# Only create a tag if the deployment process was successful
# Allow errors because the tag might already exist (like if the release
# is being done again after solving some deployment issue).
kurento_check_version.sh true || {
  log "WARNING: Command failed: kurento_check_version (tagging enabled)"
}



log "==================== END ===================="
