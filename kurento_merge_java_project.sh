#!/bin/bash -x

echo "##################### EXECUTE: kurento_merge_java_project #####################"

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

PATH=$PATH:${KURENTO_SCRIPTS_HOME}
kurento_check_version.sh true || exit 1

# Deploy to Kurento repositories
export SNAPSHOT_REPOSITORY=$MAVEN_S3_KURENTO_SNAPSHOTS
export RELEASE_REPOSITORY=$MAVEN_S3_KURENTO_RELEASES
kurento_maven_deploy.sh || exit 1

# Deploy to Central (only release)
export SNAPSHOT_REPOSITORY=
export RELEASE_REPOSITORY=$MAVEN_SONATYPE_NEXUS_STAGING
kurento_maven_deploy.sh || exit 1

# Deploy to builds only when it is release
VERSION=$(kurento_get_version.sh)
if [[ $VERSION != *-SNAPSHOT ]]; then
  # Create version file
  echo "$VERSION - $(date) - $(date +"%Y%m%d-%H%M%S")" > project.version

  [ -n "$FILES" ] && FILES=$FILES kurento_http_publish.sh || echo "No files provided. Skipping."
fi
