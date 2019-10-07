#!/usr/bin/env bash



# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

# Trace all commands
set -o xtrace



log "##################### EXECUTE: kurento_generate_java_module #####################"

# MAVEN_KURENTO_SNAPSHOTS url
#   URL of Kurento repository for maven snapshots
#
# MAVEN_KURENTO_RELEASES url
#   URL of Kurento repository for maven releases
#
# MAVEN_SONATYPE_NEXUS_STAGING url
#   URL of Central staging repositories.
#   Pass it empty to avoid deploying to nexus (private projects):
#   export MAVEN_SONATYPE_NEXUS_STAGING=
#   kurento_merge_java_project.sh

kurento_check_version.sh false || {
  log "ERROR: Command failed: kurento_check_version (tagging disabled)"
  exit 1
}

rm -rf build
mkdir build && cd build
cmake .. -DGENERATE_JAVA_CLIENT_PROJECT=TRUE -DDISABLE_LIBRARIES_GENERATION=TRUE || {
  log "ERROR: Command failed: cmake"
  exit 1
}

cd java || {
  log "ERROR: Expected directory doesn't exist: $PWD/java"
  exit 1
}

# Deploy to Kurento Archiva (both Development and Release versions)
export SNAPSHOT_REPOSITORY=$MAVEN_S3_KURENTO_SNAPSHOTS
export RELEASE_REPOSITORY=$MAVEN_S3_KURENTO_RELEASES
kurento_maven_deploy.sh || {
  log "ERROR: Command failed: kurento_maven_deploy (Kurento)"
  exit 1
}

# Deploy to Maven Central (only Release versions)
export SNAPSHOT_REPOSITORY=
export RELEASE_REPOSITORY=$MAVEN_SONATYPE_NEXUS_STAGING
kurento_maven_deploy.sh || {
  log "ERROR: Command failed: kurento_maven_deploy (Sonatype)"
  exit 1
}

# Only create a tag if the deployment process was successful
# Commented out because this is currently being done in the main kms-{core,elements,filters} job.
# Uncomment when this is sorted out and we know WHEN we want to create tags.
# kurento_check_version.sh true || {
#   log "ERROR: Command failed: kurento_check_version (tagging enabled)"
#   exit 1
# }
