#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ Install Kurento Media Server or one of its packages in a given version.



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

CFG_SERVER_PACKAGE="kurento-media-server"
CFG_SERVER_VERSION="dev"

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --server-package)
            if [[ -n "${2-}" ]]; then
                CFG_SERVER_PACKAGE="$2"
                shift
            else
                log "ERROR: --server-package expects <PackageName>"
                exit 1
            fi
            ;;
        --server-version)
            if [[ -n "${2-}" ]]; then
                CFG_SERVER_VERSION="$2"
                shift
            else
                log "ERROR: --server-version expects <Version>"
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

log "CFG_SERVER_PACKAGE=$CFG_SERVER_PACKAGE"
log "CFG_SERVER_VERSION=$CFG_SERVER_VERSION"



# Install Kurento
# ===============

# Run apt-get/dpkg without interactive dialogue.
export DEBIAN_FRONTEND=noninteractive

# Get DISTRIB_* env vars.
source /etc/upstream-release/lsb-release 2>/dev/null || source /etc/lsb-release

# Add Kurento repository key for apt-get.
apt-get update ; apt-get install --no-install-recommends --yes \
    gnupg
# Add Kurento repository key for apt-get.
gpg -k
gpg --no-default-keyring --keyring /etc/apt/keyrings/kurento.gpg \
    --keyserver hkp://keyserver.ubuntu.com:80 \
    --recv-keys 234821A61B67740F89BFD669FC8A16625AFA7A83

# Add Kurento repository line for apt-get.
tee "/etc/apt/sources.list.d/kurento.list" >/dev/null <<EOF
deb [signed-by=/etc/apt/keyrings/kurento.gpg] http://ubuntu.openvidu.io/$DOCKER_KMS_VERSION $DISTRIB_CODENAME main
EOF

# Install package.
apt-get update ; apt-get install --no-install-recommends --yes \
    "$CFG_SERVER_PACKAGE"
