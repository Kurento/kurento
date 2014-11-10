#!/bin/sh

### BEGIN INIT INFO
# Provides:          kurento-tree-server
# Required-Start:    $remote_fs $network
# Required-Stop:     $remote_fs
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Kurento Tree Server daemon.
# Description: The Kurento Tree Server is a component of Kurento Server that provides the server to control a tree of Kurento Media Server.
# processname:
### END INIT INFO

if [ -r "/lib/lsb/init-functions" ]; then
  . /lib/lsb/init-functions
else
  echo "E: /lib/lsb/init-functions not found, package lsb-base needed"
  exit 1
fi

SERVICE_NAME=KurentoTreeServer

# Find out local or system installation
KTS_DIR=$(cd $(dirname $(dirname $0)); pwd)
if [ -f $KTS_DIR/bin/start.sh -a -f $KTS_DIR/lib/kurento-tree-server.jar ]; then
    KTS_SCRIPT=$KTS_DIR/bin/start.sh
    CONSOLE_LOG=$KTS_DIR/logs/tree-server.log
    KTS_CONFIG=$KTS_DIR/config/kurento-tree.conf.json
    PIDFILE=$KTS_DIR/kurento-tree-server.pid
else
    # Only root can start Kurento in system mode
    if [ `id -u` -ne 0 ]; then
        log_failure_msg "Only root can start Kurento Tree Server"
        exit 1
    fi
    [ -f /etc/default/kurento-tree-server ] && . /etc/default/kurento-tree-server
    KTS_SCRIPT=/usr/bin/kurento-tree-server
    CONSOLE_LOG=/var/log/kurento/tree-server.log
    KTS_CONFIG=/etc/kurento/kurento-tree.conf.json
    KMC_CHUID="--chuid $DAEMON_USER"
    PIDFILE=/var/run/kurento/kurento-tree-server.pid
fi

[ -z "$DAEMON_USER" ] && DAEMON_USER=nobody

# Check startup file
if [ ! -x $KTS_SCRIPT ]; then
    log_failure_msg "$KTS_SCRIPT is not an executable!"
    exit 1
fi

# Check config file
if [ ! -f $KTS_CONFIG ]; then
    log_failure_msg "Kurento Tree Server configuration file not found: $KTS_CONFIG"
    exit 1;
fi

# Check log directory
[ -d $(dirname $CONSOLE_LOG) ] || mkdir -p $(dirname $CONSOLE_LOG)

start() {
	log_daemon_msg "$SERVICE_NAME starting"
        # clean PIDFILE before start
        if [ -f "$PIDFILE" ]; then
            if [ -n "$(ps h --pid $(cat $PIDFILE) | awk '{print $1}')" ]; then
                log_action_msg "$SERVICE_NAME is already running ..."
                return
            fi
            rm -f $PIDFILE
        fi
        # KMC instances not identified => Kill them all
        CURRENT_KMC=$(ps -ef|grep kurento-tree-server.jar |grep -v grep | awk '{print $2}')
        [ -n "$CURRENT_KMC" ] && kill -9 $CURRENT_KMC > /dev/null 2>&1
	mkdir -p $(dirname $PIDFILE)
	mkdir -p $(dirname $CONSOLE_LOG)
	cat /dev/null > $CONSOLE_LOG
        # Start daemon
	start-stop-daemon --start $KMC_CHUID \
        --make-pidfile --pidfile $PIDFILE \
	--background --no-close \
	--exec "$KTS_SCRIPT" -- >> $CONSOLE_LOG 2>&1
	log_end_msg $?
}

stop () {
	if [ -f $PIDFILE ]; then
	    read kpid < $PIDFILE
	    kwait=15

	    count=0
	    log_daemon_msg "$SERVICE_NAME stopping ..."
	    kill -15 $kpid
	    until [ `ps --pid $kpid 2> /dev/null | grep -c $kpid 2> /dev/null` -eq '0' ] || [ $count -gt $kwait ]
	    do
		sleep 1
		count=$((count+1))
	    done

	    if [ $count -gt $kwait ]; then
		kill -9 $kpid
	    fi

	    rm -f $PIDFILE
	    log_end_msg $?
	else
	    log_failure_msg "$SERVICE_NAME is not running ..."
	fi
}


status() {
	if [ -f $PIDFILE ]; then
	    read ppid < $PIDFILE
	    if [ `ps --pid $ppid 2> /dev/null | grep -c $ppid 2> /dev/null` -eq '1' ]; then
		log_daemon_msg "$prog is running (pid $ppid)"
		return 0
	    else
		log_daemon_msg "$prog dead but pid file exists"
		return 1
	    fi
	fi
	log_daemon_msg "$SERVICE_NAME is not running"
	return 3
}

case "$1" in
	start)
	    start
	    ;;
	stop)
	    stop
	    ;;
	restart)
	    $0 stop
	    $0 start
	    ;;
	status)
	    status
	    ;;
	*)
	    ## If no parameters are given, print which are avaiable.
	    log_daemon_msg "Usage: $0 {start|stop|status|restart|reload}"
	    ;;
esac
