#!/bin/bash
set -e

if [ $NAT = "true" -a -z "$EXTERNAL_IP" ]; then

  # Try to get public IP
  PUBLIC_IP=$(curl http://169.254.169.254/latest/meta-data/public-ipv4) || echo "No public ip found on http://169.254.169.254/latest/meta-data/public-ipv4"
  if [ -z "$PUBLIC_IP" ]; then
    PUBLIC_IP=$(curl http://icanhazip.com) || exit 1
  fi

  # Try to get private IP
  PRIVATE_IP=$(ifconfig | awk '/inet addr/{print substr($2,6)}' | grep -v 127.0.0.1) || exit 1
  export EXTERNAL_IP="$PUBLIC_IP/$PRIVATE_IP"
  echo "Starting turn server with external IP: $EXTERNAL_IP"
fi

echo 'min-port=49152' > /etc/turnserver.conf
echo 'max-port=65535' >> /etc/turnserver.conf
echo 'fingerprint' >> /etc/turnserver.conf
echo 'lt-cred-mech' >> /etc/turnserver.conf
echo "realm=$REALM" >> /etc/turnserver.conf
echo 'log-file stdout' >> /etc/turnserver.conf
echo "user=$TURN_USERNAME:$TURN_PASSWORD" >> /etc/turnserver.conf
[ $NAT = "true" ] && echo "external-ip=$EXTERNAL_IP" >> /etc/turnserver.conf

exec /usr/bin/turnserver "$@"
