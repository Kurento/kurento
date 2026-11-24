#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ Prepare JavaScript packages for publishing with NPM.
#/
#/ This script does NOT publish anything. Publishing is done by a separate CI
#/ job that uses NPM trusted publishing (OIDC), which only works in runners
#/ hosted by GitHub; see 'publish_js_npm.sh'. This script applies the same
#/ rules than the publishing script to decide if the package would be published
#/ (not a development version; version greater than the published one), checks
#/ the package, and leaves the verified tarball in the directory given by the
#/ environment variable NPM_TARBALL_DIR.
#/
#/ Before leaving the tarball, 'check_js_package.sh' verifies that the package
#/ installs from it. Set the environment variable
#/ CHECK_JS_PACKAGE_SKIP_INSTALL=true to skip the actual installation (e.g. when
#/ the package dependencies are not yet published in the registry).
#/
#/ Environment variables
#/ =====================
#/
#/ NPM_TARBALL_DIR
#/
#/   Directory where the verified tarball is left. Required.



# Configure shell
# ===============

SELF_DIR="$(cd -P -- "$(dirname -- "${BASH_SOURCE[0]}")" >/dev/null && pwd -P)"
source "$SELF_DIR/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="
trap_add 'log "==================== END ===================="' EXIT

# Trace all commands (to stderr).
set -o xtrace



# Check dependencies
# ==================

command -v jq >/dev/null || {
    log "ERROR: 'jq' is not installed; please install it"
    exit 1
}

command -v npm >/dev/null || {
    log "ERROR: 'npm' is not installed; please install it"
    exit 1
}

if [[ -z "${NPM_TARBALL_DIR:-}" ]]; then
    log "ERROR: NPM_TARBALL_DIR is not set"
    exit 1
fi



# Verify versions
# ===============

projectName="$(jq --raw-output '.name' package.json)"
localVersion="$(jq --raw-output '.version' package.json)"
localRelease="$(echo "$localVersion" | awk -F '-' '{print $1}')"
# If npm info returns an error, don't stop the script, just set pubVersion to an empty string
pubVersion="$(npm info --json "$projectName" 2>/dev/null | jq --raw-output '.version' || echo '')"
if [[ -n "$pubVersion" ]]; then
    pubRelease="$(echo "$pubVersion" | awk -F '-' '{print $1}')"
else
    pubRelease=""
fi


log "Local version: $localVersion (release: $localRelease)"
log "Public version: $pubVersion (release: $pubRelease)"

# Don't deploy if local version is still in development.
if [[ "$localVersion" != "$localRelease" ]]; then
    log "Skip deploying: Local version is in development"
    exit 0
fi

# Don't deploy versions lower than what is already published.
if dpkg --compare-versions "$localRelease" le "$pubRelease"; then
    log "Skip deploying: local version <= public version"
    exit 0
fi



# Fix package.json
# ================

# Apply the same auto-corrections that `npm publish` would do implicitly (e.g.
# repository.url, references to git repositories), so the package that gets
# checked is the one that gets published.
npm pkg fix



# Check the package
# =================

# Make sure that the package can be installed from the tarball that is about to
# be published, before actually publishing it. The verified tarball is the one
# that gets uploaded to the registry later, by `publish_js_npm.sh`.
CHECK_ARGS=(--keep-tarball "$NPM_TARBALL_DIR")
if [[ "${CHECK_JS_PACKAGE_SKIP_INSTALL:-false}" == "true" ]]; then
    CHECK_ARGS+=(--skip-install)
fi
"$SELF_DIR/check_js_package.sh" "${CHECK_ARGS[@]}"

log "Package ready to publish: ${projectName}@${localVersion}"
