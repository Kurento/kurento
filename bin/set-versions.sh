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
#/   in the Debian control file, ./debian/changelog
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
    if [[ "$CFG_RELEASE" != "true" ]] || [[ "$CFG_COMMIT" != "true" ]]; then
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

PACKAGE_VERSION="${CFG_VERSION}-${CFG_DEBIAN}"

if [[ "$CFG_RELEASE" == "true" ]]; then
    SUFFIX_C=""
    SUFFIX_JAVA=""
    COMMIT_MSG="Prepare release $PACKAGE_VERSION"
else
    SUFFIX_C="-dev"
    SUFFIX_JAVA="-SNAPSHOT"
    if [[ "$CFG_NEWDEVELOPMENT" == "true" ]]; then
        COMMIT_MSG="Prepare for next development iteration"
    else
        COMMIT_MSG="Bump development version to $PACKAGE_VERSION"
    fi
fi



# Helper functions
# ----------------

update_changelog() {
    local SNAPSHOT_ENTRY="* UNRELEASED"
    local RELEASE_ENTRY="* $COMMIT_MSG"

    if [[ "$CFG_RELEASE" == "true" ]]; then
        gbp dch \
            --ignore-branch \
            --git-author \
            --spawn-editor=never \
            --new-version="$PACKAGE_VERSION" \
            \
            --release \
            --distribution="testing" \
            --force-distribution \
            \
            ./debian

        # First appearance of "UNRELEASED": Put our commit message
        sed --in-place --expression="0,/${SNAPSHOT_ENTRY}/{s/${SNAPSHOT_ENTRY}/${RELEASE_ENTRY}/}" \
            ./debian/changelog

        # Remaining appearances of "UNRELEASED" (if any): Delete line
        sed --in-place --expression="/${SNAPSHOT_ENTRY}/d" \
            ./debian/changelog
    else
        gbp dch \
            --ignore-branch \
            --git-author \
            --spawn-editor=never \
            --new-version="$PACKAGE_VERSION" \
            ./debian
    fi
}

commit_and_tag() {
    [[ $# -eq 0 ]] && return 1

    if [[ "$CFG_COMMIT" == "true" ]]; then
        git add debian/changelog
        git add "$1"
        git commit -m "$COMMIT_MSG"

        if [[ "$CFG_TAG" == "true" ]]; then
            git tag -a -m "$COMMIT_MSG" "$CFG_VERSION"
        fi
    fi
}



# Apply versions
# --------------

pushd kurento-module-creator
sed --in-place --expression="
    \|<artifactId>kurento-module-creator</artifactId>$|{
        N
        s|<version>.*</version>|<version>${CFG_VERSION}${SUFFIX_JAVA}</version>|
    }" \
    ./pom.xml
update_changelog
commit_and_tag ./pom.xml
popd  # kurento-module-creator



pushd kms-cmake-utils
sed --in-place --expression="
    s|get_git_version(PROJECT_VERSION .*)|get_git_version(PROJECT_VERSION ${CFG_VERSION}${SUFFIX_C})|" \
    ./CMakeLists.txt
update_changelog
commit_and_tag ./CMakeLists.txt
popd  # kms-cmake-utils



pushd kms-jsonrpc
sed --in-place --expression="
    s|get_git_version(PROJECT_VERSION .*)|get_git_version(PROJECT_VERSION ${CFG_VERSION}${SUFFIX_C})|" \
    ./CMakeLists.txt
update_changelog
commit_and_tag ./CMakeLists.txt
popd  # kms-jsonrpc



pushd kms-core
sed --in-place --expression="
    \|\"name\": \"core\",$|{
        N
        s|\"version\": \".*\"|\"version\": \"${CFG_VERSION}${SUFFIX_C}\"|
    }" \
    ./src/server/interface/core.kmd.json
update_changelog
commit_and_tag ./src/server/interface/core.kmd.json
popd  # kms-core



pushd kms-elements
sed --in-place --expression="
    \|\"name\": \"elements\",$|{
        N
        s|\"version\": \".*\"|\"version\": \"${CFG_VERSION}${SUFFIX_C}\"|
    }" \
    ./src/server/interface/elements.kmd.json
update_changelog
commit_and_tag ./src/server/interface/elements.kmd.json
popd  # kms-elements



pushd kms-filters
sed --in-place --expression="
    \|\"name\": \"filters\",$|{
        N
        s|\"version\": \".*\"|\"version\": \"${CFG_VERSION}${SUFFIX_C}\"|
    }" \
    ./src/server/interface/filters.kmd.json
update_changelog
commit_and_tag ./src/server/interface/filters.kmd.json
popd  # kms-filters



pushd kurento-media-server
sed --in-place --expression="
    s|get_git_version(PROJECT_VERSION .*)|get_git_version(PROJECT_VERSION ${CFG_VERSION}${SUFFIX_C})|" \
    ./CMakeLists.txt
update_changelog
commit_and_tag ./CMakeLists.txt
popd  # kurento-media-server
