#!/usr/bin/env bash

#/ CI job - Generate a Debian package from the GSStreamer Rust plugins.
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
#/ JOB_GIT_REPO
#/
#/   Git REpository containing the rust project to build package
#/
#/ JOB_GIT_NAME
#/
#/   Git branch or tag that should be checked out, if it exists.
#/
#/ GSTREAMER_RUST_PATCH_DIR
#/
#/   Path to the diffs to apply to gstreamer rust
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
#/



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
GST_RUST_PACKAGE="gst-plugin-rtp"
CURRENT_PWD=$PWD

# Apply diff to generate deb package
if [ -d xxtmpRepoxx ]; then rm -rf xxtmpRepoxx; fi
mkdir xxtmpRepoxx
cd xxtmpRepoxx
git clone $JOB_GIT_REPO
cd *
git checkout $JOB_GIT_NAME
git apply "$GSTREAMER_RUST_PATCH_DIR"/debian.diff


# Arguments to kurento-buildpackage.
KURENTO_BUILDPACKAGE_ARGS=()

if [[ "$JOB_RELEASE" == "true" ]]; then
    KURENTO_BUILDPACKAGE_ARGS+=(--release)
fi

if [[ "${DISABLE_APT_PROXY:-}" != "true" ]]; then
    KURENTO_BUILDPACKAGE_ARGS+=(--apt-proxy http://proxy.openvidu.io:3142)
fi
if [[ -n "$GST_RUST_PACKAGE" ]]; then
    KURENTO_BUILDPACKAGE_ARGS+=(--package "$GST_RUST_PACKAGE")
fi

# Skip configuring the platform argument (--platform) to leave it as default for now (x86_64-unknown-linux-gnu)

# Build
# -----

docker run --pull always --rm \
    --mount type=bind,src="$CURRENT_PWD",dst=/hostdir \
    --mount type=bind,src="$KURENTO_SCRIPTS_HOME",dst=/ci-scripts \
    --mount type=bind,src="${INSTALL_PATH:-$PWD}",dst=/packages \
    "kurento/rust-buildpackage:${JOB_DISTRO}" \
        --timestamp "$JOB_TIMESTAMP" \
        "${KURENTO_BUILDPACKAGE_ARGS[@]}"

cd ..
rm -rf xxtmpRepoxx

log "==================== END ===================="
