#!/usr/bin/env bash

#/ CI job - Deploy Java artifacts with Maven.
#/
#/ This script is meant to be called from the "Execute shell" section of all
#/ Jenkins jobs which want to deploy Java artifacts.
#/
#/
#/ Arguments
#/ ---------
#/
#/ None.
#/
#/
#/ Variables
#/ ---------
#/
#/ This script expects some environment variables to be exported.
#/
#/ * Variable(s) from job parameters (with "This project is parameterized"):
#/
#/ JOB_GIT_NAME (not required, only used to enforce "master", see below)
#/
#/   Git branch or tag that should be checked out, if it exists.
#/
#/
#/ * Variable(s) from job Custom Tools (with "Install custom tools"):
#/
#/ KURENTO_SCRIPTS_HOME
#/
#/   Jenkins path to 'adm-scripts', containing all Kurento CI scripts.



# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="

# Trace all commands
set -o xtrace



# Job setup
# ---------

# Don't build from experimental branches. Otherwise we'd need to have some
# mechanism to publish experimental module builds, which we don't have for
# Java and JavaScript modules.
#
# Maybe in the future we might have something like experimental Maven or NPM
# repositories, then we'd want to build experimental branches for them. But
# for now, just skip and avoid polluting the default builds repositories.
GIT_DEFAULT="$(kurento_git_default_branch.sh)"
JOB_GIT_NAME="${JOB_GIT_NAME:-master}"
if [[ "$JOB_GIT_NAME" != "$GIT_DEFAULT" ]]; then
    log "Skip building from experimental branch '$JOB_GIT_NAME'"
    exit 0
fi

# Check out the requested branch
"${KURENTO_SCRIPTS_HOME}/kurento_git_checkout_name.sh" --name "$JOB_GIT_NAME"



# Build
# -----

RUN_COMMANDS=(
    # Install any .jar files that might have been passed to this job
    # (with "Copy artifacts from another project")
    "find . -iname '*.jar' -print0 | xargs --no-run-if-empty -0 -P0 -I{} mvn --batch-mode install:install-file -Dfile='{}'"

    # Compile, package, and deploy the current project.
    "kurento_maven_deploy.sh"

    # Only create a tag if the deployment process was successful
    # Allow errors because the tag might already exist (like if the release
    # is being done again after solving some deployment issue).
    "kurento_check_version.sh true || { log 'WARNING: Command failed: kurento_check_version (tagging enabled)'; }"
)

export CONTAINER_IMAGE="kurento/kurento-ci-buildtools:xenial"
"${KURENTO_SCRIPTS_HOME}/kurento_ci_container_job_setup.sh" "${RUN_COMMANDS[@]}"



log "==================== END ===================="
