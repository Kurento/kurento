#!/usr/bin/env bash

#/ Docker script - Run Kurento Media Server with AddressSanitizer.

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trace all commands
set -o xtrace

# Generate BaseRtpEndpoint settings
CONF_FILE="/etc/kurento/modules/kurento/BaseRtpEndpoint.conf.ini"
true >"$CONF_FILE"
if [[ -n "${KMS_MTU:-}" ]]; then
    echo "mtu=$KMS_MTU" >>"$CONF_FILE"
fi

# Generate WebRtcEndpoint settings
CONF_FILE="/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini"
true >"$CONF_FILE"
if [[ -n "${KMS_NETWORK_INTERFACES:-}" ]]; then
    echo "networkInterfaces=$KMS_NETWORK_INTERFACES" >>"$CONF_FILE"
fi
if [[ -n "${KMS_STUN_IP:-}" ]] && [[ -n "${KMS_STUN_PORT:-}" ]]; then
    echo "stunServerAddress=$KMS_STUN_IP" >>"$CONF_FILE"
    echo "stunServerPort=$KMS_STUN_PORT"  >>"$CONF_FILE"
fi
if [[ -n "${KMS_TURN_URL:-}" ]]; then
    echo "turnURL=$KMS_TURN_URL" >>"$CONF_FILE"
fi

# Remove the IPv6 loopback until IPv6 is well supported
# Note: `sed -i /etc/hosts` won't work inside a Docker container
cat /etc/hosts | sed '/::1/d' | tee /etc/hosts >/dev/null

# Use ASAN_OPTIONS recommended for aggressive diagnostics:
# https://github.com/google/sanitizers/wiki/AddressSanitizer#faq
# NOTE: "detect_stack_use_after_return=1" breaks Kurento execution (more study needed to see why)
# NOTE: GST_PLUGIN_DEFINE() causes ODR violations so this check must be disabled
LD_PRELOAD="$PWD/libasan.so" \
ASAN_OPTIONS='detect_odr_violation=0 detect_leaks=1 detect_invalid_pointer_pairs=2 strict_string_checks=1 detect_stack_use_after_return=0 check_initialization_order=1 strict_init_order=1' \
LD_LIBRARY_PATH="$PWD" \
GST_DEBUG_NO_COLOR=1 \
GST_DEBUG='3,Kurento*:4,kms*:4,sdp*:4,webrtc*:4,*rtpendpoint:4,rtp*handler:4,rtpsynchronizer:4,agnosticbin:4' \
./kurento-media-server \
    --modules-path="$PWD" \
    --gst-plugin-path="$PWD"
