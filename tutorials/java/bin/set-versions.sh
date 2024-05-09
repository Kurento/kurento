#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

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
#/ needed to update from an nonexistent version (like if some component is
#/ skipping a patch number, during separate development of different modules),
#/ or when doing a Release (when the release version is not yet available).
#/
#/ It ends up being less troublesome to just edit the pom.xml directly.
#/
#/
#/ Arguments
#/ =========
#/
#/ <Version>
#/
#/   Base version number to set. When '--release' is used, this version will
#/   be used as-is; otherwise, a development/snapshot suffix is added.
#/
#/   <Version> should be in Semantic Versioning format, such as "1.0.0"
#/   ("<Major>.<Minor>.<Patch>").
#/
#/ --release
#/
#/   Use version numbers intended for Release builds, such as "1.0.0". If this
#/   option is not given, a development/snapshot suffix is added.
#/
#/   If '--commit' is also enabled, this option uses the commit message
#/   "Prepare release <Version>". The convention is to use this message to
#/   make a new release.
#/
#/   Optional. Default: Disabled.
#/
#/ --new-development
#/
#/   Mark the start of a new development iteration.
#/
#/   If '--commit' is also enabled, this option uses the commit message
#/   "Prepare for next development iteration". The convention is to use this
#/   message to start development on a new project version after a release.
#/
#/   If neither '--release' nor '--new-development' are given, the commit
#/   message will simply be "Update version to <Version>".
#/
#/   Optional. Default: Disabled.
#/
#/ --commit
#/
#/   Commit changes to Git. This will commit only the changed files.
#/
#/   Optional. Default: Disabled.



# Configure shell
# ===============

# Absolute Canonical Path to the directory that contains this script.
SELF_DIR="$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null && pwd -P)"
source "$SELF_DIR/../../../ci-scripts/bash.conf.sh" || exit 1

# Trace all commands (to stderr).
set -o xtrace



# Check dependencies
# ==================

command -v xmlstarlet >/dev/null || {
    log "ERROR: 'xmlstarlet' is not installed; please install it"
    exit 1
}



# Parse call arguments
# ====================

CFG_VERSION=""
CFG_RELEASE="false"
CFG_NEWDEVELOPMENT="false"
CFG_COMMIT="false"

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --kms-api)
            # Ignore argument.
            if [[ -n "${2-}" ]]; then
                shift
            fi
            ;;
        --release)
            CFG_RELEASE="true"
            ;;
        --new-development)
            CFG_NEWDEVELOPMENT="true"
            ;;
        --commit)
            CFG_COMMIT="true"
            ;;
        *)
            CFG_VERSION="$1"
            ;;
    esac
    shift
done



# Validate config
# ===============

if [[ -z "$CFG_VERSION" ]]; then
    log "ERROR: Missing <Version>"
    exit 1
fi

REGEX='^[[:digit:]]+\.[[:digit:]]+\.[[:digit:]]+$'
[[ "$CFG_VERSION" =~ $REGEX ]] || {
    log "ERROR: '$CFG_VERSION' is not SemVer (<Major>.<Minor>.<Patch>)"
    exit 1
}

if [[ "$CFG_RELEASE" == "true" ]]; then
    CFG_NEWDEVELOPMENT="false"
fi

log "CFG_VERSION=$CFG_VERSION"
log "CFG_RELEASE=$CFG_RELEASE"
log "CFG_NEWDEVELOPMENT=$CFG_NEWDEVELOPMENT"
log "CFG_COMMIT=$CFG_COMMIT"



# Control variables
# =================

if [[ "$CFG_RELEASE" == "true" ]]; then
    VERSION_JAVA="$CFG_VERSION"

    COMMIT_MSG="Prepare Java tutorials release $VERSION_JAVA"
else
    VERSION_JAVA="${CFG_VERSION}-SNAPSHOT"

    if [[ "$CFG_NEWDEVELOPMENT" == "true" ]]; then
        COMMIT_MSG="Prepare for next development iteration"
    else
        COMMIT_MSG="Update Java tutorials version to $VERSION_JAVA"
    fi
fi



# Helper functions
# ================

