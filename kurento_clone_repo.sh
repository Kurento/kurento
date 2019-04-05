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
#/      Default: $JOB_GIT_REF, or "master".
#/
#/   3: Destination directory.
#/      Optional.
#/      Default: Repository name.
#/
#/
#/ Environment variables:
#/
#/ KURENTO_GIT_REPOSITORY="git@github.com:Kurento"
#/   Defined in Jenkins


# ------------ Shell setup ------------

# Shell options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Logging functions
# These disable and re-enable debug mode (only if it was already set)
# Source: https://superuser.com/a/1338887/922762
shopt -s expand_aliases  # This trick requires enabling aliases in Bash
BASENAME="$(basename "$0")"  # Complete file name
echo_and_restore() {
    echo "[${BASENAME}] $(cat -)"
    case "$flags" in (*x*) set -x ; esac
}
alias log='({ flags="$-"; set +x; } 2>/dev/null; echo_and_restore) <<<'

# Trap functions
on_error() { ERROR=1; }
trap on_error ERR
on_exit() {
    (( ${ERROR-${?}} )) && log "ERROR" || log "SUCCESS"
    log "==================== END ===================="
}
trap on_exit EXIT

# Print help message
usage() { grep '^#/' "$0" | cut -c 4-; exit 0; }
expr match "${1-}" '^\(-h\|--help\)$' >/dev/null && usage

# Enable debug mode
set -o xtrace

log "#################### BEGIN ####################"



# ------------ Script start ------------

# Load arguments, with default fallbacks
CLONE_NAME="${1:-${KURENTO_PROJECT}}"
CLONE_REF="${2:-${JOB_GIT_REF:-master}}"
CLONE_DIR="${3:-${CLONE_NAME}}"

# Internal variables
CLONE_URL="${KURENTO_GIT_REPOSITORY}/${CLONE_NAME}.git"

log "Git clone $CLONE_URL ($CLONE_REF) to $PWD/$CLONE_DIR"

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

    git fetch . refs/changes/*:refs/changes/* \
    || { log "ERROR Command failed: git fetch"; exit 1; }

    git checkout "$CLONE_REF" \
    || { log "ERROR Command failed: git checkout"; exit 1; }

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
