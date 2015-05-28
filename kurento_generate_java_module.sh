#!/bin/bash

if [ $# -lt 3 ]
then
  echo "Usage: $0 <full_release(0|1)> <KURENTO_SNAPSHOTS_REPOSITORY> <KURENTO_RELEASE_REPOSITORY> [<SONATYPE_NEXUS_STAGING_REPOSITORY>]"
  exit 1
fi

[ -n "$1" ] && FULL_RELEASE=$1 || exit 1
[ -n "$2" ] && KURENTO_SNAPSHOTS_REPOSITORY=$2 || exit 1
[ -n "$3" ] && KURENTO_RELEASE_REPOSITORY=$3 || exit 1
[ -n "$4" ] && SONATYPE_NEXUS_STAGING_REPOSITORY=$4

rm -rf build
mkdir build && cd build && cmake .. -DGENERATE_JAVA_CLIENT_PROJECT=TRUE -DDISABLE_LIBRARIES_GENERATION=TRUE

cd java || exit 1
PROJECT_VERSION=`mvn help:evaluate -Dexpression=project.version 2>/dev/null| grep -v "^\[" | grep -v "Down"`

OPTS="package"
[ $FULL_RELEASE -eq 1 ] && OPTS="$OPTS javadoc:jar source:jar gpg:sign"
OPTS="$OPTS org.apache.maven.plugins:maven-deploy-plugin:2.8:deploy -Dmaven.test.skip=true -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true"
OPTS="$OPTS -U -Prelease"

echo $OPTS

if [[ ${PROJECT_VERSION} != *-SNAPSHOT ]]; then
  mvn --settings ${MAVEN_SETTINGS} clean deploy $OPTS -DaltDeploymentRepository=${KURENTO_RELEASE_REPOSITORY} || exit 1
  if [[ -n $SONATYPE_NEXUS_STAGING_REPOSITORY ]]; then
  	mvn --settings ${MAVEN_SETTINGS} deploy $OPTS -DaltDeploymentRepository=${SONATYPE_NEXUS_STAGING_REPOSITORY} || exit 1
  fi
else
  mvn --settings ${MAVEN_SETTINGS} clean deploy $OPTS -DaltDeploymentRepository=${KURENTO_SNAPSHOTS_REPOSITORY} -DaltSnapshotDeploymentRepository=${KURENTO_SNAPSHOTS_REPOSITORY} || exit 1
fi

