#!/usr/bin/env bash

#/ Version change helper.
#/
#/ This shell script will traverse all subprojects and change their
#/ versions to the one provided as argument.
#/
#/
#/ Arguments
#/ ---------
#/
#/ <Version>
#/
#/   Base version number to set. When '--release' is used, this version will
#/   be used as-is; otherwise, a nightly/snapshot indicator will be appended.
#/
#/   <Version> should be in a format compatible with Semantic Versioning,
#/   such as "1.2.3" or, in general terms, "<Major>.<Minor>.<Patch>".
#/
#/ --debian <DebianVersion>
#/
#/   Debian version used for packaging. This gets appended to the base version
#/   in the Debian control file, debian/changelog
#/
#/   Check the Debian Policy for the syntax of this field:
#/      https://www.debian.org/doc/debian-policy/ch-controlfields.html#version
#/
#/   For Kurento packages, <DebianVersion> should be like "0kurento1":
#/   - The "0" indicates upstream package version, typically 0 meaning that the
#/     package doesn't exist in upstream distribution (Ubuntu).
#/   - The "1" indicates the package revision from Kurento. If the same version
#/     of some software is repackaged, this should be incremented, e.g.
#/     "0kurento2".
#/
#/   Optional. Default: "0kurento1".
#/
#/ --release
#/
#/   Use version numbers intended for Release builds, such as "1.2.3" instead
#/   of "1.2.3-SNAPSHOT" or "1.2.3-dev".
#/
#/   This option uses the standard commit message "Prepare release <Version>"
#/   if '--commit' is also present. The convention is to use this message to
#/   make a new final release.
#/
#/   If this option is not given, a nightly/snapshot indicator is appended:
#/   "-dev" for C/C++ projects, and "-SNAPSHOT" for Java projects.
#/
#/   Optional. Default: Disabled.
#/
#/ --new-development
#/
#/   Mark the start of a new development iteration.
#/
#/   This option only has the effect of using the standard commit message
#/   "Prepare for next development iteration" if '--commit' is also present. The
#/   convention is to use this message to start development on a new project
#/   version after having finished a release.
#/
#/   If neither '--release' nor '--new-development' are given, the commit
#/   message will be "Bump development version to <Version>", because this
#/   script doesn't know if you are changing the version number after a release,
#/   or just as part of normal development (e.g. according to SemVer, after
#/   adding a new feature you should bump the Minor version number).
#/
#/   Optional. Default: Disabled.
#/
#/ --commit
#/
#/   Commit to Git the version changes. This will commit only the changed files.
#/
#/   Optional. Default: Disabled.
#/
#/ --tag
#/
#/   Create Git annotated tags with the results of the version change. This
#/   requires that '--commit' is used too. Also, to avoid mistakes, this can
#/   only be used together with '--release'.
#/
#/   Optional. Default: Disabled.



# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

# Check requirements
command -v perl >/dev/null || {
    log "ERROR: 'perl' is not installed; please install it"
    exit 1
}
command -v xmlstarlet >/dev/null || {
    log "ERROR: 'xmlstarlet' is not installed; please install it"
    exit 1
}

set -o xtrace


# Parse call arguments
# --------------------

CFG_VERSION_DEFAULT="0.0.0"
CFG_VERSION="$CFG_VERSION_DEFAULT"
CFG_DEBIAN="0kurento1"
CFG_RELEASE="false"
CFG_NEWDEVELOPMENT="false"
CFG_COMMIT="false"
CFG_TAG="false"

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --debian)
            if [[ -n "${2-}" ]]; then
                CFG_DEBIAN="$2"
                shift
            else
                log "ERROR: --debian expects <DebianVersion>"
                log "Run with '--help' to read usage details"
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
        --tag)
            CFG_TAG="true"
            ;;
        *)
            CFG_VERSION="$1"
            ;;
    esac
    shift
done



# Apply config restrictions
# -------------------------

if [[ "$CFG_RELEASE" == "true" ]]; then
    CFG_NEWDEVELOPMENT="false"
fi

if [[ "$CFG_TAG" == "true" ]]; then
    if [[ "$CFG_RELEASE" != "true" || "$CFG_COMMIT" != "true" ]]; then
        log "WARNING: Ignoring '--tag': Requires '--release' and '--commit'"
        CFG_TAG="false"
    fi
fi

if [[ "$CFG_VERSION" == "$CFG_VERSION_DEFAULT" ]]; then
    log "ERROR: Missing <Version>"
    exit 1
fi

REGEX='^[[:digit:]]+\.[[:digit:]]+\.[[:digit:]]+$'
[[ "$CFG_VERSION" =~ $REGEX ]] || {
    log "ERROR: '$CFG_VERSION' must be compatible with Semantic Versioning: <Major>.<Minor>.<Patch>"
    exit 1
}

log "CFG_VERSION=$CFG_VERSION"
log "CFG_DEBIAN=$CFG_DEBIAN"
log "CFG_RELEASE=$CFG_RELEASE"
log "CFG_NEWDEVELOPMENT=$CFG_NEWDEVELOPMENT"
log "CFG_COMMIT=$CFG_COMMIT"
log "CFG_TAG=$CFG_TAG"



