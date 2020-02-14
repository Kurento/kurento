#!/usr/bin/env bash

# This script gets project version from CMakeList.txt, pom.xml, and more.
# Call it like this:
#
#     PROJECT_VERSION="$(kurento_get_version.sh)" || {
#         echo "ERROR: Command failed: kurento_get_version"
#         exit 1
#     }



# NOTE: Other scripts expect that this script only prints the version.
# Don't print any debug messages, and if you do, make sure they are redirected
# to stderr.
#
# For this reason, we don't load our shell setup script here: bash.conf.sh

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trace all commands
set -o xtrace



# WARNING: Several scripts have implicit dependency on the ORDER of these checks.
# For example: kurento_mavenize_js_project assumes that pom.xml will be checked
# BEFORE package.json.

if [ -f CMakeLists.txt ]
then
  echo "Getting version from CMakeLists.txt" >&2
  TEMPDIR="$(mktemp --tmpdir="$PWD")"
  cd "$TEMPDIR" || exit 1
  echo "@PROJECT_VERSION@" >version.txt.in
  echo 'configure_file(${CMAKE_BINARY_DIR}/version.txt.in version.txt)' >>../CMakeLists.txt
  cmake .. -DCALCULATE_VERSION_WITH_GIT=FALSE -DDISABLE_LIBRARIES_GENERATION=TRUE >/dev/null || {
    echo "[kurento_get_version] ERROR: Command failed: cmake" >&2
    exit 1
  }
  PROJECT_VERSION="$(cat version.txt)"
  cd ..
  rm -rf "$TEMPDIR"
  sed -i '$ d' CMakeLists.txt
elif [ -f pom.xml ]
then
  echo "Getting version from pom.xml" >&2
  MAVEN_CMD='mvn --batch-mode --non-recursive exec:exec -Dexec.executable=echo -Dexec.args=\${project.version}'
  [ -n "$MAVEN_SETTINGS" ] && MAVEN_CMD="$MAVEN_CMD --settings $MAVEN_SETTINGS"
  MAVEN_CMD_QUIET="$MAVEN_CMD --quiet 2>/dev/null"
  PROJECT_VERSION="$(eval "$MAVEN_CMD_QUIET")" || {
    echo "[kurento_get_version] ERROR: Command failed: mvn echo project.version" >&2
    echo "[kurento_get_version] Run again to get log messages ..." >&2
    eval "$MAVEN_CMD" >&2  # This is just to print all output from Maven, eases debugging
    exit 1
  }
elif [ -f configure.ac ]
then
  echo "Getting version from configure.ac" >&2
  PROJECT_VERSION="$(grep AC_INIT configure.ac | cut -d "," -f 2 | cut -d "[" -f 2 | cut -d "]" -f 1 | tr -d '[:space:]')"
elif [ -f configure.in ]
then
  echo "Getting version from configure.in" >&2
  PROJECT_VERSION="$(grep AC_INIT configure.in | cut -d "," -f 2 | cut -d "[" -f 2 | cut -d "]" -f 1 | tr -d '[:space:]')"
elif [ -f package.json ]
then
  echo "Getting version from package.json" >&2
  PROJECT_VERSION="$(grep version package.json | cut -d ":" -f 2 | cut -d "\"" -f 2)"
elif [ -f VERSIONS.conf.sh ]
then
  echo "Getting version from VERSIONS.conf.sh" >&2
  # shellcheck source=VERSIONS.conf.sh
  source VERSIONS.conf.sh
  PROJECT_VERSION="${PROJECT_VERSIONS[VERSION_DOC]}"
elif [ "$(find . -regex '.*/package.json' | sed -n 1p)" ]
then
  echo "Getting version from package.json recursing into folders" >&2
  PROJECT_VERSION="$(grep version "$(find . -regex '.*/package.json' | sed -n 1p)" | cut -d ":" -f 2 | cut -d "\"" -f 2)"
elif [ "$(find . -regex '.*/bower.json' | sed -n 1p)" ]
then
  echo "Getting version from bower recursing into folders" >&2
  PROJECT_VERSION="$(grep version "$(find . -regex '.*/bower.json' | sed -n 1p)" | cut -d ":" -f 2 | cut -d "\"" -f 2)"
elif [ -f GNUmakefile ]
then
  echo "Getting version from GNUMakeFile" >&2
  PROJECT_VERSION_MAJOR="$(grep 'LIBS3_VER_MAJOR ?=' GNUmakefile | cut -d "=" -f 2 | cut -d " " -f 2)"
  PROJECT_VERSION_MINOR="$(grep 'LIBS3_VER_MINOR ?=' GNUmakefile | cut -d "=" -f 2 | cut -d " " -f 2)"
  PROJECT_VERSION="$PROJECT_VERSION_MAJOR.$PROJECT_VERSION_MINOR"
else
  echo "PROJECT_VERSION not defined, need CMakeLists.txt, pom.xml, configure.ac, configure.in or package.json file" >&2
  exit 1
fi

if [ "${PROJECT_VERSION}x" = "x" ]; then
  echo "PROJECT_VERSION not defined" >&2
  exit 1
fi

echo "${PROJECT_VERSION}"
