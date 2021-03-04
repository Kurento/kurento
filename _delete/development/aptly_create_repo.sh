#!/bin/bash -x

# Strict error checking
set -o errexit -o errtrace -o nounset

# Will CREATE or UPDATE a debian repository

echo "##################### EXECUTE: aptly_create_repo #####################"

# Check if repo exists
EXITS=$(aptly repo list | grep kurento-experimental-${DISTRIBUTION} | wc -l)

if [ "${EXITS}" == 0 ]; then
	aptly repo create -distribution="${DISTRIBUTION}" -component=kms6 "kurento-experimental-${DISTRIBUTION}"
fi

aptly repo add -force-replace "kurento-experimental-${DISTRIBUTION}" ./*.*deb

if [ "$EXITS" == 0 ]; then
	aptly -gpg-key="${GPGKEY}" publish repo "kurento-experimental-${DISTRIBUTION}" s3:ubuntu:${DISTRIBUTION}
else
	aptly -gpg-key="${GPGKEY}" publish update "${DISTRIBUTION}" s3:ubuntu:${DISTRIBUTION}
fi

