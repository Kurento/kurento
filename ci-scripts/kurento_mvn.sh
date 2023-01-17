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
# MAVEN_MODULE string
#    Optional
#    Identifies the module to execute within a reactor project.
#    DEFAULT: none
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

# Set default environment
[ -z "$MAVEN_GOALS" ] && MAVEN_GOALS="verify"
[ -n "$MAVEN_SETTINGS" ] && PARAM_MAVEN_SETTINGS="--settings $MAVEN_SETTINGS"
[ -n "$MAVEN_MODULE" -a -f $MAVEN_MODULE/pom.xml ] && PARAM_PL="-pl $MAVEN_MODULE -am"
MAVEN_OPTIONS="$MAVEN_OPTIONS -DfailIfNoTests=false"

# Do not compile if file ignore has been added
[ -f ignore ] && {
  echo "[kurento_mvn] Skip compilation: File 'ignore' exists"
  exit 0
}

export AWS_ACCESS_KEY_ID=$UBUNTU_PRIV_S3_ACCESS_KEY_ID
export AWS_SECRET_ACCESS_KEY=$UBUNTU_PRIV_S3_SECRET_ACCESS_KEY_ID
mvn --fail-at-end --batch-mode $PARAM_MAVEN_SETTINGS $PARAM_PL clean $MAVEN_GOALS $MAVEN_OPTIONS
