#!/bin/bash -x
#
echo "##################### EXECUTE: kurento_maven_checkout #####################"

# This tool is intended to checkout several maven projects, place them in
# a given reference and install their artifacts in a maven repository
#
# PARAMETERS
#
# GERRIT_NEWREV
#     Reference to the incoming commit to be verified. Commit message will be
#     parsed for entries of the form:
#
#         dependency: <projec-name> = <refspec>
#
#     All valid references found will be cloned.
#
# GERRIT_PROJECT
#     Project  where reference specified by GERRIT_NEWREV belongs to
#
# GERRIT_USER
#     Gerrit username required to clone projects. Default value is current
#     user
#
# GERRIT_KEY
#     Gerrit key used by authenticate with GERRIT server
#
# GERRIT_HOST
#     IP address or DNS name of host where GERRIT_PROJECT is hosted
#     Default value is code.kurento.org
#
# GERRIT_PORT
#     TCP port where gerit server hosting GERRIT_PROJECT listens for requests.
#     Default value is 12345
#
# KURENTO_PROJECTS
#     A list of projects hosted in the same server as GERRIT_PROJECT that are
#     inconditionally cloned. This parameter contains space separated key value
#     pairs with format:
#
#         <project-name>[=<refspec>]
#
# MAVEN_SETTINGS
#     Maven settings
#
# Verify mandatory parameters
[ -z "$GERRIT_HOST" ] && GERRIT_HOST=code.kurento.org
[ -z "$GERRIT_PORT" ] && GERRIT_PORT=12345
[ -z "$GERRIT_NEWREV" ] && GERRIT_NEWREV=master
[ -z "$GERRIT_USER" ] && GERRIT_USER=$(whoami)
[ -n "$MAVEN_SETTINGS" ] && PARAM_MAVEN_SETTINGS="--settings $MAVEN_SETTINGS"

GERRIT_URL=ssh://$GERRIT_USER@$GERRIT_HOST:$GERRIT_PORT

# Clone gerrit reference if provided
if [ -n "$GERRIT_PROJECT" ]; then
  GERRIT_PROJECT_URL=$GERRIT_URL/$GERRIT_PROJECT
  git clone $GERRIT_PROJECT_URL  $GERRIT_PROJECT || exit 1
  ( cd $GERRIT_PROJECT && git fetch $GERRIT_PROJECT_URL $GERRIT_NEWREV && git checkout FETCH_HEAD
    if [ -f pom.xml ]; then
      mvn $PARAM_MAVEN_SETTINGS clean install -Dmaven.test.skip=true
    fi
  ) || exit 1

  # Collect GERRIT references
  REFS=$(cd $GERRIT_PROJECT &&
    git log -n1 | egrep "^\s*dependency\s*:\s*(\w\S*)\s*=\s*(\S+)\s*$" | awk -F ':' '{print $2}' )
fi

# Add inconditional references
REFS="$REFS $KURENTO_PROJECTS"

# Look for dependency references
for REF in $REFS; do
  DEPENDENCY_PROJECT=$(echo $REF | awk -F '=' '{print $1}' | tr -d '[[:space:]]')
  DEPENDENCY_PROJECT_URL=$GERRIT_URL/$DEPENDENCY_PROJECT
  DEPENDENCY_REFSPEC=$(echo $REF | awk -F '=' '{print $2}' | tr -d '[[:space:]]')
  [ -z "$DEPENDENCY_REFSPEC" ] && DEPENDENCY_REFSPEC=master
  # Clone and install artifacts only if not already cloned
  if [ ! -d "$DEPENDENCY_PROJECT" ]; then
    git clone $DEPENDENCY_PROJECT_URL || exit 1
    (cd $DEPENDENCY_PROJECT &&
      git fetch $DEPENDENCY_PROJECT_URL $DEPENDENCY_REFSPEC &&
      git checkout FETCH_HEAD
      # Execute maven for maven projects
      if [ -f pom.xml ]; then
        mvn --batch-mode $PARAM_MAVEN_SETTINGS clean install -Dmaven.test.skip=true
      fi
    ) || exit 1
  fi
done
