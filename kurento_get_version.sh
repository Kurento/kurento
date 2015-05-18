#!/bin/bash

# This scripts gets project version from CMakeList.txt, pom.xml or configure.ac

exec 3>&1 >/dev/tty || exec 3>&1 >/dev/null

if [ -f CMakeLists.txt ]
then
  echo "Getting version from CMakeLists.txt"
  mkdir check_version
  cd check_version
  echo "@PROJECT_VERSION@" > version.txt.in
  echo 'configure_file(${CMAKE_BINARY_DIR}/version.txt.in version.txt)' >> ../CMakeLists.txt
  cmake .. -DCALCULATE_VERSION_WITH_GIT=FALSE -DDISABLE_LIBRARIES_GENERATION=TRUE -DCMAKE_MODULE_PATH=/usr/share/cmake-2.8/Modules > /dev/null
  PROJECT_VERSION=`cat version.txt`
  echo ${PROJECT_VERSION}
  cd ..
  rm -rf check_version
  sed -i '$ d' CMakeLists.txt
elif [ -f pom.xml ]
then
  echo "Getting version from pom.xml"
  mvn help:evaluate -Dexpression=project.version
  PROJECT_VERSION=`mvn help:evaluate -Dexpression=project.version 2>/dev/null| grep -v "^\[" | grep -v "Down"`
elif [ -f configure.ac ]
then
  echo "Getting version from configure.ac"
  PROJECT_VERSION=`grep AC_INIT configure.ac | cut -d "," -f 2 | cut -d "[" -f 2 | cut -d "]" -f 1 | tr -d '[[:space:]]'`
elif [ -f package.json ]
then
  echo "Getting version from package.json"
  PROJECT_VERSION=$(grep version package.json | cut -d ":" -f 2 | cut -d "\"" -f 2)
else
  echo "PROJECT_VERSION not defined, need CMakeLists.txt, pom.xml or configure.ac file"
  exit 1
fi

if [ "${PROJECT_VERSION}x" = "x" ]; then
  echo "PROJECT_VERSION not defined"
  exit 1
fi

exec >&3-
echo ${PROJECT_VERSION}
