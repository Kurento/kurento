#!/usr/bin/env bash

# ------------ Shell setup ------------

# Shell options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trap functions
on_error() {
    echo "ERROR ($?)"
    exit 1
}
trap on_error ERR



# ------------ Script start ------------

cd /build

# Note: "$@" expands to all quoted arguments, as passed to this script
/adm-scripts/kurento-buildpackage.sh "$@"

BASENAME="$(basename "$0")"  # Complete file name
echo "[$BASENAME] Output files:"
find .. -maxdepth 1 -type f ! -name "$BASENAME"

mv ../*.*deb ./ 2>/dev/null || true
