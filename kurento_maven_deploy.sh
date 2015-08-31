#!/bin/bash -x

echo "##################### EXECUTE: kurento_maven_deploy.sh #####################"

# Param management
if [ $# -lt 1 ]
then
  echo "Usage: $0 <MAVEN_SETTINGS> [<repository> <sign artifacts (yes|no)>]"
  exit 1
fi


# Maven settins
[ -n "$1" ] && MAVEN_SETTINGS=$1 || exit 1

# Maven repository
[ -n "$2" ] && REPOSITORY=$2 || REPOSITORY=$MAVEN_KURENTO_SNAPSHOTS

# Sign artifacts (no, yes). We never sign SNAPSHOT artifacts
[ -n "$3" ] && SIGN_ARTIFACTS=$3 || SIGN_ARTIFACTS="yes"

# Maven options
OPTS="-Dmaven.test.skip=true -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true"

PROJECT_VERSION=$(kurento_get_version.sh)
echo "Deploying version $PROJECT_VERSION"

if [[ ${PROJECT_VERSION} == *-SNAPSHOT ]]; then
	echo "Deploying SNAPSHOT version"
	mvn --settings $MAVEN_SETTINGS clean package org.apache.maven.plugins:maven-deploy-plugin:2.8:deploy -Pdefault $OPTS -DaltSnapshotDeploymentRepository=$REPOSITORY || exit 1
else
	OPTS="-Pdeploy -Pkurento-release -Pgpg-sign $OPTS"
	if [[ $SIGN_ARTIFACTS == yes ]]; then
		echo "Deploying release version signing artifacts"
		# Deploy signing artifacts
		mvn --settings $MAVEN_SETTINGS clean package javadoc:jar source:jar gpg:sign org.apache.maven.plugins:maven-deploy-plugin:2.8:deploy $OPTS -DaltReleaseDeploymentRepository=$REPOSITORY || exit 1

		#Verify signed files (if any)
		SIGNED_FILES=$(find ./target -type f | egrep '\.asc$')

		if [ -z "$SIGNED_FILES" ]; then
			echo "No signed files found"
			exit 0
		fi

		for FILE in $SIGNED_FILES
		do
		    SIGNED_FILE=`echo $FILE | sed 's/.asc\+$//'`
		    gpg --verify $FILE $SIGNED_FILE || exit 1
		done

	else
		echo "Deploying release version without signing artifacts"
		# Deploy without signing artifacts
		mvn --settings $MAVEN_SETTINGS clean package javadoc:jar source:jar org.apache.maven.plugins:maven-deploy-plugin:2.8:deploy -U $OPTS -DaltReleaseDeploymentRepository=$REPOSITORY || exit 1
	fi
fi
