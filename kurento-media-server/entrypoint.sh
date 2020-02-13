#!/usr/bin/env bash

#/ Docker script - Run Kurento Media Server.

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trace all commands
set -o xtrace

# Settings
BASE_RTP_FILE="/etc/kurento/modules/kurento/BaseRtpEndpoint.conf.ini"
WEBRTC_FILE="/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini"

# Aux function: set value to a given parameter
function set_parameter() {
    # Assignments fail if any argument is missing (set -o nounset)
    local FILE="$1"
    local PARAM="$2"
    local VALUE="$3"

    local COMMENT=";"  # Kurento .ini files use ';' for comment lines
    local REGEX="^${COMMENT}?\s*${PARAM}=.*"

    if grep --extended-regexp --quiet "$REGEX" "$FILE"; then
        sed --regexp-extended --in-place "s/${REGEX}/${PARAM}=${VALUE}/" "$FILE"
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

# Remove the IPv6 loopback until IPv6 is well supported.
# Notes:
# - `cat /etc/hosts | sed | tee` because `sed -i /etc/hosts` won't work inside a
#   Docker container.
# - `|| true` to avoid errors if the container is not run with the root user.
#   E.g. `docker run --user=1234`.
# shellcheck disable=SC2002
cat /etc/hosts | sed '/::1/d' | tee /etc/hosts >/dev/null || true

# Run Kurento Media Server
exec /usr/bin/kurento-media-server "$@"
