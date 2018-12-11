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


# ==== Clone or update 'adm-scripts' ====
if [[ -d /adm-scripts ]]; then
    echo "Kurento 'adm-scripts' found: update"
    cd /adm-scripts
    git pull
else
    echo "Kurento 'adm-scripts' not found: clone"
    git clone https://github.com/Kurento/adm-scripts.git /adm-scripts
fi


# ==== Build the project ====
cd /build
/adm-scripts/kurento-buildpackage.sh "$@"
# Note: "$@" expands to all quoted arguments, as passed to this script


# ==== Finish ====
BASENAME="$(basename "$0")"  # Complete file name
echo "[$BASENAME] Output files:"
find .. -maxdepth 1 -type f ! -name "$BASENAME"

# Get results out from the Docker container
mv ../*.*deb ./ 2>/dev/null || true
