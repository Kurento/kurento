#!/bin/bash

# This scripts gets project version from CMakeList.txt, pom.xml or configure.ac

exec 3>&1 >/dev/tty || exec 3>&1 >./get_version_logs

if [ -f CMakeLists.txt ]
then
  echo "Getting version from CMakeLists.txt"
  mkdir check_version
  cd check_version
  echo "@PROJECT_VERSION@" > version.txt.in
  echo 'configure_file(${CMAKE_BINARY_DIR}/version.txt.in version.txt)' >> ../CMakeLists.txt
  cmake .. -DCALCULATE_VERSION_WITH_GIT=FALSE -DDISABLE_LIBRARIES_GENERATION=TRUE > /dev/null
  PROJECT_VERSION=`cat version.txt`
  echo ${PROJECT_VERSION}
  cd ..
  rm -rf check_version
  sed -i '$ d' CMakeLists.txt
elif [ -f pom.xml ]
then
  echo "Getting version from pom.xml"
  if [ "${MAVEN_SETTINGS}x" = "x" ]
  then
    PROJECT_VERSION=`mvn help:evaluate -Dexpression=project.version 2>/dev/null| grep -v "^\[" | grep -v "Down"`
  else
    PROJECT_VERSION=`mvn --settings $MAVEN_SETTINGS help:evaluate -Dexpression=project.version 2>/dev/null| grep -v "^\[" | grep -v "Down"`
  fi
elif [ -f configure.ac ]
then
  echo "Getting version from configure.ac"
  PROJECT_VERSION=`grep AC_INIT configure.ac | cut -d "," -f 2 | cut -d "[" -f 2 | cut -d "]" -f 1 | tr -d '[[:space:]]'`
elif [ -f configure.in ]
then
  echo "Getting version from configure.in"
  PROJECT_VERSION=`grep AC_INIT configure.in | cut -d "," -f 2 | cut -d "[" -f 2 | cut -d "]" -f 1 | tr -d '[[:space:]]'`
elif [ -f package.json ]
then
  echo "Getting version from package.json"
  PROJECT_VERSION=$(grep version package.json | cut -d ":" -f 2 | cut -d "\"" -f 2)
elif [ -f Makefile ]
then
  echo "Getting version from Makefile"
  PROJECT_VERSION=$(grep "DOC_VERSION =" Makefile | cut -d "=" -f 2)
elif [ $(find . -regex '.*/package.json' | sed -n 1p) ]
then
  echo "Getting version from package.json recursing into folders"
  PROJECT_VERSION=$(grep version `find . -regex '.*/package.json' | sed -n 1p` | cut -d ":" -f 2 | cut -d "\"" -f 2)
elif [ $(find . -regex '.*/bower.json' | sed -n 1p) ]
then
  echo "Getting version from bower recursing into folders"
  PROJECT_VERSION=$(grep version `find . -regex '.*/bower.json' | sed -n 1p` | cut -d ":" -f 2 | cut -d "\"" -f 2)
else
  echo "PROJECT_VERSION not defined, need CMakeLists.txt, pom.xml, configure.ac, configure.in or package.json file"
  exit 1
fi

if [ "${PROJECT_VERSION}x" = "x" ]; then
  echo "PROJECT_VERSION not defined"
  exit 1
fi

exec >&3-
echo ${PROJECT_VERSION}
