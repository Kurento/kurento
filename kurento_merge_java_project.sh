#!/bin/bash -x
PATH=$PATH:${KURENTO_SCRIPTS_HOME}
kurento_check_version.sh true

# Deploy to Kurento repositories
export SNAPSHOT_REPOSITORY=$MAVEN_KURENTO_SNAPSHOTS
export RELEASE_REPOSITORY=$MAVEN_KURENTO_RELEASES
kurento_maven_deploy.sh "$MAVEN_SETTINGS"

# Deploy to Central (only release)
export SNAPSHOT_REPOSITORY=
export RELEASE_REPOSITORY=$MAVEN_SONATYPE_NEXUS_STAGING
kurento_maven_deploy.sh

# Upload to builds
VERSION=$(kurento_get_version.sh)
echo "$VERSION" > project-version.txt
