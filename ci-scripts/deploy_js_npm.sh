#!/usr/bin/env bash
# Checked with ShellCheck (https://www.shellcheck.net/)

#/ Deploy JavaScript packages with NPM.



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



# Deploy
# ======

log "Deploying to NPM: ${projectName}@${localVersion}"

# The NPM access token should be available in an environment variable
# named as given here. Note this is a static string: no var expansion done.
# shellcheck disable=SC2016
NPM_CONFIG_USERCONFIG=<(echo '//registry.npmjs.org/:_authToken=${KURENTO_NPM_TOKEN}') \
npm publish
