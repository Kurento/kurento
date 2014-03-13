#!/bin/sh

### BEGIN INIT INFO
# Provides:          kmf-media-connector
# Required-Start:    $remote_fs $network
# Required-Stop:     $remote_fs
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Kurento Media Connector daemon.
# Description: The Kurento kmf-media-connector project is a Webserver frontend for the Thrift API of the Kurento Media Server.
# processname: 
### END INIT INFO

# Variables
SERVICE_NAME=Kurento Media Connector

if [ -z "$KMF_MEDIA_CONNECTOR_HOME" ]; then
  KMF_MEDIA_CONNECTOR_HOME=/opt/kmf-media-connector/
fi

if [ -z "$SHUTDOWN_WAIT" ]; then
  SHUTDOWN_WAIT=30
fi

export KMF_MEDIA_CONNECTOR_HOME

PIDFILE=/var/run/kmf-media-connector.pid


KMF_MEDIA_CONNECTOR_SCRIPT=$KMF_MEDIA_CONNECTOR_HOME/bin/standalone.sh

start() {
	echo -n "Starting $SERVICE_NAME ..."
	if [ ! -f $PIDFILE ]; then
		LAUNCH_JBOSS_IN_BACKGROUND=1 $KMF_MEDIA_CONNECTOR_SCRIPT
		echo -n "$SERVICE_NAME started ..."
	else
		echo -n "$SERVICE_NAME is already running ..."
	fi
}

stop () {
	if [ -f $PIDFILE ]; then
		read kpid < $PIDFILE
		let kwait=$SHUTDOWN_WAIT

		echo -n "$SERVICE_NAME stopping ..."
		kill -15 $kpid
		until [ `ps --pid $kpid 2> /dev/null | grep -c $kpid 2> /dev/null` -eq '0' ] || [ $count -gt $kwait ]
		do
			sleep 1
			let count=$count+1;
		done
    		
		if [ $count -gt $kwait ]; then
			kill -9 $kpid
		fi
    		
		rm -f $PIDFILE
		echo -n "$SERVICE_NAME stopped ..."
	else
		echo -n "$SERVICE_NAME is not running ..."
	fi
}


status() {
	if [ -f $PIDFILE ]; then
		read ppid < $PIDFILE
		if [ `ps --pid $ppid 2> /dev/null | grep -c $ppid 2> /dev/null` -eq '1' ]; then
			echo -n "$prog is running (pid $ppid)"
			return 0
		else
			echo -n "$prog dead but pid file exists"
			return 1
		fi
	fi
	echo -n "$prog is not running"
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
		echo -n "Usage: $0 {start|stop|status|restart|reload}"
		exit 1
		;;
esac
