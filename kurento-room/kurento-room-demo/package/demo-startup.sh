#!/bin/bash
# description: my-spring-boot-api start stop restart
# processname: my-spring-boot-api
# chkconfig: 234 20 80

echo "Service [$APP_NAME] - [$1]"

APP_HOME=/var/lib/kurento
APP_NAME=${project.artifactId}
APP_VERSION=${project.version}
APP=$APP_NAME-$APP_VERSION
APP_PORT=${demo.port}
CONSOLE_LOG=/var/log/kurento-media-server/$APP_NAME.log

# Setup the JVM
if [ "x$JAVA" = "x" ]; then
    if [ "x$JAVA_HOME" != "x" ]; then
        JAVA="$JAVA_HOME/bin/java"
    else
        JAVA="java"
    fi
fi

# Check log directory
[ -d $(dirname $CONSOLE_LOG) ] || mkdir -p $(dirname $CONSOLE_LOG)

echo "========================================================================="
echo ""
echo "  $APP Bootstrap Environment"
echo ""
echo "  APP_HOME: $APP_HOME"
echo ""
echo "  JAVA: $JAVA $JAVA_OPTS"
echo ""
echo "========================================================================="
echo ""

function start {
    if pkill -0 -f $APP_NAME.jar > /dev/null 2>&1
    then
        echo "Service [$APP_NAME] is already running. Ignoring startup request."
        exit 1
    fi
    echo "Starting application $APP_NAME in port $APP_PORT..."
    nohup $JAVA -Dserver.port=$APP_PORT -jar $APP_HOME/$APP_NAME.jar \
        < /dev/null > $CONSOLE_LOG 2>&1 &
}

function stop {
    if ! pkill -0 -f $APP_NAME.jar > /dev/null 2>&1
    then
        echo "Service [$APP_NAME] is not running. Ignoring shutdown request."
        exit 1
    fi

    # First, we will try to trigger a controlled shutdown using 
    # spring-boot-actuator
    curl -X POST http://127.0.0.1:$APP_PORT/shutdown < /dev/null > /dev/null 2>&1

    # Wait until the server process has shut down
    attempts=0
    while pkill -0 -f $APP_NAME.jar > /dev/null 2>&1
    do
        attempts=$[$attempts + 1]
        if [ $attempts -gt 5 ]
        then
            # We have waited too long. Kill it.
            pkill -f $APP_NAME.jar > /dev/null 2>&1
        fi
        sleep 1s
    done
}

case $1 in
start)
    start
;;
stop)
    stop
;;
restart)
    stop
    start
;;
esac
exit 0
