#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

# Strict error checking and other utilities for Bash scripts.
#
# This file applies safer settings than the defaults in a Bash shell,
# and also defines some useful functions that other scripts can use.
#
# To use this file, source it from any other Bash script:
#
#     source bash.conf.sh
#
#
# Features
# ========
#
# * Strict error checking.
#   Errors won't be allowed in either standalone or piped commands.
#   Accessing undefined variables is also caught as an error.
#   Any of these situations will abort the script and exit, instead of
#   the default behavior of ignoring silently and continuing execution.
#
# * Exit traps.
#   To complement the strict error checking, trap functions will
#   run before the script exits, to print information about its success.
#
# * Log function.
#   The `log` function prints log messages. If the shell debug trace mode
#   is enabled (xtrace), this function takes care of first disabling it;
#   that way, the `echo` commands don't get duplicated at the output.
#
# * Help comments.
#   All script comments that start with `#/` will be printed as a help
#   message when the script is called with the "-h" or "--help" argument.
#   This allows to write self-documenting headers in script files.



# Shell setup
# ===========

# Bash options for strict error checking.
set -o errexit -o errtrace -o pipefail -o nounset
shopt -s inherit_errexit 2>/dev/null || true

# Trace all commands (to stderr).
#set -o xtrace

# Log function.
# Disables and re-enables command traces, if needed.
# Source: https://superuser.com/a/1338887/922762
shopt -s expand_aliases # This trick requires enabling aliases in Bash.
function echo_and_restore {
    local SELF_FILE; SELF_FILE="$(basename "${BASH_SOURCE[-1]}")" # File name of the running script.
    echo "[$SELF_FILE] $(cat -)"
    # shellcheck disable=SC2154
    case "$flags" in (*x*) set -o xtrace; esac
}
alias log='({ flags="$-"; set +o xtrace; } 2>/dev/null; echo_and_restore) <<<'

# Error trap function.
# Captures the return code of any error as soon as it happens.
# NOTE: Not sure under what conditions this is needed? Commented out for now.
# function on_error {
#     _RC=$?
# }
# trap on_error ERR

# Exit trap function.
# Runs always at the end, either on success or error (errexit).
function on_exit {
    { _RC=$?; set +o xtrace; } 2>/dev/null
    if ((_RC)); then log "ERROR ($_RC)"; fi
}
trap on_exit EXIT

# Add custom commands to run on trap handler.
function trap_add {
    local COMMAND="$1"
    local NAME="$2"
    local TRAP;
    # Append the new command to the already existing trap command, if any.
    TRAP="$(trap -p "$NAME" | { grep -o "'.*'" || true; } | tr -d "'"); $COMMAND"
    # shellcheck disable=SC2064
    trap "$TRAP" "$NAME"
}

# Help message.
# Extracts and prints text from special comments in the script header.
function usage { grep '^#/' "${BASH_SOURCE[-1]}" | cut -c 4-; exit 0; }
if [[ "${1:-}" =~ ^(-h|--help)$ ]]; then usage; fi



# Apt utilities
# =============

# Run `apt-get update` with bounded retries.
#
# Apt can fail transiently when a mirror is being synchronized: indexes with
# an unexpected size ("Mirror sync in progress?"), indexes that fail to
# download, etc. Plain `apt-get update` may then exit successfully, or the
# caller may ignore the exit code, and the following `apt-get install` would run
# with incomplete indexes.
#
# This function retries while `apt-get update` exits with an error, or while its
# output has "Failed to fetch" errors or "Some index files failed to download".
# Waits grow linearly between attempts, up to a maximum.
#
# If all attempts fail, a WARNING is logged and the function returns 0, which
# is the same as plain `apt-get update` when its result is ignored; this
# tolerates optional or flaky repositories (e.g. Ubuntu ddebs), and a later
# `apt-get install` still fails if the indexes it needs are really missing.
# With APT_UPDATE_STRICT=true, an ERROR is logged and it returns 1 instead.
#
# Extra arguments are passed to `apt-get`, e.g.: `apt_update_retry -qq`.
#
# Environment variables (all optional):
# * APT_UPDATE_RETRIES: Number of attempts (default: 5).
# * APT_UPDATE_DELAY: Seconds to wait after the first failure (default: 15).
#   The wait grows by the same amount in each attempt (15, 30, 45, ...).
# * APT_UPDATE_DELAY_MAX: Maximum wait between attempts (default: 60).
# * APT_UPDATE_STRICT: If "true", fail when all attempts fail (default: false).
#
# Usage: replace `apt-get update` with `apt_update_retry`.
function apt_update_retry {
    local MAX="${APT_UPDATE_RETRIES:-5}"
    local DELAY="${APT_UPDATE_DELAY:-15}"
    local DELAY_MAX="${APT_UPDATE_DELAY_MAX:-60}"
    local STRICT="${APT_UPDATE_STRICT:-false}"
    local OUT; OUT="$(mktemp)"
    local N=1 RC WAIT

    while true; do
        RC=0
        # Show the output as it comes, and keep a copy to inspect it.
        apt-get update "$@" 2>&1 | tee "$OUT" || RC="${PIPESTATUS[0]}"

        if ((RC == 0)) && ! grep -qE \
            '^(E|W): Failed to fetch|Some index files failed to download' \
            "$OUT"; then
            rm -f "$OUT"
            return 0
        fi

        if ((N >= MAX)); then
            rm -f "$OUT"
            if [[ "$STRICT" == "true" ]]; then
                log "ERROR: 'apt-get update' failed after $MAX attempts"
                return 1
            fi
            log "WARNING: 'apt-get update' failed after $MAX attempts; continuing, indexes may be incomplete"
            return 0
        fi

        WAIT=$((DELAY * N))
        if ((WAIT > DELAY_MAX)); then WAIT="$DELAY_MAX"; fi
        log "WARNING: 'apt-get update' failed (attempt $N of $MAX, exit code $RC); retrying in $WAIT seconds"
        # Don't leave the temporary file behind if interrupted while waiting.
        sleep "$WAIT" || { rm -f "$OUT"; return 1; }
        N=$((N + 1))
    done
}
