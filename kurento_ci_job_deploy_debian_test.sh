#!/usr/bin/env bash

#/ CI job - Test Debian deployment.
#/
#/ This script is meant to be called from the "Execute shell" section of a
#/ Jenkins job that wants to test the correct deployment of Kurento packages.
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

# Define parameters for the Docker container
if [[ "$JOB_RELEASE" == "true" ]]; then
    log "Release version"
    DOCKER_KMS_VERSION="$KMS_VERSION"
elif [[ "$DEPLOY_SPECIAL" == "true" ]]; then
    log "Experimental feature version"
    DOCKER_KMS_VERSION="$JOB_DEPLOY_NAME"
else
    log "Nightly development version"
    DOCKER_KMS_VERSION="dev"
fi



# Test Local Installation and Build from sources
# ----------------------------------------------

# This follows the instructions given in several sections of the documentation:
# * Local Installation: https://doc-kurento.readthedocs.io/en/latest/user/installation.html#local-installation
# * Install debug symbols: https://doc-kurento.readthedocs.io/en/latest/dev/dev_guide.html#install-debug-symbols
# * Build from sources: https://doc-kurento.readthedocs.io/en/latest/dev/dev_guide.html#build-from-sources

# In-place Docker container commands BEGIN
docker run --pull always --rm -i \
    --env DOCKER_KMS_VERSION="$DOCKER_KMS_VERSION" \
    "ubuntu:$JOB_DISTRO" /bin/bash <<'DOCKERCOMMANDS'

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trace all commands
set -o xtrace

# Disable Apt interactive mode
export DEBIAN_FRONTEND=noninteractive

# Install required tools
apt-get update ; apt-get install --no-install-recommends --yes \
    build-essential \
    ca-certificates \
    cmake \
    git \
    gnupg \
    pkg-config

# Add Kurento repository
{
    apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83

    source /etc/lsb-release

    tee "/etc/apt/sources.list.d/kurento.list" >/dev/null <<EOF
deb [arch=amd64] http://ubuntu.openvidu.io/$DOCKER_KMS_VERSION $DISTRIB_CODENAME kms6
EOF
}

# Install KMS
apt-get update ; apt-get install --no-install-recommends --yes \
    kurento-media-server

# Install debug symbols
{
    apt-key adv \
        --keyserver keyserver.ubuntu.com \
        --recv-keys F2EDC64DC5AEE1F6B9C621F0C8CAB6595FDFF622

    source /etc/lsb-release

    tee "/etc/apt/sources.list.d/ddebs.list" >/dev/null <<EOF
deb http://ddebs.ubuntu.com ${DISTRIB_CODENAME} main restricted universe multiverse
deb http://ddebs.ubuntu.com ${DISTRIB_CODENAME}-updates main restricted universe multiverse
EOF

    apt-get update ; apt-get install --no-install-recommends --yes \
        kurento-dbg
}

# Install build dependencies
apt-get update ; apt-get install --no-install-recommends --yes \
    kurento-media-server-dev

# Download KMS source code
git clone https://github.com/Kurento/kms-omni-build.git
cd kms-omni-build/
git submodule update --init --recursive
git submodule update --remote
if [[ "$DOCKER_KMS_VERSION" == "dev" ]]; then
    # Use the repo default branch.
    REF="$(grep -Po 'refs/remotes/origin/\K(.*)' .git/refs/remotes/origin/HEAD)"
else
    REF="$DOCKER_KMS_VERSION"
fi
git checkout "$REF" || true
git submodule foreach "git checkout $REF || true"

# Build and run KMS
export MAKEFLAGS="-j$(nproc)"
./bin/kms-build-run.sh --build-only

echo "Everything got installed and built successfully!"

DOCKERCOMMANDS
# In-place Docker container commands END



log "==================== END ===================="
