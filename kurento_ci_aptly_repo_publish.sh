#!/usr/bin/env bash

#/ CI - Create or update Aptly repos to store Debian packages.
#/
#/ This script will handle all Aptly operations to create or update already
#/ existing repositories.
#/
#/ The script is meant to run in the remote server that hosts Aptly repos. It
#/ won't work if you run this script locally or in the Jenkins machine; instead,
#/ you should copy it to the target server and run remotelly via SSH.
#/
#/
#/ Arguments
#/ ---------
#/
#/ --distro-name <DistroName>
#/
#/   Name of the Ubuntu distribution for which the repo will be created or
#/   updated. E.g.: "xenial", "bionic"
#/
#/ --repo-name <RepoName>
#/
#/   Name of the repository that will be created or updated.
#/   E.g.: "kurento-packages"
#/
#/ --publish-name <PublishName>
#/
#/   Name of the Aptly publishing endpoint that should be used.
#/   E.g.: "mypackages"
#/
#/   The repository URL will be like:
#/
#/       http://ubuntu.openvidu.io/<PublishName> <DistroName> kms6
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

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

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
                echo "ERROR: --distro-name expects <DistroName>"
                echo "Run with '--help' to read usage details"
                exit 1
            fi
            ;;
        --repo-name)
            if [[ -n "${2-}" ]]; then
                CFG_REPO_NAME="$2"
                shift
            else
                echo "ERROR: --repo-name expects <RepoName>"
                echo "Run with '--help' to read usage details"
                exit 1
            fi
            ;;
        --publish-name)
            if [[ -n "${2-}" ]]; then
                CFG_PUBLISH_NAME="$2"
                shift
            else
                echo "ERROR: --publish-name expects <PublishName>"
                echo "Run with '--help' to read usage details"
                exit 1
            fi
            ;;
        --release)
            CFG_RELEASE="true"
            ;;
        *)
            echo "ERROR: Unknown argument '${1-}'"
            echo "Run with '--help' to read usage details"
            exit 1
            ;;
    esac
    shift
done



# Apply config restrictions
# -------------------------

if [[ "$CFG_DISTRO_NAME" == "$CFG_NAME_DEFAULT" ]]; then
    echo "ERROR: Missing --distro-name <DistroName>"
    exit 1
fi

if [[ "$CFG_REPO_NAME" == "$CFG_NAME_DEFAULT" ]]; then
    echo "ERROR: Missing --repo-name <RepoName>"
    exit 1
fi

if [[ "$CFG_PUBLISH_NAME" == "$CFG_NAME_DEFAULT" ]]; then
    echo "ERROR: Missing --publish-name <PublishName>"
    exit 1
fi

echo "CFG_DISTRO_NAME=$CFG_DISTRO_NAME"
echo "CFG_REPO_NAME=$CFG_REPO_NAME"
echo "CFG_PUBLISH_NAME=$CFG_PUBLISH_NAME"
echo "CFG_RELEASE=$CFG_RELEASE"



# Step 1: Create repo
# -------------------

REPO_EXISTS="$(aptly repo list | grep --count "$CFG_REPO_NAME")" || true
if [[ "$REPO_EXISTS" == "0" ]]; then
    echo "Create new repo: $CFG_REPO_NAME"
    aptly repo create -distribution="$CFG_DISTRO_NAME" -component=kms6 "$CFG_REPO_NAME"
fi



# Step 2: Add files to repo
# -------------------------

aptly repo add -force-replace "$CFG_REPO_NAME" ./*.*deb



# Step 3: Publish repo
# --------------------

PUBLISH_ENDPOINT="s3:ubuntu:${CFG_PUBLISH_NAME}"

if [[ "$CFG_RELEASE" == "true" ]]; then
    SNAP_NAME="snap-${CFG_REPO_NAME}"
    echo "Create and publish new release snapshot: $SNAP_NAME"
    aptly snapshot create "$SNAP_NAME" from repo "$CFG_REPO_NAME"
    aptly -gpg-key="$GPGKEY" publish snapshot "$SNAP_NAME" "$PUBLISH_ENDPOINT"
else
    REPO_PUBLISHED="$(aptly publish list -raw | grep --count "$PUBLISH_ENDPOINT $CFG_DISTRO_NAME")" || true
    if [[ "$REPO_PUBLISHED" == "0" ]]; then
        echo "Publish new development repo: $CFG_REPO_NAME"
        aptly -gpg-key="$GPGKEY" publish repo "$CFG_REPO_NAME" "$PUBLISH_ENDPOINT"
    else
        echo "Update already published development repo: $CFG_REPO_NAME"
        aptly -gpg-key="$GPGKEY" publish update "$CFG_DISTRO_NAME" "$PUBLISH_ENDPOINT"
    fi
fi



echo "Done!"
