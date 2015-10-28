#!/bin/bash

echo "##################### EXECUTE: kurento-js-sanity-checks #####################"

# Param management
if [ $# -lt 1 ]
then
  echo "Usage: $0 <kurento_js_release_url>"
  exit 1
fi

# Kurento js release URL
[ -n "$1" ] && KURENTO_JS_RELEASE_URL=$1 || exit 1

if [ -z "$MAVEN_SETTINGS" ]; then
  echo "MAVEN_SETTINGS must be available as an environment variable"
fi

echo "Building $KURENTO_JS_RELEASE_URL"

# Test autostart KMS

TEST_GROUP="org.kurento.commons.testing.SanityTests"

# Create temporary folder for container
TEST_WORKSPACE=$WORKSPACE/tmp
mkdir $TEST_WORKSPACE

# Create Integration container
TEST_HOME=/opt/kurento-java

MAVEN_OPTS=""
MAVEN_OPTS="$MAVEN_OPTS -Dtest.kms.docker.image.forcepulling=false"
MAVEN_OPTS="$MAVEN_OPTS -Djava.awt.headless=true"
MAVEN_OPTS="$MAVEN_OPTS -Dwdm.chromeDriverUrl=http://chromedriver.kurento.org/"
MAVEN_OPTS="$MAVEN_OPTS -Dtest.kms.autostart=test"
MAVEN_OPTS="$MAVEN_OPTS -Dtest.kms.scope=docker"
MAVEN_OPTS="$MAVEN_OPTS -Dtest.kms.docker.image.name=kurento/kurento-media-server-dev:latest"
MAVEN_OPTS="$MAVEN_OPTS -Dproject.path=$TEST_HOME/kurento-integration-tests/kurento-sanity-test"
MAVEN_OPTS="$MAVEN_OPTS -Dtest.selenium.scope=docker"
MAVEN_OPTS="$MAVEN_OPTS -Dtest.selenium.record=$TEST_SELENIUM_RECORD"
MAVEN_OPTS="$MAVEN_OPTS -Dtest.files=/var/lib/test-files/kurento"
MAVEN_OPTS="$MAVEN_OPTS -Dtest.workspace=$TEST_HOME/tmp"
MAVEN_OPTS="$MAVEN_OPTS -Dtest.workspace.host=$TEST_WORKSPACE"
MAVEN_OPTS="$MAVEN_OPTS -Dgroups=$TEST_GROUP"
MAVEN_OPTS="$MAVEN_OPTS -Dkms.ws.uri=$WS_URI"
MAVEN_OPTS="$MAVEN_OPTS -Dkurento.release.url=$KURENTO_JS_RELEASE_URL"

# Execute Presenter test
docker run --rm \
  --name $BUILD_TAG-INTEGRATION \
  -v /var/lib/jenkins/test-files:/var/lib/test-files \
  -v $MAVEN_SETTINGS:/opt/kurento-settings.xml \
  -v $KURENTO_SCRIPTS_HOME:/opt/adm-scripts \
  -v $WORKSPACE:$TEST_HOME \
  -v $TEST_WORKSPACE:$TEST_HOME/tmp \
  -e "WORKSPACE=$TEST_HOME" \
  -e "MAVEN_SETTINGS=/opt/kurento-settings.xml" \
  -e "MAVEN_OPTS=$MAVEN_OPTS" \
  -w $TEST_HOME \
  -u "root" \
  kurento/dev-integration:jdk-8-node-0.12 \
  /opt/adm-scripts/kurento_capability_test.sh kurento-integration-tests/kurento-sanity-test || status=$?

exit $status
