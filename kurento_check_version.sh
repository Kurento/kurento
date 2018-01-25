#!/bin/bash -x

echo "##################### EXECUTE: kurento_check_version #####################"

# Force creating a Git tag when a Release version is detected.
# This is default 'false' because sometimes we need to do several actual
# changes before being able to mark a commit as an official "Release".
if [ -n "$1" ]; then
  CREATE_TAG="$1"
else
  if [ -z "$CREATE_TAG" ]; then
    CREATE_TAG="false"
  fi
fi

[ -n "$CHECK_SUBMODULES" ] || CHECK_SUBMODULES="yes"

PATH=$PATH:$(realpath $(dirname "$0"))

echo Create tag $CREATE_TAG

PROJECT_VERSION=`kurento_get_version.sh`

if [ "${PROJECT_VERSION}x" = "x" ]; then
  echo "Could not find project version"
  exit 1
fi

if [[ ${PROJECT_VERSION} == *-SNAPSHOT ]]; then
  echo "SNAPSHOT version ${PROJECT_VERSION}"
  exit 0
fi

if [[ ${PROJECT_VERSION} == *-dev ]]; then
  echo "dev version"
  exit 0
fi

if [[ $(echo $PROJECT_VERSION | grep -o '\.' | wc -l) -gt 2 ]]
then
  echo "Found more than two dots, should be a configure.ac dev version"
  exit 0
fi

# Check if all submodules are in a tag
if [[ ${CHECK_SUBMODULES} == yes ]]; then
  git submodule foreach "
    if [ x = \"x\`git tag --contains HEAD | head -1\`\" ]; then
      exit 1
    fi
  "
  if [ $? -eq 1 ]; then
    echo "Not all the projects are in a tag"
    exit 1
  fi
fi

if [ -s debian/changelog ]
   then
  # check changelog version
  ver=$(head -1 debian/changelog | sed -e "s@.* (\(.*\)) .*@\1@")
  if [[ $ver != ${PROJECT_VERSION} ]]; then
    echo "Version in changelog is different to current version"
    exit 1
  fi
fi

# Check that release version conforms to semver
kurento_check_semver.sh ${PROJECT_VERSION} || {
  echo "kurento_check_semver.sh failed"
  exit 1
}

if [ ${CREATE_TAG} = true ]
then
  #Create a new tag
  echo "Creating tag"
  tag_name=${PROJECT_VERSION}
  if git tag ${tag_name}
  then
    echo "Push tag"
    git push --tags
  fi
fi
