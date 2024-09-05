#!/usr/bin/env bash

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trace all commands
set -o xtrace

# Trap functions
function on_error() {
    echo "[Docker entrypoint] ERROR ($?)"
    exit 1
}
trap on_error ERR

# Settings
if [[ "${APT_KEEP_CACHE:-}" == "true" ]]; then
    # Disable the cleaning of Apt package cache
    if [[ -w /etc/apt/apt.conf.d/docker-clean ]]; then
        # Comment out all line(s) that weren't already a comment
        sed --in-place "s|^[^/]|//|" /etc/apt/apt.conf.d/docker-clean
    fi
fi

# Find or clone 'ci-scripts'
KURENTO_SCRIPTS_PATH="/ci-scripts"
if [[ -e "$KURENTO_SCRIPTS_PATH/rust-buildpackage.sh" ]]; then
    echo "[Docker entrypoint] rust ci-scripts found in $KURENTO_SCRIPTS_PATH"
else
    echo "[Docker entrypoint] rust ci-scripts not found in $KURENTO_SCRIPTS_PATH"
    echo "[Docker entrypoint] rust ci-scripts from Git repo..."
    git clone --depth 1 https://github.com/Kurento/kurento.git /tmp/kurento
    mkdir -p "$KURENTO_SCRIPTS_PATH"
    cp -a /tmp/kurento/ci-scripts/* "$KURENTO_SCRIPTS_PATH"
fi

# Check the environment
if [[ -d /hostdir ]]; then
    rm -rf /build
    cp -a /hostdir /build
    cd /build/
fi

# Build packages for current dir
# Note: "$@" expands to all quoted arguments, as passed to this script
"$KURENTO_SCRIPTS_PATH/rust-buildpackage.sh" "$@"

# Get generated packages out from the Docker container
if [[ -d /hostdir ]]; then
    mv ./target/debian/*.*deb /hostdir/ 2>/dev/null || true
else
    echo "[Docker entrypoint] WARNING: No host dir where to put built packages"
fi
