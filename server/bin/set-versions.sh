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
#/ --debian <DebianVersion>
#/
#/   Debian version used for packaging. This gets appended to the base version
#/   in the Debian control file, debian/changelog
#/
#/   Check the Debian Policy for the syntax of this field:
#/      https://www.debian.org/doc/debian-policy/ch-controlfields.html#version
#/
#/   For Kurento packages, <DebianVersion> should be like "1kurento1":
#/   - The "0" indicates upstream package version, typically 0 meaning that the
#/     package doesn't exist in upstream distribution (Ubuntu).
#/   - The "1" indicates the package revision from Kurento. If the same version
#/     of some software is repackaged, this should be incremented, e.g.
#/     "1kurento2".
#/
#/   Optional. Default: "1kurento1".
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
source "$SELF_DIR/../../ci-scripts/bash.conf.sh" || exit 1

# Trace all commands (to stderr).
set -o xtrace



# Check dependencies
# ==================

command -v perl >/dev/null || {
    log "ERROR: 'perl' is not installed; please install it"
    exit 1
}
command -v xmlstarlet >/dev/null || {
    log "ERROR: 'xmlstarlet' is not installed; please install it"
    exit 1
}
command -v indent >/dev/null || {
    log "ERROR: 'indent' is not installed; please install it"
    exit 1
}
command -v astyle >/dev/null || {
    log "ERROR: 'astyle' is not installed; please install it"
    exit 1
}



# Parse call arguments
# ====================

CFG_VERSION=""
CFG_DEBIAN="1kurento1"
CFG_RELEASE="false"
CFG_NEWDEVELOPMENT="false"
CFG_COMMIT="false"

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --debian)
            if [[ -n "${2-}" ]]; then
                CFG_DEBIAN="$2"
                shift
            else
                log "ERROR: --debian expects <DebianVersion>"
                exit 1
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
    log "ERROR: '$CFG_VERSION' must be compatible with Semantic Versioning: <Major>.<Minor>.<Patch>"
    exit 1
}

if [[ "$CFG_RELEASE" == "true" ]]; then
    CFG_NEWDEVELOPMENT="false"
fi

log "CFG_VERSION=$CFG_VERSION"
log "CFG_DEBIAN=$CFG_DEBIAN"
log "CFG_RELEASE=$CFG_RELEASE"
log "CFG_NEWDEVELOPMENT=$CFG_NEWDEVELOPMENT"
log "CFG_COMMIT=$CFG_COMMIT"



# Control variables
# =================

VERSION_PKG="${CFG_VERSION}-${CFG_DEBIAN}"

if [[ "$CFG_RELEASE" == "true" ]]; then
    VERSION_C="$CFG_VERSION"
    VERSION_JAVA="$CFG_VERSION"

    COMMIT_MSG="Prepare server release $VERSION_PKG"
else
    VERSION_C="${CFG_VERSION}-dev"
    VERSION_JAVA="${CFG_VERSION}-SNAPSHOT"

    if [[ "$CFG_NEWDEVELOPMENT" == "true" ]]; then
        COMMIT_MSG="Prepare for next development iteration"
    else
        COMMIT_MSG="Update server version to $VERSION_PKG"
    fi
fi



# Helper functions
# ================

# Edit debian/changelog to add a new section with the given version.
# NOTICE: This is based on `ci-scripts/kurento-buildpackage.sh`.
function update_debian_changelog {
    # debchange (dch) requires an email being set on the system.
    if [[ -z "${DEBFULLNAME:-}${NAME:-}" || -z "${DEBEMAIL:-}${EMAIL:-}" ]]; then
        DEBFULLNAME="$(git config --default 'Kurento' --get user.name)"; export DEBFULLNAME
        DEBEMAIL="$(git config --default 'kurento@openvidu.io' --get user.email)"; export DEBEMAIL
    fi

    dch \
        --newversion "$VERSION_PKG" \
        ""

    if [[ "$CFG_COMMIT" == "true" ]]; then
        git add debian/changelog
    fi
}

