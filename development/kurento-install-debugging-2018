#!/usr/bin/env bash
set -eu -o pipefail  # Abort on errors, disallow undefined variables
IFS=$'\n\t'          # Apply word splitting only on newlines and tabs

# Install every package with debugging symbols for Kurento Media Server.
#
# Changes:
# 2018-02-12 Juan Navarro <juan.navarro@gmx.es>
# - Initial version (merged from other install scripts).
# - Add package: libsrtp1-dbg.

# Check root permissions
[ "$(id -u)" -eq 0 ] || { echo "Please run as root"; exit 1; }

PACKAGES=(
  # Kurento external libraries
  gstreamer1.5-plugins-base-dbg
  gstreamer1.5-plugins-good-dbg
  gstreamer1.5-plugins-ugly-dbg
  gstreamer1.5-plugins-bad-dbg
  gstreamer1.5-libav-dbg
  libgstreamer1.5-0-dbg
  libnice-dbg
  libsrtp1-dbg
  openwebrtc-gst-plugins-dbg
  kmsjsoncpp-dbg

  # KMS main components
  kms-jsonrpc-dbg
  kms-core-dbg
  kms-elements-dbg
  kms-filters-dbg
  kurento-media-server-dbg

  # KMS extra modules
  kms-chroma-dbg
  kms-crowddetector-dbg
  kms-platedetector-dbg
  kms-pointerdetector-dbg
)

apt-get update
apt-get install --no-install-recommends --yes "${PACKAGES[@]}"

echo "All packages installed successfully"

# ------------

echo ""
echo "[$0] Done."
