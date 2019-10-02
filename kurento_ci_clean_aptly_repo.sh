#!/usr/bin/env bash

#/ CI - Clean repositories from Aptly.
#/
#/ This script will clean/remove all package from the repository provides.
#/ During day to day operations dev repositories tend to grow a lot and 
#/ is necesary a purge to avoid strange conflicts and behaviours.
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
#/   Name of the Ubuntu distribution for which the repo will be purge.
#/   E.g.: "xenial", "bionic"
#/
#/ --repo-name <RepoName>
#/
#/   Name of the repository that will be purge.
#/   E.g.: "kurento-openvidu-xenial-dev", "kurento-openvidu-bionic-dev"
#/
#/ --publish-name <PublishName>
#/
#/   Name of the Aptly publishing endpoint that should be used.
#/   E.g.: "s3:ubuntu:dev"
#/
#/   The repository URL will be like:
#/
#/       http://ubuntu.openvidu.io/<PublishName> <DistroName> kms6
#/



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



# Step 1: Check if repo exists
# ----------------------------

REPO_EXISTS="$(aptly repo list | grep --count "$CFG_REPO_NAME")" || true
if [[ "$REPO_EXISTS" == "0" ]]; then
    echo "Repo: $CFG_REPO_NAME does not exist"
    exit 0
fi



# Step 2: Unpublish the repo
# --------------------------

aptly publish drop "$CFG_DISTRO_NAME" "$PUBLISH_ENDPOINT"



# Step 3: Create a list with the content of the repository
# --------------------------------------------------------

PKG_LIST=$(mktemp -t kurento-purge-XXX --suffix .sh)
aptly repo show -with-packages "$CFG_REPO_NAME" \
  | awk '{ print "aptly repo remove $CFG_REPO_NAME " $1 }' > ${PKG_LIST}



# Step 4: Purge the repository
# ----------------------------

/bin/bash ${PKG_LIST} || true



echo "Done!"