# Edit debian/control to set all Kurento dependencies to the given version.
function update_debian_control {
    # Regex translation:
    # ^         : Start of the line.
    # \s+       : 1 or more spaces.
    # (kms-|kurento-) : Initial name of the package.
    # \S+       : 1 or more non-space characters (could be underlines, dots, etc).
    # \([<=>]+  : An open paren, followed by any of '<', '=', '>'.
    # \K        : Discard everything before here from the actual regex match.
    # \d\S*     : A number followed by any non-space characters.
    #             This is the version number that will be matched and replaced.
    # (?=\))    : Positive lookahead for a close paren.
    #             This ensures a close paren is present, but doesn't match it.
    #
    # The final match of this regex is just the version number. This match is
    # then replaced with the provided value `${CFG_VERSION}`.
    perl -i -pe \
        "s/^\s+(kms-|kurento-)\S+ \([<=>]+ \K\d\S*(?=\))/${CFG_VERSION}/" \
        debian/control

    if [[ "$CFG_COMMIT" == "true" ]]; then
        git add debian/control
    fi
}

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
    if git diff --staged --quiet --exit-code; then
        return 0
    fi

    # Amend the last commit if one already exists with same message.
    local GIT_COMMIT_ARGS=(--message "$COMMIT_MSG")
    if ! git log --max-count 1 --grep "^${COMMIT_MSG}$" --format="" --exit-code; then
        GIT_COMMIT_ARGS+=(--amend)
    fi

    git commit "${GIT_COMMIT_ARGS[@]}"
}



# Set version
# ===========

# Perl Regex doc: https://perldoc.perl.org/perlre
#
# Some tips:
#
# \K
#     Zero-width positive lookbehind assertion.
#     Require the stuff left of the \K, but don't include it in the match.
#
# (?=pattern)
#     Zero-width positive lookahead assertion.
#     Require the stuff inside the (?=), but don't include it in the match.
#     (?=\") looks for a double quote after the regex match.
#
# \s
#     Match a whitespace character.
#     \s* matches all consecutive whitespace characters.
#
# \S
#     Match a non-whitespace character.
#     \S* matches all consecutive non-whitespace characters.

{
    pushd module-creator/
    update_debian_changelog
    update_debian_control
    xmlstarlet edit -S --inplace \
        --update "/_:project/_:version" \
        --value "$VERSION_JAVA" \
        pom.xml
    git_commit \
        pom.xml \
        src/main/templates/maven/model_pom_xml.ftl
    popd
}

{
    pushd cmake-utils/
    update_debian_changelog
    update_debian_control
    perl -i -pe \
        "s/get_git_version\(PROJECT_VERSION \K.*(?=\))/${VERSION_C}/" \
        CMakeLists.txt
    git_commit \
        CMakeLists.txt
    popd
}

{
    pushd jsonrpc/
    update_debian_changelog
    update_debian_control
    perl -i -pe \
        "s/get_git_version\(PROJECT_VERSION \K.*(?=\))/${VERSION_C}/" \
        CMakeLists.txt
    git_commit \
        CMakeLists.txt
    popd
}

{
    pushd module-core/
    update_debian_changelog
    update_debian_control
    perl -i -pe \
        "s/\"version\":\s*\"\K\S*(?=\")/${VERSION_C}/" \
        src/server/interface/core.kmd.json

    # Don't be too happy to update the version requirement of Kurento Module Creator.
    # Older versions of Kurento should be able to generate code for new ones.
    # This line is left here for reference, but should be run manually only
    # on those cases where a breaking change has been introduced in the module creator
    # and the upgraded requirement is actually a necessity.
    #perl -i -pe \
    #    "s/generic_find\(LIBNAME KurentoModuleCreator VERSION \K.*(?= REQUIRED\))/^${VERSION_C}/" \
    #    cmake/Kurento/CodeGenerator.cmake

    git_commit \
        src/server/interface/core.kmd.json \
        cmake/Kurento/CodeGenerator.cmake
    popd
}

