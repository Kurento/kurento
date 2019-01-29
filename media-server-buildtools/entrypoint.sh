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
if [[ -d /adm-scripts/.git ]]; then
    echo "Kurento 'adm-scripts' found: update"
    cd /adm-scripts
    git pull --rebase || true
else
    echo "Kurento 'adm-scripts' not found: clone"
    git clone https://github.com/Kurento/adm-scripts.git /adm-scripts
fi


# ==== Build the project ====
rm -rf /workdir
cp -a /hostdir /workdir
cd /workdir
/adm-scripts/kurento-buildpackage.sh "$@"
# Note: "$@" expands to all quoted arguments, as passed to this script


# ==== Finish ====
BASENAME="$(basename "$0")"  # Complete file name
echo "[$BASENAME] Generated files:"
# `dh_builddeb` puts the generated .'deb' files in '../'
find .. -maxdepth 1 -type f ! -name "$BASENAME"

# Get results out from the Docker container
mv ../*.*deb /hostdir 2>/dev/null || true
