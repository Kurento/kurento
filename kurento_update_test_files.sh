#!/bin/bash -x

echo "##################### EXECUTE: update-test-files #####################"
# JENKINS_HOME
#    Root directory where test files are located. Default value is /template

# Set default environment
[ -z "$JENKINS_HOME" ] && JENKINS_HOME=/tmp

if [ ! -d $JENKINS_HOME/test-files/.svn ]; then
	mkdir -p $JENKINS_HOME/test-files || exit 1
	cd $JENKINS_HOME/test-files || exit 1
	svn checkout http://files.kurento.org/svn/kurento . || exit 1
fi

for i in {1..3}
do
	cd $JENKINS_HOME/test-files
	svn update && exit 0
	sleep $((5*$i))
done
