#!/bin/bash -x

# Strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

echo "##################### EXECUTE: aptly_delete_repo #####################"

aptly publish drop ${DISTRIBUTION} s3:ubuntu:${DISTRIBUTION}
aptly repo drop kurento-experimental-${DISTRIBUTION}