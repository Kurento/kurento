#!/usr/bin/env bash
set -eu -o pipefail  # Abort on errors, disallow undefined variables
IFS=$'\n\t'          # Apply word splitting only on newlines and tabs

# Install dependencies required for development of KMS.
#
# Notes:
# - "gstreamer1.5-x" is needed for the "timeoverlay" GStreamer plugin,
# used by some tests in kms-elements.
#
# Sources:
# - kurento_build_run.txt
#
# Changes:
# 2017-10-03 Juan Navarro <juan.navarro@gmx.es>
# - Initial version.
# 2018-02-02
# - Use a Bash Array to define all packages; run a single `apt-get` command.

# Check root permissions
[ "$(id -u)" -eq 0 ] || { echo "Please run as root"; exit 1; }

PACKAGES=(
  # Development tools
  build-essential
  gdb
  pkg-config
  cmake
  clang
  debhelper
  valgrind
  git
  wget
  maven
  default-jdk

  # Development libraries for KMS
  libboost-dev
  libboost-filesystem-dev
  libboost-log-dev
  libboost-program-options-dev
  libboost-regex-dev
  libboost-system-dev
  libboost-test-dev
  libboost-thread-dev
  libevent-dev
  libglib2.0-dev
  libglibmm-2.4-dev
  libopencv-dev
  libsigc++-2.0-dev
  libsoup2.4-dev
  libssl-dev
  libvpx-dev
  libxml2-utils
  uuid-dev

  # KMS fork libraries & rest of tools
  gstreamer1.5-libav
  gstreamer1.5-nice
  gstreamer1.5-plugins-bad
  gstreamer1.5-plugins-base
  gstreamer1.5-plugins-good
  gstreamer1.5-plugins-ugly
  gstreamer1.5-x
  libgstreamer1.5-dev
  libgstreamer-plugins-base1.5-dev
  libnice-dev
  openh264-gst-plugins-bad-1.5
  openwebrtc-gst-plugins-dev
  kmsjsoncpp-dev
  ffmpeg

  # [Optional] Debug symbols
  gstreamer1.5-libav-dbg
  gstreamer1.5-plugins-bad-dbg
  gstreamer1.5-plugins-base-dbg
  gstreamer1.5-plugins-good-dbg
  gstreamer1.5-plugins-ugly-dbg
  libgstreamer1.5-0-dbg
  libnice-dbg
  openwebrtc-gst-plugins-dbg
  kmsjsoncpp-dbg
)

apt-get update
apt-get install --no-install-recommends --yes "${PACKAGES[@]}"

echo "All packages installed successfully"

# ------------

echo ""
echo "[$0] Done."
