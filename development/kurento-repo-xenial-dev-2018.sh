#!/usr/bin/env bash
set -eu -o pipefail  # Abort on errors, disallow undefined variables
IFS=$'\n\t'          # Apply word splitting only on newlines and tabs

# Configure the Kurento package repository for `apt-get`.
#
# Sources:
# - kurento_build_run.txt
#
# Changes:
# 2017-10-03 Juan Navarro <juan.navarro@gmx.es>
# - Initial version.
# 2018-02-06 Fede Diaz <nordri@gmail.com>
# - New version for the new repositories

# Check root permissions
[ "$(id -u)" -eq 0 ] || { echo "Please run as root"; exit 1; }

apt-get update
apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83
apt-get install -y software-properties-common
add-apt-repository "deb http://ubuntu.openvidu.io/dev xenial kms6"
add-apt-repository "deb http://ubuntu.openvidu.io/externals xenial kms6-externals"
apt-get update

# ------------

echo ""
echo "[$0] Done."

