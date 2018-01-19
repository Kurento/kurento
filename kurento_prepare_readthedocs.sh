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

git config --global http.postBuffer 2M

# Build
if [ -n $GERRIT_REFSPEC ] ; then
  kurento_clone_repo.sh $KURENTO_PROJECT $GERRIT_REFSPEC || { echo "Couldn't clone $KURENTO_PROJECT repository"; exit 1; }
else
  kurento_clone_repo.sh $KURENTO_PROJECT $GERRIT_REFNAME || { echo "Couldn't clone $KURENTO_PROJECT repository"; exit 1; }
fi

# Note: This modifies the source files!
# However, all changes are correctly discarded because everything is running
# inside a disposable container.

pushd $KURENTO_PROJECT
LAST_RELEASE="$(git describe --tags --abbrev=0)"
COMMIT_MSG="Commits since release $LAST_RELEASE

$(git log $LAST_RELEASE..HEAD --oneline)"
sed -e "s@mvn@mvn --batch-mode --settings $MAVEN_SETTINGS@g" Makefile > Makefile.ci
make --file="Makefile.ci" ci-readthedocs || { echo "Building $KURENTO_PROJECT failed"; exit 1; }
rm Makefile.ci
popd

echo "Preparing readthedocs project: $KURENTO_PROJECT-readthedocs"

# Our ReadTheDocs account is configured to watch the 'master' branch of
# https://github.com/Kurento/doc-kurento-readthedocs
READTHEDOCS_PROJECT=$KURENTO_PROJECT-readthedocs
kurento_clone_repo.sh $READTHEDOCS_PROJECT master || { echo "Couldn't clone $READTHEDOCS_PROJECT repository"; exit 1; }

rm -rf $READTHEDOCS_PROJECT/*
cp -a $KURENTO_PROJECT/* $READTHEDOCS_PROJECT/

pushd $READTHEDOCS_PROJECT
echo "Commiting changes to $READTHEDOCS_PROJECT repository"
git add --all .
git status
git commit -m "$COMMIT_MSG" || exit 1
git push origin "$GERRIT_REFNAME" || { echo "Couldn't push changes to $READTHEDOCS_PROJECT repository"; exit 1; }
popd
