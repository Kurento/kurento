#!/usr/bin/env bash

#/ Check out a given branch or tag name, if it exists.
#/
#/ This script will try to check out the provided name in the current Git
#/ repository, and will revert back to the name 'master' if the desired one
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
#/   Optional. Default: 'master'.
#/   See also: '--fallback'.
#/
#/ --fallback <FallbackName>
#/
#/   Branch name that should be checked out when a <GitName> has been
#/   requested but it doesn't exist in the current repository.
#/
#/   Optional. Default: 'master'.
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

CFG_NAME="master"
CFG_FALLBACK="master"

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

log "CFG_NAME=$CFG_NAME"
log "CFG_FALLBACK=$CFG_FALLBACK"



# Check out the given branch or tag
# ---------------------------------

BRANCH_NAME="refs/remotes/origin/${CFG_NAME}"
TAG_NAME="refs/tags/${CFG_NAME}"

if git rev-parse --verify --quiet "$BRANCH_NAME"; then
    git checkout --track "$BRANCH_NAME"
elif git rev-parse --verify --quiet "$TAG_NAME"; then
    git checkout "$TAG_NAME"
else
    # Use the fallback name
    case "$CFG_FALLBACK" in
        xenial|bionic)
            BRANCH_NAME="refs/remotes/origin/ubuntu/${CFG_FALLBACK}"
            ;;
        *)
            BRANCH_NAME="refs/remotes/origin/${CFG_FALLBACK}"
            ;;
    esac
    if git rev-parse --verify --quiet "$BRANCH_NAME"; then
        git checkout --track "$BRANCH_NAME"
    else
        git checkout master
    fi
fi



log "==================== END ===================="

exit ${status:-0}
