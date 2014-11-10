#!/bin/sh

# ${project.name} installator for Ubuntu 14.04
if [ `id -u` -ne 0 ]; then
    echo ""
    echo "Only root can start Kurento"
    echo ""
    exit 1
fi

APP_HOME=$(dirname $(readlink -f $0))

# Install binaries
install -o root -g root -m 755 $APP_HOME/demo-startup.sh /etc/init.d/${project.artifactId}
mkdir -p /var/lib/kurento
install -o root -g root $APP_HOME/${project.artifactId}.jar /var/lib/kurento/

# enable demo at startup
# update-rc.d ${project.artifactId} defaults

# start demo
/etc/init.d/${project.artifactId} start
