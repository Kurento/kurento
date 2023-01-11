#!/usr/bin/env bash

#/ Version change helper.
#/
#/ This shell script will traverse all subprojects and change their
#/ versions to the one provided as argument.
#/
#/
#/ Notice
#/ ======
#/
#/ This script does not use the Maven `versions` plugin (with goals such as
#/ `versions:update-parent`, `versions:update-child-modules`, or `versions:set`)
#/ because running Maven requires that the *current* versions are all correct
#/ and existing (available for download or installed locally).
#/
#/ We have frequently found that this is a limitation, because some times it is
#/ needed to update from an unexisting version (like if some component is
#/ skipping a patch number, during separate development of different modules),
#/ or when doing a Release (when the release version is not yet available).
#/
#/ It ends up being less troublesome to just edit the pom.xml directly.
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
#/ --kms-api <KmsVersion>
#/
#/   Also change version of the Kurento Media Server Java API packages.
#/
#/   This argument is used when a new version of the Media Server has been
#/   released, and the Java packages should be made to depend on the new API
#/   definition ones (which get published as part of the Media Server release).
#/
#/   <KmsVersion> is a full Maven version, such as "6.12.0-SNAPSHOT" or "6.12.0".
#/
#/   Optional. Default: None.
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
command -v xmlstarlet >/dev/null || {
    echo "ERROR: 'xmlstarlet' is not installed; please install it"
    exit 1
}

# Trace all commands.
set -o xtrace



# Parse call arguments
# ====================

CFG_VERSION=""
CFG_RELEASE="false"
CFG_KMS_API=""
CFG_GIT_ADD="false"

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --release)
            CFG_RELEASE="true"
            ;;
        --kms-api)
            if [[ -n "${2-}" ]]; then
                CFG_KMS_API="$2"
                shift
            else
                echo "ERROR: --kms-api expects <KmsVersion>"
                exit 1
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
echo "CFG_KMS_API=$CFG_KMS_API"
echo "CFG_GIT_ADD=$CFG_GIT_ADD"



# Internal variables
# ==================

if [[ "$CFG_RELEASE" == "true" ]]; then
    VERSION_JAVA="$CFG_VERSION"
else
    VERSION_JAVA="${CFG_VERSION}-SNAPSHOT"
fi

VERSION_KMS="$CFG_KMS_API"



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

# kurento-parent-pom
{
    pushd kurento-parent-pom/

    # NOTE: No need to update the parent version. Release docs already instruct
    # to update it manually whenever kurento-qa-pom is getting a new version.

    # Project version: Set new value.
    xmlstarlet edit -S --inplace \
        --update "/_:project/_:version" \
        --value "$VERSION_JAVA" \
        pom.xml

    # API dependencies: Set new Kurento API value.
    if [[ -n "$CFG_KMS_API" ]]; then
        MODULES=(
            kms-api-core
            kms-api-elements
            kms-api-filters
        )
        for MODULE in "${MODULES[@]}"; do
            xmlstarlet edit -S --inplace \
                --update "/_:project/_:properties/_:version.${MODULE}" \
                --value "$VERSION_KMS" \
                pom.xml
        done
    fi

    popd
}

# All except kurento-parent-pom
{
    # Parent: Update to the new version of kurento-parent-pom.
    xmlstarlet edit -S --inplace \
        --update "/_:project/_:parent/_:version" \
        --value "$VERSION_JAVA" \
        pom.xml

    # Project version: Inherited from parent.

    # Children: Make them inherit from the new parent.
    CHILDREN=(
        kurento-assembly
        kurento-basicroom
        kurento-client
        kurento-commons
        kurento-integration-tests
        kurento-jsonrpc
        kurento-repository
    )
    for CHILD in "${CHILDREN[@]}"; do
        find "$CHILD" -name pom.xml -print0 | xargs -0 -n1 \
            xmlstarlet edit -S --inplace \
                --update "/_:project/_:parent/_:version" \
                --value "$VERSION_JAVA"
    done
}

git_add \
    '*pom.xml'

echo "Done!"