{
    pushd module-elements/
    update_debian_changelog
    update_debian_control
    perl -i -pe \
        "s/generic_find\(LIBNAME KmsJsonRpc VERSION \K.*(?= REQUIRED\))/^${VERSION_C}/" \
        CMakeLists.txt
    perl -i -pe \
        "s/\"version\":\s*\"\K\S*(?=\")/${VERSION_C}/" \
        src/server/interface/elements.kmd.json
    perl -i -pe \
        "s/\"kurentoVersion\":\s*\"\K\S*(?=\")/^${VERSION_C}/" \
        src/server/interface/elements.kmd.json
    git_commit \
        CMakeLists.txt \
        src/server/interface/elements.kmd.json
    popd
}

{
    pushd module-filters/
    update_debian_changelog
    update_debian_control
    perl -i -pe \
        "s/generic_find\(LIBNAME KmsJsonRpc VERSION \K.*(?= REQUIRED\))/^${VERSION_C}/" \
        CMakeLists.txt
    perl -i -pe \
        "s/\"version\":\s*\"\K\S*(?=\")/${VERSION_C}/" \
        src/server/interface/filters.kmd.json
    perl -i -pe \
        "s/\"kurentoVersion\":\s*\"\K\S*(?=\")/^${VERSION_C}/" \
        src/server/interface/filters.kmd.json
    git_commit \
        CMakeLists.txt \
        src/server/interface/filters.kmd.json
    popd
}

{
    pushd media-server/
    update_debian_changelog
    update_debian_control
    perl -i -pe \
        "s/get_git_version\(PROJECT_VERSION \K.*(?=\))/${VERSION_C}/" \
        CMakeLists.txt
    perl -i -pe \
        "s/generic_find\(LIBNAME KmsJsonRpc VERSION \K.*(?= REQUIRED\))/^${VERSION_C}/" \
        CMakeLists.txt
    perl -i -pe \
        "s/generic_find\(LIBNAME KMSCORE VERSION \K.*(?= REQUIRED\))/^${VERSION_C}/" \
        CMakeLists.txt
    git_commit \
        CMakeLists.txt
    popd
}

{
    pushd module-examples/chroma/
    update_debian_changelog
    update_debian_control
    perl -i -pe \
        "s/generic_find\(LIBNAME KMSCORE VERSION \K.*(?= REQUIRED\))/^${VERSION_C}/" \
        CMakeLists.txt
    perl -i -pe \
        "s/\"version\":\s*\"\K\S*(?=\")/${VERSION_C}/" \
        src/server/interface/chroma.kmd.json
    perl -i -pe \
        "s/\"kurentoVersion\":\s*\"\K\S*(?=\")/^${VERSION_C}/" \
        src/server/interface/chroma.kmd.json
    git_commit \
        CMakeLists.txt \
        src/server/interface/chroma.kmd.json
    popd
}

{
    pushd module-examples/crowddetector/
    update_debian_changelog
    update_debian_control
    perl -i -pe \
        "s/generic_find\(LIBNAME KMSCORE VERSION \K.*(?= REQUIRED\))/^${VERSION_C}/" \
        CMakeLists.txt
    perl -i -pe \
        "s/\"version\":\s*\"\K\S*(?=\")/${VERSION_C}/" \
        src/server/interface/crowddetector.kmd.json
    perl -i -pe \
        "s/\"kurentoVersion\":\s*\"\K\S*(?=\")/^${VERSION_C}/" \
        src/server/interface/crowddetector.kmd.json
    git_commit \
        CMakeLists.txt \
        src/server/interface/crowddetector.kmd.json
    popd
}

{
    pushd module-examples/datachannelexample/
    update_debian_changelog
    update_debian_control
    perl -i -pe \
        "s/\"version\":\s*\"\K\S*(?=\")/${VERSION_C}/" \
        src/server/interface/kmsdatachannelexample.kmd.json
    perl -i -pe \
        "s/\"kurentoVersion\":\s*\"\K\S*(?=\")/^${VERSION_C}/" \
        src/server/interface/kmsdatachannelexample.kmd.json
    git_commit \
        src/server/interface/kmsdatachannelexample.kmd.json
    popd
}

