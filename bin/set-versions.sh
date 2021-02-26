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
#/   Use version numbers intended for Release builds, such as "1.2.3". If this
#/   option is not given, a nightly/snapshot indicator is appended: "-dev".
#/
#/   If '--commit' is also present, this option uses the commit message
#/   "Prepare release <Version>". The convention is to use this message to
#/   make a new final release.
#/
#/   Optional. Default: Disabled.
#/
#/ --new-development
#/
#/   Mark the start of a new development iteration.
#/
#/   If '--commit' is also present, this option uses the commit message
#/   "Prepare for next development iteration". The convention is to use this
#/   message to start development on a new project version after a release.
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

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Check requirements
command -v jq >/dev/null || {
    echo "ERROR: 'jq' is not installed; please install it"
    exit 1
}

set -o xtrace



# Parse call arguments
# --------------------

CFG_VERSION=""
CFG_RELEASE="false"
CFG_NEWDEVELOPMENT="false"
CFG_COMMIT="false"
CFG_TAG="false"

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
        echo "WARNING: Ignoring '--tag': Requires '--release' and '--commit'"
        CFG_TAG="false"
    fi
fi

if [[ -z "$CFG_VERSION" ]]; then
    echo "ERROR: Missing <Version>"
    exit 1
fi

REGEX='^[[:digit:]]+\.[[:digit:]]+\.[[:digit:]]+$'
[[ "$CFG_VERSION" =~ $REGEX ]] || {
    echo "ERROR: '$CFG_VERSION' must be compatible with Semantic Versioning: <Major>.<Minor>.<Patch>"
    exit 1
}

echo "CFG_VERSION=$CFG_VERSION"
echo "CFG_RELEASE=$CFG_RELEASE"
echo "CFG_NEWDEVELOPMENT=$CFG_NEWDEVELOPMENT"
echo "CFG_COMMIT=$CFG_COMMIT"
echo "CFG_TAG=$CFG_TAG"



# Init internal variables
# -----------------------

if [[ "$CFG_RELEASE" == "true" ]]; then
    COMMIT_MSG="Prepare release $CFG_VERSION"

    VERSION_JS="$CFG_VERSION"
else
    if [[ "$CFG_NEWDEVELOPMENT" == "true" ]]; then
        COMMIT_MSG="Prepare for next development iteration"
    else
        COMMIT_MSG="Update version to $CFG_VERSION"
    fi

    VERSION_JS="${CFG_VERSION}-dev"
fi



# Helper functions
# ----------------

# Run jq, which doesn't have "in-place" mode like other UNIX tools.
function run_jq() {
    local FILTER="$1"
    local FILE="$2"
    local TEMP="$(mktemp)"
    /usr/bin/jq "$FILTER" "$FILE" >"$TEMP" && mv "$TEMP" "$FILE"
}

