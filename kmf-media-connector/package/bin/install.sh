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
cat > /etc/default/kmf-media-connector <<-EOF
# Defaults for kmf-media-connector initscript
# sourced by /etc/init.d/kmf-media-connector
# installed at /etc/default/kmf-media-connector by the maintainer scripts

#
# This is a POSIX shell fragment
#

# Commment next line to disable kmf-media-connector daemon
START_DAEMON=true

# Whom the daemons should run as
DAEMON_USER=nobody
EOF

# Install binaries
install -o root -g root -m 755 $KMC_HOME/bin/start.sh /usr/bin/kmf-media-connector
install -o root -g root -m 755 $KMC_HOME/support-files/kmf-media-connector.sh /etc/init.d/kmf-media-connector
mkdir -p /var/lib/kurento
install -o root -g root $KMC_HOME/lib/kmf-media-connector.jar /var/lib/kurento/
mkdir -p /etc/kurento/
install -o root -g root $KMC_HOME/config/application.properties /etc/kurento/media-connector.properties

# start media connector
/etc/init.d/kmf-media-connector start