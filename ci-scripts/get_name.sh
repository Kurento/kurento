#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ Obtain the project name with per-project specific methods.
#/
#/ This script should be called like this:
#/
#/     PROJECT_NAME="$(get_name.sh)" || {
#/         echo "ERROR: Command failed: get_name.sh"
#/         exit 1
#/     }



# WARNING: DO NOT PRINT LOGGING MESSAGES TO STDOUT.
# Callers of this script expect that the output only contains a name.
# If you need debug, make sure it gets redirected to stderr (`command >&2`).



# Configure shell
# ===============

SELF_DIR="$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null && pwd -P)"
source "$SELF_DIR/bash.conf.sh" || exit 1

log "==================== BEGIN ====================" >&2
trap_add 'log "==================== END ====================" >&2' EXIT

# Trace all commands (to stderr).
set -o xtrace



# Check dependencies
# ==================

command -v cmake >/dev/null || {
    log "ERROR: 'cmake' is not installed; please install it"
    exit 1
}

command -v jq >/dev/null || {
    log "ERROR: 'jq' is not installed; please install it"
    exit 1
}



# Get project name
# ================

if [[ -f CMakeLists.txt ]]; then
    log "Getting name from CMakeLists.txt" >&2
    TEMPDIR="$(mktemp --directory --tmpdir="$PWD")"
    cd "$TEMPDIR" || exit 1

    # Use CMAKE_PROJECT_INCLUDE to inject a hook on the `project()` command.
    # https://cmake.org/cmake/help/latest/variable/CMAKE_PROJECT_INCLUDE.html
    #
    # Also, abort processing immediately. CMake doesn't have a command for that,
    # so we abuse the FATAL_ERROR mode of `message()`.
    # shellcheck disable=SC2016
    echo 'message(FATAL_ERROR "PROJECT_NAME=${PROJECT_NAME}")' >project_hook.cmake
    cmake \
        -DDISABLE_LIBRARIES_GENERATION=TRUE \
        -DCMAKE_PROJECT_INCLUDE="$PWD/project_hook.cmake" \
        .. 2>&1 | grep -Po 'PROJECT_NAME=\K(.*)' >output.txt \
        || true

    PROJECT_NAME="$(cat output.txt)"
    cd ..
    rm -rf "$TEMPDIR"
elif [[ -f package.json ]]; then
    PROJECT_NAME="$(jq --raw-output '.name' package.json)"
fi

if [ -z "${PROJECT_NAME:-}" ]; then
    log "ERROR: Couldn't get PROJECT_NAME from the current project" >&2
    exit 1
fi

# log "PROJECT_NAME: <$PROJECT_NAME>" >&2 # Useful for debugging stdout of this script
echo "$PROJECT_NAME"
