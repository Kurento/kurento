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

if [ "${KURENTO_REPO_URL}x" == "x" ]
then
  echo "KURENTO_REPO_URL environment variable should be to the base url for the repos"
  exit 1
fi

echo "Preparing to clone project: ${KURENTO_REPO_URL}/${PROJECT_NAME} (${BRANCH})"

git clone ${KURENTO_REPO_URL}/${PROJECT_NAME} || exit 1

cd ${PROJECT_NAME} || exit 1

git checkout ${BRANCH} || exit 1

git submodule update --init --recursive || exit 1
