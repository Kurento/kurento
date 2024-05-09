#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ Verify that the project's version is valid.
#/
#/
#/ Arguments
#/ =========
#/
#/ --minor
#/
#/   Only expects a SemVer number with the form Major.Minor, without .Patch.
#/   Without this flag, a full Semver number is expected: Major.Minor.Patch.
#/
#/   Optional. Default: Disabled.
#/
#/ --release
#/
#/   Enforce release version numbers. The script will exit with an error if
#/   the current project's version is not appropriate for a release.
#/
#/   Optional. Default: Disabled.



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

CFG_MINOR="false"
CFG_RELEASE="false"
CFG_GET_VERSION_ARGS=()

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --minor)
            CFG_MINOR="true"
            ;;
        --release)
            CFG_RELEASE="true"
            ;;
        *)
            CFG_GET_VERSION_ARGS+=("$1")
            ;;
    esac
    shift
done



# Validate config
# ===============

log "CFG_MINOR=$CFG_MINOR"
log "CFG_RELEASE=$CFG_RELEASE"
log "CFG_GET_VERSION_ARGS=${CFG_GET_VERSION_ARGS[*]}"



# Verify version
# ==============

PROJECT_VERSION="$(kurento_get_version.sh "${CFG_GET_VERSION_ARGS[@]}")" || {
    log "ERROR: Command failed: kurento_get_version"
    exit 1
}
log "PROJECT_VERSION: $PROJECT_VERSION"

if [[ -z "$PROJECT_VERSION" ]]; then
    log "ERROR: Could not find project version"
    exit 1
fi

if [[ "$PROJECT_VERSION" == *-SNAPSHOT ]] || [[ "$PROJECT_VERSION" == *-dev ]]; then
    MESSAGE="Version is NOT release (contains development suffix)"
    if [[ "$CFG_RELEASE" == "true" ]]; then
        log "ERROR: $MESSAGE"
        exit 1
    else
        log "Exit: $MESSAGE"
        exit 0
    fi
fi

# From here, we are dealing with a release version number.

if [[ $(echo "$PROJECT_VERSION" | grep -o '\.' | wc -l) -gt 2 ]]; then
    log "Exit: Found more than two dots, should be a configure.ac dev version"
    exit 0
fi

if [[ -f debian/changelog ]]; then
    # check changelog version
    #ver=$(head -1 debian/changelog | sed -e "s@.* (\(.*\)) .*@\1@")
    CHG_VER="$(dpkg-parsechangelog --show-field Version)"
    if [[ "${CHG_VER%%-*}" != "$PROJECT_VERSION" ]]; then
        log "WARNING Version in changelog is different to current version"
        #exit 1
    fi
fi

# Check conformity to Semantic Versioning style.
if [[ "$CFG_MINOR" == "true" ]]; then
    # Major.Minor
    REGEX='^[[:digit:]]+\.[[:digit:]]+$'
else
    # Major.Minor.Patch
    REGEX='^[[:digit:]]+\.[[:digit:]]+\.[[:digit:]]+$'
fi
[[ "$PROJECT_VERSION" =~ $REGEX ]] || {
    log "ERROR: '$PROJECT_VERSION' is not SemVer (<Major>.<Minor>.<Patch?>)"
    exit 1
}
