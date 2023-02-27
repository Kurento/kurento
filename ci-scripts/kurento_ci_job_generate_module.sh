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
CFG_GENERATE_CMD_ARGS=()

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --java)
            CFG_JAVA="true"
            ;;
        --js)
            CFG_JS="true"
            ;;
        *)
            CFG_GENERATE_CMD_ARGS+=("$1")
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
log "CFG_GENERATE_CMD_ARGS=${CFG_GENERATE_CMD_ARGS[*]}"



# Run in container
# ================

if [[ "$CFG_JAVA" == "true" ]]; then
    GENERATE_CMD="kurento_generate_java_module.sh"
elif [[ "$CFG_JS" == "true" ]]; then
    GENERATE_CMD="kurento_generate_js_module.sh"
fi

docker run -i --rm --pull always \
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

# Run the module generation script.
"$GENERATE_CMD" ${CFG_GENERATE_CMD_ARGS[@]}

DOCKERCOMMANDS
