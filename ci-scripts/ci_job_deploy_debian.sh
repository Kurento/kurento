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
#/ APTLY_SERVER
#/ 
#/   IP address of the ssh server that hosts the aptly service. A DNS name is not allowed just an IPv4
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
#/   E.g.: "focal".
#/
#/
#/ * Variable(s) from job files (with "Provide Configuration files"):
#/
#/ APTLY_SSH_KEY_PATH
#/
#/   Path to SSH private key file for user 'kurento' in Aptly proxy server.
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

# Temp dir to store all packages in remote machine
TEMP_DIR="aptly-$JOB_DISTRO-$JOB_TIMESTAMP"

# Aptly runner script arguments
PUBLISH_ARGS=""

# Get a KMS_VERSION suitable for naming things in Aptly
if [[ "$JOB_RELEASE" == "true" ]]; then
    log "Deploy a release repo"

    PUBLISH_ARGS+=" --release"

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
elif [[ -n "${JOB_DEPLOY_NAME:-}" ]]; then
    log "Deploy a feature branch repo"

    KMS_VERSION="dev-$JOB_DEPLOY_NAME"
else
    log "Deploy a development branch repo"

    KMS_VERSION="dev"
fi

PUBLISH_ARGS+=" --distro-name $JOB_DISTRO"
PUBLISH_ARGS+=" --repo-name kurento-$JOB_DISTRO-$KMS_VERSION"
PUBLISH_ARGS+=" --publish-name $KMS_VERSION"



# Run commands in a clean container
# ---------------------------------

# Prepare SSH key to access Kurento Proxy machine
chmod 0400 "$APTLY_SSH_KEY_PATH"

docker run --pull always --rm -i \
    --mount type=bind,src="$KURENTO_SCRIPTS_HOME",dst=/ci-scripts \
    --mount type=bind,src="$APTLY_SSH_KEY_PATH",dst=/id_aptly_ssh \
    --mount type=bind,src="$PWD",dst=/workdir \
    --add-host aptly=$APTLY_SERVER \
    --workdir /workdir \
    buildpack-deps:20.04-scm /bin/bash <<DOCKERCOMMANDS

# Bash options for strict error checking.
set -o errexit -o errtrace -o pipefail -o nounset
shopt -s inherit_errexit 2>/dev/null || true

# Trace all commands (to stderr).
set -o xtrace

# Exit trap, used to clean up.
on_exit() {
    ssh -n -o StrictHostKeyChecking=no -i /id_aptly_ssh \
        aptly@aptly -p 3322 '\
            rm -rf "$TEMP_DIR"'
}
trap on_exit EXIT

ssh -n -o StrictHostKeyChecking=no -i /id_aptly_ssh \
    aptly@aptly -p 3322 '\
        mkdir -p "$TEMP_DIR"'

scp -o StrictHostKeyChecking=no -i /id_aptly_ssh \
    -P 3322 ./*.*deb \
    aptly@aptly:"$TEMP_DIR"

scp -o StrictHostKeyChecking=no -i /id_aptly_ssh \
    -P 3322 /ci-scripts/ci_aptly_repo_publish.sh \
    aptly@aptly:"$TEMP_DIR"

ssh -n -o StrictHostKeyChecking=no -i /id_aptly_ssh \
    aptly@aptly -p 3322 '\
        cd "$TEMP_DIR" \
        && GPGKEY="$APTLY_GPG_SUBKEY" \
           ./ci_aptly_repo_publish.sh $PUBLISH_ARGS'

DOCKERCOMMANDS



# Delete SSH key
rm -f "$APTLY_SSH_KEY_PATH"



log "==================== END ===================="
