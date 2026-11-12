#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ Check that a JavaScript package can be installed from its packed tarball.
#/
#/ Run from the directory that contains the package.json file. The script:
#/
#/ 1. Runs `npm pack` to build the tarball that `npm publish` would upload.
#/ 2. Checks that every install-time lifecycle script (preinstall, install,
#/    postinstall) that points to a local file refers to a file that is
#/    actually included in the tarball.
#/ 3. Installs the tarball with `npm install` in a clean temporary directory,
#/    and fails if the installation fails.
#/
#/ Arguments
#/ =========
#/
#/ --skip-install
#/
#/   Only run steps 1 and 2; useful when the package dependencies are not
#/   available yet from the registry (e.g. not yet published).
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

CFG_SKIP_INSTALL="false"
while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --skip-install)
            CFG_SKIP_INSTALL="true"
            ;;
        *)
            log "ERROR: Unknown argument '${1-}'"
            exit 1
            ;;
    esac
    shift
done

for cmd in jq npm tar; do
    command -v "$cmd" >/dev/null || {
        log "ERROR: '$cmd' is not installed; please install it"
        exit 1
    }
done

[[ -f package.json ]] || {
    log "ERROR: File not found: package.json"
    exit 1
}



# Pack
# ====

WORK_DIR="$(mktemp --directory)"
trap_add 'rm -rf "$WORK_DIR"' EXIT

TARBALL_NAME="$(npm pack --silent --pack-destination "$WORK_DIR" | tail -n 1)"
TARBALL="$WORK_DIR/$TARBALL_NAME"
[[ -f "$TARBALL" ]] || {
    log "ERROR: npm pack did not produce a tarball"
    exit 1
}

mkdir "$WORK_DIR/unpacked"
tar -xzf "$TARBALL" -C "$WORK_DIR/unpacked"



# Check lifecycle scripts
# =======================

# Only scripts that run when installing from the registry are relevant;
# 'prepare' & co. are not run for registry tarballs.
for hook in preinstall install postinstall; do
    cmd="$(jq --raw-output --arg h "$hook" '.scripts[$h] // empty' \
        "$WORK_DIR/unpacked/package/package.json")"
    [[ -n "$cmd" ]] || continue

    log "Package declares '$hook' script: $cmd"

    # Check each word that looks like a relative path to a local file.
    for word in $cmd; do
        case "$word" in
            -*|*://*|*=*) continue ;;
            */*|*.js|*.sh)
                [[ -e "$WORK_DIR/unpacked/package/$word" ]] || {
                    log "ERROR: '$hook' script uses '$word', which is not in the packed tarball"
                    exit 1
                }
                ;;
        esac
    done
done



# Install in a clean project
# ==========================

if [[ "$CFG_SKIP_INSTALL" == "true" ]]; then
    log "Skip installing the tarball (--skip-install)"
    exit 0
fi

mkdir "$WORK_DIR/project"
pushd "$WORK_DIR/project"
npm init --yes >/dev/null
npm install --no-audit --no-fund "$TARBALL"
popd

log "Package '$TARBALL_NAME' installs correctly"
