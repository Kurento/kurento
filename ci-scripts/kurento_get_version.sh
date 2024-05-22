#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ Obtain the project version with per-project specific methods.
#/
#/ This script should be called like this:
#/
#/     PROJECT_VERSION="$(kurento_get_version.sh)" || {
#/         echo "ERROR: Command failed: kurento_get_version"
#/         exit 1
#/     }



# WARNING: DO NOT PRINT LOGGING MESSAGES TO STDOUT.
# Callers of this script expect that the output only contains a version number.
# If you need debug, make sure it gets redirected to stderr (`command >&2`).



# Configure shell
# ===============

SELF_DIR="$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null && pwd -P)"
source "$SELF_DIR/bash.conf.sh" || exit 1

log "==================== BEGIN ====================" >&2
trap_add 'log "==================== END ====================" >&2' EXIT

# Trace all commands (to stderr).
set -o xtrace



# Parse call arguments
# ====================

CFG_MAVEN_SETTINGS_PATH=""

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --maven-settings)
            if [[ -n "${2-}" ]]; then
                CFG_MAVEN_SETTINGS_PATH="$(realpath "$2")"
                shift
            else
                log "ERROR: --maven-settings expects <Path>" >&2
                exit 1
            fi
            ;;
        *)
            log "ERROR: Unknown argument '${1-}'" >&2
            exit 1
            ;;
    esac
    shift
done



# Validate config
# ===============

log "CFG_MAVEN_SETTINGS_PATH=$CFG_MAVEN_SETTINGS_PATH" >&2



# Get project version
# ===================

# WARNING: Several scripts have implicit dependency on the ORDER of these checks.
# For example: kurento_mavenize_js_project assumes that pom.xml will be checked
# BEFORE package.json.

if [[ -f CMakeLists.txt ]]; then
    log "Getting version from CMakeLists.txt" >&2
    TEMPDIR="$(mktemp --directory --tmpdir="$PWD")"
    cd "$TEMPDIR" || exit 1
    echo "@PROJECT_VERSION@" >version.txt.in
    echo "configure_file(\${CMAKE_BINARY_DIR}/version.txt.in version.txt)" >>../CMakeLists.txt
    cmake -DCALCULATE_VERSION_WITH_GIT=FALSE -DDISABLE_LIBRARIES_GENERATION=TRUE .. >/dev/null || {
        log "ERROR: Command failed: cmake" >&2
        exit 1
    }
    PROJECT_VERSION="$(cat version.txt)"
    cd ..
    rm -rf "$TEMPDIR"
    sed -i '$ d' CMakeLists.txt
elif [[ -f pom.xml ]]; then
    log "Getting version from pom.xml" >&2
    MAVEN_CMD=(mvn --batch-mode help:evaluate -Dexpression=project.version -DforceStdout)
    if [[ -n "${CFG_MAVEN_SETTINGS_PATH:-}" ]]; then
        MAVEN_CMD+=(--settings "$CFG_MAVEN_SETTINGS_PATH")
    fi

    log "DRY RUN. Showing Maven logs:" >&2
    "${MAVEN_CMD[@]}" >&2

    log "REAL RUN. Suppressing Maven logs, just get the result:" >&2
    MAVEN_CMD+=(--quiet)
    PROJECT_VERSION="$("${MAVEN_CMD[@]}" 2>/dev/null)" || {
        log "ERROR: Command failed: mvn echo \${project.version}" >&2
        exit 1
    }
elif [[ -f configure.ac ]]; then
    log "Getting version from configure.ac" >&2
    PROJECT_VERSION="$(grep AC_INIT configure.ac | cut -d "," -f 2 | cut -d "[" -f 2 | cut -d "]" -f 1 | tr -d '[:space:]')"
elif [[ -f configure.in ]]; then
    log "Getting version from configure.in" >&2
    PROJECT_VERSION="$(grep AC_INIT configure.in | cut -d "," -f 2 | cut -d "[" -f 2 | cut -d "]" -f 1 | tr -d '[:space:]')"
elif [[ -f package.json ]]; then
    log "Getting version from package.json" >&2
    PROJECT_VERSION="$(grep version package.json | cut -d ":" -f 2 | cut -d "\"" -f 2)"
elif [[ -f VERSIONS.env ]]; then
    log "Getting version from VERSIONS.env" >&2
    # shellcheck disable=SC1091
    source VERSIONS.env
    # shellcheck disable=SC2153
    PROJECT_VERSION="${PROJECT_VERSIONS[VERSION_DOC]}"
elif [[ -f VERSION ]]; then
    log "Getting version from VERSION" >&2
    PROJECT_VERSION="$(cat VERSION)"
elif [[ "$(find . -regex '.*/package.json' | sed -n 1p)" ]]; then
    log "Getting version from package.json recursing into folders" >&2
    PROJECT_VERSION="$(grep version "$(find . -regex '.*/package.json' | sed -n 1p)" | cut -d ":" -f 2 | cut -d "\"" -f 2)"
elif [[ "$(find . -regex '.*/bower.json' | sed -n 1p)" ]]; then
    log "Getting version from bower recursing into folders" >&2
    PROJECT_VERSION="$(grep version "$(find . -regex '.*/bower.json' | sed -n 1p)" | cut -d ":" -f 2 | cut -d "\"" -f 2)"
elif [[ -f GNUmakefile ]]; then
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

# log "PROJECT_VERSION: <$PROJECT_VERSION>" >&2 # Useful for debugging stdout of this script
echo "$PROJECT_VERSION"
