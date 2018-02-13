#!/usr/bin/env bash
set -eu -o pipefail  # Abort on errors, disallow undefined variables
IFS=$'\n\t'          # Apply word splitting only on newlines and tabs

# Install dependencies required for generating Debian packages from KMS modules.
#
# Notes:
# - flex will be automatically installed by gstreamer, but for now a bug in
#   package version detection prevents that.
# - libcommons-validator-java seems to be required to build "gstreamer" (it
#   failed with lots of errors from "jade", when building documentation files)
# - realpath is used by 'adm-scripts/kurento_check_version.sh'.
# - subversion (svn) is used in the Python build script (compile_project.py) due
#   to GitHub's lack of support for git-archive protocol
#   (https://github.com/isaacs/github/issues/554).
#
# Changes:
# 2017-10-03 Juan Navarro <juan.navarro@gmx.es>
# - Initial version.
# 2018-01-24
# - Add libcommons-validator-java
# 2018-02-02
# - Use a Bash Array to define all packages; run a single `apt-get` command.

# Check root permissions
[ $(id -u) -eq 0 ] || { echo "Please run as root"; exit 1; }

PACKAGES=(
  # Packaging tools
  build-essential
  debhelper
  curl
  fakeroot
  flex
  git
  libcommons-validator-java
  python
  python-apt
  python-debian
  python-git
  python-requests
  python-yaml
  realpath
  subversion
  wget
)

apt-get update
apt-get install --no-install-recommends --yes "${PACKAGES[@]}"

echo "All packages installed successfully"

# ------------

echo ""
echo "[$0] Done."
