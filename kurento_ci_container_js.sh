#!/bin/bash -x

# This tool uses a set of variables expected to be exported by tester
# WORKSPACE path
#    Mandatory
#    Jenkins workspace path. This variable is expected to be exported by
#    script caller.
#
# BUILD_TAG string
#    Mandatory
#    A name to uniquely identify containers created within this job
#
# KMS_AUTOSTART [ true | false ]
#    Optional
#    How will kms be managed
#    DEFAULT: false
#
# KMS_WS_URI url
#    Optional
#    URL where kms can be reached. Only needed when KMS_AUTOSTART==false.
#    KMS should be reacheble from within the containers.
#
# BUILD_COMMAND
#   Optional
#   Command to run for building project.
#   DEFAULT: npm -d install && npm test && node_modules/.bin/grunt && node_modules/.bin/grunt sync:bower
#

[ -z "$KMS_AUTOSTART" ] && KMS_AUTOSTART="false"
if [ -z "$BUILD_COMMAND" ]; then
  if [ "$KMS_AUTOSTART" = "false" ]; then
    BUILD_COMMAND="node --version && npm --version && npm -d install && npm test && node_modules/.bin/grunt && node_modules/.bin/grunt sync:bower"
  else
    BUILD_COMMAND="node --version && npm --version && npm -d install && npm test -- --ws_uri=ws://kms:8888/kurento --timeout_factor=3 && node_modules/.bin/grunt && node_modules/.bin/grunt sync:bower"
  fi
fi

# Set constants and environment
TEST_HOME=/opt/kurento-java

# Create kms container if necessary
if [ "$KMS_AUTOSTART" = "true" ]; then
  KMS_CONTAINER_NAME=${BUILD_TAG}-kms
  docker run \
    --name $KMS_CONTAINER_NAME \
    -d \
      kurento/kurento-media-server-dev:latest

  # Execute Presenter test
  docker run \
    --rm \
    --name $BUILD_TAG-INTEGRATION \
    -v $WORKSPACE:$TEST_HOME \
    --link $KMS_CONTAINER_NAME:kms \
    -e "WORKSPACE=$TEST_HOME" \
    -w $TEST_HOME \
    -u "root" \
      kurento/dev-integration:jdk-8-node-0.12 \
        bash -c "$BUILD_COMMAND" || status=$?
else
  # Execute Presenter test
  docker run \
    --rm \
    --name $BUILD_TAG-INTEGRATION \
    -v $WORKSPACE:$TEST_HOME \
    -e "WORKSPACE=$TEST_HOME" \
    -w $TEST_HOME \
    -u "root" \
      kurento/dev-integration:jdk-8-node-0.12 \
        bash -c "$BUILD_COMMAND" || status=$?
fi


exit $status
