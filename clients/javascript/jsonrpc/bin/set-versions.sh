#!/usr/bin/env bash

#/ Version change helper.
#/
#/ This shell script will traverse all subprojects and change their
#/ versions to the one provided as argument.
#/
#/
#/ Arguments
#/ =========
#/
#/ <Version>
#/
#/   Base version number to set. When '--release' is used, this version will
#/   be used as-is; otherwise, a nightly/snapshot indicator will be appended.
#/
#/   <Version> should be in a format compatible with Semantic Versioning,
#/   such as "1.2.3" or, in general terms, "<Major>.<Minor>.<Patch>".
#/
#/ --release
#/
#/   Use version numbers intended for Release builds, such as "1.2.3". If this
#/   option is not given, a nightly/snapshot indicator is appended: "-dev".
#/
#/   Optional. Default: Disabled.
#/
#/ --git-add
#/
#/   Add changes to the Git stage area. Useful to leave everything ready for a
#/   commit.
#/
#/   Optional. Default: Disabled.



# Shell setup
# ===========

# Bash options for strict error checking.
set -o errexit -o errtrace -o pipefail -o nounset

# Check dependencies.
command -v jq >/dev/null || {
    echo "ERROR: Dependency 'jq' is not installed; please install it"
    exit 1
}

# Trace all commands.
set -o xtrace



# Parse call arguments
# ====================

CFG_VERSION=""
CFG_RELEASE="false"
CFG_GIT_ADD="false"

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --release)
            CFG_RELEASE="true"
            ;;
        --git-add)
            CFG_GIT_ADD="true"
            ;;
        *)
            CFG_VERSION="$1"
            ;;
    esac
    shift
done



# Config restrictions
# ===================

if [[ -z "$CFG_VERSION" ]]; then
    echo "ERROR: Missing <Version>"
    exit 1
fi

REGEX='^[[:digit:]]+\.[[:digit:]]+\.[[:digit:]]+$'
[[ "$CFG_VERSION" =~ $REGEX ]] || {
    echo "ERROR: '$CFG_VERSION' must be compatible with Semantic Versioning: <Major>.<Minor>.<Patch>"
    exit 1
}

echo "CFG_VERSION=$CFG_VERSION"
echo "CFG_RELEASE=$CFG_RELEASE"
echo "CFG_GIT_ADD=$CFG_GIT_ADD"



# Internal variables
# ==================

if [[ "$CFG_RELEASE" == "true" ]]; then
    VERSION="$CFG_VERSION"
else
    VERSION="${CFG_VERSION}-dev"
fi



# Helper functions
# ================

# Run jq, which doesn't have "in-place" mode like other UNIX tools.
function run_jq() {
    [[ $# -ge 2 ]] || {
        echo "ERROR [run_jq]: Missing argument(s): <filter> <file>"
        return 1
    }
    local FILTER="$1"
    local FILE="$2"

    local TEMP
    TEMP="$(mktemp)"

    /usr/bin/jq "$FILTER" "$FILE" >"$TEMP" && mv "$TEMP" "$FILE"
}

# Add the given file(s) to the Git stage area.
function git_add() {
    [[ $# -ge 1 ]] || {
        echo "ERROR [git_add]: Missing argument(s): <file1> [<file2> ...]"
        return 1
    }

    if [[ "$CFG_GIT_ADD" == "true" ]]; then
        git add -- "$@"
    fi
}



# Apply version
# =============

run_jq ".version = \"$VERSION\"" package.json
git_add \
    package.json

echo "Done!"
