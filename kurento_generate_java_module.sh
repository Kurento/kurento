#!/bin/bash -x

if [ -z "$KURENTO_PUBLIC_PROJECT" ];
then
  [ -n $2 ] && PUBLIC=$2 || PUBLIC=no
else
  PUBLIC=$KURENTO_PUBLIC_PROJECT
fi

rm -rf build
mkdir build && cd build
cmake .. -DGENERATE_JAVA_CLIENT_PROJECT=TRUE -DDISABLE_LIBRARIES_GENERATION=TRUE || exit 1

cd java || exit 1
PATH=$PATH:$(realpath $(dirname "$0"))
PROJECT_VERSION=`kurento_get_version.sh`

GOALS="clean package"
if [[ ${PROJECT_VERSION} != *-SNAPSHOT ]]; then
  [ "$PUBLIC" == "yes" ] && GOALS+=" javadoc:jar source:jar gpg:sign"
fi
GOALS+=" org.apache.maven.plugins:maven-deploy-plugin:2.8:deploy"
OPTS="$OPTS -Dmaven.test.skip=true -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -U"

echo $OPTS

if [[ ${PROJECT_VERSION} != *-SNAPSHOT ]]; then
  echo "Deploying release version to ${MAVEN_S3_KURENTO_RELEASES} and ${MAVEN_SONATYPE_NEXUS_STAGING}"
  echo "Deploying with options $OPTS"
  mvn --batch-mode --settings ${MAVEN_SETTINGS} $GOALS $OPTS -DaltReleaseDeploymentRepository=${MAVEN_S3_KURENTO_RELEASES} || exit 1
  if [ "$PUBLIC" == "yes" ]
  then
    mvn --batch-mode --settings ${MAVEN_SETTINGS} $GOALS $OPTS -DaltReleaseDeploymentRepository=${MAVEN_SONATYPE_NEXUS_STAGING} || exit 1
  fi
else
  echo "Deploying snapshot version to ${MAVEN_S3_KURENTO_SNAPSHOTS}"
  mvn --batch-mode --settings ${MAVEN_SETTINGS} $GOALS $OPTS -DaltSnapshotDeploymentRepository=${MAVEN_S3_KURENTO_SNAPSHOTS} || exit 1
fi
