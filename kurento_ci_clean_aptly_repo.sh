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
        *)
            echo "ERROR: Unknown argument '${1-}'"
            echo "Run with '--help' to read usage details"
            exit 1
            ;;
    esac
    shift
done



# Feed the variables
CFG_REPO_NAME="kurento-openvidu-${CFG_DISTRO_NAME}-dev"
CFG_PUBLISH_NAME="dev"


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



# Build the script which will be run in proxy
cat >run.sh<<EOF
# Step 1: Check if repo exists
# ----------------------------

REPO_EXISTS=\$(aptly repo list | grep --count "$CFG_REPO_NAME") || true
if [[ "\$REPO_EXISTS" == "0" ]]; then
    echo "Repo: $CFG_REPO_NAME does not exist"
    exit 0
fi

# Step 2: Unpublish the repo
# --------------------------

PUBLISH_ENDPOINT="s3:ubuntu:${CFG_PUBLISH_NAME}"
aptly publish drop "$CFG_DISTRO_NAME" "\$PUBLISH_ENDPOINT"

# Step 3: Create a list with the content of the repository
# --------------------------------------------------------

PKG_LIST=\$(mktemp -t kurento-purge-XXX --suffix .sh)
aptly repo show -with-packages "$CFG_REPO_NAME" \
  | awk '{ print "aptly repo remove $CFG_REPO_NAME " \$1 }' > \${PKG_LIST}

# Step 4: Purge the repository
# ----------------------------

/bin/bash \${PKG_LIST} || true
EOF



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
	# TODO: Maybe a more descriptive message?
    echo "There was an error running the script."
}
trap on_exit EXIT

scp -o StrictHostKeyChecking=no -i secret.pem \
    run.sh \
    ubuntu@proxy.openvidu.io:

ssh -n -o StrictHostKeyChecking=no -i ./secret.pem \
    ubuntu@proxy.openvidu.io '\
        ./run.sh'

ssh -n -o StrictHostKeyChecking=no -i ./secret.pem \
    ubuntu@proxy.openvidu.io '\
        rm run.sh'
EOF



# Delete SSH key
rm secret.pem

echo "Done!"
