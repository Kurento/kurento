#!/bin/bash -x

echo "##################### EXECUTE: maven-deploy #####################"

# Param management

# Refspec
[ -n "$1" ] && REFSPEC=$1 || exit 1

# Maven settins
[ -n "$2" ] && MAVEN_SETTINGS=$2 || exit 1

# Maven repositories
[ -n "$3" ] && SNAPSHOT_REPOSITORY="-DaltDeploymentRepository=$3"

# Sign artifacts (0 -> no, 1 -> yes)
[ -n "$4" ] && SIGN_ARTIFACTS=$4

# Maven options
OPTS="-Dmaven.test.skip=true -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true" 
[ -n "$5" ] && OPTS="$OPTS $5"

git checkout $REFSPEC || exit 1

if [ SIGN_ARTIFACTS -eq 1 ]; then
	# Deploy to requested repo
	mvn --settings $MAVEN_SETTINGS clean package javadoc:jar source:jar gpg:sign org.apache.maven.plugins:maven-deploy-plugin:2.8:deploy $OPTS $REPO || exit 1
	
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
	# Deploy to requested repo
	mvn --settings $MAVEN_SETTINGS clean package javadoc:jar source:jar org.apache.maven.plugins:maven-deploy-plugin:2.8:deploy $OPTS $REPO || exit 1
fi
