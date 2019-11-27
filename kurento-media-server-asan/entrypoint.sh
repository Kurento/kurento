#!/usr/bin/env bash

#/ Docker script - Run Kurento Media Server with AddressSanitizer.

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trace all commands
set -o xtrace

# Settings
BASE_RTP_FILE="/etc/kurento/modules/kurento/BaseRtpEndpoint.conf.ini"
WEBRTC_FILE="/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini"

# Aux function: set value to a given parameter
function set_parameter() {
    # Assignments fail if any argument is missing
    local FILE="$1"
    local PARAM="$2"
    local VALUE="$3"

    local COMMENT=";"  # Kurento .ini files use ';' for comment lines
    local REGEX="^${COMMENT}?\s*${PARAM}=.*"

    if grep -q -E "$REGEX" "$FILE"; then
        sed -i -r "s/${REGEX}/${PARAM}=${VALUE}/" "$FILE"
    else
        echo "${PARAM}=${VALUE}" >>"$FILE"
    fi
}

# BaseRtpEndpoint settings
if [[ -n "${KMS_MTU:-}" ]]; then
    set_parameter "$BASE_RTP_FILE" "mtu" "$KMS_MTU"
fi

# WebRtcEndpoint settings
if [[ -n "${KMS_NETWORK_INTERFACES:-}" ]]; then
    set_parameter "$WEBRTC_FILE" "networkInterfaces" "$KMS_NETWORK_INTERFACES"
fi
if [[ -n "${KMS_STUN_IP:-}" ]] && [[ -n "${KMS_STUN_PORT:-}" ]]; then
    set_parameter "$WEBRTC_FILE" "stunServerAddress" "$KMS_STUN_IP"
    set_parameter "$WEBRTC_FILE" "stunServerPort" "$KMS_STUN_PORT"
fi
if [[ -n "${KMS_TURN_URL:-}" ]]; then
    set_parameter "$WEBRTC_FILE" "turnURL" "$KMS_TURN_URL"
fi

# Remove the IPv6 loopback until IPv6 is well supported
# Note: `sed -i /etc/hosts` won't work inside a Docker container
cat /etc/hosts | sed '/::1/d' | tee /etc/hosts >/dev/null

# Run Kurento Media Server
# Use ASAN_OPTIONS recommended for aggressive diagnostics:
# https://github.com/google/sanitizers/wiki/AddressSanitizer#faq
# NOTE: "detect_stack_use_after_return=1" breaks Kurento execution (more study needed to see why)
# NOTE: GST_PLUGIN_DEFINE() causes ODR violations so this check must be disabled
LD_PRELOAD="$PWD/libasan.so" \
ASAN_OPTIONS='detect_odr_violation=0 detect_leaks=1 detect_invalid_pointer_pairs=2 strict_string_checks=1 detect_stack_use_after_return=0 check_initialization_order=1 strict_init_order=1' \
LD_LIBRARY_PATH="$PWD" \
./kurento-media-server \
    --modules-path="$PWD" \
    --gst-plugin-path="$PWD"
