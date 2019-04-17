#!/usr/bin/env bash

#/ CI - Create or update Aptly repos to store Debian packages.
#/
#/ This script is meant to run in the remote server that hosts Aptly repos. It
#/ won't work if you run this script locally or in the Jenkins machine; instead,
#/ you should copy it to the target server and run remotelly via SSH.
#/
#/
#/ Arguments
#/ ---------
#/
#/ --distro-name <Name>
#/
#/   Name of the Ubuntu distribution for which the repo will be created or
#/   updated. E.g.: "xenial", "bionic"
#/
#/ --repo-name <Name>
#/
#/   Name of the repository that will be created or updated.
#/   E.g.: "kurento-packages"
#/
#/ --publish-name <Name>
#/
#/   Name of the Aptly publishing endpoint that should be used.
#/   E.g.: "s3:ubuntu:packages"
#/
#/ --release
#/
#/   Create release snapshot repositories, to hold all packages that have been
#/   generated as part of a project release procedure.
#/
#/   If this option is not given, repositories are created or updated to hold
#/   nightly snapshot versions of the packages.
#/
#/   Optional. Default: Disabled.



# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

# Trace all commands
set -o xtrace



# Parse call arguments
# --------------------

CFG_NAME_DEFAULT="0"
CFG_DISTRO_NAME="$CFG_NAME_DEFAULT"
CFG_REPO_NAME="$CFG_NAME_DEFAULT"
CFG_PUBLISH_NAME="$CFG_NAME_DEFAULT"
CFG_RELEASE="false"

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --distro-name)
            if [[ -n "${2-}" ]]; then
                CFG_DISTRO_NAME="$2"
                shift
            else
                log "ERROR: --distro-name expects <Name>"
                log "Run with '--help' to read usage details"
                exit 1
            fi
            ;;
        --repo-name)
            if [[ -n "${2-}" ]]; then
                CFG_REPO_NAME="$2"
                shift
            else
                log "ERROR: --repo-name expects <Name>"
                log "Run with '--help' to read usage details"
                exit 1
            fi
            ;;
        --publish-name)
            if [[ -n "${2-}" ]]; then
                CFG_PUBLISH_NAME="$2"
                shift
            else
                log "ERROR: --publish-name expects <Name>"
                log "Run with '--help' to read usage details"
                exit 1
            fi
            ;;
        --release)
            CFG_RELEASE="true"
            ;;
        *)
            log "ERROR: Unknown argument '${1-}'"
            log "Run with '--help' to read usage details"
            exit 1
            ;;
    esac
    shift
done



# Apply config restrictions
# -------------------------

if [[ "$CFG_DISTRO_NAME" == "$CFG_NAME_DEFAULT" ]]; then
    log "ERROR: Missing --distro-name <Name>"
    exit 1
fi

if [[ "$CFG_REPO_NAME" == "$CFG_NAME_DEFAULT" ]]; then
    log "ERROR: Missing --repo-name <Name>"
    exit 1
fi

if [[ "$CFG_PUBLISH_NAME" == "$CFG_NAME_DEFAULT" ]]; then
    log "ERROR: Missing --publish-name <Name>"
    exit 1
fi

log "CFG_DISTRO_NAME=${CFG_DISTRO_NAME}"
log "CFG_REPO_NAME=${CFG_REPO_NAME}"
log "CFG_PUBLISH_NAME=${CFG_PUBLISH_NAME}"
log "CFG_RELEASE=${CFG_RELEASE}"



# Step 1: Create repo
# -------------------

REPO_EXISTS="$(aptly repo list | grep --count "$CFG_REPO_NAME")" || true
if [[ "$REPO_EXISTS" == "0" ]]; then
    log "Create new repo: $CFG_REPO_NAME"
    aptly repo create -distribution="$CFG_DISTRO_NAME" -component=kms6 "$CFG_REPO_NAME"
fi



# Step 2: Add files to repo
# -------------------------

aptly repo add -force-replace "$CFG_REPO_NAME" ./*.*deb



# Step 3: Publish repo
# --------------------

if [[ "$CFG_RELEASE" == "true" ]]; then
    SNAP_NAME="snap-${CFG_REPO_NAME}"
    log "Create and publish new release snapshot: $SNAP_NAME"
    aptly snapshot create "$SNAP_NAME" from repo "$CFG_REPO_NAME"
    aptly -gpg-key="$GPGKEY" publish snapshot "$SNAP_NAME" "$CFG_PUBLISH_NAME"
else
    REPO_PUBLISHED="$(aptly publish list -raw | grep --count "$CFG_PUBLISH_NAME $CFG_DISTRO_NAME")" || true
    if [[ "$REPO_PUBLISHED" == "0" ]]; then
        log "Publish new development repo: $CFG_REPO_NAME"
        aptly -gpg-key="$GPGKEY" publish repo "$CFG_REPO_NAME" "$CFG_PUBLISH_NAME"
    else
        log "Update already published development repo: $CFG_REPO_NAME"
        aptly -gpg-key="$GPGKEY" publish update "$CFG_DISTRO_NAME" "$CFG_PUBLISH_NAME"
    fi
fi



log "Done!"
