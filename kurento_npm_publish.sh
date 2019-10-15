#!/usr/bin/env bash

# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

# Trace all commands
set -o xtrace



log "##################### EXECUTE: npm-publish #####################"

# Get project data
projectName="$(jshon -e name -u <package.json)" || {
    log "ERROR: Command failed: jshon -e name"
    exit 1
}

localVersion="$(jshon -e version -u <package.json)" || {
    log "ERROR: Command failed: jshon -e version"
    exit 1
}

pubVersion="$(npm info --json "$projectName" | jshon -e version -u)" || {
    log "ERROR: Command failed: npm info"
    exit 1
}

localRelease="$(echo "$localVersion" | awk -F '-' '{print $1}')" || {
    log "ERROR: Command failed: awk localVersion"
    exit 1
}

pubRelease="$(echo "$pubVersion" | awk -F '-' '{print $1}')" || {
    log "ERROR: Command failed: awk pubVersion"
    exit 1
}

log "Local version: $localVersion ($localRelease)"
log "Public version: $pubVersion ($pubRelease)"

# Don't publish if local version is still in development
[[ "$localVersion" != "$localRelease" ]] && {
    log "Skip publishing: Local version is development"
    exit 0
}

# Don't publish versions lower than what is already published
if dpkg --compare-versions "$localRelease" gt "$pubRelease"; then
    log "Publishing to NPM: ${projectName}-${localVersion}"
    npm publish || {
        log "ERROR: Command failed: npm publish"
        exit 1
    }
else
    log "Skip publishing: local version <= public version"
    exit 0
fi
