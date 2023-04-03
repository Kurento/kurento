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



# Parse call arguments
# ====================

CFG_JAVA="false"
CFG_JS="false"
CFG_SERVER_VERSION="dev"

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --java)
            CFG_JAVA="true"
            ;;
        --js)
            CFG_JS="true"
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

if [[ "$CFG_JAVA" != "true" ]] && [[ "$CFG_JS" != "true" ]]; then
    log "ERROR: Either of '--java' or '--js' must be used"
    exit 1
fi

if [[ "$CFG_JAVA" == "true" ]] && [[ "$CFG_JS" == "true" ]]; then
    log "ERROR: '--java' and '--js' cannot be used together"
    exit 1
fi

log "CFG_JAVA=$CFG_JAVA"
log "CFG_JS=$CFG_JS"
log "CFG_SERVER_VERSION=$CFG_SERVER_VERSION"



# Create container
# ================

if [[ "$CFG_JS" == "true" ]]; then
    # shellcheck disable=SC2054
    DOCKER_ARGS+=(--mount type=bind,src="$GIT_SSH_KEY_PATH",dst=/id_git_ssh)
fi

CONTAINER_NAME="kurento_ci_job_generate_module_${JOB_TIMESTAMP}_$(mktemp --dry-run XXXXXX)"

# Create a new container that runs Bash indefinitely.
# To keep Bash alive, a terminal is attached to the container (`-t`).
docker run -t --detach \
    --pull always \
    --rm --name "$CONTAINER_NAME" \
    --mount type=bind,src="$CI_SCRIPTS_PATH",dst=/ci-scripts \
    --mount type=bind,src="$PWD",dst=/workdir \
    kurento/kurento-ci-buildtools:focal

# Stop (which also removes, due to `--rm`) the container upon script exit.
trap_add "docker stop --time 3 $CONTAINER_NAME" EXIT



# Prepare container
# =================

# Install the required version of Kurento into a container.
docker exec -i \
    --workdir /workdir \
    "$CONTAINER_NAME" bash <<DOCKERCOMMANDS

# Bash options for strict error checking.
set -o errexit -o errtrace -o pipefail -o nounset
shopt -s inherit_errexit 2>/dev/null || true

# Trace all commands (to stderr).
set -o xtrace

# Configure the environment.
export PATH="/ci-scripts:\$PATH"

# Get the name of the current project, to only install its dev package.
PROJECT_NAME="\$(kurento_get_name.sh)" || {
   echo "ERROR: Command failed: kurento_get_name"
   exit 1
}

# Install the appropriate dev package.
kurento_install_server.sh \
    --server-package "\${PROJECT_NAME}-dev" \
    --server-version "$CFG_SERVER_VERSION"

DOCKERCOMMANDS



# Run in container
# ================

GENERATE_ARGS=()

if [[ "$CFG_JAVA" == "true" ]]; then
    GENERATE_CMD="kurento_generate_java_module.sh"
elif [[ "$CFG_JS" == "true" ]]; then
    GENERATE_CMD="kurento_generate_js_module.sh"
    GENERATE_ARGS+=(--git-ssh-key /id_git_ssh)

    if [[ "$JOB_RELEASE" == "true" ]]; then
        GENERATE_ARGS+=(--release)
    fi
fi

# `--user` is needed to avoid creating files as root, which would make next
# jobs fail because the runner cannot do workspace cleanup.
docker exec -i \
    --user "$(id -u)":"$(id -g)" \
    --workdir /workdir \
    --env-file "$ENV_PATH" \
    "$CONTAINER_NAME" bash <<DOCKERCOMMANDS

# Bash options for strict error checking.
set -o errexit -o errtrace -o pipefail -o nounset
shopt -s inherit_errexit 2>/dev/null || true

# Trace all commands (to stderr).
set -o xtrace

# Configure the environment.
export PATH="/ci-scripts:\$PATH"

# Run the module generation script.
"$GENERATE_CMD" ${GENERATE_ARGS[@]}

DOCKERCOMMANDS
