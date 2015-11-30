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

cat >./.root-config <<EOL
StrictHostKeyChecking no
User jenkins
IdentityFile /opt/id_rsa
EOL

CONTAINER_WORKSPACE=/opt/kurento
docker run \
  --name $BUILD_TAG-MERGE_PROJECT \
  --rm \
  -v $KURENTO_SCRIPTS_HOME:/opt/adm-scripts \
  -v $WORKSPACE:$CONTAINER_WORKSPACE \
  -v $KEY:/opt/id_rsa \
  -v $CERT:/opt/jenkins.crt \
  -v $PWD/.root-config:/root/.ssh/config \
  -e "KURENTO_PROJECT=$KURENTO_PROJECT" \
  -e "BASE_NAME=$BASE_NAME" \
  -e "KURENTO_GIT_REPOSITORY_SERVER=$KURENTO_GIT_REPOSITORY_SERVER" \
  -e "MAVEN_KURENTO_SNAPSHOTS=$MAVEN_KURENTO_SNAPSHOTS" \
  -e "MAVEN_KURENTO_RELEASES=$MAVEN_KURENTO_RELEASES" \
  -e "MAVEN_SONATYPE_NEXUS_STAGING=$MAVEN_SONATYPE_NEXUS_STAGING" \
  -e "KEY=/opt/id_rsa" \
  -e "CERT=/opt/jenkins.crt" \
  -u "root" \
  -w "$CONTAINER_WORKSPACE" \
    kurento/dev-integration:jdk-8-node-0.12 \
      /opt/adm-scripts/kurento_merge_js_project.sh || status=$?

exit $status
