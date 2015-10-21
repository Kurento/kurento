#!/bin/bash -x

echo "##################### EXECUTE: capability-test #####################"
# This tool uses a set of variables expected to be exported by tester
# PROJECT_PATH string
#    Identifies the module to execute within a reactor project.
#
# WORKSPACE path
#    Jenkins workspace path. This variable is expected to be exported by
#    script caller.
#
# MAVEN_SETTINGS path
#     Location of the settings.xml file used by maven
#
# MAVEN_OPTS string
#     All settings defined in this varible will be added to mvn command line
#
# RECORD_TEST [ true | false ]
#    Activates session recording in case ffmpeg is available
#    DEFAULT: false
#

# Get CLI parameter for backward compatibility
[ -n "$1" ] && PROJECT_PATH=$1

# Set default environment if required
export DISPLAY=:1
mavenOpts=""
[ -z "$WORKSPACE" ] && WORKSPACE="."
if [ -n "$PROJECT_PATH" ]; then
  mavenOpts="$mavenOpts -am -pl $PROJECT_PATH"
  mavenOpts="$mavenOpts -Dproject.path=$WORKSPACE/$PROJECT_PATH"
fi
mavenOpts="$mavenOpts -Dkurento.workspace=$WORKSPACE"
mavenOpts="$mavenOpts -DfailIfNoTests=false"
mavenOpts="$mavenOpts -U"

# Compile kurento-java if directory is present
[ -d $WORKSPACE/kurento-java ] &&  \
  (cd kurento-java &&  mvn --settings $MAVEN_SETTINGS clean install -Pdeploy -U -Dmaven.test.skip=true)

# Execute capability test
mvn --settings $MAVEN_SETTINGS clean compile -DskipTests=true
mvn --settings $MAVEN_SETTINGS clean verify $mavenOpts $MAVEN_OPTS
