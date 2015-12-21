#!/bin/bash

echo "##################### EXECUTE: kurento_merge_doc_project #####################"
env

[ -z "$KURENTO_PROJECT" ] && (echo "KURENTO_PROJECT variable not defined"; exit 1;)
kurento_check_version.sh || exit 1

export DOC_PROJECT=$KURENTO_PROJECT
export GIT_SSH_KEY=$GIT_KEY
kurento_prepare_readthedocs.sh || exit 1

pushd $KURENTO_PROJECT-readthedocs
kurento_check_version.sh || exit 1

# Extract version
VERSION=$(kurento_get_version.sh)
echo "Version built: $VERSION"
