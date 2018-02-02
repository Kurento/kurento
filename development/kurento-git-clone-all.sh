#!/usr/bin/env bash
set -eu -o pipefail  # Abort on errors, disallow undefined variables
IFS=$'\n\t'          # Apply word splitting only on newlines and tabs

# Clone all Git repos that are related to Kurento Media Server.
#
# Changes:
# 2018-01-24 Juan Navarro <juan.navarro@gmx.es>
# - Initial version.
# 2018-02-02
# - Use a Bash Array to define all repos.

# Settings
BASE_URL="https://github.com/Kurento"

REPOS=(
  # Main repositories of Kurento Media Server
  kms-omni-build
  kms-cmake-utils
  kms-jsonrpc
  kms-core
  kms-elements
  kms-filters

  # Extra repos, not a core part of KMS
  kms-pointerdetector
  kms-platedetector
  kms-crowddetector
  kms-chroma
  kms-opencv-plugin-sample
  kms-markerdetector
  kms-datachannelexample
  kms-plugin-sample

  # Tools and documentation
  adm-scripts
  bugtracker
  doc-kurento
  kurento-module-creator

  # Client-related repos
  kurento-java
  kurento-tutorial-java
  kurento-media-server
  kurento-tutorial-test
  kurento-maven-plugin
  kurento-qa-pom
)

echo "==== Clone Git repositories ===="
echo "This script will clone all KMS repos"
read -p "Are you sure? Type 'yes': " -r SURE
[ "$SURE" != "yes" ] && [ "$SURE" != "YES" ] && { echo "Aborting"; exit 1; }

echo "Working..."

for REPO in "${REPOS[@]}"; do
  REPO_URL="${BASE_URL}/${REPO}"
  if [ -d "$REPO" ]; then
    echo "Skip repository: $REPO"
  else
    echo "Clone repository: $REPO"
    git clone "$REPO_URL" >/dev/null 2>&1
  fi
done

echo ""
echo "Git repositories cloned at ${PWD}/"

# ------------

echo ""
echo "[$0] Done."
