#!/bin/bash -x

echo "##################### EXECUTE: kurento_mvn #####################"

# Executes capability tests

# Following environment variables can be exported when calling script
#
# MAVEN_GOALS
#   Optional
#   Maven goals to be executed. Default value is
#   DEFAULT: verify
#
# MAVEN_SETTINGS path
#    Mandatory
#    Location of the settings.xml file used by maven
#
# MAVEN_OPTIONS string
#    Optional
#    All settings defined in this varible will be added to mvn command line
#    DEFAULT: none
#
# PROJECT_MODULE string
#    Optional
#    Identifies the module to execute within a reactor project.
#    DEFAULT: none
#
# WORKSPACE path
#    Optional
#    Jenkins workspace path. This variable is expected to be exported by
#    script caller.
#    DEFAULT: .
#

# Set default environment
[ -z "$MAVEN_GOALS" ] && MAVEN_GOALS="verify"
[ -z "$WORKSPACE" ] && WORKSPACE="."
MAVEN_OPTIONS="$MAVEN_OPTIONS -DfailIfNoTests=false"

# Execute capability test
POM_FILE=$WORKSPACE/$PROJECT_MODULE/pom.xml
if [ -n "$PROJECT_MODULE" -a -f $POM_FILE ]; then
  # Install siblings dependencies first.
  mvn --settings $MAVEN_SETTINGS -pl $PROJECT_MODULE -am clean $MAVEN_GOALS -U $MAVEN_OPTIONS
else
  mvn --settings $MAVEN_SETTINGS clean $MAVEN_GOALS -U $MAVEN_OPTIONS
fi
