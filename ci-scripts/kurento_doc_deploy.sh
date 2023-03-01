#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ Generate and commit source files for Read The Docs.
#/
#/
#/ Arguments
#/ =========
#/
#/ --release
#/
#/   Build documentation sources intended for a Release build.
#/   If this option is not given, sources are built as development snapshots.
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

CFG_RELEASE="false"
CFG_MAVEN_SETTINGS_PATH=""

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --release)
            CFG_RELEASE="true"
            ;;
        --maven-settings)
            if [[ -n "${2-}" ]]; then
                CFG_MAVEN_SETTINGS_PATH="$(realpath "$2")"
                shift
            else
                log "ERROR: --maven-settings expects <Path>"
                exit 1
            fi
            ;;
        *)
            log "ERROR: Unknown argument '${1-}'"
            exit 1
            ;;
    esac
    shift
done



# Validate config
# ===============

log "CFG_RELEASE=$CFG_RELEASE"
log "CFG_MAVEN_SETTINGS_PATH=$CFG_MAVEN_SETTINGS_PATH"



# Generate doc
# ============

./configure.sh

if [[ -z "${CFG_MAVEN_SETTINGS_PATH:-}" ]]; then
    cp Makefile Makefile.ci
else
    sed "s|MAVEN_ARGS :=|MAVEN_ARGS := --settings $CFG_MAVEN_SETTINGS_PATH|" Makefile >Makefile.ci
fi

make --file=Makefile.ci ci-readthedocs
rm Makefile.ci

if [[ "$CFG_RELEASE" == "true" ]]; then
    log "Command: kurento_check_version (tagging enabled)"
    kurento_check_version.sh --create-git-tag
else
    log "Command: kurento_check_version (tagging disabled)"
    kurento_check_version.sh
fi



# Commit generated doc
# ====================

# Create a temp directory for the ReadTheDocs sources, and commit everything.

RTD_DIR="$(mktemp --directory)"

git clone "git@github.com:Kurento/doc-kurento-readthedocs.git" "$RTD_DIR"

rsync -av --delete \
    --exclude-from=.gitignore \
    --exclude='.git*' \
    ./ "$RTD_DIR"/

log "Commit and push changes to doc-kurento-readthedocs.git"
GIT_COMMIT="$(git rev-parse --short HEAD)"

{
    pushd "$RTD_DIR"

    # Check if there are any changes; if so, commit them.
    if ! git diff-index --quiet HEAD; then
        # `--all` to include possibly deleted files.
        git add --all .

        git commit -m "Code autogenerated from Kurento/kurento@${GIT_COMMIT}"

        git push
    fi

    if [[ "$CFG_RELEASE" == "true" ]]; then
        log "Command: kurento_check_version (tagging enabled)"
        kurento_check_version.sh --create-git-tag
    else
        log "Command: kurento_check_version (tagging disabled)"
        kurento_check_version.sh
    fi

    popd  # $RTD_DIR
}
