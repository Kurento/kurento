#!/usr/bin/env bash



# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

# Trace all commands
set -o xtrace



log "##################### EXECUTE: npm-publish #####################"

env

# Functions
vercomp () {
    if [[ $1 == $2 ]]
    then
        return 0
    fi
    local IFS=.
    local i ver1=($1) ver2=($2)
    # fill empty fields in ver1 with zeros
    for ((i=${#ver1[@]}; i<${#ver2[@]}; i++))
    do
        ver1[i]=0
    done
    for ((i=0; i<${#ver1[@]}; i++))
    do
        if [[ -z ${ver2[i]} ]]
        then
            # fill empty fields in ver2 with zeros
            ver2[i]=0
        fi
        if ((10#${ver1[i]} > 10#${ver2[i]}))
        then
            return 1
        fi
        if ((10#${ver1[i]} < 10#${ver2[i]}))
        then
            return 2
        fi
    done
    return 0
}

# Get project data
projectName=$(jshon -e name -u < package.json) || {
  log "ERROR: Command failed: jshon -e name"
  exit 1
}
localVersion=$(jshon -e version -u < package.json ) || {
  log "ERROR: Command failed: jshon -e version"
  exit 1
}
pubVersion=$(npm info --json $projectName | jshon -e version -u || echo "0.0.0") || {
  log "ERROR: Command failed: npm info"
  exit 1
}

localRelease=$(echo $localVersion | awk -F"-" '{print $1}') || {
  log "ERROR: Command failed: awk localVersion"
  exit 1
}
pubRelease=$(echo $pubVersion | awk -F"-" '{print $1}') || {
  log "ERROR: Command failed: awk pubVersion"
  exit 1
}

log "Local version found, V: $localVersion, R: $localRelease"
log "Public version found, V: $pubVersion, R: $pubRelease"

[[ "$localRelease" != "$localVersion" ]] && {
  log "Exit: Version is development"
  exit 0
}

# Publish release only if greater than published
vercomp $localRelease $pubRelease
different=$?
if [ $different -eq 1 ]; then
  log "Publishing to npm $projectName version $localVersion"
  npm publish || {
    log "ERROR: Command failed: npm publish"
    exit 1
  }
else
  log "Exit: public version is already greater or equal than local"
  exit 0
fi
