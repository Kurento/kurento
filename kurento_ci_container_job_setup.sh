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
# START_MONGO_CONTAINER [ true | false ]
#    Optional
#    Specifies if a MongoDB container must be started and linked to the
#    maven container. Hostname mongo will be used
#    DEFAULT false
#
# START_KMS_CONTAINER [ true | false ]
#    Optional
#    Specifies if a KMS container must be started and linked to the
#    maven container. Hostname kms will be used
#    DEFAULT false
#

# Constants
CONTAINER_WORKSPACE=/opt/kurento
CONTAINER_KEY=/opt/id_rsa
CONTAINER_CERT=/opt/jenkins.crt
CONTAINER_MAVEN_SETTINGS=/opt/kurento-settings.xml
CONTAINER_ADM_SCRIPTS=/opt/adm-scripts
CONTAINER_GIT_CONFIG=/root/.gitconfig
CONTAINER_GNUPG_KEY=/opt/gnupg_key

# Verify mandatory parameters
[ -z "$KURENTO_PROJECT" ] && KURENTO_PROJECT=$GERRIT_PROJECT
[ -z "$KURENTO_GIT_REPOSITORY_SERVER" ] && exit 1
[ -z "$BASE_NAME" ] && BASE_NAME=$KURENTO_PROJECT

[ -z "$BUILD_COMMAND" ] && BUILD_COMMAND="kurento_merge_js_project.sh"

# Set default Parameters
STATUS=0
[ -z "$WORKSPACE" ] && WORKSPACE="."
[ -z "$KMS_AUTOSTART" ] && KMS_AUTOSTART="test"
[ -z "$KMS_SCOPE" ] && KMS_SCOPE="docker"
[ -z "$MAVEN_GOALS" ] && MAVEN_GOALS="verify"
[ -z "$MAVEN_LOCAL_REPOSITORY" ] && MAVEN_LOCAL_REPOSITORY="$WORKSPACE/m2"
[ -z "$RECORD_TEST" ] && RECORD_TEST="false"

# Create temporary folders
[ -d $WORKSPACE/tmp ] || mkdir -p $WORKSPACE/tmp
[ -d $MAVEN_LOCAL_REPOSITORY ] || mkdir -p $MAVEN_LOCAL_REPOSITORY

# Verify if Mongo container must be started
if [ "$START_MONGO_CONTAINER" == 'true' ]; then
    MONGO_CONTAINER_ID=$(docker run -d \
      --name $BUILD_TAG-MONGO \
      mongo:2.6.11)
    # Guard time for mongo startup
    sleep 10
fi

# Verify if Mongo container must be started
if [ "$START_KMS_CONTAINER" == 'true' ]; then
    KMS_CONTAINER_ID=$(docker run -d \
      --name $BUILD_TAG-MONGO \
      kurento/kurento-media-server-dev:latest)
fi


# Set maven options
MAVEN_OPTIONS="$MAVEN_OPTIONS -Dtest.kms.docker.image.forcepulling=false"
MAVEN_OPTIONS="$MAVEN_OPTIONS -Djava.awt.headless=true"
MAVEN_OPTIONS="$MAVEN_OPTIONS -Dtest.kms.autostart=$KMS_AUTOSTART"
MAVEN_OPTIONS="$MAVEN_OPTIONS -Dtest.kms.scope=$KMS_SCOPE"
[ -n "$KMS_WS_URI" ] && MAVEN_OPTIONS="$MAVEN_OPTIONS -Dkms.ws.uri=$KMS_WS_URI"
MAVEN_OPTIONS="$MAVEN_OPTIONS -Dproject.path=$TEST_HOME$([ -n "$PROJECT_MODULE" ] && echo "/$PROJECT_MODULE")"
MAVEN_OPTIONS="$MAVEN_OPTIONS -Dtest.workspace=$TEST_HOME/tmp"
MAVEN_OPTIONS="$MAVEN_OPTIONS -Dtest.workspace.host=$WORKSPACE/tmp"
MAVEN_OPTIONS="$MAVEN_OPTIONS -Dtest.files=/opt/test-files/kurento"
[ -n "$DOCKER_HUB_IMAGE" ] && MAVEN_OPTIONS="$MAVEN_OPTIONS -Ddocker.hub.image=$DOCKER_HUB_IMAGE"
[ -n "$DOCKER_NODE_KMS_IMAGE" ] && MAVEN_OPTIONS="$MAVEN_OPTIONS -Dtest.kms.docker.image.name=$DOCKER_NODE_KMS_IMAGE"
[ -n "$DOCKER_NODE_CHROME_IMAGE" ] && MAVEN_OPTIONS="$MAVEN_OPTIONS -Ddocker.node.chrome.image=$DOCKER_NODE_CHROME_IMAGE"
[ -n "$DOCKER_NODE_FIREFOX_IMAGE" ] && MAVEN_OPTIONS="$MAVEN_OPTIONS -Ddocker.node.firefox.image=$DOCKER_NODE_FIREFOX_IMAGE"
MAVEN_OPTIONS="$MAVEN_OPTIONS -Dtest.selenium.scope=docker"
MAVEN_OPTIONS="$MAVEN_OPTIONS -Dtest.selenium.record=$RECORD_TEST"
MAVEN_OPTIONS="$MAVEN_OPTIONS -Dwdm.chromeDriverUrl=http://chromedriver.kurento.org/"
[ -n "$TEST_GROUP" ] && MAVEN_OPTIONS="$MAVEN_OPTIONS -Dgroups=$TEST_GROUP"
[ -n "$TEST_NAME" ] && MAVEN_OPTIONS="$MAVEN_OPTIONS -Dtest=$TEST_NAME"
[ -n "$BOWER_RELEASE_URL" ] && MAVEN_OPTIONS="$MAVEN_OPTIONS -Dbower.release.url=$BOWER_RELEASE_URL"
[ -n "$MONGO_CONTAINER_ID" ] && MAVEN_OPTIONS="$MAVEN_OPTIONS -Drepository.mongodb.urlConn=mongodb://mongo"

