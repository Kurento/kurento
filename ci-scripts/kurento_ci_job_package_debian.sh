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
#/ * Variable(s) from shell execution:
#/
#/ DISABLE_APT_PROXY
#/
#/   Set to "true" to skip using the Apt proxy URL.
#/   Added for GitHub Actions: No proxy is needed.
#/   Optional.
#/
#/ INSTALL_PATH
#/
#/   Path where to find the packages that should be installed.
#/   Added for GitHub Actions: The path is not ".".
#/   Optional.
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
#/ JOB_GIT_NAME
#/
#/   Git branch or tag that should be checked out, if it exists.
#/
#/ JOB_GIT_NAME_FALLBACK
#/
#/   Git branch or tag that should be checked out, if `JOB_GIT_NAME` does not
#/   exist. If this fails too, then `JOB_DISTRO` will be used, and failing that,
#/   `main` will be used as a last recourse.
#/
#/   Optional.
#/
#/
#/ * Variable(s) from job Multi-Configuration ("Matrix") Project axis:
#/
#/ JOB_DISTRO
#/
#/   Name of the Ubuntu distribution where this job is run.
#/   E.g.: "focal".
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

# Trace all commands
set -o xtrace



# Job setup
# ---------

# Check out the requested branch
GIT_DEFAULT="$(kurento_git_default_branch.sh)"
"${KURENTO_SCRIPTS_HOME}/kurento_git_checkout_name.sh" \
    --name "${JOB_GIT_NAME:-$GIT_DEFAULT}" \
    --fallback "${JOB_GIT_NAME_FALLBACK:-$JOB_DISTRO}"

# Arguments to kurento-buildpackage.
KURENTO_BUILDPACKAGE_ARGS=()

if [[ "$JOB_RELEASE" == "true" ]]; then
    KURENTO_BUILDPACKAGE_ARGS+=(--release)
fi

if [[ "${DISABLE_APT_PROXY:-}" != "true" ]]; then
    KURENTO_BUILDPACKAGE_ARGS+=(--apt-proxy http://proxy.openvidu.io:3142)
fi


# Build
# -----

# Environment variables passed to the container:
# * CMake:
#   - ARGS: Used to pass arguments to CTest (controls running of all tests).
#     Eg: ARGS="--verbose"
#     - `--verbose` enables printing logs from all tests.
#     - `--tests-regex <regex>` can be used to select test targets to run.
#       Targets are implemented with either Check (C code) or Boost (C++ code),
#       so test selection can then be fine tuned with those tools.
#
# * Boost test framework (C++ code):
#   - BOOST_TEST_LOG_LEVEL: Log level (disabled by default).
#     Eg: BOOST_TEST_LOG_LEVEL="test_suite"
#   - BOOST_TEST_RUN_FILTERS: Select tests with a filter (check docs for syntax).
#     Eg: BOOST_TEST_RUN_FILTERS="WebRtcEndpoint"
#
# * Check test framework (C code):
#   - CK_RUN_CASE: Select a Check TCase, as created with `tcase_create("name")`.
#     Eg: CK_RUN_CASE="element"
#   - CK_RUN_SUITE: Select a Check Suite, as created with `suite_create("name")`.
#     Eg: CK_RUN_SUITE="webrtcendpoint"
#
# * Debhelper (debian/rules file, dh_auto_* tools):
#   - DEB_BUILD_OPTIONS: Options passed to dpkg tools. Can be used to limit
#     parallelization: DEB_BUILD_OPTIONS="parallel=1".
#
# * GLib / GStreamer:
#   - GST_DEBUG: To set the default logging level of Kurento (GStreamer).
#   - G_DEBUG: Debug configuration for GLib and GStreamer. Typically used to
#     pass G_DEBUG="fatal-warnings", to enable breaking on code assertions.
#   - G_MESSAGES_DEBUG: To set GLib logging categories. Used for libnice logs.

docker run --pull always --rm \
    --mount type=bind,src="$PWD",dst=/hostdir \
    --mount type=bind,src="$KURENTO_SCRIPTS_HOME",dst=/ci-scripts \
    --mount type=bind,src="${INSTALL_PATH:-$PWD}",dst=/packages \
    --env ARGS \
    --env BOOST_TEST_LOG_LEVEL \
    --env BOOST_TEST_RUN_FILTERS \
    --env CK_RUN_CASE \
    --env CK_RUN_SUITE \
    --env DEB_BUILD_OPTIONS \
    --env GST_DEBUG \
    --env G_DEBUG \
    --env G_MESSAGES_DEBUG \
    "kurento/kurento-buildpackage:${JOB_DISTRO}" \
        --install-files /packages \
        --timestamp "$JOB_TIMESTAMP" \
        "${KURENTO_BUILDPACKAGE_ARGS[@]}"



log "==================== END ===================="
