#!/usr/bin/env bash

# This script gets project version from CMakeList.txt, pom.xml, and more.
# Call it like this:
#
#     PROJECT_VERSION="$(kurento_get_version.sh)" || {
#         echo "ERROR: Command failed: kurento_get_version"
#         exit 1
#     }



# NOTE: DO NOT print debug messages to stdout; callers expect that this script
# only outputs its intended result.
# If you need debug, make sure it gets redirected to stderr.



# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

log "==================== BEGIN ====================" >&2

# Trace all commands
set -o xtrace



# WARNING: Several scripts have implicit dependency on the ORDER of these checks.
# For example: kurento_mavenize_js_project assumes that pom.xml will be checked
# BEFORE package.json.

if [ -f CMakeLists.txt ]
then
  log "Getting version from CMakeLists.txt" >&2
  TEMPDIR="$(mktemp --directory --tmpdir="$PWD")"
  cd "$TEMPDIR" || exit 1
  echo "@PROJECT_VERSION@" >version.txt.in
  echo 'configure_file(${CMAKE_BINARY_DIR}/version.txt.in version.txt)' >>../CMakeLists.txt
  cmake .. -DCALCULATE_VERSION_WITH_GIT=FALSE -DDISABLE_LIBRARIES_GENERATION=TRUE >/dev/null || {
    log "ERROR: Command failed: cmake" >&2
    exit 1
  }
  PROJECT_VERSION="$(cat version.txt)"
  cd ..
  rm -rf "$TEMPDIR"
  sed -i '$ d' CMakeLists.txt
elif [ -f pom.xml ]
then
  log "Getting version from pom.xml" >&2
  MAVEN_CMD="mvn --batch-mode --non-recursive exec:exec -Dexec.executable=echo -Dexec.args='\${project.version}'"
  if [ -n "${MAVEN_SETTINGS:-}" ]; then
    MAVEN_CMD="$MAVEN_CMD --settings $MAVEN_SETTINGS"
  fi
  PROJECT_VERSION="$(eval $MAVEN_CMD --quiet)" || {
    log "ERROR: Command failed: mvn echo \${project.version}" >&2
    log "Running again to print the whole output:" >&2
    eval "$MAVEN_CMD" # This is just to print all output from Maven, eases debugging
    exit 1
  }
elif [ -f configure.ac ]
then
  log "Getting version from configure.ac" >&2
  PROJECT_VERSION="$(grep AC_INIT configure.ac | cut -d "," -f 2 | cut -d "[" -f 2 | cut -d "]" -f 1 | tr -d '[:space:]')"
elif [ -f configure.in ]
then
  log "Getting version from configure.in" >&2
  PROJECT_VERSION="$(grep AC_INIT configure.in | cut -d "," -f 2 | cut -d "[" -f 2 | cut -d "]" -f 1 | tr -d '[:space:]')"
elif [ -f package.json ]
then
  log "Getting version from package.json" >&2
  PROJECT_VERSION="$(grep version package.json | cut -d ":" -f 2 | cut -d "\"" -f 2)"
elif [ -f VERSIONS.conf.sh ]
then
  log "Getting version from VERSIONS.conf.sh" >&2
  # shellcheck source=VERSIONS.conf.sh
  source VERSIONS.conf.sh
  PROJECT_VERSION="${PROJECT_VERSIONS[VERSION_DOC]}"
elif [ -f VERSION ]
then
  log "Getting version from VERSION" >&2
  PROJECT_VERSION="$(cat VERSION)"
elif [ "$(find . -regex '.*/package.json' | sed -n 1p)" ]
then
  log "Getting version from package.json recursing into folders" >&2
  PROJECT_VERSION="$(grep version "$(find . -regex '.*/package.json' | sed -n 1p)" | cut -d ":" -f 2 | cut -d "\"" -f 2)"
elif [ "$(find . -regex '.*/bower.json' | sed -n 1p)" ]
then
  log "Getting version from bower recursing into folders" >&2
  PROJECT_VERSION="$(grep version "$(find . -regex '.*/bower.json' | sed -n 1p)" | cut -d ":" -f 2 | cut -d "\"" -f 2)"
elif [ -f GNUmakefile ]
then
  log "Getting version from GNUMakeFile" >&2
  PROJECT_VERSION_MAJOR="$(grep 'LIBS3_VER_MAJOR ?=' GNUmakefile | cut -d "=" -f 2 | cut -d " " -f 2)"
  PROJECT_VERSION_MINOR="$(grep 'LIBS3_VER_MINOR ?=' GNUmakefile | cut -d "=" -f 2 | cut -d " " -f 2)"
  PROJECT_VERSION="$PROJECT_VERSION_MAJOR.$PROJECT_VERSION_MINOR"
else
  log "PROJECT_VERSION not defined, need CMakeLists.txt, pom.xml, configure.ac, configure.in or package.json file" >&2
  exit 1
fi

if [ -z "${PROJECT_VERSION:-}" ]; then
  log "ERROR: Couldn't get PROJECT_VERSION from the current project" >&2
  exit 1
fi

#log "PROJECT_VERSION: <${PROJECT_VERSION}>" # Useful for debugging stdout of this script
echo "${PROJECT_VERSION}"



log "==================== END ====================" >&2
