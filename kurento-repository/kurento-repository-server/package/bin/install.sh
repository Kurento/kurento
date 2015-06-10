#!/bin/sh

# Kurento Repository installator for Ubuntu 14.04
if [ `id -u` -ne 0 ]; then
    echo ""
    echo "Only root can start Kurento Repository"
    echo ""
    exit 1
fi

KREPO_HOME=$(dirname $(dirname $(readlink -f $0)))

# Create directories
mkdir -p /etc/kurento/
mkdir -p /var/lib/kurento
mkdir -p /var/log/kurento && chown nobody /var/log/kurento

# Install binary & config
install -o root -g root -m 755 $KREPO_HOME/bin/start.sh /usr/bin/kurento-repo
install -o root -g root $KREPO_HOME/lib/kurento-repo.jar /var/lib/kurento/kurento-repo.jar
install -o root -g root $KREPO_HOME/config/kurento-repo.conf.json /etc/kurento/kurento-repo.conf.json
install -o root -g root $KREPO_HOME/config/kurento-repo-log4j.properties /etc/kurento/kurento-repo-log4j.properties

DIST=$(lsb_release -i | awk '{print $3}')
[ -z "$DIST" ] && { echo "Unable to get distribution information"; exit 1; } 
case "$DIST" in
    Ubuntu)
        mkdir -p /etc/default
        echo "# Defaults for Kurento Repository initscript" > /etc/default/kurento-repo
        echo "# sourced by /etc/init.d/kurento-repo" >> /etc/default/kurento-repo
        echo "# installed at /etc/default/kurento-repo by the maintainer scripts" >> /etc/default/kurento-repo
        echo "" >> /etc/default/kurento-repo
        echo "#" >> /etc/default/kurento-repo
        echo "# This is a POSIX shell fragment" >> /etc/default/kurento-repo
        echo "#" >> /etc/default/kurento-repo  
        echo "" >> /etc/default/kurento-repo
        echo "# Comment next line to disable Kurento Repository daemon" >> /etc/default/kurento-repo
        echo "START_DAEMON=true" >> /etc/default/kurento-repo
        echo "" >> /etc/default/kurento-repo
        echo "# Whom the daemons should run as" >> /etc/default/kurento-repo
        echo "DAEMON_USER=nobody" >> /etc/default/kurento-repo
        
        install -o root -g root -m 755 $KREPO_HOME/support-files/kurento-repo.sh /etc/init.d/kurento-repo
        update-rc.d kurento-repo defaults
        /etc/init.d/kurento-repo restart
        ;;
    CentOS)
        install -o root -g root -m  644 $KREPO_HOME/support-files/kurento-repo.service /usr/lib/systemd/system/kurento-repo.service
        systemctl daemon-reload
        systemctl enable kurento-repo.service
        systemctl restart kurento-repo
        ;;
esac
