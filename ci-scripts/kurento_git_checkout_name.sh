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

BRANCH_NAME="refs/remotes/$CFG_NAME"
BRANCH_NAME_REMOTE="refs/remotes/origin/$CFG_NAME"
TAG_NAME="refs/tags/$CFG_NAME"

CHECKOUT_TARGET=""

if git rev-parse --quiet --verify --end-of-options "$BRANCH_NAME"; then
    CHECKOUT_TARGET="$BRANCH_NAME"
elif git rev-parse --quiet --verify --end-of-options "$BRANCH_NAME_REMOTE"; then
    CHECKOUT_TARGET="$BRANCH_NAME_REMOTE"
elif git rev-parse --quiet --verify --end-of-options "$TAG_NAME"; then
    CHECKOUT_TARGET="$TAG_NAME"
else
    # Use the fallback name
    case "$CFG_FALLBACK" in
        xenial|bionic|focal|jammy|noble)
            BRANCH_NAME="refs/remotes/origin/ubuntu/$CFG_FALLBACK"
            ;;
        *)
            BRANCH_NAME="refs/remotes/origin/$CFG_FALLBACK"
            ;;
    esac
    if git rev-parse --quiet --verify --end-of-options "$BRANCH_NAME"; then
        CHECKOUT_TARGET="$BRANCH_NAME"
    else
        # Default HEAD branch in the remote origin.
        CHECKOUT_TARGET="$(kurento_git_default_branch.sh)"
    fi
fi

# Do nothing it the requested ref turns out to be the current one.
if [[ "$CHECKOUT_TARGET" == "$(git rev-parse --verify HEAD)" ]]; then
    log "Current commit is already '$CHECKOUT_TARGET'; nothing to do"
    exit 0
fi

# Before checkout: Deinit submodules.
# Needed because submodule state is not carried over when switching branches.
git submodule deinit --all

git checkout "$CHECKOUT_TARGET"

# After checkout: Re-init submodules.
git submodule update --init --recursive



log "==================== END ===================="

exit ${status:-0}
