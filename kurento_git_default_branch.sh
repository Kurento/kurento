#!/usr/bin/env bash

#/ Get the name of the Git repo default branch.
#/
#/ The default branch is the branch where the HEAD of the remote repository is
#/ pointing. In GitHub, this coincides with the "default branch" setting that is
#/ configured in the web interface.
#/
#/
#/ Arguments
#/ ---------
#/
#/ None.



# NOTE: DO NOT print debug messages to stdout; callers expect that this script
# only outputs its intended result.
# If you need debug, make sure it gets redirected to stderr.



# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

# Trace all commands (to stderr).
#set -o xtrace



# Get the remote default branch (remote HEAD).
BRANCH=""

# Query the remote heads, to get the latest available data.
# Fails if the Git URL is SSH, but the key is not available:
#     Permission denied (publickey).
#     fatal: Could not read from remote repository.
if OUT="$(git remote show origin 2>/dev/null | grep -Po 'HEAD branch: \K(.*)')"; then
    BRANCH="$OUT"

# Try directly against the remote URL.
elif OUT="$(git remote show "$(git ls-remote --get-url)"  2>/dev/null | grep -Po 'HEAD branch: \K(.*)')"; then
    BRANCH="$OUT"

# Maybe it failed due to missing SSH keys. Try with HTTPS.
else
    # Save previous config.
    CONFIG="$(git config url."https://github.com/".insteadOf)" || true

    # Use HTTPS instead of SSH.
    git config url."https://github.com/".insteadOf "git@github.com:"

    # Query through the new URL.
    if OUT="$(git remote show origin 2>/dev/null | grep -Po 'HEAD branch: \K(.*)')"; then
        BRANCH="$OUT"
    fi

    # Restore previous config.
    if [[ -n "$CONFIG" ]]; then
        git config url."https://github.com/".insteadOf "$CONFIG"
    else
        git config --remove-section url."https://github.com/"
    fi
fi

# If none of the remote query methods worked, look in the cached data.
# This works only if this repo is a clone; will fail otherwise, for example with
# a detached fetch like what the Jenkins Git plugin does (because the remote
# HEAD doesn't get cached locally).
if [[ -z "$BRANCH" ]] && OUT="$(grep -Po 'refs/remotes/origin/\K(.*)' .git/refs/remotes/origin/HEAD 2>/dev/null)"; then
    BRANCH="$OUT"
fi

if [[ -n "$BRANCH" ]]; then
    echo "$BRANCH"
else
    echo "master"
fi
