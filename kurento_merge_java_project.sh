#!/bin/bash -x

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
kurento_check_version.sh true

# Deploy to Kurento repositories
export SNAPSHOT_REPOSITORY=$MAVEN_KURENTO_SNAPSHOTS
export RELEASE_REPOSITORY=$MAVEN_KURENTO_RELEASES
kurento_maven_deploy.sh

# Deploy to Central (only release)
export SNAPSHOT_REPOSITORY=
export RELEASE_REPOSITORY=$MAVEN_SONATYPE_NEXUS_STAGING
kurento_maven_deploy.sh

# Deploy to builds only when it is release
VERSION=$(kurento_get_version.sh)
if [[ $VERSION != *-SNAPSHOT ]]; then
  # Create version file
  echo "$VERSION - $(date) - $(date +"%Y%m%d-%H%M%S")" > project.version

  export FILES
  kurento_http_publish.sh
fi
