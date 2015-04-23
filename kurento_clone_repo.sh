#!/bin/bash

if [ $# -lt 1 ]
then
  echo "Usage: $0 <project_name> [<branch>]"
  exit 1
fi

PROJECT_NAME=$1
BRANCH=$2

if [ "${BRANCH}x" == "x" ]
then
  BRANCH=master
fi

if [ "${KURENTO_GIT_REPOSITORY}x" == "x" ]
then
  echo "KURENTO_GIT_REPOSITORY environment variable should be to the base url for the repos"
  exit 1
fi

echo "Preparing to clone project: ${KURENTO_GIT_REPOSITORY}/${PROJECT_NAME} (${BRANCH})"

if [ ! "${GIT_SSH_KEY}x" == "x" ]
then
  echo "Setting private key permissions to 600"
  chmod 600 ${GIT_SSH_KEY}
fi

if [ "${GIT_SSH_KEY}x" == "x" ]
then
  git clone ${KURENTO_GIT_REPOSITORY}/${PROJECT_NAME} || exit 1
else
  ssh-agent bash -c "ssh-add ${GIT_SSH_KEY}; git clone ${KURENTO_GIT_REPOSITORY}/${PROJECT_NAME} || exit 1" || exit 1
fi

cd ${PROJECT_NAME} || exit 1

git checkout ${BRANCH} || exit 1

if [ "${GIT_SSH_KEY}x" == "x" ]
then
  git submodule update --init --recursive || exit 1
else
  ssh-agent bash -c "ssh-add ${GIT_SSH_KEY}; git submodule update --init --recursive || exit 1" || exit 1
fi
