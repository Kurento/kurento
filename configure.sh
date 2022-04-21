#!/usr/bin/env bash
#Checked with ShellCheck (https://www.shellcheck.net/)

#/ Initialization script for Kurento documentation.
#/
#/ This shell script prepares the sources for building. The main work done here
#/ is replacing all |PLACEHOLDER| variables with their actual values, as defined
#/ in `VERSIONS.env`.
#/
#/ Arguments: None.



# Shell setup
# ===========

# Bash options for strict error checking.
set -o errexit -o errtrace -o pipefail -o nounset
shopt -s inherit_errexit 2>/dev/null || true

# Trace all commands (to stderr).
#set -o xtrace

# Help message.
# Extracts and prints text from special comments in the script header.
function usage { grep '^#/' "${BASH_SOURCE[-1]}" | cut -c 4-; exit 0; }
if [[ "${1:-}" =~ ^(-h|--help)$ ]]; then usage; fi

# Log function.
BASENAME="$(basename "$0")"  # Complete file name
log() { echo "[$BASENAME] $*"; }



# Load VERSIONS file
# ==================

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path

CONF_FILE="$BASEPATH/VERSIONS.env"

[[ -f "$CONF_FILE" ]] || {
    log "ERROR: Shell config file not found: $CONF_FILE"
    exit 1
}

# shellcheck source=VERSIONS.env
source "$CONF_FILE"



# Replace placeholders
# ====================

# This expects a Bash Associative Array;
# enumerates all entries to use their name and values.

for NAME in "${!PROJECT_VERSIONS[@]}"; do
    VALUE="${PROJECT_VERSIONS[$NAME]}"

    # Use long option names for readability. Equivalent to this:
    # grep -lIrZ -- '<OldPattern>' | xargs -0 -L1 -r sed -i -e 's/<OldPattern>/<NewPattern>/g'

    grep \
        --files-with-matches \
        --binary-files=without-match \
        --recursive \
        --null \
        "|$NAME|" "$BASEPATH" \
        | xargs \
            --null \
            --max-lines=1 \
            --no-run-if-empty \
            sed \
                --in-place --expression="s/|$NAME|/$VALUE/g"
done

log "Done!"
exit 0
