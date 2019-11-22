#!/bin/bash

#/ Docker script - Run Kurento Media Server.

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

exec /usr/bin/kurento-media-server "$@"