# Init internal variables
# -----------------------

VERSION_PKG="${CFG_VERSION}-${CFG_DEBIAN}"

if [[ "$CFG_RELEASE" == "true" ]]; then
    COMMIT_MSG="Prepare release $VERSION_PKG"

    VERSION_C="$CFG_VERSION"
    VERSION_JAVA="$CFG_VERSION"
else
    if [[ "$CFG_NEWDEVELOPMENT" == "true" ]]; then
        COMMIT_MSG="Prepare for next development iteration"
    else
        COMMIT_MSG="Update version to $VERSION_PKG"
    fi

    VERSION_C="${CFG_VERSION}-dev"
    VERSION_JAVA="${CFG_VERSION}-SNAPSHOT"
fi



# Helper functions
# ----------------

# Edits debian/changelog to add a new section with the given version.
update_debian_changelog() {
    local SNAPSHOT_ENTRY="* UNRELEASED"
    local RELEASE_ENTRY="* $COMMIT_MSG"

    if [[ "$CFG_RELEASE" == "true" ]]; then
        gbp dch \
            --ignore-branch \
            --git-author \
            --spawn-editor=never \
            --new-version="$VERSION_PKG" \
            \
            --release \
            --distribution="testing" \
            --force-distribution \
            \
            debian/

        # First appearance of "UNRELEASED": Put our commit message
        sed -i "0,/${SNAPSHOT_ENTRY}/{s/${SNAPSHOT_ENTRY}/${RELEASE_ENTRY}/}" \
            debian/changelog

        # Remaining appearances of "UNRELEASED" (if any): Delete line
        sed -i "/${SNAPSHOT_ENTRY}/d" \
            debian/changelog
    else
        gbp dch \
            --ignore-branch \
            --git-author \
            --spawn-editor=never \
            --new-version="$VERSION_PKG" \
            debian/
    fi

    git add debian/changelog
}

# Edits debian/control to set all Kurento dependencies to the given version.
update_debian_control() {
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

    git add debian/control
}

