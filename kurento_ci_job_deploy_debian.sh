#!/usr/bin/env bash

#/ CI job - Deploy Debian packages with Aptly.
#/
#/ This script is meant to be called from the "Execute shell" section of all
#/ Jenkins jobs which want to deploy Debian packages.

# ${KEY_PUB}
# ${JOB_RELEASE}
# ${JOB_DISTRO}
# ${JOB_TIMESTAMP}
# ${APTLY_GPG_SUBKEY}



# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

# Trace all commands
set -o xtrace



# Job setup
# ---------

# Temp dir to store all packages in remote machine
TEMP_DIR="pkg_${JOB_DISTRO}_${JOB_TIMESTAMP}"

# Aptly runner script arguments
ARGS="--distro-name $JOB_DISTRO"

if [[ "$JOB_RELEASE" == "true" ]]; then
    log "Deploy to release repo"
    # shellcheck disable=SC2012
    KMS_DEB_PKG="$(ls -v -1 kurento-media-server_*.deb | tail -n 1)"
    if [[ -z "$KMS_DEB_PKG" ]]; then
        log "ERROR: Cannot find KMS package file: kurento-media-server_*.deb"
        exit 1
    fi

    KMS_VERSION="$(
        dpkg --field "$KMS_DEB_PKG" Version \
            | grep --perl-regexp --only-matching '^(\d+\.\d+\.\d+)'
    )"
    if [[ -z "$KMS_VERSION" ]]; then
        log "ERROR: Cannot parse KMS Version field"
        exit 1
    fi

    ARGS="$ARGS --repo-name kurento-${JOB_DISTRO}-${KMS_VERSION}"
    ARGS="$ARGS --publish-name s3:ubuntu:${KMS_VERSION}"
    ARGS="$ARGS --release"
elif [[ -z "$JOB_DEPLOY_NAME" ]]; then
    log "Deploy to nightly packages repo"
    ARGS="$ARGS --repo-name kurento-openvidu-${JOB_DISTRO}-dev"
    ARGS="$ARGS --publish-name s3:ubuntu:dev"
else
    log "Deploy to experimental feature repo"
    ARGS="$ARGS --repo-name kurento-labs-${JOB_DISTRO}-${JOB_DEPLOY_NAME}"
    ARGS="$ARGS --publish-name s3:ubuntu:${JOB_DEPLOY_NAME}"
fi



# Run commands in a clean container
# ---------------------------------

# Prepare SSH key to access Kurento Proxy machine
cat "$KEY_PUB" >secret.pem
chmod 0400 secret.pem

docker run --rm -i \
    --mount type=bind,src="$PWD",dst=/workdir -w /workdir \
    --mount type=bind,src="$KURENTO_SCRIPTS_HOME",dst=/adm-scripts \
    buildpack-deps:xenial-scm /bin/bash <<EOF

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trace all commands
set -o xtrace

# Exit trap, used to clean up
on_exit() {
    ssh -n -o StrictHostKeyChecking=no -i ./secret.pem \
        ubuntu@193.147.51.5 '\
            rm -rf "$TEMP_DIR"'
}
trap on_exit EXIT

ssh -n -o StrictHostKeyChecking=no -i ./secret.pem \
    ubuntu@193.147.51.5 '\
        mkdir -p "$TEMP_DIR"'

scp -o StrictHostKeyChecking=no -i ./secret.pem \
    ./*.*deb \
    ubuntu@193.147.51.5:"$TEMP_DIR"

scp -o StrictHostKeyChecking=no -i secret.pem \
    /adm-scripts/kurento_ci_aptly_repo_create.sh \
    ubuntu@193.147.51.5:"$TEMP_DIR"

ssh -n -o StrictHostKeyChecking=no -i ./secret.pem \
    ubuntu@193.147.51.5 '\
        cd "$TEMP_DIR" \
        && GPGKEY="$APTLY_GPG_SUBKEY" \
           ./kurento_ci_aptly_repo_create.sh $ARGS'
EOF



# Delete SSH key
rm secret.pem
