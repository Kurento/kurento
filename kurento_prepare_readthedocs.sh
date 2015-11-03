#!/bin/bash

# This tool uses a set of variables expected to be exported by tester
# DOC_PROJECT string
#    Mandatory
#    Identifies the original documentation git repository
#
# BRANCH string
#    Mandatory
#    Identifies the branch to be synchronized with readthedocs repository
#
# MAVEN_SETTINGS path
#    Mandatory
#     Location of the settings.xml file used by maven
#

PATH=$PATH:$(realpath $(dirname "$0"))

# Build
kurento_clone_repo.sh $DOC_PROJECT $BRANCH
pushd $DOC_PROJECT
COMMIT_ID=`git rev-parse HEAD`
sed -e "s@mvn@mvn --settings $MAVEN_SETTINGS@g" < Makefile > Makefile.jenkins
make -f Makefile.jenkins clean readthedocs

popd

READTHEDOCS_PROJECT=$DOC_PROJECT-readthedocs
kurento_clone_repo.sh $READTHEDOCS_PROJECT $BRANCH

rm -rf $READTHEDOCS_PROJECT/*
cp -r $DOC_PROJECT/* $READTHEDOCS_PROJECT/

pushd $READTHEDOCS_PROJECT
git add .
git ci -m "$COMMIT_ID"

# Build
sed -e "s@mvn@mvn --settings $MAVEN_SETTINGS@g" < Makefile > Makefile.jenkins
make -f Makefile.jenkins clean langdoc || make -f Makefile.jenkins javadoc
make -f Makefile.jenkins html epub latexpdf dist

git push origin $BRANCH

popd
