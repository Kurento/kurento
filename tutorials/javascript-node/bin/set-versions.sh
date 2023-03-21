#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

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

command -v jq >/dev/null || {
    log "ERROR: 'jq' is not installed; please install it"
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
    VERSION_JS="$CFG_VERSION"

    COMMIT_MSG="Prepare JavaScript Node.js tutorial release $VERSION_JS"
else
    VERSION_JS="${CFG_VERSION}-dev"

    if [[ "$CFG_NEWDEVELOPMENT" == "true" ]]; then
        COMMIT_MSG="Prepare for next development iteration"
    else
        COMMIT_MSG="Update JavaScript Node.js tutorial version to $VERSION_JS"
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
    local GIT_COMMIT_ARGS=(--message "$COMMIT_MSG")
    if ! git log --max-count 1 --grep "^${COMMIT_MSG}$" --format="" --exit-code; then
        GIT_COMMIT_ARGS+=(--amend)
    fi

    git commit "${GIT_COMMIT_ARGS[@]}"
}

# Run jq, which doesn't have "in-place" mode like other UNIX tools.
function run_jq() {
    [[ $# -ge 2 ]] || {
        log "ERROR [run_jq]: Missing argument(s): <filter> <file>"
        return 1
    }
    local FILTER="$1"
    local FILE="$2"
    local TEMP; TEMP="$(mktemp)"

    /usr/bin/jq "$FILTER" "$FILE" >"$TEMP" && mv "$TEMP" "$FILE"
}



# Set version
# ===========

# Dirs that contain common dependencies (kurento-client and kurento-utils).
DIRS_COMMON=(
    chroma
    crowddetector
    hello-world
    magic-mirror
    one2many-call
    one2one-call
    platedetector
    pointerdetector
)

# Dirs that contain module dependencies.
MODULES=(
    chroma
    crowddetector
    platedetector
    pointerdetector
)

for DIR in "${DIRS_COMMON[@]}"; do
    pushd "$DIR"

    run_jq ".version = \"$VERSION_JS\"" package.json

    if [[ "$CFG_RELEASE" == "true" ]]; then
        run_jq "
            .dependencies.\"kurento-client\" = \"$VERSION_JS\"
        " package.json
        run_jq "
            .dependencies.\"kurento-utils\" = \"$VERSION_JS\"
        " static/bower.json
    else
        run_jq "
            .dependencies.\"kurento-client\" = \"git+https://github.com/Kurento/kurento-client-js.git\"
        " package.json
        run_jq "
            .dependencies.\"kurento-utils\" = \"git+https://github.com/Kurento/kurento-utils-bower.git\"
        " static/bower.json
    fi

    git_commit package.json static/bower.json

    popd
done

for MODULE in "${MODULES[@]}"; do
    pushd "$MODULE"

    if [[ "$CFG_RELEASE" == "true" ]]; then
        run_jq "
            .dependencies.\"kurento-module-${MODULE}\" = \"$VERSION_JS\"
        " package.json
    else
        run_jq "
            .dependencies.\"kurento-module-${MODULE}\" = \"git+https://github.com/Kurento/kurento-module-${MODULE}-js.git\"
        " package.json
    fi

    git_commit package.json

    popd
done

{
    pushd module-tests-api/

    run_jq ".version = \"$VERSION_JS\"" package.json

    if [[ "$CFG_RELEASE" == "true" ]]; then
        run_jq "
            .dependencies.\"kurento-client\" = \"$VERSION_JS\"
            | .dependencies.\"kurento-module-chroma\" = \"$VERSION_JS\"
            | .dependencies.\"kurento-module-crowddetector\" = \"$VERSION_JS\"
            | .dependencies.\"kurento-module-platedetector\" = \"$VERSION_JS\"
            | .dependencies.\"kurento-module-pointerdetector\" = \"$VERSION_JS\"
        " package.json
    else
        run_jq "
            .dependencies.\"kurento-client\" = \"git+https://github.com/Kurento/kurento-client-js.git\"
            | .dependencies.\"kurento-module-chroma\" = \"git+https://github.com/Kurento/kurento-module-chroma-js.git\"
            | .dependencies.\"kurento-module-crowddetector\" = \"git+https://github.com/Kurento/kurento-module-crowddetector-js.git\"
            | .dependencies.\"kurento-module-platedetector\" = \"git+https://github.com/Kurento/kurento-module-platedetector-js.git\"
            | .dependencies.\"kurento-module-pointerdetector\" = \"git+https://github.com/Kurento/kurento-module-pointerdetector-js.git\"
        " package.json
    fi

    git_commit package.json

    popd
}

log "Done!"
