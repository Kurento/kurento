#!/bin/sh

# Kurento Control Server installator for Ubuntu 14.04
if [ `id -u` -ne 0 ]; then
    echo ""
    echo "Only root can start Kurento"
    echo ""
    exit 1
fi

KTS_HOME=$(dirname $(dirname $(readlink -f $0)))

# Create defaults
mkdir -p /etc/default
cat > /etc/default/kurento-tree-server <<-EOF
# Defaults for kurento-tree-server initscript
# sourced by /etc/init.d/kurento-tree-server
# installed at /etc/default/kurento-tree-server by the maintainer scripts

#
# This is a POSIX shell fragment
#

# Commment next line to disable kurento-tree-server daemon
START_DAEMON=true

# Whom the daemons should run as
DAEMON_USER=nobody
EOF

# Install binaries
install -o root -g root -m 755 $KTS_HOME/bin/start.sh /usr/bin/kurento-tree-server
install -o root -g root -m 755 $KTS_HOME/support-files/kurento-tree-server.sh /etc/init.d/kurento-tree-server
mkdir -p /var/lib/kurento
install -o root -g root $KTS_HOME/lib/kurento-tree-server.jar /var/lib/kurento/
mkdir -p /etc/kurento/
install -o root -g root $KTS_HOME/config/kurento-tree.conf.json /etc/kurento/kurento-tree.conf.json

# enable media connector
update-rc.d kurento-tree-server defaults

# start media connector
/etc/init.d/kurento-tree-server restart
