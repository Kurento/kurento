#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ Install Module build dependencies.



# Configure shell
# ===============

SELF_DIR="$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null && pwd -P)"
source "$SELF_DIR/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="
trap_add 'log "==================== END ===================="' EXIT

# Trace all commands (to stderr).
set -o xtrace

log "Install build dependencies"

# Notes:
# * DEBIAN_FRONTEND: In clean Ubuntu systems 'tzdata' might not be installed
#   yet, but it may be now, so make sure interactive dialogues are disabled.
# * Debug::pkgProblemResolver=yes: Show details about the dependency resolution.
#   Doc: http://manpages.ubuntu.com/manpages/man5/apt.conf.5.html
# * --target-release '*-backports': Prefer installing newer versions of packages
#   from the backports repository.
(
    export DEBIAN_FRONTEND=noninteractive

    apt-get update ; mk-build-deps --install --remove \
        --tool="apt-get -o Debug::pkgProblemResolver=yes --no-install-recommends --no-remove --yes" \
        ./debian/control
)


