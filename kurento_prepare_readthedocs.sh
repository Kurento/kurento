#!/bin/bash

echo "##################### EXECUTE: kurento_prepare_readthedocs #####################"

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

echo "Building $BRANCH of $DOC_PROJECT"

# Build
kurento_clone_repo.sh $DOC_PROJECT $BRANCH || { echo "Couldn't clone $DOC_PROJECT repository"; exit 1; }
pushd $DOC_PROJECT
COMMIT_MSG=$(git log -1 --pretty=format:%s)
sed -e "s@mvn@mvn --batch-mode --settings $MAVEN_SETTINGS@g" < Makefile > Makefile.jenkins
make -f Makefile.jenkins clean readthedocs || { echo "Building $DOC_PROJECT failed"; exit 1; }

popd

READTHEDOCS_PROJECT=$DOC_PROJECT-readthedocs
kurento_clone_repo.sh $READTHEDOCS_PROJECT $BRANCH || { echo "Couldn't clone $READTHEDOCS_PROJECT repository"; exit 1; }

rm -rf $READTHEDOCS_PROJECT/*
cp -r $DOC_PROJECT/* $READTHEDOCS_PROJECT/

pushd $READTHEDOCS_PROJECT
echo "Commiting changes to $READTHEDOCS_PROJECT repository"
git add .
git ci -m "$COMMIT_MSG"

# Build
sed -e "s@mvn@mvn --batch-mode --settings $MAVEN_SETTINGS@g" < Makefile > Makefile.jenkins
make -f Makefile.jenkins clean langdoc || make -f Makefile.jenkins javadoc || { echo "Building $READTHEDOCS_PROJECT failed"; exit 1; }
make -f Makefile.jenkins html epub latexpdf dist || { echo "Building $READTHEDOCS_PROJECT failed"; exit 1; }

git push origin $BRANCH || { echo "Couldn't push changes to $READTHEDOCS_PROJECT repository"; exit 1; }

popd
