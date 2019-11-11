#!/bin/bash

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trace all commands
set -o xtrace

# Prepare the Kurento's WebRTC STUN/TURN settings file
true >/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini

if [[ -n "${KMS_TURN_URL}" ]]; then
    echo "turnURL=${KMS_TURN_URL}" >>/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini
fi

if [[ -n "${KMS_STUN_IP}" ]] && [[ -n "${KMS_STUN_PORT}" ]]; then
    echo "stunServerAddress=${KMS_STUN_IP}" >>/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini
    echo "stunServerPort=${KMS_STUN_PORT}"  >>/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini
fi

if [[ -n "${KMS_PUBLIC_IP}" ]]; then
	if [[ "${KMS_PUBLIC_IP}" == "AUTO" ]]; then
		echo "externalAddresses=$(curl ifconfig.co)" >>/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini
	else
		echo "externalAddresses=${KMS_PUBLIC_IP}" >>/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini
	fi
fi

# Remove the IPv6 loopback until IPv6 is well supported
# Note: `sed -i /etc/hosts` won't work inside a Docker container
cat /etc/hosts | sed '/::1/d' | tee /etc/hosts >/dev/null

exec /usr/bin/kurento-media-server "$@"
