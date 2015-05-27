#!/bin/bash -x

echo "##################### EXECUTE: maven-deploy #####################"

# Param management

# Refspec
[ -n "$1" ] && REFSPEC=$1 || exit 1

# Maven settins
[ -n "$2" ] && MAVEN_SETTINGS=$2 || exit 1

# Maven repository
[ -n "$3" ] && REPO="-DaltDeploymentRepository=$3"

# Maven options
OPTS="-Dmaven.test.skip=true -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true" 
[ -n "$4" ] && OPTS="$OPTS $4"

git checkout $REFSPEC || exit 1

if [ -n "$REPO" ]; then
	# Deploy to requested repo
	mvn --settings $MAVEN_SETTINGS clean package javadoc:jar source:jar gpg:sign org.apache.maven.plugins:maven-deploy-plugin:2.8:deploy $OPTS $REPO || exit 1
else
	# Deploy to all known repos
	mvn --settings $MAVEN_SETTINGS clean package javadoc:jar source:jar gpg:sign org.apache.maven.plugins:maven-deploy-plugin:2.8:deploy -U $OPTS \
		-DaltReleaseDeploymentRepository=$MAVEN_KURENTO_RELEASES \
		-DaltSnapshotDeploymentRepository=$MAVEN_KURENTO_SNAPSHOTS || exit 1
	mvn --settings $MAVEN_SETTINGS clean package javadoc:jar source:jar gpg:sign org.apache.maven.plugins:maven-deploy-plugin:2.8:deploy -U $OPTS  \
		-DaltReleaseDeploymentRepository=$MAVEN_SONATYPE_NEXUS_STAGING \
		-DaltSnapshotDeploymentRepository=$MAVEN_KURENTO_SNAPSHOTS || exit 1
fi

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
