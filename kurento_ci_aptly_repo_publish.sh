#!/usr/bin/env bash

#/ CI - Create or update Aptly repos to store Debian packages.
#/
#/ This script will handle all Aptly operations to create or update already
#/ existing repositories.
#/
#/ The script is meant to run in the remote server that hosts Aptly repos. It
#/ won't work if you run this script locally or in the Jenkins machine; instead,
#/ you should copy it to the target server and run remotely via SSH.
#/
#/
#/ Arguments
#/ ---------
#/
#/ --distro-name <DistroName>
#/
#/   Name of the Ubuntu distribution for which the repo will be created or
#/   updated. E.g.: "xenial", "bionic".
#/
#/   Required. Default: None.
#/
#/ --repo-name <RepoName>
#/
#/   Name of the repository that will be created or updated.
#/   E.g.: "kurento-xenial-6.12.0", "kurento-xenial-test-branch"
#/
#/   Required. Default: None.
#/
#/ --publish-name <PublishName>
#/
#/   Name of the Aptly publishing endpoint that should be used.
#/   E.g.: "6.12.0", "test-branch"
#/
#/   The repository URL will be like:
#/
#/       http://ubuntu.openvidu.io/<PublishName> <DistroName> kms6
#/
#/   Required. Default: None.
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
# ===========

# Bash options for strict error checking.
set -o errexit -o errtrace -o pipefail -o nounset
shopt -s inherit_errexit 2>/dev/null || true

# Trace all commands (to stderr).
set -o xtrace



# Parse call arguments
# ====================

CFG_DISTRO_NAME=""
CFG_REPO_NAME=""
CFG_PUBLISH_NAME=""
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



# Config restrictions
# ===================

if [[ -z "$CFG_DISTRO_NAME" ]]; then
    echo "ERROR: Missing --distro-name <DistroName>"
    exit 1
fi

if [[ -z "$CFG_REPO_NAME" ]]; then
    echo "ERROR: Missing --repo-name <RepoName>"
    exit 1
fi

if [[ -z "$CFG_PUBLISH_NAME" ]]; then
    echo "ERROR: Missing --publish-name <PublishName>"
    exit 1
fi

echo "CFG_DISTRO_NAME=$CFG_DISTRO_NAME"
echo "CFG_REPO_NAME=$CFG_REPO_NAME"
echo "CFG_PUBLISH_NAME=$CFG_PUBLISH_NAME"
echo "CFG_RELEASE=$CFG_RELEASE"



# Step 1: Create repo
# ===================

REPO_EXISTS="$(aptly repo list -raw | grep --count "$CFG_REPO_NAME")" || true
if [[ "$REPO_EXISTS" == "0" ]]; then
    echo "Create new repo: $CFG_REPO_NAME"
    aptly repo create -distribution="$CFG_DISTRO_NAME" -component=kms6 "$CFG_REPO_NAME"
    # TODO FIXME: For Kurento 7.0, change to `-component=main`.
fi



# Step 2: Add files to repo
# =========================

aptly repo add -force-replace -remove-files "$CFG_REPO_NAME" ./*.*deb



# Step 3: Publish repo
# ====================

PUBLISH_ENDPOINT="s3:ubuntu:${CFG_PUBLISH_NAME}"

if [[ "$CFG_RELEASE" == "true" ]]; then
    echo "Create and publish new release snapshot: $SNAP_NAME"

    # Aptly docs:
    # > It is not recommended to publish local repositories directly unless the
    # > repository is for testing purposes and changes happen frequently. For
    # > production usage please take snapshot of repository and publish it.
    SNAP_NAME="snap-${CFG_REPO_NAME}"
    aptly snapshot create "$SNAP_NAME" from repo "$CFG_REPO_NAME"

    aptly publish snapshot -gpg-key="$GPGKEY" "$SNAP_NAME" "$PUBLISH_ENDPOINT"
else
    REPO_PUBLISHED="$(aptly publish list -raw | grep --count "$PUBLISH_ENDPOINT $CFG_DISTRO_NAME")" || true
    if [[ "$REPO_PUBLISHED" == "0" ]]; then
        echo "Publish new development repo: $CFG_REPO_NAME"
        aptly publish repo -gpg-key="$GPGKEY" -force-overwrite "$CFG_REPO_NAME" "$PUBLISH_ENDPOINT"
    else
        echo "Update already published development repo: $CFG_REPO_NAME"
        aptly publish update -gpg-key="$GPGKEY" -force-overwrite "$CFG_DISTRO_NAME" "$PUBLISH_ENDPOINT"
    fi
fi



echo "Done!"
