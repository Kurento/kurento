#!/bin/bash -x
echo "##################### EXECUTE: kurento_merge_js_project_container #####################"

# KURENTO_PROJECT string
#   Name of the project to be merged
#
# BASE_NAME
#   Optional
#   Name of the artifact.
#   Default: PROJECT_NAME
#
# KURENTO_GIT_REPOSITORY_SERVER string
#   URL of Kurento code repository
#
# MAVEN_KURENTO_SNAPSHOTS url
#   URL of Kurento repository for maven snapshots
#
# MAVEN_KURENTO_RELEASES url
#   URL of Kurento repository for maven releases
#
# MAVEN_SONATYPE_NEXUS_STAGING url
#   URL of Central staging repositories
#
# BOWER_REPOSITORY url
#   URL to bower repository

# Verify mandatory parameters
[ -z "$KURENTO_PROJECT" ] && exit 1
[ -z "$KURENTO_GIT_REPOSITORY_SERVER" ] && exit 1

[ -z "$BASE_NAME" ] && BASE_NAME=$KURENTO_PROJECT

# Verify project structure
[ -f package.json ] || exit 1

# Parameters relative to container filesystem
CONTAINER_KEY=/opt/id_rsa
CONTAINER_CERT=/opt/jenkins.crt
CONTAINER_MAVEN_SETTINGS=/opt/kurento-settings.xml
CONTAINER_ADM_SCRIPTS=/opt/adm-scripts

cat >./.root-config <<EOL
StrictHostKeyChecking no
User jenkins
IdentityFile $CONTAINER_KEY
EOL

CONTAINER_WORKSPACE=/opt/kurento
docker run \
  --name $BUILD_TAG-MERGE_PROJECT \
  --rm \
  -v $KURENTO_SCRIPTS_HOME:$CONTAINER_ADM_SCRIPTS \
  -v $WORKSPACE:$CONTAINER_WORKSPACE \
  -v $MAVEN_SETTINGS:$CONTAINER_MAVEN_SETTINGS \
  -v $KEY:$CONTAINER_KEY \
  -v $CERT:$CONTAINER_CERT \
  -v $PWD/.root-config:/root/.ssh/config \
  -e "KURENTO_PROJECT=$KURENTO_PROJECT" \
  -e "BASE_NAME=$BASE_NAME" \
  -e "MAVEN_SETTINGS=$CONTAINER_MAVEN_SETTINGS"
  -e "KURENTO_GIT_REPOSITORY_SERVER=$KURENTO_GIT_REPOSITORY_SERVER" \
  -e "MAVEN_KURENTO_SNAPSHOTS=$MAVEN_KURENTO_SNAPSHOTS" \
  -e "MAVEN_KURENTO_RELEASES=$MAVEN_KURENTO_RELEASES" \
  -e "MAVEN_SONATYPE_NEXUS_STAGING=$MAVEN_SONATYPE_NEXUS_STAGING" \
  -e "KEY=$CONTAINER_KEY" \
  -e "CERT=/opt/jenkins.crt" \
  -u "root" \
  -w "$CONTAINER_WORKSPACE" \
    kurento/dev-integration:jdk-8-node-0.12 \
      /opt/adm-scripts/kurento_merge_js_project.sh || status=$?

exit $status
