#!/usr/bin/env bash

#/ Version change helper.
#/
#/ This shell script will traverse all KMS projects and change their
#/ project versions to the one provided as argument.
#/
#/ Arguments:
#/
#/ <Version>
#/
#/    Apply the provided version number.
#/    <Version> should be in a format compatible with Semantic Versioning,
#/    such as "1.2.3" or, in general terms, "<Major>.<Minor>.<Patch>".
#/
#/ --release
#/
#/     Use version numbers intended for Release builds.
#/     If this option is not given, versions are set for nightly snapshots.
#/
#/     Optional. Default: Disabled.
#/
#/ --commit
#/
#/     Commit to Git the version changes.
#/     This will commit only the changed files.
#/
#/     Optional. Default: Disabled.
#/
#/ --tag
#/
#/     Create Git annotated tags with the results of the version change.
#/     This only can only be used if '--commit' is used too.
#/     Also, to avoid mistakes, this can only be used together with '--release'.
#/
#/     Optional. Default: Disabled.



# ------------ Shell setup ------------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path

CONF_FILE="$BASEPATH/bash.conf.sh"
[[ -f "$CONF_FILE" ]] || {
    echo "[$0] ERROR: Config file not found: $CONF_FILE"
    exit 1
}
# shellcheck source=bash.conf.sh
source "$CONF_FILE"



# ------------ Script start ------------

PARAM_VERSION_DEFAULT="0.0.0"



# ---- Parse arguments ----

PARAM_RELEASE="false"
PARAM_COMMIT="false"
PARAM_TAG="false"
PARAM_VERSION="$PARAM_VERSION_DEFAULT"

while [[ $# -gt 0 ]]; do
case "${1-}" in
    --release)
        PARAM_RELEASE="true"
        shift
        ;;
    --commit)
        PARAM_COMMIT="true"
        shift
        ;;
    --tag)
        PARAM_TAG="true"
        shift
        ;;
    *)
        PARAM_VERSION="$1"
        shift
        ;;
esac
done

# Enforce restrictions
if [[ "$PARAM_RELEASE" != "true" ]] || [[ "$PARAM_COMMIT" != "true" ]]; then
    PARAM_TAG="false"
fi

if [[ "$PARAM_VERSION" = "$PARAM_VERSION_DEFAULT" ]]; then
    log "ERROR: Missing <Version>"
    exit 1
fi

log "PARAM_RELEASE=${PARAM_RELEASE}"
log "PARAM_COMMIT=${PARAM_COMMIT}"
log "PARAM_TAG=${PARAM_TAG}"
log "PARAM_VERSION=${PARAM_VERSION}"



# ---- Helper functions ----

commit_and_tag() {
    [[ $# -eq 0 ]] && return 1

    if [[ "$PARAM_COMMIT" = "true" ]]; then
        git add "$1"
        git commit -m "$COMMIT_MSG"

        if [[ "$PARAM_TAG" = "true" ]]; then
            git tag -a -m "$COMMIT_MSG" "$PARAM_VERSION"
        fi
    fi
}



# ---- Apply versions ----

if [[ "$PARAM_RELEASE" = "true" ]]; then
    COMMIT_MSG="Prepare release $PARAM_VERSION"
else
    COMMIT_MSG="Prepare for next development iteration"
fi



# kurento-module-creator

VERSION="$PARAM_VERSION"
[[ "$PARAM_RELEASE" != "true" ]] && VERSION="${VERSION}-SNAPSHOT"

pushd kurento-module-creator

sed --in-place --expression="
    \|<artifactId>kurento-module-creator</artifactId>$|{
        N
        s|<version>.*</version>|<version>${VERSION}</version>|
    }" \
    ./pom.xml

commit_and_tag ./pom.xml

popd



# kms-cmake-utils

VERSION="$PARAM_VERSION"
[[ "$PARAM_RELEASE" != "true" ]] && VERSION="${VERSION}-dev"

pushd kms-cmake-utils

sed --in-place --expression="
    s|get_git_version(PROJECT_VERSION .*)|get_git_version(PROJECT_VERSION ${VERSION})|" \
    ./CMakeLists.txt

commit_and_tag ./CMakeLists.txt

popd



# kms-jsonrpc

VERSION="$PARAM_VERSION"
[[ "$PARAM_RELEASE" != "true" ]] && VERSION="${VERSION}-dev"

pushd kms-jsonrpc

sed --in-place --expression="
    s|get_git_version(PROJECT_VERSION .*)|get_git_version(PROJECT_VERSION ${VERSION})|" \
    ./CMakeLists.txt

commit_and_tag ./CMakeLists.txt

popd



# kms-core

VERSION="$PARAM_VERSION"
[[ "$PARAM_RELEASE" != "true" ]] && VERSION="${VERSION}-dev"

pushd kms-core

sed --in-place --expression="
    \|\"name\": \"core\",$|{
        N
        s|\"version\": \".*\"|\"version\": \"${VERSION}\"|
    }" \
    ./src/server/interface/core.kmd.json

commit_and_tag ./src/server/interface/core.kmd.json

popd



# kms-elements

VERSION="$PARAM_VERSION"
[[ "$PARAM_RELEASE" != "true" ]] && VERSION="${VERSION}-dev"

pushd kms-elements

sed --in-place --expression="
    \|\"name\": \"elements\",$|{
        N
        s|\"version\": \".*\"|\"version\": \"${VERSION}\"|
    }" \
    ./src/server/interface/elements.kmd.json

commit_and_tag ./src/server/interface/elements.kmd.json

popd



# kms-filters

VERSION="$PARAM_VERSION"
[[ "$PARAM_RELEASE" != "true" ]] && VERSION="${VERSION}-dev"

pushd kms-filters

sed --in-place --expression="
    \|\"name\": \"filters\",$|{
        N
        s|\"version\": \".*\"|\"version\": \"${VERSION}\"|
    }" \
    ./src/server/interface/filters.kmd.json

commit_and_tag ./src/server/interface/filters.kmd.json

popd



# kurento-media-server

VERSION="$PARAM_VERSION"
[[ "$PARAM_RELEASE" != "true" ]] && VERSION="${VERSION}-dev"

pushd kurento-media-server

sed --in-place --expression="
    s|get_git_version(PROJECT_VERSION .*)|get_git_version(PROJECT_VERSION ${VERSION})|" \
    ./CMakeLists.txt

commit_and_tag ./CMakeLists.txt

popd
