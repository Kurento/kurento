#!/bin/bash -x
echo "##################### EXECUTE: kurento_ci_container_job_setup #####################"

# KURENTO_GIT_REPOSITORY_SERVER string
#   URL of Kurento code repository
#
# BUILD_COMMAND
#   Command to run in the container after initialization
#
# CERT
#   Jenkins certificate to upload artifacts to http services
#
# GNUPK_KEY
#   Private GNUPG key used to sign kurento artifacts
#
# KEY
#   Gerrit ssh key
#


# Verify mandatory parameters
[ -z "$KURENTO_PROJECT" ] && KURENTO_PROJECT=$GERRIT_PROJECT
[ -z "$KURENTO_GIT_REPOSITORY_SERVER" ] && exit 1

[ -z "$BASE_NAME" ] && BASE_NAME=$KURENTO_PROJECT

[ -z "$BUILD_COMMAND" ] && BUILD_COMMAND="kurento_merge_js_project.sh"

# Set default Parameters
STATUS=0
[ -z "$WORKSPACE" ] && WORKSPACE="."

# Parameters relative to container filesystem
CONTAINER_WORKSPACE=/opt/kurento
CONTAINER_KEY=/opt/id_rsa
CONTAINER_CERT=/opt/jenkins.crt
CONTAINER_MAVEN_SETTINGS=/opt/kurento-settings.xml
CONTAINER_ADM_SCRIPTS=/opt/adm-scripts
CONTAINER_GIT_CONFIG=/root/.gitconfig
CONTAINER_GNUPG_KEY=/opt/gnupg_key

docker run \
  --name $BUILD_TAG-MERGE_PROJECT \
  --rm \
  -v $KURENTO_SCRIPTS_HOME:$CONTAINER_ADM_SCRIPTS \
  -v $WORKSPACE:$CONTAINER_WORKSPACE \
  -v $MAVEN_SETTINGS:$CONTAINER_MAVEN_SETTINGS \
  $([ -f "$CERT" ] && echo "-v $CERT:$CONTAINER_CERT") \
  $([ -f "$KEY" ] && echo "-v $KEY:$CONTAINER_KEY" ) \
  $([ -f "$GIT_CONFIG" ] && echo "-v $GIT_CONFIG:$CONTAINER_GIT_CONFIG") \
  $([ -f "$GNUPG_KEY" ] && echo "-v $GNUPG_KEY:$CONTAINER_GNUPG_KEY") \
  -e "KURENTO_PROJECT=$KURENTO_PROJECT" \
  -e "BASE_NAME=$BASE_NAME" \
  -e "MAVEN_SETTINGS=$CONTAINER_MAVEN_SETTINGS" \
  -e "KURENTO_GIT_REPOSITORY_SERVER=$KURENTO_GIT_REPOSITORY_SERVER" \
  -e "MAVEN_KURENTO_SNAPSHOTS=$MAVEN_KURENTO_SNAPSHOTS" \
  -e "MAVEN_KURENTO_RELEASES=$MAVEN_KURENTO_RELEASES" \
  -e "MAVEN_SONATYPE_NEXUS_STAGING=$MAVEN_SONATYPE_NEXUS_STAGING" \
  -e "MAVEN_SHELL_SCRIPT=$MAVEN_SHELL_SCRIPT" \
  -e "ASSEMBLY_FILE=$ASSEMBLY_FILE" \
  -e "BOWER_REPOSITORY=$BOWER_REPOSITORY" \
  -e "FILES=$FILES" \
  -e "BUILDS_HOST=$BUILDS_HOST" \
  -e "KEY=$CONTAINER_KEY" \
  -e "CERT=$CONTAINER_CERT" \
  -e "GNUPG_KEY=$CONTAINER_GNUPG_KEY" \
  -e "CREATE_TAG=$CREATE_TAG" \
  -e "GERRIT_HOST=$GERRIT_HOST" \
  -e "GERRIT_PORT=$GERRIT_PORT" \
  -e "GERRIT_USER=$GERRIT_USER" \
  -e "GERRIT_PROJECT=$GERRIT_PROJECT" \
  -e "GERRIT_NEWREV=$GERRIT_NEWREV" \
  -u "root" \
  -w "$CONTAINER_WORKSPACE" \
    kurento/dev-integration:jdk-8-node-0.12 \
      /opt/adm-scripts/kurento_ci_container_entrypoint.sh $BUILD_COMMAND
