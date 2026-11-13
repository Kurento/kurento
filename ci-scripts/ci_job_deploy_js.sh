#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ CI job - Deploy JavaScript packages with Maven, and prepare them for NPM.
#/
#/ Packages for NPM are not published here: the verified tarball is left in the
#/ directory NPM_TARBALL_PATH for a separate publishing job.
#/
#/ This script is meant to be called from any CI job wanting to deploy JavaScript
#/ packages from its current working directory.



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

# `--user` is needed to avoid creating files as root, which would make next
# jobs fail because the runner cannot do workspace cleanup.
# The verified NPM tarball is left in NPM_TARBALL_PATH, in the host, so a next
# step of the workflow can upload it. Publishing to NPM is not done here.
mkdir -p "$NPM_TARBALL_PATH"

docker run -i --rm --pull always \
    --user "$(id -u)":"$(id -g)" \
    --mount type=bind,src="$CI_SCRIPTS_PATH",dst=/ci-scripts \
    --mount type=bind,src="$MAVEN_SETTINGS_PATH",dst=/maven-settings.xml \
    --mount type=bind,src="$MAVEN_GPG_KEY_PATH",dst=/maven.gpg \
    --mount type=bind,src="$NPM_TARBALL_PATH",dst=/npm-tarballs \
    --mount type=bind,src="$PWD",dst=/workdir \
    --workdir /workdir \
    --env-file "$ENV_PATH" \
    kurento/kurento-ci-buildtools:noble <<DOCKERCOMMANDS

# Bash options for strict error checking.
set -o errexit -o errtrace -o pipefail -o nounset
shopt -s inherit_errexit 2>/dev/null || true

# Trace all commands (to stderr).
set -o xtrace

# Configure the environment.
export PATH="/ci-scripts:\$PATH"
export NPM_TARBALL_DIR="/npm-tarballs"

# Compile, package, and deploy the current project.
deploy_js.sh \
    --maven-settings /maven-settings.xml \
    --maven-sign-key /maven.gpg

DOCKERCOMMANDS
