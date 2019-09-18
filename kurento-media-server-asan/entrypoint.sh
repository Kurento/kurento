#!/usr/bin/env bash

#/ Docker script - Run Kurento Media Server with AddressSanitizer.

# Bash options for strict error checking
set -o errexit -o errtrace -o pipefail -o nounset

# Trace all commands
set -o xtrace


# Prepare the Kurento's WebRTC STUN/TURN settings file
true >"$PWD/config/kurento/WebRtcEndpoint.conf.ini"

if [[ -n "$KMS_TURN_URL" ]]; then
    echo "turnURL=$KMS_TURN_URL" >>"$PWD/config/kurento/WebRtcEndpoint.conf.ini"
fi

if [[ -n "$KMS_STUN_IP" ]] && [[ -n "$KMS_STUN_PORT" ]]; then
    echo "stunServerAddress=$KMS_STUN_IP" >>"$PWD/config/kurento/WebRtcEndpoint.conf.ini"
    echo "stunServerPort=$KMS_STUN_PORT"  >>"$PWD/config/kurento/WebRtcEndpoint.conf.ini"
fi

# Remove the IPv6 loopback until IPv6 is well supported
# Note: `sed -i /etc/hosts` won't work inside a Docker container
cat /etc/hosts | sed '/::1/d' | tee /etc/hosts >/dev/null

if [[ ! -d /logs ]]; then
    # Ensure the logs destination dir exists
    mkdir /logs
fi

LD_PRELOAD="$PWD/libasan.so" \
ASAN_OPTIONS='detect_leaks=1 detect_invalid_pointer_pairs=2 strict_string_checks=1 check_initialization_order=1 strict_init_order=1' \
LD_LIBRARY_PATH="$PWD" \
GST_DEBUG_NO_COLOR=1 \
GST_DEBUG='3,Kurento*:4,kms*:4,sdp*:4,webrtc*:4,*rtpendpoint:4,rtp*handler:4,rtpsynchronizer:4,agnosticbin:4' \
./kurento-media-server \
    --modules-path="$PWD:/usr/lib/x86_64-linux-gnu/kurento/modules" \
    --modules-config-path="$PWD/config" \
    --conf-file="$PWD/config/kurento.conf.json" \
    --gst-plugin-path="$PWD"
