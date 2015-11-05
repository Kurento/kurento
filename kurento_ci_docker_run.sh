#!/bin/bash

# This tool uses a set of variables expected to be exported by tester
# TEST_GROUP string
#    Mandatory
#    Identifies the test category to run
#
# TEST_NAME regexp
#    Optional
#    Identifies the tests within the category to run. Wildcards can be used.
#    When no present, all tests within the category are run
#
# PROJECT_PATH string
#    Optional
#    Identifies the module to execute within a reactor project.
#
# WORKSPACE path
#    Mandatory
#    Jenkins workspace path. This variable is expected to be exported by
#    script caller.
#
# MAVEN_SETTINGS path
#     Mandatory
#     Location of the settings.xml file used by maven
#
# RECORD_TEST [ true | false ]
#    Optional
#    Activates session recording in case ffmpeg is available
#    DEFAULT: false
#
# KMS_AUTOSTART [ false | test | testsuite ]
#    Optional
#    How will kms be managed from tests
#    DEFAULT: test
#
# KMS_SCOPE [ local | docker ]
#    Optional
#    The scope for kms when KMS_AUTOSTART==test || KMS_AUTOSTART==testsuite
#    DEFAULT: docker
#
# KMS_WS_URI url
#    Optional
#    URL where kms can be reached. Only needed when KMS_AUTOSTART==false.
#    KMS should be reacheble from within the containers.
#

[ -z "$TEST_GROUP" ] && echo "Running all tests, as TEST_GROUP was not specify"
[ -z "$TEST_NAME" ] && echo "Running all tests, as TEST_NAME was not specify"
[ -z "$PROJECT_PATH" ] && PROJECT_PATH="kurento-integration-tests/kurento-test"
[ -z "$RECORD_TEST" ] && RECORD_TEST="false"
[ -z "$KMS_AUTOSTART" ] && KMS_AUTOSTART="test"
[ -z "$KMS_SCOPE" ] && KMS_SCOPE="docker"

# Set constants and environment

# Create test files container
TEST_FILES_NAME="$BUILD_TAG-TEST-FILES"
docker run \
  --rm \
	--name $TEST_FILES_NAME \
    -v /var/lib/jenkins/test-files:/var/lib/jenkins/test-files \
    -w /var/lib/jenkins/test-files \
     kurento/svn-client:1.0.0 svn checkout http://files.kurento.org/svn/kurento

# Create temporary folder for container
TEST_WORKSPACE=$WORKSPACE/tmp
mkdir $TEST_WORKSPACE

# Craete Integration container
TEST_HOME=/opt/kurento-java

MAVEN_OPTS=""
MAVEN_OPTS="$MAVEN_OPTS -Dtest.kms.docker.image.forcepulling=false"
MAVEN_OPTS="$MAVEN_OPTS -Djava.awt.headless=true"
MAVEN_OPTS="$MAVEN_OPTS -Dwdm.chromeDriverUrl=http://chromedriver.kurento.org/"
MAVEN_OPTS="$MAVEN_OPTS -Dtest.kms.autostart=$KMS_AUTOSTART"
MAVEN_OPTS="$MAVEN_OPTS -Dtest.kms.scope=$KMS_SCOPE"
[ -n "$KMS_WS_URI" ] && MAVEN_OPTS="$MAVEN_OPTS -Dkms.ws.uri=$KMS_WS_URI"
[ -n "$PROJECT_PATH" ] && MAVEN_OPTS="$MAVEN_OPTS -Dproject.path=$TEST_HOME/$PROJECT_PATH"
MAVEN_OPTS="$MAVEN_OPTS -Dtest.workspace=$TEST_HOME/tmp"
MAVEN_OPTS="$MAVEN_OPTS -Dtest.workspace.host=$TEST_WORKSPACE"
MAVEN_OPTS="$MAVEN_OPTS -Dtest.files=/var/lib/test-files/kurento"
MAVEN_OPTS="$MAVEN_OPTS -Dtest.kms.docker.image.name=kurento/kurento-media-server-dev:latest"
MAVEN_OPTS="$MAVEN_OPTS -Dtest.selenium.scope=docker"
[ -n "$DOCKER_HUB_IMAGE" ] && MAVEN_OPTS="$MAVEN_OPTS -Ddocker.hub.image=$DOCKER_HUB_IMAGE"
[ -n "$DOCKER_NODE_CHROME_IMAGE" ] && MAVEN_OPTS="$MAVEN_OPTS -Ddocker.node.chrome.image=$DOCKER_NODE_CHROME_IMAGE"
[ -n "$DOCKER_NODE_FIREFOX_IMAGE" ] && MAVEN_OPTS="$MAVEN_OPTS -Ddocker.node.firefox.image=$DOCKER_NODE_FIREFOX_IMAGE"
MAVEN_OPTS="$MAVEN_OPTS -Dtest.selenium.record=$RECORD_TEST"
[ -n "$TEST_GROUP" ] && MAVEN_OPTS="$MAVEN_OPTS -Dgroups=$TEST_GROUP"
[ -n "$TEST_NAME" ] && MAVEN_OPTS="$MAVEN_OPTS -Dtest=$TEST_NAME"

# Execute Presenter test
docker run --rm \
  --name $BUILD_TAG-INTEGRATION \
  -v /var/lib/jenkins/test-files:/var/lib/test-files \
  -v $MAVEN_SETTINGS:/opt/kurento-settings.xml \
  -v $KURENTO_SCRIPTS_HOME:/opt/adm-scripts \
  -v $WORKSPACE:$TEST_HOME \
  -v $TEST_WORKSPACE:$TEST_HOME/tmp \
  -e "WORKSPACE=$TEST_HOME" \
  -e "PROJECT_PATH=$PROJECT_PATH" \
  -e "MAVEN_SETTINGS=/opt/kurento-settings.xml" \
  -e "MAVEN_OPTS=$MAVEN_OPTS" \
  -w $TEST_HOME \
  -u "root" \
  kurento/dev-integration:jdk-8-node-0.12 \
  /opt/adm-scripts/kurento_capability_test.sh || status=$?

exit $status