# Create main container
docker run \
  --name $BUILD_TAG-MERGE_PROJECT \
  --rm \
  -v $KURENTO_SCRIPTS_HOME:$CONTAINER_ADM_SCRIPTS \
  -v $WORKSPACE$([ -n "$PROJECT_DIR" ] && echo "/$PROJECT_DIR"):$CONTAINER_WORKSPACE \
  $([ -f "$MAVEN_SETTINGS" ] && echo "-v $MAVEN_SETTINGS:$CONTAINER_MAVEN_SETTINGS") \
  -v $WORKSPACE/tmp:$CONTAINER_WORKSPACE/tmp \
  $([ -f "$CERT" ] && echo "-v $CERT:$CONTAINER_CERT") \
  $([ -f "$KEY" ] && echo "-v $KEY:$CONTAINER_KEY" ) \
  $([ -f "$GIT_CONFIG" ] && echo "-v $GIT_CONFIG:$CONTAINER_GIT_CONFIG") \
  $([ -f "$GNUPG_KEY" ] && echo "-v $GNUPG_KEY:$CONTAINER_GNUPG_KEY") \
  -e "KURENTO_PROJECT=$KURENTO_PROJECT" \
  -e "BASE_NAME=$BASE_NAME" \
  -e "KURENTO_GIT_REPOSITORY_SERVER=$KURENTO_GIT_REPOSITORY_SERVER" \
  -e "MAVEN_GOALS=$MAVEN_GOALS" \
  -e "MAVEN_KURENTO_SNAPSHOTS=$MAVEN_KURENTO_SNAPSHOTS" \
  -e "MAVEN_KURENTO_RELEASES=$MAVEN_KURENTO_RELEASES" \
  -e "MAVEN_MODULE=$MAVEN_MODULE" \
  -e "MAVEN_OPTIONS=$MAVEN_OPTIONS" \
  -e "MAVEN_SETTINGS=$CONTAINER_MAVEN_SETTINGS" \
  -e "MAVEN_SHELL_SCRIPT=$MAVEN_SHELL_SCRIPT" \
  -e "MAVEN_SONATYPE_NEXUS_STAGING=$MAVEN_SONATYPE_NEXUS_STAGING" \
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
  -e "WORKSPACE=$CONTAINER_WORKSPACE" \
  $([ -n "$MONGO_CONTAINER_ID" ] && echo "--link $MONGO_CONTAINER_ID:mongo") \
  $([ -n "$KMS_CONTAINER_ID" ] && echo "--link $KMS_CONTAINER_ID:kms") \
  -u "root" \
  -w "$CONTAINER_WORKSPACE" \
    kurento/dev-integration:jdk-8-node-0.12 \
      /opt/adm-scripts/kurento_ci_container_entrypoint.sh $BUILD_COMMAND || STATUS=$?

# Stop detached containers if started
# MONGO
[ -n "$MONGO_CONTAINER_ID" ] && \
    mkdir -p $WORKSPACE/report-files && \
    docker logs $MONGO_CONTAINER_ID > $WORKSPACE/report-files/external-mongodb.log && \
    zip $WORKSPACE/report-files/external-mongodb.log.zip $WORKSPACE/report-files/external-mongodb.log && \
    docker stop $MONGO_CONTAINER_ID && docker rm -v $MONGO_CONTAINER_ID
# KMS
[ -n "$KMS_CONTAINER_ID" ] && \
  mkdir -p $WORKSPACE/report-files && \
  docker logs $MONGO_CONTAINER_ID > $WORKSPACE/report-files/external-kms.log && \
  zip $WORKSPACE/report-files/external-kms.log.zip $WORKSPACE/report-files/external-kms.log && \
  docker stop $KMS_CONTAINER_ID && docker rm -v $KMS_CONTAINER_ID

exit $STATUS
