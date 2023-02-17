#!/bin/sh

# ${project.artifactId} installer for Ubuntu 14.04
if [ `id -u` -ne 0 ]; then
    echo ""
    echo "Only root can start ${project.artifactId}"
    echo ""
    exit 1
fi

APP_HOME=$(dirname $(dirname $(readlink -f $0)))
CONFIG_FILE="$APP_HOME/config/configuration.conf.json"

# Create defaults
mkdir -p /etc/default
cat > /etc/default/${project.artifactId} <<-EOF
# Defaults for ${project.artifactId} initscript
# sourced by /etc/init.d/${project.artifactId}
# installed at /etc/default/${project.artifactId} by the maintainer scripts

#
# This is a POSIX shell fragment
#

# Commment next line to disable ${project.artifactId} daemon
START_DAEMON=true

# Whom the daemons should run as
DAEMON_USER=nobody
EOF

# Install binaries
install -o root -g root -m 755 $APP_HOME/bin/start.sh /usr/bin/${project.artifactId}
install -o root -g root -m 755 $APP_HOME/support-files/kurento-demo.sh /etc/init.d/${project.artifactId}
mkdir -p /var/lib/kurento
install -o root -g root $APP_HOME/lib/${project.artifactId}.jar /var/lib/kurento/
[ -f $CONFIG_FILE ] && ( mkdir -p /etc/kurento/ && install -o root -g root $CONFIG_FILE /etc/kurento/${project.artifactId}.conf.json )

# enable ${project.artifactId} start at boot time
# update-rc.d ${project.artifactId} defaults

# start ${project.artifactId}
/etc/init.d/${project.artifactId} restart
