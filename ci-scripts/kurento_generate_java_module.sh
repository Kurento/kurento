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
