#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ CI job - Deploy Java artifacts with Maven.
#/
#/ This script is meant to be called from any CI job wanting to deploy Java
#/ artifacts from its current working directory.



# Configure shell
# ===============

SELF_DIR="$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null && pwd -P)"
source "$SELF_DIR/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="
trap_add 'log "==================== END ===================="' EXIT

# Trace all commands (to stderr).
set -o xtrace



# Run in container
# ================

docker run -i --rm --pull always \
    --mount type=bind,src="$CI_SCRIPTS_PATH",dst=/ci-scripts \
    --mount type=bind,src="$PWD",dst=/workdir \
    --workdir /workdir \
    kurento/kurento-ci-buildtools:focal /bin/bash <<DOCKERCOMMANDS

# Bash options for strict error checking.
set -o errexit -o errtrace -o pipefail -o nounset
shopt -s inherit_errexit 2>/dev/null || true

# Trace all commands (to stderr).
set -o xtrace

# Add ci-scripts to PATH.
export PATH="/ci-scripts:\$PATH"

# Compile, package, and deploy the current project.
kurento_maven_deploy.sh

# Only create a tag if the deployment process was successful.
# Allow errors because the tag might already exist (like if the release
# is being done again after solving some deployment issue).
kurento_check_version.sh true || {
    echo "WARNING: Command failed: kurento_check_version (tagging enabled)"
}

DOCKERCOMMANDS