# Creates a commit with the given files, and tags the commit.
# This function can be called multiple times over the same contents.
commit_and_tag() {
    [[ $# -eq 0 ]] && return 1

    if [[ "$CFG_COMMIT" == "true" ]]; then
        git add -- "$@"

        # If there are new staged changes ready to be committed:
        if ! git diff --staged --quiet -- "$@"; then
            git commit -m "$COMMIT_MSG"

            if [[ "$CFG_TAG" == "true" ]]; then
                # --force: Replace tag if it exists (instead of failing).
                git tag --force -a -m "$COMMIT_MSG" "$CFG_VERSION"
            fi
        fi
    fi
}



# Apply versions
# --------------

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
# \S
#     Match a non-whitespace character.
#     \S* matches all consecutive non-whitespace character.



pushd kurento-module-creator/
TEMP="$(mktemp)"
FILE="pom.xml"
xmlstarlet edit -S --update "/_:project/_:version" --value "$VERSION_JAVA" "$FILE" \
    >"$TEMP" && mv "$TEMP" "$FILE"
update_debian_changelog
update_debian_control
commit_and_tag "$FILE" src/main/templates/maven/model_pom_xml.ftl
popd



pushd kurento-maven-plugin/
FILE="pom.xml"
xmlstarlet edit -S --inplace \
    --update "/_:project/_:version" \
    --value "$VERSION_JAVA" \
    "$FILE"
xmlstarlet edit -S --inplace \
    --update "/_:project/_:dependencies/_:dependency[_:artifactId='kurento-module-creator']/_:version" \
    --value "$VERSION_JAVA" \
    "$FILE"
commit_and_tag "$FILE"
popd



pushd kms-cmake-utils/
FILE="CMakeLists.txt"
perl -i -pe \
    "s/get_git_version\(PROJECT_VERSION \K.*(?=\))/${VERSION_C}/" \
    "$FILE"
update_debian_changelog
update_debian_control
commit_and_tag "$FILE"
popd



pushd kms-jsonrpc/
FILE="CMakeLists.txt"
perl -i -pe \
    "s/get_git_version\(PROJECT_VERSION \K.*(?=\))/${VERSION_C}/" \
    "$FILE"
update_debian_changelog
update_debian_control
commit_and_tag "$FILE"
popd



pushd kms-core/
FILE="src/server/interface/core.kmd.json"
perl -i -pe \
    "s/\"version\": \"\K\S*(?=\")/${VERSION_C}/" \
    "$FILE"
update_debian_changelog
update_debian_control
commit_and_tag "$FILE"
popd



pushd kms-elements/
FILE="src/server/interface/elements.kmd.json"
perl -i -pe \
    "s/\"version\": \"\K\S*(?=\")/${VERSION_C}/" \
    "$FILE"
if [[ "$CFG_RELEASE" == "true" ]]; then
    perl -i -pe \
        "s/\"kurentoVersion\": \"\K\S*(?=\")/^${VERSION_C}/" \
        "$FILE"
fi
update_debian_changelog
update_debian_control
commit_and_tag "$FILE"
popd



pushd kms-filters/
FILE="src/server/interface/filters.kmd.json"
perl -i -pe \
    "s/\"version\": \"\K\S*(?=\")/${VERSION_C}/" \
    "$FILE"
if [[ "$CFG_RELEASE" == "true" ]]; then
    perl -i -pe \
        "s/\"kurentoVersion\": \"\K\S*(?=\")/^${VERSION_C}/" \
        "$FILE"
fi
update_debian_changelog
update_debian_control
commit_and_tag "$FILE"
popd



pushd kurento-media-server/
FILE="CMakeLists.txt"
perl -i -pe \
    "s/get_git_version\(PROJECT_VERSION \K.*(?=\))/${VERSION_C}/" \
    "$FILE"
perl -i -pe \
    "s/generic_find\(LIBNAME KMSCORE VERSION \K.*(?= REQUIRED\))/^${VERSION_C}/" \
    "$FILE"
update_debian_changelog
update_debian_control
commit_and_tag "$FILE"
popd



pushd module/kms-chroma/
FILE="src/server/interface/chroma.kmd.json"
perl -i -pe \
    "s/\"version\": \"\K\S*(?=\")/${VERSION_C}/" \
    "$FILE"
if [[ "$CFG_RELEASE" == "true" ]]; then
    perl -i -pe \
        "s/\"kurentoVersion\": \"\K\S*(?=\")/^${VERSION_C}/" \
        "$FILE"
fi
update_debian_changelog
update_debian_control
commit_and_tag "$FILE"
popd



pushd module/kms-crowddetector/
FILE="src/server/interface/crowddetector.kmd.json"
perl -i -pe \
    "s/\"version\": \"\K\S*(?=\")/${VERSION_C}/" \
    "$FILE"
if [[ "$CFG_RELEASE" == "true" ]]; then
    perl -i -pe \
        "s/\"kurentoVersion\": \"\K\S*(?=\")/^${VERSION_C}/" \
        "$FILE"
fi
update_debian_changelog
update_debian_control
commit_and_tag "$FILE"
popd



pushd module/kms-datachannelexample/
FILE="src/server/interface/kmsdatachannelexample.kmd.json"
perl -i -pe \
    "s/\"version\": \"\K\S*(?=\")/${VERSION_C}/" \
    "$FILE"
if [[ "$CFG_RELEASE" == "true" ]]; then
    perl -i -pe \
        "s/\"kurentoVersion\": \"\K\S*(?=\")/^${VERSION_C}/" \
        "$FILE"
fi
update_debian_changelog
update_debian_control
commit_and_tag "$FILE"
popd



pushd module/kms-markerdetector/
FILE="src/server/interface/armarkerdetector.kmd.json"
perl -i -pe \
    "s/\"version\": \"\K\S*(?=\")/${VERSION_C}/" \
    "$FILE"
if [[ "$CFG_RELEASE" == "true" ]]; then
    perl -i -pe \
        "s/\"kurentoVersion\": \"\K\S*(?=\")/^${VERSION_C}/" \
        "$FILE"
fi
update_debian_changelog
update_debian_control
commit_and_tag "$FILE"
popd



pushd module/kms-platedetector/
FILE="src/server/interface/platedetector.kmd.json"
perl -i -pe \
    "s/\"version\": \"\K\S*(?=\")/${VERSION_C}/" \
    "$FILE"
if [[ "$CFG_RELEASE" == "true" ]]; then
    perl -i -pe \
        "s/\"kurentoVersion\": \"\K\S*(?=\")/^${VERSION_C}/" \
        "$FILE"
fi
update_debian_changelog
update_debian_control
commit_and_tag "$FILE"
popd



pushd module/kms-pointerdetector/
FILE="src/server/interface/pointerdetector.kmd.json"
perl -i -pe \
    "s/\"version\": \"\K\S*(?=\")/${VERSION_C}/" \
    "$FILE"
if [[ "$CFG_RELEASE" == "true" ]]; then
    perl -i -pe \
        "s/\"kurentoVersion\": \"\K\S*(?=\")/^${VERSION_C}/" \
        "$FILE"
fi
update_debian_changelog
update_debian_control
commit_and_tag "$FILE"
popd



# Changes for kms-omni-build itself
FILE="CMakeLists.txt"
perl -i -pe \
    "s/generic_find\(LIBNAME KurentoModuleCreator VERSION \K.*(?=\))/^${VERSION_C}/" \
    "$FILE"
commit_and_tag "$FILE" \
    kurento-module-creator \
    kurento-maven-plugin \
    kms-cmake-utils \
    kms-jsonrpc \
    kms-core \
    kms-elements \
    kms-filters \
    kurento-media-server \
    module/kms-chroma \
    module/kms-crowddetector \
    module/kms-datachannelexample \
    module/kms-markerdetector \
    module/kms-opencv-plugin-sample \
    module/kms-platedetector \
    module/kms-plugin-sample \
    module/kms-pointerdetector
