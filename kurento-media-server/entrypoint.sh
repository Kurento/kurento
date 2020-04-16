#!/usr/bin/env bash

#/ Docker script - Run Kurento Media Server.

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
# Instead, run with `docker run -e KMS_UID=1234`.
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
if [[ -n "${KMS_EXTERNAL_ADDRESS:-}" ]]; then
    if [[ "$KMS_EXTERNAL_ADDRESS" == "auto" ]]; then
        # shellcheck disable=SC2015
        IP="$(curl ifconfig.co 2>/dev/null)" \
            && set_parameter "$WEBRTC_FILE" "externalAddress" "$IP" \
            || true
    else
        set_parameter "$WEBRTC_FILE" "externalAddress" "$KMS_EXTERNAL_ADDRESS"
    fi
fi
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
    export GST_DEBUG="3,Kurento*:4,kms*:4,sdp*:4,webrtc*:4,*rtpendpoint:4,rtp*handler:4,rtpsynchronizer:4,agnosticbin:4"
fi

# Run Kurento Media Server, changing to requested user (if any)
RUN_UID="$(id -u)"
if [[ -n "${KMS_UID:-}" && "$KMS_UID" != "$RUN_UID" ]]; then
    echo "[Docker entrypoint] Start Kurento Media Server, UID: $KMS_UID"

    groupmod \
        --gid "$KMS_UID" \
        kurento

    usermod \
        --uid "$KMS_UID" \
        --gid "$KMS_UID" \
        kurento

    # Run KMS as a child PID; normally, Docker best practices dictate that
    # the main process should run as PID 1. However this is OK as `runuser`
    # monitors and wraps the child process, passing through all input/output
    # and any incoming signal.
    # See: https://unix.stackexchange.com/questions/269254/why-does-util-linux-runuser-su-fork/269330#269330
    exec runuser --user kurento --group kurento -- /usr/bin/kurento-media-server "$*"
else
    echo "[Docker entrypoint] Start Kurento Media Server, UID: $RUN_UID"

    # Run KMS as PID 1 (replace the current process)
    exec /usr/bin/kurento-media-server "$*"
fi
