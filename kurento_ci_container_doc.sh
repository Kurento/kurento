#!/bin/bash

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
# MAVEN_SETTINGS path
#     Mandatory
#     Location of the settings.xml file used by maven
#

CONTAINER_WORKSPACE=/opt/kurento
docker run \
  --rm \
  --name $BUILD_TAG-DOCUMENTATION \
  -v $MAVEN_SETTINGS:/opt/kurento-settings.xml \
  -v $KURENTO_SCRIPTS_HOME:/opt/adm-scripts \
  -v $WORKSPACE:/opt/kurento \
  -e "WORKSPACE=$CONTAINER_WORKSPACE" \
  -e "MAVEN_SETTINGS=/opt/kurento-settings.xml" \
  -w $CONTAINER_WORKSPACE \
  -u "root" \
    kurento/dev-documentation:1.0.0 \
      /opt/adm-scripts/kurento_merge_doc_project.sh || status=$?

exit $status
