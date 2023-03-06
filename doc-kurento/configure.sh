#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ Initialization script for Kurento documentation.
#/
#/ This shell script prepares the sources for building. The main work done here
#/ is replacing all |PLACEHOLDER| variables with their actual values, as defined
#/ in `VERSIONS.env`.
#/
#/ Arguments: None.



# Configure shell
# ===============

# Bash options for strict error checking.
set -o errexit -o errtrace -o pipefail -o nounset
shopt -s inherit_errexit 2>/dev/null || true

# Trace all commands (to stderr).
set -o xtrace

# Absolute Canonical Path to the directory that contains this script.
SELF_DIR="$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null && pwd -P)"

# Help message.
# Extracts and prints text from special comments in the script header.
function usage { grep '^#/' "${BASH_SOURCE[-1]}" | cut -c 4-; exit 0; }
if [[ "${1:-}" =~ ^(-h|--help)$ ]]; then usage; fi



# Load VERSIONS file
# ==================

CONF_FILE="$SELF_DIR/VERSIONS.env"

[[ -f "$CONF_FILE" ]] || {
    echo "ERROR: Shell config file not found: $CONF_FILE"
    exit 1
}

# shellcheck source=VERSIONS.env
source "$CONF_FILE"



# Parse call arguments
# ====================

CFG_SOURCE_PATH="$SELF_DIR"

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --source)
            if [[ -n "${2-}" ]]; then
                CFG_SOURCE_PATH="$(realpath "$2")"
                shift
            else
                log "ERROR: --source expects <Path>"
                exit 1
            fi
            ;;
        *)
            log "ERROR: Unknown argument '${1-}'"
            exit 1
            ;;
    esac
    shift
done



# Replace placeholders
# ====================

# This expects a Bash Associative Array;
# enumerates all entries to use their name and values.

for NAME in "${!PROJECT_VERSIONS[@]}"; do
    VALUE="${PROJECT_VERSIONS[$NAME]}"

    # Use long option names for readability. Equivalent to this:
    # grep -lIrZ -- '<OldPattern>' | xargs -0 -L1 -r sed -i -e 's/<OldPattern>/<NewPattern>/g'

    # grep's exit code 1 means no lines selected, and >1 means an error.
    # Thus, exit code 1 must be handled to prevent `-o pipefail` from failing.

    {
        grep \
            --files-with-matches \
            --binary-files=without-match \
            --recursive \
            --null \
            "|$NAME|" "$CFG_SOURCE_PATH" \
        || [[ $? == 1 ]]
    } | xargs \
        --null \
        --max-lines=1 \
        --no-run-if-empty \
        sed \
            --in-place --expression="s/|$NAME|/$VALUE/g"
done

echo "Done!"
exit 0