# Create a commit with the already staged files + any extra provided ones.
# This function can be called multiple times over the same tree.
function git_commit {
    [[ $# -ge 1 ]] || {
        log "ERROR [git_commit]: Missing argument(s): <file1> [<file2> ...]"
        return 1
    }

    if [[ "$CFG_COMMIT" != "true" ]]; then
        return 0
    fi

    git add -- "$@"

    # Check if there are new staged changes ready to be committed.
    if git diff --cached --quiet --exit-code; then
        return 0
    fi

    # Amend the last commit if one already exists with same message.
    local GIT_COMMIT_ARGS=()
    if git show --no-patch --format='format:%s' HEAD | grep --quiet "^${COMMIT_MSG}$"; then
        GIT_COMMIT_ARGS=(--amend --no-edit)
    else
        GIT_COMMIT_ARGS=(--message "$COMMIT_MSG")
    fi

    git commit "${GIT_COMMIT_ARGS[@]}"
}



# Set version
# ===========

{
    # Parent: Inherit from the new version of kurento-qa-pom.
    xmlstarlet edit -S --inplace \
        --update "/_:project/_:parent/_:version" \
        --value "$VERSION_JAVA" \
        pom.xml

    git_commit pom.xml
}

{
    # Children: Inherit from the new version.
    CHILDREN=(
        chroma
        crowddetector
        datachannel-send-qr
        datachannel-show-text
        facedetector
        group-call
        hello-world
        hello-world-recording
        magic-mirror
        one2many-call
        one2one-call
        one2one-call-advanced
        one2one-call-recording
        platedetector
        player
        pointerdetector
        rtp-receiver
    )
    for CHILD in "${CHILDREN[@]}"; do
        mapfile -t FILES < <(find "$CHILD" -name pom.xml)
        for FILE in "${FILES[@]}"; do
            xmlstarlet edit -S --inplace \
                --update "/_:project/_:parent/_:version" \
                --value "$VERSION_JAVA" \
                "$FILE"

            git_commit "$FILE"
        done
    done
}

{
    pushd chroma/

    # Dependency on kurento-module-chroma.
    xmlstarlet edit -S --inplace \
        --update "/_:project/_:dependencies/_:dependency[_:artifactId='chroma']/_:version" \
        --value "$VERSION_JAVA" \
        pom.xml

    git_commit pom.xml

    popd
}

{
    pushd crowddetector/

    # Dependency on kurento-module-crowddetector.
    xmlstarlet edit -S --inplace \
        --update "/_:project/_:dependencies/_:dependency[_:artifactId='crowddetector']/_:version" \
        --value "$VERSION_JAVA" \
        pom.xml

    git_commit pom.xml

    popd
}

{
    pushd datachannel-send-qr/

    # Dependency on kurento-module-datachannelexample.
    xmlstarlet edit -S --inplace \
        --update "/_:project/_:dependencies/_:dependency[_:artifactId='datachannelexample']/_:version" \
        --value "$VERSION_JAVA" \
        pom.xml

    git_commit pom.xml

    popd
}

{
    pushd datachannel-show-text/

    # Dependency on kurento-module-datachannelexample.
    xmlstarlet edit -S --inplace \
        --update "/_:project/_:dependencies/_:dependency[_:artifactId='datachannelexample']/_:version" \
        --value "$VERSION_JAVA" \
        pom.xml

    git_commit pom.xml

    popd
}

{
    pushd facedetector/

    # Dependency on kurento-module-datachannelexample.
    xmlstarlet edit -S --inplace \
        --update "/_:project/_:dependencies/_:dependency[_:artifactId='datachannelexample']/_:version" \
        --value "$VERSION_JAVA" \
        pom.xml

    git_commit pom.xml

    popd
}

{
    pushd platedetector/

    # Dependency on kurento-module-platedetector.
    xmlstarlet edit -S --inplace \
        --update "/_:project/_:dependencies/_:dependency[_:artifactId='platedetector']/_:version" \
        --value "$VERSION_JAVA" \
        pom.xml

    git_commit pom.xml

    popd
}

{
    pushd pointerdetector/

    # Dependency on kurento-module-pointerdetector.
    xmlstarlet edit -S --inplace \
        --update "/_:project/_:dependencies/_:dependency[_:artifactId='pointerdetector']/_:version" \
        --value "$VERSION_JAVA" \
        pom.xml

    git_commit pom.xml

    popd
}

log "Done!"
