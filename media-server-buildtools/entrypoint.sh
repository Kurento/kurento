#!/usr/bin/env bash

# ------------ Shell setup ------------

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trap functions
on_error() {
    echo "ERROR ($?)"
    exit 1
}
trap on_error ERR



# ------------ Script start ------------

ADM_SCRIPTS_PATH="/adm-scripts"


# ==== Clone or update 'adm-scripts' ====

if [[ -d "$ADM_SCRIPTS_PATH/.git" ]]; then
    echo "Kurento 'adm-scripts' found in $ADM_SCRIPTS_PATH"
else
    echo "Kurento 'adm-scripts' not found in $ADM_SCRIPTS_PATH"
    echo "Clone 'adm-scripts' from Git repo ..."
    git clone https://github.com/Kurento/adm-scripts.git "$ADM_SCRIPTS_PATH"
fi


# ==== Build the project ====

if [[ -d /hostdir ]]; then
    rm -rf /workdir
    cp -a /hostdir /workdir
    cd /workdir
fi

/adm-scripts/kurento-buildpackage.sh "$@"
# Note: "$@" expands to all quoted arguments, as passed to this script


# ==== Finish ====

BASENAME="$(basename "$0")"  # Complete file name
echo "[$BASENAME] Generated files:"
# `dh_builddeb` puts the generated .'deb' files in '../'
find .. -maxdepth 1 -type f ! -name "$BASENAME"

# Get results out from the Docker container
if [[ -d /hostdir ]]; then
    mv ../*.*deb /hostdir/ 2>/dev/null || true
else
    echo "WARNING: No host dir where to put build artifacts"
fi
