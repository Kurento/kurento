#!/bin/sh

### BEGIN INIT INFO
# Provides:          ${project.artifactId}
# Required-Start:    $remote_fs $network
# Required-Stop:     $remote_fs
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: ${project.artifactId} daemon.
# Description: Kurento Demo project
# processname:
### END INIT INFO

if [ -r "/lib/lsb/init-functions" ]; then
  . /lib/lsb/init-functions
else
  echo "E: /lib/lsb/init-functions not found, package lsb-base needed"
  exit 1
fi

SERVICE_NAME=${project.artifactId}

# Find out local or system installation
DEMO_DIR=$(cd $(dirname $(dirname $0)); pwd)
if [ -f $DEMO_DIR/bin/start.sh -a -f $DEMO_DIR/lib/${project.artifactId}.jar ]; then
    DEMO_SCRIPT=$DEMO_DIR/bin/start.sh
    CONSOLE_LOG=$DEMO_DIR/logs/${project.artifactId}.log
    DEMO_CONFIG=$DEMO_DIR/config/${project.artifactId}.conf.json
    PIDFILE=$DEMO_DIR/${project.artifactId}.pid
else
    # Only root can start ${project.artifactId} in system mode
    if [ `id -u` -ne 0 ]; then
        log_failure_msg "Only root can start ${project.artifactId}"
        exit 1
    fi
    [ -f /etc/default/${project.artifactId} ] && . /etc/default/${project.artifactId}
    DEMO_SCRIPT=/usr/bin/${project.artifactId}
    CONSOLE_LOG=/var/log/kurento-media-server/${project.artifactId}.log
    DEMO_CONFIG=/etc/kurento/${project.artifactId}.conf.json
    DEMO_CHUID="--chuid $DAEMON_USER"
    PIDFILE=/var/run/kurento/${project.artifactId}.pid
fi

[ -z "$DAEMON_USER" ] && DAEMON_USER=nobody

# Check startup file
if [ ! -x $DEMO_SCRIPT ]; then
    log_failure_msg "$DEMO_SCRIPT is not an executable!"
    exit 1
fi

# Check config file
# if [ ! -f $DEMO_CONFIG ]; then
    #    log_failure_msg "${project.artifactId} configuration file not found: $DEMO_CONFIG"
    # exit 1;
#fi

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
        # DEMO instances not identified => Kill them all
        CURRENT_DEMO=$(ps -ef|grep ${project.artifactId}.jar |grep -v grep | awk '{print $2}')
        [ -n "$CURRENT_DEMO" ] && kill -9 $CURRENT_DEMO > /dev/null 2>&1
	mkdir -p $(dirname $PIDFILE)
	mkdir -p $(dirname $CONSOLE_LOG)
	cat /dev/null > $CONSOLE_LOG
        # Start daemon
	start-stop-daemon --start $DEMO_CHUID \
        --make-pidfile --pidfile $PIDFILE \
	--background --no-close \
	--exec "$DEMO_SCRIPT" -- >> $CONSOLE_LOG 2>&1
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
