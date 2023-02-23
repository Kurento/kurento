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
    --user="$(id -u)":"$(id -g)" \
    --mount type=bind,src="$CI_SCRIPTS_PATH",dst=/ci-scripts \
    --mount type=bind,src="$MAVEN_LOCAL_REPOSITORY_PATH",dst=/maven-repository \
    --mount type=bind,src="$MAVEN_SETTINGS_PATH",dst=/maven-settings.xml \
    --mount type=bind,src="$PWD",dst=/workdir \
    --workdir /workdir \
    --env-file "$ENV_PATH" \
    kurento/kurento-ci-buildtools:focal <<DOCKERCOMMANDS

# Bash options for strict error checking.
set -o errexit -o errtrace -o pipefail -o nounset
shopt -s inherit_errexit 2>/dev/null || true

# Trace all commands (to stderr).
set -o xtrace

# Configure the environment.
export PATH="/ci-scripts:\$PATH"
export MAVEN_LOCAL_REPOSITORY_PATH="/maven-repository"

# Compile, package, and deploy the current project.
kurento_maven_deploy.sh --maven-settings-path /maven-settings.xml

DOCKERCOMMANDS
