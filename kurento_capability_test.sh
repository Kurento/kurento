#!/bin/bash -x

echo "##################### EXECUTE: capability-test #####################"
# This tool uses a set of variables expected to be exported by tester
# PROJECT_PATH string
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
# MAVEN_OPTS string
#     Optional
#     All settings defined in this varible will be added to mvn command line
#

# Get CLI parameter for backward compatibility
[ -n "$1" ] && PROJECT_PATH=$1

# Set default environment if required
mavenOpts=""
[ -z "$WORKSPACE" ] && WORKSPACE="."
mavenOpts="$mavenOpts -DfailIfNoTests=false"
mavenOpts="$mavenOpts -U"

# Compile kurento-java if directory is present
[ -d $WORKSPACE/kurento-java ] &&  \
  (cd kurento-java &&  mvn --settings $MAVEN_SETTINGS clean install -Pdeploy -U -Dmaven.test.skip=true && cd ..)

# Execute capability test

POM_FILE=$WORKSPACE/$PROJECT_PATH/pom.xml
if [ -n "$PROJECT_PATH" -a -f $POM_FILE ]; then
  mvn --settings $MAVEN_SETTINGS -pl $PROJECT_PATH -am verify $mavenOpts $MAVEN_OPTS
else
  mvn --settings $MAVEN_SETTINGS verify $mavenOpts $MAVEN_OPTS
fi
