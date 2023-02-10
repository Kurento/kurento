#!/usr/bin/env bash

#/ CI job - Generate RPC API client module from the current project.
#/
#/ This script is meant to be called from the "Execute shell" section of all
#/ Jenkins jobs which want to generate RPC API client code for their projects.
#/
#/
#/ Arguments
#/ ---------
#/
#/ --java
#/
#/   Generate client code for Java.
#/
#/ --js
#/
#/   Generate client code for JavaScript.
#/
#/ Either '--java' or '--js' must be provided.
#/
#/
#/ Variables
#/ ---------
#/
#/ This script expects some environment variables to be exported.
#/
#/ * Variable(s) from job parameters (with "This project is parameterized"):
#/
#/ JOB_GIT_NAME (not required, only used to enforce "main", see below)
#/
#/   Git branch or tag that should be checked out, if it exists.
#/
#/
#/ * Variable(s) from job Custom Tools (with "Install custom tools"):
#/
#/ KURENTO_SCRIPTS_HOME
#/
#/   Jenkins path to 'ci-scripts', containing all Kurento CI scripts.



# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="

# Trace all commands
set -o xtrace



# Parse call arguments
# --------------------

CFG_JAVA="false"
CFG_JS="false"

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --java)
            CFG_JAVA="true"
            ;;
        --js)
            CFG_JS="true"
            ;;
        *)
            log "ERROR: Unknown argument '${1-}'"
            log "Run with '--help' to read usage details"
            exit 1
            ;;
    esac
    shift
done



# Apply config restrictions
# -------------------------

if [[ "$CFG_JAVA" != "true" ]] && [[ "$CFG_JS" != "true" ]]; then
    log "ERROR: Either '--java' or '--js' must be provided"
    exit 1
fi

log "CFG_JAVA=$CFG_JAVA"
log "CFG_JS=$CFG_JS"



# Job setup
# ---------

# Don't build from experimental branches. Otherwise we'd need to have some
# mechanism to publish experimental module builds, which we don't have for
# Java and JavaScript modules.
#
# Maybe in the future we might have something like experimental Maven or NPM
# repositories, then we'd want to build experimental branches for them. But
# for now, just skip and avoid polluting the default builds repositories.
if [[ -n "${JOB_GIT_NAME:-}" ]]; then
    log "Skip building from experimental branch '$JOB_GIT_NAME'"
    exit 0
fi
# Check out the requested branch
# if [[ -n "${JOB_GIT_NAME:-}" ]]; then
#     "${KURENTO_SCRIPTS_HOME}/kurento_git_checkout_name.sh" --name "$JOB_GIT_NAME"
# fi



# Build
# -----

if [[ "$CFG_JAVA" == "true" ]]; then
    GEN_SCRIPT="kurento_generate_java_module.sh"
elif [[ "$CFG_JS" == "true" ]]; then
    GEN_SCRIPT="kurento_generate_js_module.sh"
fi

RUN_COMMANDS=(
    # Install any .deb files that might have been passed to this job
    # (with "Copy artifacts from another project")
    "dpkg --install ./*.*deb || { apt-get update ; apt-get install --fix-broken --no-remove --yes; }"

    # Install any .jar files that might have been passed to this job
    # (with "Copy artifacts from another project")
    # "find . -iname '*.jar' -print0 | xargs --no-run-if-empty -0 -P0 -I{} mvn --batch-mode org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file -Dfile='{}'"

    "$GEN_SCRIPT"
)

export CONTAINER_IMAGE="kurento/kurento-ci-buildtools:focal"
"${KURENTO_SCRIPTS_HOME}/kurento_ci_container_job_setup.sh" "${RUN_COMMANDS[@]}"



log "==================== END ===================="
