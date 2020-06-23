#!/usr/bin/env bash

#/ CI job - Deploy Docker images to Docker Hub.
#/
#/ This script is meant to be called from the "Execute shell" section of all
#/ Jenkins jobs which want to deploy Docker images.
#/
#/
#/ Variables
#/ ---------
#/
#/ This script expects some environment variables to be exported.
#/
#/ * Variable(s) from job parameters (with "This project is parameterized"):
#/
#/ JOB_RELEASE
#/
#/   "true" for release versions. "false" for nightly snapshot builds.
#/
#/ JOB_TIMESTAMP
#/
#/   Numeric timestamp shown in the version of nightly packages.
#/
#/ JOB_DISTRO
#/
#/   Name of the Ubuntu distribution where this job is run.
#/   E.g.: "xenial", "bionic".
#/
#/ JOB_DEPLOY_NAME
#/
#/   Special identifier for the repository.
#/   This variable can be empty or unset, in which case the default of "dev"
#/   will be used for nightly repos, or "<Version>" for release repos.
#/
#/
#/ * Variable(s) from job Custom Tools (with "Install custom tools"):
#/
#/ KURENTO_SCRIPTS_HOME
#/
#/   Jenkins path to 'adm-scripts', containing all Kurento CI scripts.



# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

# Trace all commands
set -o xtrace



# Job setup
# ---------

# Check optional parameters
if [[ -z "${JOB_DEPLOY_NAME:-}" ]]; then
    DEPLOY_SPECIAL="false"
else
    DEPLOY_SPECIAL="true"
fi

# Get version number from the package file itself
# shellcheck disable=SC2012
KMS_DEB_FILE="$(ls -v -1 kurento-media-server_*.deb | tail -n 1)"
if [[ -z "$KMS_DEB_FILE" ]]; then
    log "ERROR: Cannot find KMS package file: kurento-media-server_*.deb"
    exit 1
fi
KMS_VERSION="$(
    dpkg --field "$KMS_DEB_FILE" Version \
        | grep --perl-regexp --only-matching '^(\d+\.\d+\.\d+)'
)"
if [[ -z "$KMS_VERSION" ]]; then
    log "ERROR: Cannot parse KMS Version field"
    exit 1
fi

# Extract version number components
KMS_VERSION_MAJ_MIN="$(echo "$KMS_VERSION" | cut -d. -f1,2)"
KMS_VERSION_MAJ="$(echo "$KMS_VERSION" | cut -d. -f1)"

# Define parameters for the Docker container
if [[ "$JOB_RELEASE" == "true" ]]; then
    log "Release version"
    DOCKER_KMS_VERSION="$KMS_VERSION"
    DOCKER_NAME_SUFFIX=""
elif [[ "$DEPLOY_SPECIAL" == "true" ]]; then
    log "Experimental feature version"
    DOCKER_KMS_VERSION="$JOB_DEPLOY_NAME"
    DOCKER_NAME_SUFFIX="-exp"
else
    log "Nightly development version"
    DOCKER_KMS_VERSION="dev"
    DOCKER_NAME_SUFFIX="-dev"
fi

pushd ./kurento-media-server/  # Enter kurento-media-server/

# Run the Docker image builder
export PUSH_IMAGES="yes"
export BUILD_ARGS="UBUNTU_CODENAME=$JOB_DISTRO KMS_VERSION=$DOCKER_KMS_VERSION"
export TAG_COMMIT="no"
export IMAGE_NAME_SUFFIX="$DOCKER_NAME_SUFFIX"
if [[ "$JOB_RELEASE" == "true" ]]; then
    # Main tag: "1.2.3"
    # Moving tags: "1.2", "1", "latest"
    export TAG="${KMS_VERSION}"
    export EXTRA_TAGS="$KMS_VERSION_MAJ_MIN $KMS_VERSION_MAJ latest"
elif [[ "$DEPLOY_SPECIAL" == "true" ]]; then
    # Main tag: "experiment"
    export TAG="${JOB_DEPLOY_NAME}"
    export EXTRA_TAGS=""
else
    # Main tag: "1.2.3-20191231235959"
    # Moving tags: "1.2.3", "1.2", "1", "latest"
    export TAG="${KMS_VERSION}-${JOB_TIMESTAMP}"
    export EXTRA_TAGS="$KMS_VERSION $KMS_VERSION_MAJ_MIN $KMS_VERSION_MAJ latest"
fi
"${KURENTO_SCRIPTS_HOME}/kurento_container_build.sh"

log "New Docker image built: 'kurento/kurento-media-server${IMAGE_NAME_SUFFIX}:${TAG}'"

popd  # Exit kurento-media-server/
