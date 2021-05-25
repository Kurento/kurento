#!/usr/bin/env bash
# File checked with ShellCheck (https://www.shellcheck.net/)

#/ Initialization script for Kurento documentation.
#/
#/ This shell script prepares the sources for building. The main work done here
#/ is replacing all placeholder variables with their actual values, as defined
#/ in 'VERSIONS.env'.
#/
#/ Arguments: None.



# ------------ Shell setup ------------

# Shell options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Help message (extracted from script headers)
usage() {
    grep '^#/' "$0" | cut --characters=4-
    exit 0
}
REGEX='^(-h|--help)$'
if [[ "${1:-}" =~ $REGEX ]]; then
    usage
fi

# Log function
BASENAME="$(basename "$0")"  # Complete file name
log() { echo "[$BASENAME] $*"; }



# ------------ Script start ------------

# ---- Load VERSIONS file ----

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
CONF_FILE="$BASEPATH/VERSIONS.env"
[[ -f "$CONF_FILE" ]] || {
    log "ERROR: Shell config file not found: $CONF_FILE"
    exit 1
}
# shellcheck source=VERSIONS.env
source "$CONF_FILE"



# ---- Put actual values in their placeholders ----

# This expects a Bash Associative Array;
# enumerates all entries to use their name and values
for NAME in "${!PROJECT_VERSIONS[@]}"; do
    VALUE="${PROJECT_VERSIONS[$NAME]}"

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
