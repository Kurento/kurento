#!/bin/bash

rm -rf build
mkdir build && cd build && cmake .. -DGENERATE_JAVA_CLIENT_PROJECT=TRUE -DDISABLE_LIBRARIES_GENERATION=TRUE

cd java || exit 1
PATH=$PATH:$(realpath $(dirname "$0"))
PROJECT_VERSION=`kurento_get_version.sh`

OPTS="package"
if [[ ${PROJECT_VERSION} != *-SNAPSHOT ]]; then
  [ $FULL_RELEASE -eq 1 ] && OPTS="$OPTS javadoc:jar source:jar gpg:sign"
fi
OPTS="$OPTS org.apache.maven.plugins:maven-deploy-plugin:2.8:deploy -Dmaven.test.skip=true -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -U"

echo $OPTS

if [[ ${PROJECT_VERSION} != *-SNAPSHOT ]]; then
  echo "Deploying release version to ${KURENTO_RELEASE_REPOSITORY} and ${SONATYPE_NEXUS_STAGING_REPOSITORY}"
  mvn --settings ${MAVEN_SETTINGS} clean deploy $OPTS -DaltDeploymentRepository=${KURENTO_RELEASE_REPOSITORY} || exit 1
	mvn --settings ${MAVEN_SETTINGS} deploy $OPTS -DaltDeploymentRepository=${SONATYPE_NEXUS_STAGING_REPOSITORY} || exit 1
else
  echo "Deploying snapshot version to ${KURENTO_SNAPSHOTS_REPOSITORY}"
  mvn --settings ${MAVEN_SETTINGS} clean deploy $OPTS -DaltDeploymentRepository=${KURENTO_SNAPSHOTS_REPOSITORY} -DaltSnapshotDeploymentRepository=${KURENTO_SNAPSHOTS_REPOSITORY} || exit 1
fi
