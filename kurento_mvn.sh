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
#    Optional
#    Location of the settings.xml file used by maven. Default one is used if
#    not provided
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
[ -n "$MAVEN_SETTINGS" ] && PARAM_MAVEN_SETTINGS="--settings $MAVEN_SETTINGS"
[ -z "$WORKSPACE" ] && WORKSPACE="."
[ -f $WORKSPACE/$PROJECT_MODULE/pom.xml ] && PARAM_PL="-pl $PROJECT_MODULE -am"
MAVEN_OPTIONS="$MAVEN_OPTIONS -DfailIfNoTests=false"

mvn --batch-mode $PARAM_MAVEN_SETTINGS $PARAM_PL clean $MAVEN_GOALS -U $MAVEN_OPTIONS
