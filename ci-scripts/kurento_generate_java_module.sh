#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ Generate API client module for the current project.
#/
#/ Generates client code from Kurento API definition files (.kmd).



# Configure shell
# ===============

SELF_DIR="$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null && pwd -P)"
source "$SELF_DIR/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="
trap_add 'log "==================== END ===================="' EXIT

# Trace all commands (to stderr).
set -o xtrace



# Parse call arguments
# ====================

CFG_SERVER_VERSION="dev"

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --server-version)
            if [[ -n "${2-}" ]]; then
                CFG_SERVER_VERSION="$2"
                shift
            else
                log "ERROR: --server-version expects <KurentoVersion>"
                exit 1
            fi
            ;;
        *)
            log "ERROR: Unknown argument '${1-}'"
            exit 1
            ;;
    esac
    shift
done



# Validate config
# ===============

log "CFG_SERVER_VERSION=$CFG_SERVER_VERSION"



# Install development packages
# ============================

# Run apt-get/dpkg without interactive dialogue.
export DEBIAN_FRONTEND=noninteractive

# Get DISTRIB_* env vars.
source /etc/upstream-release/lsb-release 2>/dev/null || source /etc/lsb-release

# Get the name of the current project, to only install its dev package.
PROJECT_NAME="$(kurento_get_name.sh)" || {
    log "ERROR: Command failed: kurento_get_name"
    exit 1
}

apt-get update ; apt-get install --no-install-recommends --yes \
    gnupg

# Add Kurento repository key for apt-get.
apt-key adv \
    --keyserver keyserver.ubuntu.com \
    --recv-keys 234821A61B67740F89BFD669FC8A16625AFA7A83

# Add Kurento repository line for apt-get.
tee "/etc/apt/sources.list.d/kurento.list" >/dev/null <<EOF
deb [arch=amd64] http://ubuntu.openvidu.io/$CFG_SERVER_VERSION $DISTRIB_CODENAME main
EOF

# Install development packages.
apt-get update ; apt-get install --no-install-recommends --yes \
    "${PROJECT_NAME}-dev"



# Generate client code
# ====================

rm -rf build/
mkdir build/
cd build/

cmake -DGENERATE_JAVA_CLIENT_PROJECT=TRUE -DDISABLE_LIBRARIES_GENERATION=TRUE ..

cd java || {
  log "ERROR: Expected directory doesn't exist: $PWD/java"
  exit 1
}
