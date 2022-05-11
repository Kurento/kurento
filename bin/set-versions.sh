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
#/ <BaseVersion>
#/
#/   Base version number to use. When '--release' is used, this string will
#/   be set as-is; otherwise, a nightly/snapshot suffix is added.
#/
#/   <BaseVersion> must be in the Semantic Versioning format, such as "1.2.3"
#/   ("<Major>.<Minor>.<Patch>").
#/
#/ --release
#/
#/   Do not add nightly/snapshot suffix to the base version number.
#/   The resulting value will be valid for a Release build.
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
command -v mvn >/dev/null || {
    echo "ERROR: 'mvn' is not installed; please install it"
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
        --kms-api)
            # Ignore argument.
            if [[ -n "${2-}" ]]; then
                shift
            fi
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
    echo "ERROR: '$CFG_VERSION' is not SemVer (<Major>.<Minor>.<Patch>)"
    exit 1
}

echo "CFG_VERSION=$CFG_VERSION"
echo "CFG_RELEASE=$CFG_RELEASE"
echo "CFG_GIT_ADD=$CFG_GIT_ADD"



# Internal variables
# ==================

if [[ "$CFG_RELEASE" == "true" ]]; then
    VERSION_JAVA="$CFG_VERSION"
else
    VERSION_JAVA="${CFG_VERSION}-SNAPSHOT"
fi



# Helper functions
# ================

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

MVN_ARGS=()

if [[ "$CFG_RELEASE" == "true" ]]; then
    MVN_ALLOW_SNAPSHOTS="false"
else
    MVN_ALLOW_SNAPSHOTS="true"
    MVN_ARGS+=(-U -Psnapshot)
fi

# Parent version: Update to latest available.
mvn "${MVN_ARGS[@]}" versions:update-parent \
    -DgenerateBackupPoms=false \
    -DparentVersion="[$VERSION_JAVA,)" \
    -DallowSnapshots="$MVN_ALLOW_SNAPSHOTS"

# Children versions: Make them inherit from parent.
mvn "${MVN_ARGS[@]}" versions:update-child-modules \
    -DgenerateBackupPoms=false \
    -DallowSnapshots="$MVN_ALLOW_SNAPSHOTS"

git_add \
    '*pom.xml'

echo "Done!"
