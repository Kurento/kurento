#!/usr/bin/env bash

#/ CI job - Generate a Debian package from the current project.
#/
#/ This script is meant to be called from the "Execute shell" section of all
#/ Jenkins jobs which want to create Debian packages for their projects.



# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

# Trace all commands
set -o xtrace



# Job setup
# ---------

# Check out the requested branch
"${KURENTO_SCRIPTS_HOME}/kurento_git_checkout_name.sh" "${JOB_GIT_NAME}"

# Set build arguments
ARGS="--timestamp ${JOB_TIMESTAMP}"
[[ "${JOB_RELEASE}" == "true" ]] && ARGS="${ARGS} --release"



# Build
# -----

CONTAINER_IMAGE="kurento/kurento-buildpackage:${JOB_DISTRO}"
docker pull "$CONTAINER_IMAGE"
docker run --rm \
    --mount type=bind,src="${PWD}",dst=/hostdir \
    --mount type=bind,src="${KURENTO_SCRIPTS_HOME}",dst=/adm-scripts \
    "$CONTAINER_IMAGE" \
    --install-files . \
    ${ARGS}
