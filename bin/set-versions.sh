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
#/ --release
#/
#/   Use version numbers intended for Release builds, such as "1.2.3".
#/   If this option is not given, a nightly/snapshot indicator is appended.
#/
#/   Optional. Default: Disabled.
#/
#/ --commit
#/
#/   Commit to Git the version changes.
#/   This will commit only the changed files.
#/
#/   Optional. Default: Disabled.
#/
#/ --tag
#/
#/   Create Git annotated tags with the results of the version change.
#/   This requires that '--commit' is used too.
#/   Also, to avoid mistakes, this can only be used together with '--release'.
#/
#/   Optional. Default: Disabled.



# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1



# Parse call arguments
# --------------------

CFG_RELEASE="false"
CFG_COMMIT="false"
CFG_TAG="false"
CFG_VERSION_DEFAULT="0.0.0"
CFG_VERSION="$CFG_VERSION_DEFAULT"

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --release)
            CFG_RELEASE="true"
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

log "CFG_RELEASE=${CFG_RELEASE}"
log "CFG_COMMIT=${CFG_COMMIT}"
log "CFG_TAG=${CFG_TAG}"
log "CFG_VERSION=${CFG_VERSION}"



# Helper functions
# ----------------

update_changelog() {
    if [[ "$CFG_RELEASE" == "true" ]]; then
        # First appearance of 'UNRELEASED': Put our commit message
        sed --in-place --expression="s|* UNRELEASED|* ${COMMIT_MSG}|" \
            ./debian/changelog

        gbp dch \
            --ignore-branch \
            --git-author \
            --new-version="$CFG_VERSION" \
            --spawn-editor=never \
            \
            --release \
            --distribution='testing' \
            --force-distribution \
            \
            ./debian/

        # Remaining appearances of 'UNRELEASED': Delete line
        sed --in-place --expression="/* UNRELEASED/d" \
            ./debian/changelog
    else
        gbp dch \
            --ignore-branch \
            --git-author \
            --new-version="$CFG_VERSION" \
            --spawn-editor=never \
            ./debian/
    fi
}

commit_and_tag() {
    [[ $# -eq 0 ]] && return 1

    if [[ "$CFG_COMMIT" == "true" ]]; then
        git add "$1"
        git add debian/changelog
        git commit -m "$COMMIT_MSG"

        if [[ "$CFG_TAG" == "true" ]]; then
            git tag -a -m "$COMMIT_MSG" "$CFG_VERSION"
        fi
    fi
}



# Apply versions
# --------------

if [[ "$CFG_RELEASE" == "true" ]]; then
    COMMIT_MSG="Prepare release $CFG_VERSION"
    SUFFIX_C=""
    SUFFIX_JAVA=""
else
    COMMIT_MSG="Prepare for next development iteration"
    SUFFIX_C="-dev"
    SUFFIX_JAVA="-SNAPSHOT"
fi



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
