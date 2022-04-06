#!/usr/bin/env bash

#/ Check out a given branch or tag name, if it exists.
#/
#/ This script will try to switch to the provided name in the current Git
#/ repository, and will revert back to the default branch if the desired one
#/ does not exist.
#/
#/
#/ Arguments
#/ ---------
#/
#/ --name <GitName>
#/
#/   Git branch or tag name that should be checked out, if it exists.
#/
#/   Optional. Default: Default repo branch.
#/   See also: '--fallback'.
#/
#/ --fallback <FallbackName>
#/
#/   Branch name that should be checked out when a <GitName> has been
#/   requested but it doesn't exist in the current repository.
#/
#/   Optional. Default: Default repo branch.
#/   See also: '--name'.



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

CFG_NAME=""
CFG_FALLBACK=""

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --name)
            if [[ -n "${2-}" ]]; then
                CFG_NAME="$2"
                shift
            else
                log "ERROR: --name expects <GitName>"
                log "Run with '--help' to read usage details"
                exit 1
            fi
            ;;
        --fallback)
            if [[ -n "${2-}" ]]; then
                CFG_FALLBACK="$2"
                shift
            else
                log "ERROR: --fallback expects <FallbackName>"
                log "Run with '--help' to read usage details"
                exit 1
            fi
            ;;
    esac
    shift
done

if [[ -z "$CFG_NAME" || -z "$CFG_FALLBACK" ]]; then
    # Default repo branch.
    GIT_DEFAULT="$(kurento_git_default_branch.sh)"

    CFG_NAME="${CFG_NAME:-$GIT_DEFAULT}"
    CFG_FALLBACK="${CFG_FALLBACK:-$GIT_DEFAULT}"
fi

log "CFG_NAME=$CFG_NAME"
log "CFG_FALLBACK=$CFG_FALLBACK"



# Check out the given branch or tag
# ---------------------------------

# Before checkout: Deinit submodules.
# Needed because submodule state is not carried over when switching branches.
# TODO UPGRADE: Change when dropping support for Xenial.
git submodule deinit . || true
#git submodule deinit --all

BRANCH_NAME="refs/remotes/origin/${CFG_NAME}"
TAG_NAME="refs/tags/${CFG_NAME}"

if git rev-parse --verify --quiet "$BRANCH_NAME"; then
    git checkout --track "$BRANCH_NAME"
elif git rev-parse --verify --quiet "$TAG_NAME"; then
    git checkout "$TAG_NAME"
else
    # Use the fallback name
    case "$CFG_FALLBACK" in
        xenial|bionic|focal|jammy)
            BRANCH_NAME="refs/remotes/origin/ubuntu/${CFG_FALLBACK}"
            ;;
        *)
            BRANCH_NAME="refs/remotes/origin/${CFG_FALLBACK}"
            ;;
    esac
    if git rev-parse --verify --quiet "$BRANCH_NAME"; then
        git checkout --track "$BRANCH_NAME"
    else
        # Default repo branch.
        GIT_DEFAULT="$(kurento_git_default_branch.sh)"
        git checkout "$GIT_DEFAULT"
    fi
fi

# After checkout: Re-init submodules.
git submodule update --init --recursive



log "==================== END ===================="

exit ${status:-0}
