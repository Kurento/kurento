#!/bin/bash
set -e

if [ ! -z "$COTURN_PORT_3478_TCP_ADDR" ]; then
  if [ ! -z "$COTURN_PORT_3478_TCP_PORT" ]; then
    # Generate WebRtcEndpoint configuration
    echo "stunServerAddress=$COTURN_PORT_3478_TCP_ADDR" > /etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini
    echo "stunServerPort=$COTURN_PORT_3478_TCP_PORT" >> /etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini
  fi
fi

exec /usr/bin/kurento-media-server "$0"
