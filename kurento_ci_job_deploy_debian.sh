#!/usr/bin/env bash

#/ CI job - Deploy Debian packages with Aptly.
#/
#/ This script is meant to be called from the "Execute shell" section of all
#/ Jenkins jobs which want to deploy Debian packages.
#/
#/
#/ Variables
#/ ---------
#/
#/ This script expects some environment variables to be exported.
#/
#/ * Variable(s) from Jenkins global configuration:
#/
#/ APTLY_GPG_SUBKEY
#/
#/   The GnuPG key used to sign Debian package repositories with Aptly.
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
#/ JOB_DEPLOY_NAME
#/
#/   Special identifier for the repository.
#/   This variable can be empty or unset, in which case the default of "dev"
#/   will be used for nightly repos, or "<Version>" for release repos.
#/
#/
#/ * Variable(s) from Multi-Configuration ("Matrix") Project axis:
#/
#/ JOB_DISTRO
#/
#/   Name of the Ubuntu distribution where this job is run.
#/   E.g.: "xenial", "bionic".
#/
#/
#/ * Variable(s) from job files (with "Provide Configuration files"):
#/
#/ KEY_PUB
#/
#/   Public SSH key file for user 'kurento' in Aptly proxy server.
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

# Temp dir to store all packages in remote machine
TEMP_DIR="aptly_${JOB_DISTRO}_${JOB_TIMESTAMP}"

# Aptly repository name prefix to use for the repo name
REPO_NAME_PREFIX="kurento-${JOB_DISTRO}"

# Aptly runner script arguments
ARGS="--distro-name $JOB_DISTRO"

# Define parameters for the repository creation
if [[ "$JOB_RELEASE" == "true" ]]; then
    log "Deploy to release repo"

    ARGS="$ARGS --release"

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
elif [[ "$DEPLOY_SPECIAL" == "true" ]]; then
    log "Deploy to experimental feature repo"

    REPO_NAME_PREFIX+="-exp"
    KMS_VERSION="${JOB_DEPLOY_NAME}"
else
    log "Deploy to nightly packages repo"

    KMS_VERSION="dev"
fi

ARGS="$ARGS --repo-name ${REPO_NAME_PREFIX}-${KMS_VERSION}"
ARGS="$ARGS --publish-name $KMS_VERSION"



# Run commands in a clean container
# ---------------------------------

# Prepare SSH key to access Kurento Proxy machine
cat "$KEY_PUB" >secret.pem
chmod 0400 secret.pem

docker run --pull always --rm -i \
    --mount type=bind,src="$PWD",dst=/workdir -w /workdir \
    --mount type=bind,src="$KURENTO_SCRIPTS_HOME",dst=/adm-scripts \
    buildpack-deps:xenial-scm /bin/bash <<DOCKERCOMMANDS

# Bash options for strict error checking.
set -o errexit -o errtrace -o pipefail -o nounset
shopt -s inherit_errexit 2>/dev/null || true

# Trace all commands (to stderr).
set -o xtrace

# Exit trap, used to clean up.
on_exit() {
    ssh -n -o StrictHostKeyChecking=no -i secret.pem \
        ubuntu@proxy.openvidu.io '\
            rm -rf "$TEMP_DIR"'
}
trap on_exit EXIT

ssh -n -o StrictHostKeyChecking=no -i secret.pem \
    ubuntu@proxy.openvidu.io '\
        mkdir -p "$TEMP_DIR"'

scp -o StrictHostKeyChecking=no -i secret.pem \
    ./*.*deb \
    ubuntu@proxy.openvidu.io:"$TEMP_DIR"

scp -o StrictHostKeyChecking=no -i secret.pem \
    /adm-scripts/kurento_ci_aptly_repo_publish.sh \
    ubuntu@proxy.openvidu.io:"$TEMP_DIR"

ssh -n -o StrictHostKeyChecking=no -i secret.pem \
    ubuntu@proxy.openvidu.io '\
        cd "$TEMP_DIR" \
        && GPGKEY="$APTLY_GPG_SUBKEY" \
           ./kurento_ci_aptly_repo_publish.sh $ARGS'

DOCKERCOMMANDS



# Delete SSH key
rm secret.pem



# Test Local Installation
# -----------------------

# This follows the "Local Installation" instructions from the documentation:
# https://doc-kurento.readthedocs.io/en/latest/user/installation.html#local-installation

# And the "Install debug symbols" instructions:
# https://doc-kurento.readthedocs.io/en/latest/dev/dev_guide.html#install-debug-symbols

docker run --pull always --rm -i "ubuntu:$JOB_DISTRO" /bin/bash <<DOCKERCOMMANDS

# Bash options for strict error checking.
set -o errexit -o errtrace -o pipefail -o nounset
shopt -s inherit_errexit 2>/dev/null || true

# Trace all commands (to stderr).
set -o xtrace

# Disable Apt interactive mode.
export DEBIAN_FRONTEND=noninteractive

# Local Installation.
apt-get update ; apt-get install --no-install-recommends --yes \
    gnupg
apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83
tee "/etc/apt/sources.list.d/kurento.list" >/dev/null <<EOF
deb [arch=amd64] http://ubuntu.openvidu.io/$KMS_VERSION $JOB_DISTRO kms6
EOF
apt-get update ; apt-get install --no-install-recommends --yes \
    kurento-media-server

# Install debug symbols.
apt-key adv \
    --keyserver keyserver.ubuntu.com \
    --recv-keys F2EDC64DC5AEE1F6B9C621F0C8CAB6595FDFF622
tee "/etc/apt/sources.list.d/ddebs.list" >/dev/null <<EOF
deb http://ddebs.ubuntu.com ${JOB_DISTRO} main restricted universe multiverse
deb http://ddebs.ubuntu.com ${JOB_DISTRO}-updates main restricted universe multiverse
EOF
apt-get update ; apt-get install --no-install-recommends --yes \
    kurento-dbg

echo "Kurento packages were installed successfully!"

DOCKERCOMMANDS



log "==================== END ===================="
