#!/bin/sh

# Kurento Media Connector installartor for Ubuntu 14.04
if [ `id -u` -ne 0 ]; then
    echo ""
    echo "Only root can start Kurento"
    echo ""
    exit 1
fi

KMC_HOME=$(cd $(dirname $(dirname $0));pwd)

# Create defaults
mkdir -p /etc/default
cat > /etc/default/kurento-media-connector <<-EOF
# Defaults for kurento-media-connector initscript
# sourced by /etc/init.d/kurento-media-connector
# installed at /etc/default/kurento-media-connector by the maintainer scripts

#
# This is a POSIX shell fragment
#

# Commment next line to disable kurento-media-connector daemon
START_DAEMON=true

# Whom the daemons should run as
DAEMON_USER=nobody
EOF

# Install binaries
install -o root -g root -m 755 $KMC_HOME/bin/start.sh /usr/bin/kurento-media-connector
install -o root -g root -m 755 $KMC_HOME/support-files/kmf-media-connector.sh /etc/init.d/kurento-media-connector
mkdir -p /var/lib/kurento
install -o root -g root $KMC_HOME/lib/kmf-media-connector.jar /var/lib/kurento/
mkdir -p /etc/kurento/
install -o root -g root $KMC_HOME/config/application.properties /etc/kurento/media-connector.conf

# start media connector
/etc/init.d/kurento-media-connector start