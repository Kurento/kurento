#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ Verify that the project's version is valid.


# Configure shell
# ===============

SELF_DIR="$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null && pwd -P)"
source "$SELF_DIR/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="
trap_add 'log "==================== END ===================="' EXIT

# Trace all commands (to stderr).
set -o xtrace



# Parse call arguments
# ====================

CFG_CREATE_GIT_TAG="false"
CFG_GET_VERSION_ARGS=()

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --create-git-tag)
            CFG_CREATE_GIT_TAG="true"
            ;;
        *)
            CFG_GET_VERSION_ARGS+=("$1")
            ;;
    esac
    shift
done



# Validate config
# ===============

log "CFG_CREATE_GIT_TAG=$CFG_CREATE_GIT_TAG"
log "CFG_GET_VERSION_ARGS=${CFG_GET_VERSION_ARGS[*]}"



# Verify version
# ==============

PROJECT_VERSION="$(kurento_get_version.sh "${GET_VERSION_ARGS[@]}")" || {
    log "ERROR: Command failed: kurento_get_version"
    exit 1
}
log "PROJECT_VERSION: $PROJECT_VERSION"

if [ "${PROJECT_VERSION}x" = "x" ]; then
    log "ERROR: Could not find project version"
    exit 1
fi

if [[ ${PROJECT_VERSION} == *-SNAPSHOT ]]; then
    log "Exit: Version is SNAPSHOT: ${PROJECT_VERSION}"
    exit 0
fi

if [[ ${PROJECT_VERSION} == *-dev ]]; then
    log "Exit: Version is DEV"
    exit 0
fi

if [[ $(echo "$PROJECT_VERSION" | grep -o '\.' | wc -l) -gt 2 ]]; then
    log "Exit: Found more than two dots, should be a configure.ac dev version"
    exit 0
fi

if [ -f debian/changelog ]; then
    # check changelog version
    #ver=$(head -1 debian/changelog | sed -e "s@.* (\(.*\)) .*@\1@")
    CHG_VER="$(dpkg-parsechangelog --show-field Version)"
    if [[ "${CHG_VER%%-*}" != "$PROJECT_VERSION" ]]; then
        log "WARNING Version in changelog is different to current version"
        #exit 1
    fi
fi

# Check that release version conforms to Semantic Versioning.
kurento_check_semver.sh "$PROJECT_VERSION" || {
    log "ERROR: Command failed: kurento_check_semver"
    exit 1
}



# Create git tag
# ==============

# Create a Git tag when a Release version is detected.
# This is default 'false' because sometimes we need to do several actual
# changes before being able to mark a commit as an official Release.
if [[ "$CFG_CREATE_GIT_TAG" == "true" ]]; then
    TAG_MSG="Tag version $PROJECT_VERSION"
    TAG_NAME="$PROJECT_VERSION"

    log "Create git tag: '$TAG_NAME'"

    if git tag -a -m "$TAG_MSG" "$TAG_NAME"; then
        log "Tag created, push to remote"
        git push origin "$TAG_NAME"
    else
        log "ERROR: Command failed: git tag $TAG_NAME"
        exit 1
    fi
fi
