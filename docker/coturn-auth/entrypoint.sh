#!/bin/bash -x
set -eu -o pipefail

EXTERNAL_IP=$(curl icanhazip.com)
LISTENING_DEVICE=$(route | grep default | awk '{ print $8 }')

cat >/etc/turnserver.conf<<EOF
external-ip=$EXTERNAL_IP
listening-device=$LISTENING_DEVICE
listening-port=$LISTENING_PORT
lt-cred-mech
max-port=65535
min-port=49152
pidfile="/var/run/turnserver.pid"
realm=$REALM
secure-stun
simple-log
userdb=/var/local/turndb
verbose
EOF

# Adding first user
turnadmin -a -b /var/local/turndb -u $USER -r $REALM -p $PASSWORD

# Starting coturn
/usr/bin/turnserver --no-cli >>/var/log/turnserver.log 2>&1