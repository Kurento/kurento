#!/bin/bash -x

echo "##################### EXECUTE: kurento_maven_deploy.sh #####################"
# MAVEN_SETTINGS path
#   Path to settings.xml file used by maven
#
# SNAPSHOT_REPOSITORY url
#   Repository used to deploy snapshot artifacts. Deployment is cancelled when
#   not provided
#
# RELEASE_REPOSITORY url
#   Repository used to deploy release artifacts. Depployment is cancelled when
#   not provided
#
# SIGN_ARTIFACTS true | false
#   Wheter to sign artifacts before deployment. Default value is true
#

PATH=$PATH:$(realpath $(dirname "$0"))

# Get command line parameters for backward compatibility
[ -n "$1" ] && MAVEN_SETTINGS=$1
[ -n "$2" ] && SNAPSHOT_REPOSITORY=$2
[ -n "$2" ] && RELEASE_REPOSITORY=$2
[ -n "$3" ] && SIGN_ARTIFACTS=$3

# Validate parameters
if [ -n "$MAVEN_SETTINGS" ];then
  [ -f "$MAVEN_SETTINGS" ] || exit 1
  PARAM_MAVEN_SETTINGS="--settings $MAVEN_SETTINGS"
fi
[ -z "$SIGN_ARTIFACTS" ] && SIGN_ARTIFACTS="true"

# needed env vars
export AWS_ACCESS_KEY_ID=$UBUNTU_PRIV_S3_ACCESS_KEY_ID
export AWS_SECRET_ACCESS_KEY=$UBUNTU_PRIV_S3_SECRET_ACCESS_KEY_ID

# Maven options
OPTS="-Dmaven.test.skip=true -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true"

PROJECT_VERSION=$(kurento_get_version.sh)
echo "Deploying version $PROJECT_VERSION"
# Build all packages
mvn --batch-mode $PARAM_MAVEN_SETTINGS clean package $OPTS || exit 1
if [[ ${PROJECT_VERSION} == *-SNAPSHOT ]] && [ -n "$SNAPSHOT_REPOSITORY" ]; then
	echo "Deploying SNAPSHOT version"
	mvn --batch-mode $PARAM_MAVEN_SETTINGS package org.apache.maven.plugins:maven-deploy-plugin:2.8:deploy -Pdefault $OPTS -DaltSnapshotDeploymentRepository=$SNAPSHOT_REPOSITORY || exit 1
elif [[ ${PROJECT_VERSION} != *-SNAPSHOT ]] && [ -n "$RELEASE_REPOSITORY" ]; then
	OPTS="-Pdeploy -Pkurento-release -Pgpg-sign $OPTS"
	if [[ $SIGN_ARTIFACTS == "true" ]]; then
		echo "Deploying release version signing artifacts"
		# Deploy signing artifacts
		mvn --batch-mode $PARAM_MAVEN_SETTINGS package javadoc:jar source:jar gpg:sign org.apache.maven.plugins:maven-deploy-plugin:2.8:deploy $OPTS -DaltReleaseDeploymentRepository=$RELEASE_REPOSITORY || exit 1

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
		mvn --batch-mode $PARAM_MAVEN_SETTINGS package javadoc:jar source:jar org.apache.maven.plugins:maven-deploy-plugin:2.8:deploy -U $OPTS -DaltReleaseDeploymentRepository=$RELEASE_REPOSITORY || exit 1
	fi
fi