{
    pushd module-examples/gstreamer-example/
    update_debian_changelog
    update_debian_control
    perl -i -pe \
        "s/generic_find\(LIBNAME KMSCORE VERSION \K.*(?= REQUIRED\))/^${VERSION_C}/" \
        CMakeLists.txt
    perl -i -pe \
        "s/\"version\":\s*\"\K\S*(?=\")/${VERSION_C}/" \
        src/server/interface/gstreamerexample.kmd.json
    perl -i -pe \
        "s/\"kurentoVersion\":\s*\"\K\S*(?=\")/^${VERSION_C}/" \
        src/server/interface/gstreamerexample.kmd.json
    git_commit \
        CMakeLists.txt \
        src/server/interface/gstreamerexample.kmd.json
    popd
}

{
    pushd module-examples/markerdetector/
    update_debian_changelog
    update_debian_control
    perl -i -pe \
        "s/generic_find\(LIBNAME KMSCORE VERSION \K.*(?= REQUIRED\))/^${VERSION_C}/" \
        CMakeLists.txt
    perl -i -pe \
        "s/\"version\":\s*\"\K\S*(?=\")/${VERSION_C}/" \
        src/server/interface/armarkerdetector.kmd.json
    perl -i -pe \
        "s/\"kurentoVersion\":\s*\"\K\S*(?=\")/^${VERSION_C}/" \
        src/server/interface/armarkerdetector.kmd.json
    git_commit \
        CMakeLists.txt \
        src/server/interface/armarkerdetector.kmd.json
    popd
}

{
    pushd module-examples/opencv-example/
    update_debian_changelog
    update_debian_control
    perl -i -pe \
        "s/generic_find\(LIBNAME KMSCORE VERSION \K.*(?= REQUIRED\))/^${VERSION_C}/" \
        CMakeLists.txt
    perl -i -pe \
        "s/\"version\":\s*\"\K\S*(?=\")/${VERSION_C}/" \
        src/server/interface/opencvexample.kmd.json
    perl -i -pe \
        "s/\"kurentoVersion\":\s*\"\K\S*(?=\")/^${VERSION_C}/" \
        src/server/interface/opencvexample.kmd.json
    git_commit \
        CMakeLists.txt \
        src/server/interface/opencvexample.kmd.json
    popd
}

{
    pushd module-examples/platedetector/
    update_debian_changelog
    update_debian_control
    perl -i -pe \
        "s/generic_find\(LIBNAME KMSCORE VERSION \K.*(?= REQUIRED\))/^${VERSION_C}/" \
        CMakeLists.txt
    perl -i -pe \
        "s/\"version\":\s*\"\K\S*(?=\")/${VERSION_C}/" \
        src/server/interface/platedetector.kmd.json
    perl -i -pe \
        "s/\"kurentoVersion\":\s*\"\K\S*(?=\")/^${VERSION_C}/" \
        src/server/interface/platedetector.kmd.json
    git_commit \
        CMakeLists.txt \
        src/server/interface/platedetector.kmd.json
    popd
}

{
    pushd module-examples/pointerdetector/
    update_debian_changelog
    update_debian_control
    perl -i -pe \
        "s/generic_find\(LIBNAME KMSCORE VERSION \K.*(?= REQUIRED\))/^${VERSION_C}/" \
        CMakeLists.txt
    perl -i -pe \
        "s/\"version\":\s*\"\K\S*(?=\")/${VERSION_C}/" \
        src/server/interface/pointerdetector.kmd.json
    perl -i -pe \
        "s/\"kurentoVersion\":\s*\"\K\S*(?=\")/^${VERSION_C}/" \
        src/server/interface/pointerdetector.kmd.json
    git_commit \
        CMakeLists.txt \
        src/server/interface/pointerdetector.kmd.json
    popd
}

{
    # server/
    perl -i -pe \
        "s/generic_find\(LIBNAME KurentoModuleCreator VERSION \K.*(?=\))/^${VERSION_C}/" \
        CMakeLists.txt
    git_commit \
        CMakeLists.txt
}

log "Done!"
