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
#/ * Variable(s) from shell execution:
#/
#/ PACKAGES_PATH
#/
#/   Path where to find the packages that should be installed.
#/   This is used to obtain the Kurento version from its package file.
#/   Required.
#/
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
#/   E.g.: "focal".
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
#/   Jenkins path to 'ci-scripts', containing all Kurento CI scripts.



# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="

# Trace all commands (to stderr).
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
KMS_DEB_FILE="$(ls -v -1 "$PACKAGES_PATH"/kurento-media-server_*.deb | tail -n 1)"
if [[ -z "$KMS_DEB_FILE" ]]; then
    log "ERROR: Cannot find KMS package file: $PACKAGES_PATH/kurento-media-server_*.deb"
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

# Define parameters for the Docker container.
# NOTE: `DOCKER_KMS_VERSION` must match an existing Debian repo with that name.
if [[ "$JOB_RELEASE" == "true" ]]; then
    log "Deploy a release image"
    DOCKER_KMS_VERSION="$KMS_VERSION"
elif [[ "$DEPLOY_SPECIAL" == "true" ]]; then
    log "Deploy a feature branch image"
    DOCKER_KMS_VERSION="dev-$JOB_DEPLOY_NAME"
else
    log "Deploy a development branch image"
    DOCKER_KMS_VERSION="dev"
fi

# Run the Docker image builder
export PUSH_IMAGES="yes"
BUILD_ARGS=""
BUILD_ARGS+=" UBUNTU_CODENAME=$JOB_DISTRO"
BUILD_ARGS+=" KMS_VERSION=$DOCKER_KMS_VERSION"
export BUILD_ARGS
export TAG_COMMIT="no"
if [[ "$JOB_RELEASE" == "true" ]]; then
    # Main tag: "1.2.3"
    # Moving tag(s): "1.2", "1", "latest"
    export TAG="$KMS_VERSION"
    export EXTRA_TAGS="$KMS_VERSION_MAJ_MIN $KMS_VERSION_MAJ latest"
elif [[ "$DEPLOY_SPECIAL" == "true" ]]; then
    # Main tag: "dev-deploy-name"
    export TAG="dev-$JOB_DEPLOY_NAME"
    export EXTRA_TAGS=""
else
    # Main tag: "dev-1.2.3"
    # Moving tag(s): "dev-1.2", "dev"
    export TAG="dev-$KMS_VERSION"
    export EXTRA_TAGS="dev-$KMS_VERSION_MAJ_MIN dev-$KMS_VERSION_MAJ dev"
fi
"$KURENTO_SCRIPTS_HOME/kurento_container_build.sh"

log "New Docker image built: 'kurento/kurento-media-server:$TAG'"



log "==================== END ===================="
