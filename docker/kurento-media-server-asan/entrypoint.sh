#!/usr/bin/env bash

#/ Docker script - Run Kurento Media Server with AddressSanitizer.

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trace all commands
set -o xtrace

# Trap functions
function on_error() {
    echo "[Docker entrypoint] ERROR ($?)"
    exit 1
}
trap on_error ERR

# Settings
BASE_RTP_FILE="/etc/kurento/modules/kurento/BaseRtpEndpoint.conf.ini"
WEBRTC_FILE="/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini"

# Check root permissions -- Overriding the Docker container run user (e.g. with
# `docker run --user=1234`) is not supported, because this entrypoint script
# needs to edit root-owned files under "/etc".
[[ "$(id -u)" -eq 0 ]] || {
    echo "[Docker entrypoint] ERROR: Please run container as root user"
    exit 1
}

# Aux function: set value to a given parameter
function set_parameter() {
    # Assignments fail if any argument is missing (set -o nounset)
    local FILE="$1"
    local PARAM="$2"
    local VALUE="$3"

    local COMMENT=";"  # Kurento .ini files use ';' for comment lines
    local REGEX="^${COMMENT}?\s*${PARAM}=.*"

    if grep --extended-regexp -q "$REGEX" "$FILE"; then
        sed --regexp-extended -i "s/${REGEX}/${PARAM}=${VALUE}/" "$FILE"
    else
        echo "${PARAM}=${VALUE}" >>"$FILE"
    fi
}

# BaseRtpEndpoint settings
if [[ -n "${KMS_MIN_PORT:-}" ]]; then
    set_parameter "$BASE_RTP_FILE" "minPort" "$KMS_MIN_PORT"
fi
if [[ -n "${KMS_MAX_PORT:-}" ]]; then
    set_parameter "$BASE_RTP_FILE" "maxPort" "$KMS_MAX_PORT"
fi
if [[ -n "${KMS_MTU:-}" ]]; then
    set_parameter "$BASE_RTP_FILE" "mtu" "$KMS_MTU"
fi

# WebRtcEndpoint settings
if [[ -n "${KMS_EXTERNAL_IPV4:-}" ]]; then
    if [[ "$KMS_EXTERNAL_IPV4" == "auto" ]]; then
        if IP="$(/getmyip.sh --ipv4)"; then
            set_parameter "$WEBRTC_FILE" "externalIPv4" "$IP"
        fi
    else
        set_parameter "$WEBRTC_FILE" "externalIPv4" "$KMS_EXTERNAL_IPV4"
    fi
fi
if [[ -n "${KMS_EXTERNAL_IPV6:-}" ]]; then
    if [[ "$KMS_EXTERNAL_IPV6" == "auto" ]]; then
        if IP="$(/getmyip.sh --ipv6)"; then
            set_parameter "$WEBRTC_FILE" "externalIPv6" "$IP"
        fi
    else
        set_parameter "$WEBRTC_FILE" "externalIPv6" "$KMS_EXTERNAL_IPV6"
    fi
fi
if [[ -n "${KMS_NETWORK_INTERFACES:-}" ]]; then
    set_parameter "$WEBRTC_FILE" "networkInterfaces" "$KMS_NETWORK_INTERFACES"
fi
if [[ -n "${KMS_ICE_TCP:-}" ]]; then
    set_parameter "$WEBRTC_FILE" "iceTcp" "$KMS_ICE_TCP"
fi
if [[ -n "${KMS_STUN_IP:-}" && -n "${KMS_STUN_PORT:-}" ]]; then
    set_parameter "$WEBRTC_FILE" "stunServerAddress" "$KMS_STUN_IP"
    set_parameter "$WEBRTC_FILE" "stunServerPort" "$KMS_STUN_PORT"
fi
if [[ -n "${KMS_TURN_URL:-}" ]]; then
    set_parameter "$WEBRTC_FILE" "turnURL" "$KMS_TURN_URL"
fi

# Remove the IPv6 loopback until IPv6 is well supported in KMS.
# Notes:
# - `cat /etc/hosts | sed | tee` because `sed -i /etc/hosts` won't work inside a
#   Docker container.
# - `|| true` to avoid errors if the container is not run with the root user.
#   E.g. `docker run --user=1234`.
# shellcheck disable=SC2002
cat /etc/hosts | sed '/::1/d' | tee /etc/hosts >/dev/null || true

# Debug logging -- If empty or unset, use suggested levels
# https://doc-kurento.readthedocs.io/en/latest/features/logging.html#suggested-levels
if [[ -z "${GST_DEBUG:-}" ]]; then
    export GST_DEBUG="2,Kurento*:4,kms*:4,sdp*:4,webrtc*:4,*rtpendpoint:4,rtp*handler:4,rtpsynchronizer:4,agnosticbin:4"
fi

# Run Kurento Media Server
# Use ASAN_OPTIONS recommended for aggressive diagnostics:
# https://github.com/google/sanitizers/wiki/AddressSanitizer#faq
# NOTE: "detect_stack_use_after_return=1" breaks Kurento execution (more study needed to see why)
# NOTE: GST_PLUGIN_DEFINE() causes ODR violations so this check must be disabled
LD_PRELOAD="$PWD/libasan.so" \
ASAN_OPTIONS='detect_odr_violation=0 detect_leaks=1 detect_invalid_pointer_pairs=2 strict_string_checks=1 detect_stack_use_after_return=0 check_initialization_order=1 strict_init_order=1' \
LD_LIBRARY_PATH="$PWD" \
KURENTO_MODULES_PATH="$PWD" \
GST_PLUGIN_PATH="$PWD" \
./kurento-media-server
