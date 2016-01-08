#!/bin/bash

echo "##################### EXECUTE: kurento_merge_doc_project #####################"
env

[ -z "$KURENTO_PROJECT" ] && (echo "KURENTO_PROJECT variable not defined"; exit 1;)
kurento_check_version.sh || exit 1

export GIT_SSH_KEY=$GIT_KEY
kurento_prepare_readthedocs.sh || exit 1

pushd $KURENTO_PROJECT-readthedocs
git push origin $GERRIT_REFNAME || { echo "Couldn't push changes to $READTHEDOCS_PROJECT repository"; exit 1; }
kurento_check_version.sh || exit 1

# Extract version
VERSION=$(kurento_get_version.sh)
echo "Version built: $VERSION"
