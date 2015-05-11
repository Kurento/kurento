#!/bin/bash

echo MAVEN_KURENTO_SNAPSHOTS $MAVEN_KURENTO_SNAPSHOTS
echo MAVEN_KURENTO_RELEASES $MAVEN_KURENTO_RELEASES
echo MAVEN_SONATYPE_NEXUS_STAGING $MAVEN_SONATYPE_NEXUS_STAGING
echo MAVEN_SETTINGS $MAVEN_SETTINGS

rm -rf build
mkdir build && cd build && cmake .. -DGENERATE_JAVA_CLIENT_PROJECT=TRUE -DDISABLE_LIBRARIES_GENERATION=TRUE

cd java || exit 1
PROJECT_VERSION=`mvn help:evaluate -Dexpression=project.version 2>/dev/null| grep -v "^\[" | grep -v "Down"`

OPTS="package javadoc:jar source:jar gpg:sign org.apache.maven.plugins:maven-deploy-plugin:2.8:deploy"
OPTS="$OPTS -Dmaven.test.skip=true -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true"
OPTS="$OPTS -U -Prelease"

echo $OPTS

if [[ ${PROJECT_VERSION} != *-SNAPSHOT ]]; then
  mvn --settings ${MAVEN_SETTINGS} clean deploy $OPTS -DaltDeploymentRepository=${MAVEN_KURENTO_RELEASES} || exit 1
  mvn --settings ${MAVEN_SETTINGS} deploy $OPTS -DaltDeploymentRepository=${MAVEN_SONATYPE_NEXUS_STAGING}  || exit 1
else
  mvn --settings ${MAVEN_SETTINGS} clean deploy $OPTS -DaltDeploymentRepository=${MAVEN_KURENTO_SNAPSHOTS} -DaltSnapshotDeploymentRepository=${MAVEN_KURENTO_SNAPSHOTS} || exit 1
fi

