#!/bin/sh
#
### BEGIN INIT INFO
# Provides:          Kurento JSON-RPC Test Server
# Required-Start:    $remote_fs $network
# Required-Stop:     $remote_fs
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Kurento JSON-RPC Test Server (KJRS) daemon.
# Description:       KJRS is a test server using JSON-RPC over WebSockets
# processname:       kjrserver
### END INIT INFO

if [ -r "/lib/lsb/init-functions" ]; then
  . /lib/lsb/init-functions
else
  echo "E: /lib/lsb/init-functions not found, package lsb-base needed"
  exit 1
fi

SERVICE_NAME=kjrserver

# Find out local or system installation
KJRS_DIR=$(cd $(dirname $(dirname $0)); pwd)
if [ -f $KJRS_DIR/bin/start.sh -a -f $KJRS_DIR/lib/kjrserver.jar ]; then
    KJRS_SCRIPT=$KJRS_DIR/bin/start.sh
    CONSOLE_LOG=$KJRS_DIR/logs/kjrserver.log
    KJRS_CONFIG=$KJRS_DIR/config/kjrserver.conf.json
    PIDFILE=$KJRS_DIR/kjrserver.pid
else
    # Only root can start Kurento in system mode
    if [ `id -u` -ne 0 ]; then
        log_failure_msg "Only root can start Kurento JSON-RPC Test Server"
        exit 1
    fi
    [ -f /etc/default/kjrserver ] && . /etc/default/kjrserver
    KJRS_SCRIPT=/usr/bin/kjrserver
    CONSOLE_LOG=/var/log/kurento/kjrserver.log
    KJRS_CONFIG=/etc/kurento/kjrserver.conf.json
    KJRS_CHUID="--chuid $DAEMON_USER"
    PIDFILE=/var/run/kurento/kjrserver.pid
fi

[ -z "$DAEMON_USER" ] && DAEMON_USER=nobody

# Check startup file
if [ ! -x $KJRS_SCRIPT ]; then
    log_failure_msg "$KJRS_SCRIPT is not an executable!"
    exit 1
fi

# Check config file
if [ ! -f $KJRS_CONFIG ]; then
    log_failure_msg "Kurento JSON-RPC Test Server configuration file not found: $KJRS_CONFIG"
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
        # KJRS instances not identified => Kill them all
        CURRENT_KJRS=$(ps -ef|grep kjrserver.jar |grep -v grep | awk '{print $2}')
        [ -n "$CURRENT_KJRS" ] && kill -9 $CURRENT_KJRS > /dev/null 2>&1
	mkdir -p $(dirname $PIDFILE)
	mkdir -p $(dirname $CONSOLE_LOG)
        # Start daemon
	start-stop-daemon --start $KJRS_CHUID \
        --make-pidfile --pidfile $PIDFILE \
	--background --no-close \
	--exec "$KJRS_SCRIPT" -- > /dev/null 2>&1
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
