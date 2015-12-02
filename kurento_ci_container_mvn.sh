#!/bin/bash -x

echo "##################### EXECUTE: kurento_ci_container_mvn #####################"

# Execute Kurento capability tests in Docker environment

# Following variables will be read from script

# BUILD_TAG string
#    Mandatory
#    A name to uniquely identify containers created within this job
#
# BOWER_RELEASE_URL
#    Optional
#    Kurento javascript modules are downloaded from this URL when running
#    JS sanity checks
#    DEFAULT: none
#
# DOCKER_HUB_IMAGE
#    Optional
#    Docker image used for Selenium HUB containers.
#    DEFAULT: selenium/hub:2.47.1
#
# DOCKER_NODE_KMS_IMAGE
#    Optional
#    Docker image used for KMS contaienrs.
#    DEFAULT: kurento/kurento-media-server-dev:latest
#
# DOCKER_NODE_CHROME_IMAGE
#    Optional
#    Docker image used for Selenium chrome.
#    DEFAULT: selenium/node-chrome:2.47.1
#
# DOCKER_NODE_FIREFOX_IMAGE
#    Optional
#    Docker image used for Selenium firefox.
#    DEFAULT: selenium/node-firefox:2.47.1
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
#    DEFAULT: none
#
# MAVEN_GOALS
#   Optional
#   Maven goals to be executed. Default value is
#   DEFAULT: verify
#
# MAVEN_LOCAL_REPOSITORY
#    Optional
#    A folder to host the maven local repository. Will be mounted as
#    a volume within the container
#    DEFAULT: $WORKSPACE/m2
#
# MAVEN_OPTIONS string
#    Optional
#    All settings defined in this varible will be added to mvn command line
#    DEFAULT: none
#
# MAVEN_SETTINGS path
#    Mandatory
#    Location of the settings.xml file used by maven
#    DEFAULT: none
#
# PROJECT_DIR path
#    Optional
#    Directory within workspace where test code is located.
#    DEFAULT: none
#
# PROJECT_MODULE string
#    Optional
#    Identifies the module to execute within a reactor project.
#    DEFAULT: none
#
# RECORD_TEST [ true | false ]
#    Optional
#    Activates session recording in case ffmpeg is available
#    DEFAULT: false
#
# TEST_GROUP string
#    Mandatory
#    Identifies the test category to run
#    DEFAULT: none
#
# TEST_NAME regexp
#    Optional
#    Identifies the tests within the category to run. Wildcards can be used.
#    When no present, all tests within the category are run.
#    DEFAULT: none
#
# WORKSPACE path
#    Mandatory
#    Jenkins workspace path.
#    DEFAULT: none
#

# Set Default values
TEST_HOME=/opt/test-home
[ -z "$KMS_AUTOSTART" ] && KMS_AUTOSTART="test"
[ -z "$KMS_SCOPE" ] && KMS_SCOPE="docker"
[ -z "$MAVEN_GOALS" ] && MAVEN_GOALS="verify"
[ -z "$MAVEN_LOCAL_REPOSITORY" ] && MAVEN_LOCAL_REPOSITORY="$WORKSPACE/m2"
[ -z "$RECORD_TEST" ] && RECORD_TEST="false"
[ -z "$WORKSPACE" ] && WORKSPACE="."

# Download or update test files
docker run \
  --rm \
	--name $BUILD_TAG-TEST-FILES \
    -v /var/lib/jenkins/test-files:/opt/test-files \
    -w /opt/test-files \
     kurento/svn-client:1.0.0 svn checkout http://files.kurento.org/svn/kurento

# Create temporary folders
[ -d $WORKSPACE/tmp ] || mkdir -p $WORKSPACE/tmp
[ -d $MAVEN_LOCAL_REPOSITORY ] || mkdir -p $MAVEN_LOCAL_REPOSITORY

# Craete Integration container
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

# Execute Presenter test
docker run --rm \
  --name $BUILD_TAG-INTEGRATION \
  -v /var/lib/jenkins/test-files:/opt/test-files \
  -v $MAVEN_LOCAL_REPOSITORY:/root/.m2 \
  -v $MAVEN_SETTINGS:/opt/kurento-settings.xml \
  -v $KURENTO_SCRIPTS_HOME:/opt/adm-scripts \
  -v $WORKSPACE$([ -n "$PROJECT_DIR" ] && echo "/$PROJECT_DIR"):$TEST_HOME \
  -v $WORKSPACE/tmp:$TEST_HOME/tmp \
  -e "WORKSPACE=$TEST_HOME" \
  -e "MAVEN_GOALS=$MAVEN_GOALS" \
  -e "MAVEN_OPTIONS=$MAVEN_OPTIONS" \
  -e "MAVEN_SETTINGS=/opt/kurento-settings.xml" \
  $([ -n "$PROJECT_MODULE" ] && echo "-e PROJECT_MODULE=$PROJECT_MODULE") \
  -w $TEST_HOME \
  -u "root" \
  kurento/dev-integration:jdk-8-node-0.12 \
  /opt/adm-scripts/kurento_mvn.sh || status=$?

exit $status
