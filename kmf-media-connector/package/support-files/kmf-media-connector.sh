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
SERVICE_NAME=KurentoMediaConnector

. /lib/lsb/init-functions



if [ -z "$KMF_MEDIA_CONNECTOR_HOME" ]; then
  KMF_MEDIA_CONNECTOR_HOME=/opt/kmf-media-connector
fi
export KMF_MEDIA_CONNECTOR_HOME

if [ -z "$SHUTDOWN_WAIT" ]; then
  SHUTDOWN_WAIT=30
fi

PIDFILE=/var/run/kurento/kmf-media-connector.pid
export PIDFILE

KMF_MEDIA_CONNECTOR_SCRIPT=$KMF_MEDIA_CONNECTOR_HOME/bin/start.sh

# Check startup file
if [ ! -x $KMF_MEDIA_CONNECTOR_SCRIPT ]; then
	log_failure_msg "$KMF_MEDIA_CONNECTOR_SCRIPT is not an executable!"
	exit 1
fi

# Location to keep the console log
if [ -z "$CONSOLE_LOG" ]; then
	CONSOLE_LOG=/var/log/kurento/media-connector/console.log
fi
export CONSOLE_LOG

#Launch in background 
LAUNCH_KMF_IN_BACKGROUND=1
export LAUNCH_KMF_IN_BACKGROUND

start() {
	log_daemon_msg "$SERVICE_NAME starting"
	if [ ! -f $PIDFILE ]; then
		mkdir -p $(dirname $PIDFILE)
		mkdir -p $(dirname $CONSOLE_LOG)
		#chown $KURENTO_USER $(dirname $PIDFILE) || true
		cat /dev/null > $CONSOLE_LOG

		start-stop-daemon --start \
 		--chdir "$KMF_MEDIA_CONNECTOR_HOME" --pidfile "$PIDFILE" \
		--exec "$KMF_MEDIA_CONNECTOR_SCRIPT" -- >> $CONSOLE_LOG 2>&1 &
		log_daemon_msg "$SERVICE_NAME started ..."
	else
		log_action_msg "$SERVICE_NAME is already running ..."
	fi
}

stop () {
	if [ -f $PIDFILE ]; then
		read kpid < $PIDFILE
		kwait=$SHUTDOWN_WAIT
		
		count=0
		log_daemon_msg "$SERVICE_NAME stopping ..."
		kill -15 $kpid
		until [ `ps --pid $kpid 2> /dev/null | grep -c $kpid 2> /dev/null` -eq '0' ] || [ $count -gt $kwait ]
		do
			sleep 1
			coount=$((count+1))
		done
    		
		if [ $count -gt $kwait ]; then
			kill -9 $kpid
		fi
    		
		rm -f $PIDFILE
		log_daemon_msg "$SERVICE_NAME stopped ..."
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
		exit 1
		;;
esac
