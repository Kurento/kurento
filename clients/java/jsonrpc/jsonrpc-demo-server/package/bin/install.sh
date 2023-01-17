#!/bin/sh

# Kurento JSON-RPC test server installator for Ubuntu 14.04
if [ `id -u` -ne 0 ]; then
    echo ""
    echo "Only root can start Kurento JSON-RPC test server"
    echo ""
    exit 1
fi

KJR_HOME=$(dirname $(dirname $(readlink -f $0)))

# Create directories
mkdir -p /etc/kurento/
mkdir -p /var/lib/kurento
mkdir -p /var/log/kurento && chown nobody /var/log/kurento

# Install binary & config
install -o root -g root -m 755 $KJR_HOME/bin/start.sh /usr/bin/kjrserver
install -o root -g root $KJR_HOME/lib/kjrserver.jar /var/lib/kurento/kjrserver.jar
install -o root -g root $KJR_HOME/config/kjrserver.conf.json /etc/kurento/kjrserver.conf.json
install -o root -g root $KJR_HOME/config/kjrserver-log4j.properties /etc/kurento/kjrserver-log4j.properties

DIST=$(lsb_release -i | awk '{print $3}')
[ -z "$DIST" ] && { echo "Unable to get distribution information"; exit 1; } 
case "$DIST" in
    Ubuntu)
        mkdir -p /etc/default
        echo "# Defaults for KJRS initscript" > /etc/default/kjrserver
        echo "# sourced by /etc/init.d/kjrserver" >> /etc/default/kjrserver
        echo "# installed at /etc/default/kjrserver by the maintainer scripts" >> /etc/default/kjrserver
        echo "" >> /etc/default/kjrserver
        echo "#" >> /etc/default/kjrserver
        echo "# This is a POSIX shell fragment" >> /etc/default/kjrserver
        echo "#" >> /etc/default/kjrserver  
        echo "" >> /etc/default/kjrserver
        echo "# Commment next line to disable KJRS daemon" >> /etc/default/kjrserver
        echo "START_DAEMON=true" >> /etc/default/kjrserver
        echo "" >> /etc/default/kjrserver
        echo "# Whom the daemons should run as" >> /etc/default/kjrserver
        echo "DAEMON_USER=nobody" >> /etc/default/kjrserver
        
        install -o root -g root -m 755 $KJR_HOME/support-files/kjrserver.sh /etc/init.d/kjrserver
        update-rc.d kjrserver defaults
        /etc/init.d/kjrserver restart
        ;;
    CentOS)
        install -o root -g root -m  644 $KJR_HOME/support-files/kjrserver.service /usr/lib/systemd/system/kjrserver.service
        systemctl daemon-reload
        systemctl enable kjrserver.service
        systemctl restart kjrserver
        ;;
esac
