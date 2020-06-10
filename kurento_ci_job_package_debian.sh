#!/usr/bin/env bash

#/ CI job - Generate a Debian package from the current project.
#/
#/ This script is meant to be called from the "Execute shell" section of all
#/ Jenkins jobs which want to create Debian packages for their projects.
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
#/ JOB_GIT_NAME
#/
#/   Git branch or tag that should be checked out, if it exists.
#/
#/
#/ * Variable(s) from job Multi-Configuration ("Matrix") Project axis:
#/
#/ JOB_DISTRO
#/
#/   Name of the Ubuntu distribution where this job is run.
#/   E.g.: "xenial", "bionic".
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

# Check out the requested branch
"${KURENTO_SCRIPTS_HOME}/kurento_git_checkout_name.sh" \
    --name "$JOB_GIT_NAME" --fallback "$JOB_DISTRO"

# Set build arguments
KURENTO_BUILDPACKAGE_ARGS="--timestamp $JOB_TIMESTAMP"
[[ "$JOB_RELEASE" == "true" ]] && KURENTO_BUILDPACKAGE_ARGS+=" --release"



# Build
# -----

CONTAINER_IMAGE="kurento/kurento-buildpackage:${JOB_DISTRO}"
docker pull "$CONTAINER_IMAGE"

# Environment variables passed to the container:
# * ARGS: A CMake variable used to pass arguments to CTest (C).
#   Typically used to pass "--verbose", to print logs from all tests.
# * BOOST_TEST_LOG_LEVEL: Print logs from Boost test framework (C++).
# * GST_DEBUG: To set the default logging level of Kurento (GStreamer).
# * G_DEBUG: Debug configuration for GLib and GStreamer. Typically used to pass
#   G_DEBUG="fatal-warnings", to enable breaking on code assertions.
# * G_MESSAGES_DEBUG: To set GLib logging categories. Used for libnice logs.

docker run --rm \
    --mount type=bind,src="$PWD",dst=/hostdir \
    --mount type=bind,src="$KURENTO_SCRIPTS_HOME",dst=/adm-scripts \
    --env ARGS \
    --env BOOST_TEST_LOG_LEVEL \
    --env GST_DEBUG \
    --env G_DEBUG \
    --env G_MESSAGES_DEBUG \
    "$CONTAINER_IMAGE" \
    --install-files . \
    $KURENTO_BUILDPACKAGE_ARGS
