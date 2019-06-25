#!/usr/bin/env bash



# Shell setup
# -----------

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trap functions
on_error() {
    echo "[Docker entrypoint] ERROR ($?)"
    exit 1
}
trap on_error ERR



# Find or clone 'adm-scripts'
# ---------------------------

ADM_SCRIPTS_PATH="/adm-scripts"

if [[ -d "$ADM_SCRIPTS_PATH/.git" ]]; then
    echo "[Docker entrypoint] Kurento 'adm-scripts' found in $ADM_SCRIPTS_PATH"
else
    echo "[Docker entrypoint] Kurento 'adm-scripts' not found in $ADM_SCRIPTS_PATH"
    echo "[Docker entrypoint] Clone 'adm-scripts' from Git repo..."
    git clone https://github.com/Kurento/adm-scripts.git "$ADM_SCRIPTS_PATH"
fi



# Check the environment
# ---------------------

if [[ -d /hostdir ]]; then
    rm -rf /workdir
    cp -a /hostdir /workdir
    cd /workdir
fi



# Build packages for current dir
# ------------------------------

# Note: "$@" expands to all quoted arguments, as passed to this script
/adm-scripts/kurento-buildpackage.sh "$@"



# Finish
# ------

echo "[Docker entrypoint] Debian packages:"
find . -maxdepth 1 -type f -name '*.*deb'

# Get results out from the Docker container
if [[ -d /hostdir ]]; then
    mv ./*.*deb /hostdir/ 2>/dev/null || true
else
    echo "[Docker entrypoint] WARNING: No host dir where to put built packages"
fi
