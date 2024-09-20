#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ CI job - Generate API client module for the current project.
#/
#/ This script is meant to be called from any CI job wanting to generate Java
#/ artifacts from a module that contains Kurento API definition files (.kmd).



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

DOC_DEPLOY_ARGS=()

if [[ "$JOB_RELEASE" == "true" ]]; then
    DOC_DEPLOY_ARGS+=(--release)
fi

# `--user` is needed to avoid creating files as root, which would make next
# jobs fail because the runner cannot do workspace cleanup.
docker run -i --rm --pull always \
    --user "$(id -u)":"$(id -g)" \
    --mount type=bind,src="$CI_SCRIPTS_PATH",dst=/ci-scripts \
    --mount type=bind,src="$GIT_SSH_KEY_PATH",dst=/id_git_ssh \
    --mount type=bind,src="$MAVEN_SETTINGS_PATH",dst=/maven-settings.xml \
    --mount type=bind,src="$PWD",dst=/workdir \
    --workdir /workdir/doc-kurento/ \
    --env-file "$ENV_PATH" \
    kurento/kurento-ci-buildtools:noble <<DOCKERCOMMANDS

# Bash options for strict error checking.
set -o errexit -o errtrace -o pipefail -o nounset
shopt -s inherit_errexit 2>/dev/null || true

# Trace all commands (to stderr).
set -o xtrace

# Configure the environment.
export PATH="/ci-scripts:\$PATH"

# Build and deploy the documentation.
doc_deploy.sh \
    --git-ssh-key /id_git_ssh \
    --maven-settings /maven-settings.xml \
    ${DOC_DEPLOY_ARGS[@]}

DOCKERCOMMANDS
