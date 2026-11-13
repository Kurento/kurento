#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ Publish packed JavaScript packages (tarballs) to NPM.
#/
#/ This script is meant to run in a GitHub-hosted runner, in a job that has the
#/ permission "id-token: write". It publishes with NPM trusted publishing
#/ (OIDC): no NPM token is used, and provenance is generated automatically.
#/
#/ The tarballs are built and verified in an earlier job with 'deploy_js_npm.sh'
#/ and 'check_js_package.sh'. This script applies the same rules than that job:
#/ development versions (-dev) are not published, and neither are versions
#/ that are not greater than the one already published.
#/
#/ Arguments
#/ =========
#/
#/ <Directory>
#/
#/   Directory that contains the '*.tgz' files to publish (searched
#/   recursively). If there are none, there is nothing to do.
#/
#/   Required.
#/
#/ --dry-run
#/
#/   Run all checks and `npm publish --dry-run`; nothing gets published. The
#/   checks for NPM and OIDC credentials are skipped, and provenance is not
#/   requested, so this mode can run anywhere.
#/
#/   Optional. Default: Disabled. Also enabled if the environment variable
#/   PUBLISH_JS_NPM_DRY_RUN is "true".



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

CFG_TARBALLS_DIR=""
CFG_DRY_RUN="false"
[[ "${PUBLISH_JS_NPM_DRY_RUN:-false}" == "true" ]] && CFG_DRY_RUN="true"

while [[ $# -gt 0 ]]; do
    case "${1-}" in
        --dry-run)
            CFG_DRY_RUN="true"
            ;;
        -*)
            log "ERROR: Unknown argument '${1-}'"
            exit 1
            ;;
        *)
            if [[ -n "$CFG_TARBALLS_DIR" ]]; then
                log "ERROR: Only one directory is expected"
                exit 1
            fi
            CFG_TARBALLS_DIR="$1"
            ;;
    esac
    shift
done

[[ -n "$CFG_TARBALLS_DIR" ]] || {
    log "ERROR: Missing argument: <Directory>"
    exit 1
}
[[ -d "$CFG_TARBALLS_DIR" ]] || {
    log "ERROR: Not a directory: $CFG_TARBALLS_DIR"
    exit 1
}

log "CFG_TARBALLS_DIR=$CFG_TARBALLS_DIR"
log "CFG_DRY_RUN=$CFG_DRY_RUN"



# Check dependencies
# ==================

for cmd in jq npm node tar dpkg; do
    command -v "$cmd" >/dev/null || {
        log "ERROR: '$cmd' is not installed; please install it"
        exit 1
    }
done

# Returns success if the first version is >= the second one.
function version_ge {
    [[ "$(printf '%s\n%s\n' "$1" "$2" | sort --version-sort | head -n1)" == "$2" ]]
}

# NPM trusted publishing requires these versions:
# https://docs.npmjs.com/trusted-publishers
NODE_VERSION="$(node --version)"
NODE_VERSION="${NODE_VERSION#v}"
NPM_VERSION="$(npm --version)"
log "Node: $NODE_VERSION; npm: $NPM_VERSION"
if ! version_ge "$NODE_VERSION" "22.14.0"; then
    log "ERROR: Node >= 22.14.0 is required for NPM trusted publishing; found $NODE_VERSION"
    exit 1
fi
if ! version_ge "$NPM_VERSION" "11.5.1"; then
    log "ERROR: npm >= 11.5.1 is required for NPM trusted publishing; found $NPM_VERSION"
    exit 1
fi



# Check credentials
# =================

if [[ "$CFG_DRY_RUN" != "true" ]]; then
    # The OIDC token can only be requested by jobs with "id-token: write".
    # Note: Use `${VAR:+x}` to test the variables, so that their values (secrets)
    # are not printed by the xtrace option.
    if [[ -z "${ACTIONS_ID_TOKEN_REQUEST_URL:+x}" || -z "${ACTIONS_ID_TOKEN_REQUEST_TOKEN:+x}" ]]; then
        log "ERROR: Cannot request an OIDC token. Run this in a GitHub-hosted runner, in a job with 'permissions: id-token: write'"
        exit 1
    fi

    # Trusted publishing is used only when no other credential is configured.
    # Any token would take precedence, hiding a failure of the OIDC setup.
    for var in NODE_AUTH_TOKEN NPM_TOKEN NPM_CONFIG_USERCONFIG; do
        if [[ -n "${!var:+x}" ]]; then
            log "ERROR: Environment variable $var is set; this script publishes only with OIDC"
            exit 1
        fi
    done
fi



# Publish
# =======

mapfile -t TARBALLS < <(find "$CFG_TARBALLS_DIR" -type f -name '*.tgz' | sort)

if [[ ${#TARBALLS[@]} -eq 0 ]]; then
    log "Nothing to publish: no tarballs in $CFG_TARBALLS_DIR"
    exit 0
fi

# First pass: read and validate all the tarballs, so that a bad one is detected
# before anything gets published.
declare -a TO_PUBLISH=()
for tarball in "${TARBALLS[@]}"; do
    packageJson="$(tar -xzOf "$tarball" package/package.json)" || {
        log "ERROR: Cannot read package/package.json from $tarball"
        exit 1
    }
    projectName="$(jq --raw-output '.name // empty' <<<"$packageJson")"
    localVersion="$(jq --raw-output '.version // empty' <<<"$packageJson")"
    if [[ -z "$projectName" || -z "$localVersion" ]]; then
        log "ERROR: Missing name or version in $tarball"
        exit 1
    fi
    localRelease="$(echo "$localVersion" | awk -F '-' '{print $1}')"

    # `npm view` fails if the package does not exist yet; that is not an error
    # here, the first version of a new package has to be published by hand.
    pubVersion="$(npm view "$projectName" version 2>/dev/null || true)"
    pubRelease=""
    if [[ -n "$pubVersion" ]]; then
        pubRelease="$(echo "$pubVersion" | awk -F '-' '{print $1}')"
    fi

    log "$projectName: local version $localVersion (release: $localRelease); public version: ${pubVersion:-<none>}"

    # Don't publish if the version is still in development.
    if [[ "$localVersion" != "$localRelease" ]]; then
        log "Skip publishing $projectName: Local version is in development"
        continue
    fi

    # Don't publish versions lower than what is already published.
    if [[ -n "$pubRelease" ]] && dpkg --compare-versions "$localRelease" le "$pubRelease"; then
        log "Skip publishing $projectName: local version <= public version"
        continue
    fi

    # NPM trusted publishing cannot create packages: they must exist, with a
    # trusted publisher configured, so the first version is published by hand.
    if [[ -z "$pubVersion" && "$CFG_DRY_RUN" != "true" ]]; then
        log "ERROR: $projectName is not in the registry yet. Publish its first version by hand, then configure its trusted publisher in npmjs.com"
        exit 1
    fi

    TO_PUBLISH+=("$tarball")
done

if [[ ${#TO_PUBLISH[@]} -eq 0 ]]; then
    log "Nothing to publish"
    exit 0
fi

# Second pass: publish.
PUBLISH_ARGS=(--access public)
if [[ "$CFG_DRY_RUN" == "true" ]]; then
    PUBLISH_ARGS+=(--dry-run)
else
    PUBLISH_ARGS+=(--provenance)
fi

for tarball in "${TO_PUBLISH[@]}"; do
    log "Publishing to NPM: $tarball"
    npm publish "$tarball" "${PUBLISH_ARGS[@]}"
done

log "Published ${#TO_PUBLISH[@]} package(s)"
