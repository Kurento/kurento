#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ Generate and commit source files for Read The Docs.



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
CFG_GIT_SSH_KEY_PATH=""
CFG_MAVEN_SETTINGS_PATH=""

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --release)
            CFG_RELEASE="true"
            ;;
        --git-ssh-key)
            if [[ -n "${2-}" ]]; then
                CFG_GIT_SSH_KEY_PATH="$(realpath "$2")"
                shift
            else
                log "ERROR: --git-ssh-key expects <Path>"
                exit 1
            fi
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
log "CFG_GIT_SSH_KEY_PATH=$CFG_GIT_SSH_KEY_PATH"
log "CFG_MAVEN_SETTINGS_PATH=$CFG_MAVEN_SETTINGS_PATH"



# Verify project
# ==============

{
    CHECK_VERSION_ARGS=()

    # Doc versions are only composed of Major.Minor, while .Patch or bug-fix
    # versions are all summarized under the same Minor release.
    CHECK_VERSION_ARGS+=(--minor)

    if [[ "$CFG_RELEASE" == "true" ]]; then
        CHECK_VERSION_ARGS+=(--release)
    fi

    check_version.sh "${CHECK_VERSION_ARGS[@]}" || {
        log "ERROR: Command failed: check_version.sh"
        exit 1
    }
}



# Generate doc
# ============

if [[ -n "${CFG_MAVEN_SETTINGS_PATH:-}" ]]; then
    sed "s|MAVEN_ARGS :=|MAVEN_ARGS := --settings $CFG_MAVEN_SETTINGS_PATH|" Makefile >Makefile.ci
else
    cp Makefile Makefile.ci
fi

make --file=Makefile.ci ci-readthedocs
rm Makefile.ci



# Commit generated doc
# ====================

if [[ -n "${CFG_GIT_SSH_KEY_PATH:-}" ]]; then
    # SSH is hardcoded to assume that the current UID is an already existing
    # system user. This was common in the past but not any more with containers.
    # Using the NSS wrapper library we can trick SSH into working with any UID.
    echo "kurento:x:$(id -u):$(id -g)::/home/kurento:/usr/sbin/nologin" >"/tmp/passwd"
    echo "kurento:x:$(id -g):" >"/tmp/group"
    # https://git-scm.com/docs/git#Documentation/git.txt-codeGITSSHCOMMANDcode
    export GIT_SSH_COMMAND="LD_PRELOAD=libnss_wrapper.so NSS_WRAPPER_PASSWD=/tmp/passwd NSS_WRAPPER_GROUP=/tmp/group ssh -i $CFG_GIT_SSH_KEY_PATH -o IdentitiesOnly=yes -o StrictHostKeychecking=no"
fi

PROJECT_VERSION="$(get_version.sh)"

REPO_NAME="doc-kurento-readthedocs"
REPO_URL="git@github.com:Kurento/$REPO_NAME.git"
REPO_DIR="$(mktemp --directory)"

git clone --depth 1 "$REPO_URL" "$REPO_DIR"

rsync -av --delete \
    --exclude-from=.gitignore \
    --exclude='.git*' \
    ./ "$REPO_DIR"/

log "Commit and push changes to Kurento/$REPO_NAME"

{
    pushd "$REPO_DIR"

    # Check if there are any changes; if so, commit them.
    git update-index -q --refresh
    if ! git diff-index --quiet --exit-code HEAD; then
        # `--all` to include possibly deleted files.
        git add --all .

        git commit -m "Code autogenerated from Kurento/kurento@$GIT_HASH_SHORT"

        git push
    fi

    # If release, create an annotated tag.
    if [[ "$CFG_RELEASE" == "true" ]]; then
        # --force: Replace tag if it exists (instead of failing).
        # This is done so it becomes possible to re-issue release builds.
        git tag --force --annotate -m "$PROJECT_VERSION" "$PROJECT_VERSION"
        git push --force origin "$PROJECT_VERSION"
    fi

    popd  # $REPO_DIR
}
