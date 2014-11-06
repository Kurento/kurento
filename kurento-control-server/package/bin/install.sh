#!/bin/sh

# Kurento Control Server installator for Ubuntu 14.04
if [ `id -u` -ne 0 ]; then
    echo ""
    echo "Only root can start Kurento"
    echo ""
    exit 1
fi

KCS_HOME=$(dirname $(dirname $(readlink -f $0)))

# Create defaults
mkdir -p /etc/default
cat > /etc/default/kurento-control-server <<-EOF
# Defaults for kurento-control-server initscript
# sourced by /etc/init.d/kurento-control-server
# installed at /etc/default/kurento-control-server by the maintainer scripts

#
# This is a POSIX shell fragment
#

# Commment next line to disable kurento-control-server daemon
START_DAEMON=true

# Whom the daemons should run as
DAEMON_USER=nobody
EOF

# Install binaries
install -o root -g root -m 755 $KCS_HOME/bin/start.sh /usr/bin/kurento-control-server
install -o root -g root -m 755 $KCS_HOME/support-files/kurento-control-server.sh /etc/init.d/kurento-control-server
mkdir -p /var/lib/kurento
install -o root -g root $KCS_HOME/lib/kurento-control-server.jar /var/lib/kurento/
mkdir -p /etc/kurento/
install -o root -g root $KCS_HOME/config/kurento.conf.json /etc/kurento/control-server.conf.json

# enable media connector
update-rc.d kurento-control-server defaults

# start media connector
/etc/init.d/kurento-control-server start
