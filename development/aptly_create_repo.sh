#!/bin/bash -x

# Strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

echo "##################### EXECUTE: aptly_create_repo #####################"

aptly repo create -distribution="${DISTRIBUTION}" -component=kms6 "kurento-experimental-${DISTRIBUTION}"
aptly repo add -force-replace "kurento-experimental-${DISTRIBUTION}" ./*.*deb
aptly snapshot create "snap-kurento-experimental-${DISTRIBUTION}" from repo "kurento-experimental-${DISTRIBUTION}"
aptly -gpg-key="${GPGKEY}" publish snapshot "snap-kurento-experimental-${DISTRIBUTION}" "s3:ubuntu:${DISTRIBUTION}"

