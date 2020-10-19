#!/usr/bin/env bash

#/ Clone a Git repository.
#/
#/ Arguments:
#/
#/   1: Repository name.
#/      Optional.
#/      Default: $KURENTO_PROJECT
#/
#/   2: Branch, tag or commit hash.
#/      Optional.
#/      Default: $JOB_GIT_REF, or repo default branch.
#/
#/   3: Destination directory.
#/      Optional.
#/      Default: Repository name.
#/
#/
#/ Environment variables:
#/
#/ KURENTO_GIT_REPOSITORY="git@github.com:Kurento"
#/   Defined in Jenkins.
#/   Required.
#/
#/ JOB_GIT_REF
#/   Defined in Jenkins job.
#/   Optional.


# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="

# Trace all commands
set -o xtrace



# ------------ Script start ------------

# Check arguments, with default fallbacks
CLONE_NAME="${1:-${KURENTO_PROJECT:-}}"
CLONE_REF="${2:-${JOB_GIT_REF:-}}"
CLONE_DIR="${3:-${CLONE_NAME}}"

[[ -n "${KURENTO_GIT_REPOSITORY:-}" ]] \
|| { echo "ERROR Missing env var: KURENTO_GIT_REPOSITORY"; exit 1; }

# Internal variables
CLONE_URL="${KURENTO_GIT_REPOSITORY}/${CLONE_NAME}.git"

if [ -z "${GIT_KEY}" ]; then
    git clone "$CLONE_URL" "$CLONE_DIR" \
    || { log "ERROR Command failed: git clone"; exit 1; }
else
    ssh-agent bash -c "\
      ssh-add $GIT_KEY || exit 1; \
      git clone $CLONE_URL $CLONE_DIR || exit 1;" \
    || { log "ERROR Command failed: ssh-agent bash -c git clone"; exit 1; }
fi

{
    pushd "$CLONE_DIR"

    # If no checkout reference, use the repo default branch.
    if [[ -z "$CLONE_REF" ]]; then
        CLONE_REF="$(kurento_git_default_branch.sh)"
    fi

    log "Git clone $CLONE_URL ($CLONE_REF) to $PWD/$CLONE_DIR"

    git fetch . refs/changes/*:refs/changes/* \
    || { log "ERROR Command failed: git fetch"; exit 1; }

    git checkout "$CLONE_REF" \
    || { log "ERROR Command failed: git checkout $CLONE_REF"; exit 1; }

    if [ -f .gitmodules ]; then
        if [ -z "${GIT_KEY}" ]; then
            git submodule update --init --recursive \
            || { log "ERROR Command failed: git submodule update"; exit 1; }
        else
            ssh-agent bash -c "\
              ssh-add $GIT_KEY || exit 1; \
              git submodule update --init --recursive || exit 1;" \
            || { log "ERROR Command failed: ssh-agent bash -c git submodule update"; exit 1; }
        fi
    fi

    popd  # $CLONE_DIR
}



log "==================== END ===================="
