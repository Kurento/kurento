#!/bin/bash -x

# SNAPSHOT_REPOSITORY
# Mandatory
#
# RELEASE_REPOSITORY
# Mandatory
#

PATH=$PATH:${KURENTO_SCRIPTS_HOME}
kurento_check_version.sh true

# Deploy to Kurento repositories
kurento_maven_deploy.sh "$MAVEN_SETTINGS"

# Deploy to Central (only release)
kurento_maven_deploy.sh

# Upload to builds
VERSION=$(kurento_get_version.sh)
echo "$VERSION" > project-version.txt