# Adds the given file(s) to the Git stage area.
function git_add() {
    [[ $# -eq 0 ]] && { echo "ERROR: file(s) expected"; return 1; }

    if [[ "$CFG_COMMIT" == "true" ]]; then
        git add -- "$@"
    fi
}

# Creates a commit and tags it.
function git_commit_and_tag() {
    [[ $# -ne 0 ]] && { echo "ERROR: No args expected"; return 1; }

    if [[ "$CFG_COMMIT" == "true" ]]; then
        # If there are new staged changes ready to be committed:
        if ! git diff --staged --quiet; then
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

pushd kurento-chroma/
TEMP="$(mktemp)"
run_jq ".version = \"$VERSION_JS\"" package.json
if [[ "$CFG_RELEASE" == "true" ]]; then
    run_jq "
        .dependencies.\"kurento-client\" = \"$VERSION_JS\"
        | .dependencies.\"kurento-module-chroma\" = \"$VERSION_JS\"
    " package.json
    run_jq "
        .dependencies.\"kurento-utils\" = \"$VERSION_JS\"
    " static/bower.json
else
    run_jq "
        .dependencies.\"kurento-client\" = \"git+https://github.com/Kurento/kurento-client-js.git\"
        | .dependencies.\"kurento-module-chroma\" = \"git+https://github.com/Kurento/kurento-module-chroma-js.git\"
    " package.json
    run_jq "
        .dependencies.\"kurento-utils\" = \"git+https://github.com/Kurento/kurento-utils-bower.git\"
    " static/bower.json
fi
git_add \
    package.json \
    static/bower.json
popd



pushd kurento-crowddetector/
TEMP="$(mktemp)"
run_jq ".version = \"$VERSION_JS\"" package.json
if [[ "$CFG_RELEASE" == "true" ]]; then
    run_jq "
        .dependencies.\"kurento-client\" = \"$VERSION_JS\"
        | .dependencies.\"kurento-module-crowddetector\" = \"$VERSION_JS\"
    " package.json
    run_jq "
        .dependencies.\"kurento-utils\" = \"$VERSION_JS\"
    " static/bower.json
else
    run_jq "
        .dependencies.\"kurento-client\" = \"git+https://github.com/Kurento/kurento-client-js.git\"
        | .dependencies.\"kurento-module-crowddetector\" = \"git+https://github.com/Kurento/kurento-module-crowddetector-js.git\"
    " package.json
    run_jq "
        .dependencies.\"kurento-utils\" = \"git+https://github.com/Kurento/kurento-utils-bower.git\"
    " static/bower.json
fi
git_add \
    package.json \
    static/bower.json
popd



pushd kurento-hello-world/
TEMP="$(mktemp)"
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
git_add \
    package.json \
    static/bower.json
popd



pushd kurento-magic-mirror/
TEMP="$(mktemp)"
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
git_add \
    package.json \
    static/bower.json
popd



pushd kurento-one2many-call/
TEMP="$(mktemp)"
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
git_add \
    package.json \
    static/bower.json
popd



pushd kurento-one2one-call/
TEMP="$(mktemp)"
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
git_add \
    package.json \
    static/bower.json
popd



pushd kurento-platedetector/
TEMP="$(mktemp)"
run_jq ".version = \"$VERSION_JS\"" package.json
if [[ "$CFG_RELEASE" == "true" ]]; then
    run_jq "
        .dependencies.\"kurento-client\" = \"$VERSION_JS\"
        | .dependencies.\"kurento-module-platedetector\" = \"$VERSION_JS\"
    " package.json
    run_jq "
        .dependencies.\"kurento-utils\" = \"$VERSION_JS\"
    " static/bower.json
else
    run_jq "
        .dependencies.\"kurento-client\" = \"git+https://github.com/Kurento/kurento-client-js.git\"
        | .dependencies.\"kurento-module-platedetector\" = \"git+https://github.com/Kurento/kurento-module-platedetector-js.git\"
    " package.json
    run_jq "
        .dependencies.\"kurento-utils\" = \"git+https://github.com/Kurento/kurento-utils-bower.git\"
    " static/bower.json
fi
git_add \
    package.json \
    static/bower.json
popd



pushd kurento-pointerdetector/
TEMP="$(mktemp)"
run_jq ".version = \"$VERSION_JS\"" package.json
if [[ "$CFG_RELEASE" == "true" ]]; then
    run_jq "
        .dependencies.\"kurento-client\" = \"$VERSION_JS\"
        | .dependencies.\"kurento-module-pointerdetector\" = \"$VERSION_JS\"
    " package.json
    run_jq "
        .dependencies.\"kurento-utils\" = \"$VERSION_JS\"
    " static/bower.json
else
    run_jq "
        .dependencies.\"kurento-client\" = \"git+https://github.com/Kurento/kurento-client-js.git\"
        | .dependencies.\"kurento-module-pointerdetector\" = \"git+https://github.com/Kurento/kurento-module-pointerdetector-js.git\"
    " package.json
    run_jq "
        .dependencies.\"kurento-utils\" = \"git+https://github.com/Kurento/kurento-utils-bower.git\"
    " static/bower.json
fi
git_add \
    package.json \
    static/bower.json
popd



# Commit all changes that have been staged until this point.
git_commit_and_tag



echo "Done!"
