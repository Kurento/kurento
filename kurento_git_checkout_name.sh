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
#/ <Name>
#/
#/   Branch or tag name that should be checked out.



# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

# Trace all commands
set -o xtrace



# Parse call arguments
# --------------------

CFG_NAME_DEFAULT="0"
CFG_NAME="$CFG_NAME_DEFAULT"

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        *)
            CFG_NAME="$1"
            ;;
    esac
    shift
done



# Apply config restrictions
# -------------------------

if [[ "$CFG_NAME" == "$CFG_NAME_DEFAULT" ]]; then
    log "ERROR: Missing <Name>"
    exit 1
fi

log "CFG_NAME=${CFG_NAME}"



# Check out the given branch or tag
# ---------------------------------

BRANCH_NAME="refs/remotes/origin/${CFG_NAME}"
TAG_NAME="refs/tags/${CFG_NAME}"

if git rev-parse --verify --quiet "$BRANCH_NAME"; then
    git checkout "$BRANCH_NAME"
elif git rev-parse --verify --quiet "$TAG_NAME"; then
    git checkout "$TAG_NAME"
else
    git checkout master
fi
