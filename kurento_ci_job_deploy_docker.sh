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
#/ JOB_DEPLOY_NAME
#/
#/   Special identifier for the repository.
#/   This variable can be empty or unset, in which case the default of "dev"
#/   will be used for nightly repos, or "<Version>" for release repos.
#/
#/ JOB_KMS_VERSION
#/
#/   Version of Kurento Media Server that will be installed in the image.
#/   Required. Default: "0.0.0" (invalid version).
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

# Check required parameters
if [[ "$JOB_KMS_VERSION" == "0.0.0" ]]; then
    log "ERROR: Missing parameter JOB_KMS_VERSION"
    exit 1
fi

# Check optional parameters
if [[ -z "${JOB_DEPLOY_NAME:-}" ]]; then
    DEPLOY_SPECIAL="false"
else
    DEPLOY_SPECIAL="true"
fi

# Extract components of KMS version
VERSION="$JOB_KMS_VERSION"
VERSION_MAJ_MIN="$(echo "$VERSION" | cut -d. -f1,2)"
VERSION_MAJ="$(echo "$VERSION" | cut -d. -f1)"

# Define parameters for the Docker image creation
if [[ "$JOB_RELEASE" == "true" ]]; then
    log "Deploy release image"
    DOCKER_KMS_VERSION="$VERSION"
    DOCKER_NAME_SUFFIX=""
elif [[ "$DEPLOY_SPECIAL" == "true" ]]; then
    log "Deploy experimental feature image"
    DOCKER_KMS_VERSION="$JOB_DEPLOY_NAME"
    DOCKER_NAME_SUFFIX="-exp"
else
    log "Deploy nightly development image"
    DOCKER_KMS_VERSION="dev"
    DOCKER_NAME_SUFFIX="-dev"
fi

cd ./kurento-media-server/

# Run the Docker image builder
export PUSH_IMAGES="yes"
export BUILD_ARGS="UBUNTU_VERSION=xenial KMS_VERSION=$DOCKER_KMS_VERSION"
export TAG_COMMIT="no"
export IMAGE_NAME_SUFFIX="$DOCKER_NAME_SUFFIX"
if [[ "$DEPLOY_SPECIAL" == "true" ]]; then
    export TAG="$JOB_DEPLOY_NAME"
    export EXTRA_TAGS=""
else
    export TAG="${VERSION}-${JOB_TIMESTAMP}"
    export EXTRA_TAGS="$VERSION $VERSION_MAJ_MIN $VERSION_MAJ latest"  # Moving tags, example: "1.2.3", "1.2", "1", "latest"
fi
"${KURENTO_SCRIPTS_HOME}/kurento_container_build.sh"

log "New Docker image built: 'kurento/kurento-media-server${IMAGE_NAME_SUFFIX}'"
