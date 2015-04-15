#!/bin/bash

# Create tag
if [ -n "$1" ]
then
  CREATE_TAG="$1"
else
  echo "Missing first parameter create tag (true/false)"
  exit 1
fi

echo Create tag $CREATE_TAG

if [ -f CMakeLists.txt ]
then
  mkdir check_version
  cd check_version
  echo "@PROJECT_VERSION@" > version.txt.in
  echo "@PROJECT_NAME@" > name.txt.in
  echo 'configure_file(${CMAKE_BINARY_DIR}/version.txt.in version.txt)' >> ../CMakeLists.txt
  echo 'configure_file(${CMAKE_BINARY_DIR}/name.txt.in name.txt)' >> ../CMakeLists.txt
  cmake .. -DCALCULATE_VERSION_WITH_GIT=FALSE -DDISABLE_LIBRARIES_GENERATION=TRUE 2>%1 > /dev/null
  PROJECT_VERSION=`cat version.txt`
  PROJECT_NAME=`cat name.txt`
  echo Version: ${PROJECT_VERSION}
  echo ProjectName: ${PROJECT_NAME}
  cd ..
  rm -rf check_version
  sed -i '$ d' CMakeLists.txt
  sed -i '$ d' CMakeLists.txt

  # check dev version
  if [[ ${PROJECT_VERSION} == *-dev ]]; then
    echo "dev version"
    exit 0
  fi
fi

if [ -f pom.xml ]
then
  PROJECT_VERSION=`mvn help:evaluate -Dexpression=project.version 2>/dev/null| grep -v "^\[" | grep -v "Down"`
  PROJECT_NAME=`mvn help:evaluate -U -Dexpression=project.artifactId 2>/dev/null| grep -v "^\[" | grep -v "Down"`

  echo Version: ${PROJECT_VERSION}
  echo ProjectName: ${PROJECT_NAME}

  if [[ ${PROJECT_VERSION} == *-SNAPSHOT ]]; then
    echo "SNAPSHOT version ${PROJECT_VERSION}"
    exit 0
  fi
fi

if [ "${PROJECT_VERSION}x" = "x" ]; then
  echo "PROJECT_VERSION not defined, need a CMakeLists.txt or a pom.xml file"
  exit 1
fi

if [ "${PROJECT_NAME}x" = "x" ]; then
  echo "PROJECT_NAME not defined"
  exit 1
fi

# Check if all submodules are in a tag
git submodule foreach "
  if [ x = \"x\`git tag --contains HEAD | head -1\`\" ]; then
    exit 1
  fi
"
if [ $? -eq 1 ]; then
  echo "Not all the projects are in a tag"
  exit 1
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

if [ ${CREATE_TAG} = true ]
then
  #Create a new tag
  echo "Creating tag"
  tag_name=v${PROJECT_VERSION}
  #tag_name=${PROJECT_NAME}-${PROJECT_VERSION}
  #git branch -f master
  #git checkout master
  if git tag ${tag_name}
  then
    git push --tags
    #git push origin master || echo Push failed
  fi
fi
