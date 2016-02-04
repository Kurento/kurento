#!/bin/bash -x

echo "##################### EXECUTE: kurento_prepare_readthedocs #####################"

# This tool uses a set of variables expected to be exported by tester
# KURENTO_PROJECT string
#    Mandatory
#    Identifies the original documentation git repository
#
# GERRIT_REFNAME string
#    Mandatory
#    Identifies the GERRIT_REFNAME to be synchronized with readthedocs repository
#
# MAVEN_SETTINGS path
#    Mandatory
#     Location of the settings.xml file used by maven
#

PATH=$PATH:$(realpath $(dirname "$0"))

echo "Building $GERRIT_REFNAME of $KURENTO_PROJECT"

# Build
if [ -n $GERRIT_REFSPEC ] ; then
  kurento_clone_repo.sh $KURENTO_PROJECT $GERRIT_REFSPEC || { echo "Couldn't clone $KURENTO_PROJECT repository"; exit 1; }
else
  kurento_clone_repo.sh $KURENTO_PROJECT $GERRIT_REFNAME || { echo "Couldn't clone $KURENTO_PROJECT repository"; exit 1; }
fi
pushd $KURENTO_PROJECT
COMMIT_MSG=$(git log -1 --pretty=format:%s)
sed -e "s@mvn@mvn --batch-mode --settings $MAVEN_SETTINGS@g" < Makefile > Makefile.jenkins
make -f Makefile.jenkins clean readthedocs || { echo "Building $KURENTO_PROJECT failed"; exit 1; }

popd

echo "Preparing readthedocs project: $KURENTO_PROJECT-readthedocs"

READTHEDOCS_PROJECT=$KURENTO_PROJECT-readthedocs
kurento_clone_repo.sh $READTHEDOCS_PROJECT $GERRIT_REFNAME || { echo "Couldn't clone $READTHEDOCS_PROJECT repository"; exit 1; }

rm -rf $READTHEDOCS_PROJECT/*
cp -r $KURENTO_PROJECT/* $READTHEDOCS_PROJECT/

pushd $READTHEDOCS_PROJECT
echo "Commiting changes to $READTHEDOCS_PROJECT repository"
git add --all .
git st
git commit -m "$COMMIT_MSG" || exit 1

# Build
sed -e "s@mvn@mvn --batch-mode --settings $MAVEN_SETTINGS@g" < Makefile > Makefile.jenkins
make -f Makefile.jenkins clean langdoc || make -f Makefile.jenkins javadoc || { echo "Building $READTHEDOCS_PROJECT failed"; exit 1; }
make -f Makefile.jenkins html || { echo "Building $READTHEDOCS_PROJECT failed"; exit 1; }

popd
