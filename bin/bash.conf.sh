#!/bin/bash
# (shebang not really needed, but required by ShellCheck)

# Default settings for Bash scripts.
#
# This config file sets safer settings than the defaults in a Bash shell,
# and also defines some useful functions that other scripts can use.
#
# Checking your scripts with ShellCheck.net is still needed!
# ShellCheck is an online static analyzer that will help you
# write better and safer scripts. USE IT!
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
#   Also, no access to undefined variables will be accepted.
#   Any of these situations will abort the script and exit, instead of
#   the normal behavior of continuing silently.
#
# * Exit traps.
#   To complement the strict error checking, trap functions will
#   run before the script exits, to print information about its success.
#
# * Log function.
#   The log() function prints log messages. If the shell debug trace mode
#   is enabled (`set -x`), this function takes care of first disabling it;
#   that way, the `echo` commands don't get duplicated at the output.
#
# * Help comments.
#   All script comments that start with '#/' will be printed as a help
#   message when the script is called with '-h' or '--help' arguments.
#   This allows to write self-documenting headers in script files.



# Shell setup
# -----------

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Log function
# This disables and re-enables debug trace mode (only if it was already set)
# Source: https://superuser.com/a/1338887/922762
shopt -s expand_aliases  # This trick requires enabling aliases in Bash
BASENAME="$(basename "$0")"  # Complete file name
echo_and_restore() {
    echo "[${BASENAME}] $(cat -)"
    # shellcheck disable=SC2154
    case "$flags" in (*x*) set -x ; esac
}
alias log='({ flags="$-"; set +x; } 2>/dev/null; echo_and_restore) <<<'

# Trap functions
on_error() {
    _ERR=$?
}
trap on_error ERR
on_exit() {
    _ERR="${_ERR:-$?}"  # Get either trap code, or this script's exit code
    if ((_ERR)); then log "ERROR ($_ERR)"; else log "SUCCESS"; fi
    log "#################### END ####################"
}
trap on_exit EXIT



log "==================== BEGIN ===================="



# Help message (extracted from script headers)
usage() { grep '^#/' "$0" | cut --characters=4- ; exit 0 ; }
REGEX='^(-h|--help)$'
[[ "${1:-}" =~ $REGEX ]] && usage || true
