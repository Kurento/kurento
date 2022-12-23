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
    # { _RC=${_RC:-$?}; set +o xtrace; } 2>/dev/null
    { _RC=$?; set +o xtrace; } 2>/dev/null
    if ((_RC)); then log "ERROR ($_RC)"; fi
}
trap on_exit EXIT

# Help message.
# Extracts and prints text from special comments in the script header.
function usage { grep '^#/' "${BASH_SOURCE[-1]}" | cut -c 4-; exit 0; }
if [[ "${1:-}" =~ ^(-h|--help)$ ]]; then usage; fi
