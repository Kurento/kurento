#!/bin/bash -x

echo "##################### EXECUTE: capability-test #####################"
# Parameter management

if [ $# -lt 2 ]; then
  echo "Usage: $0 <TEST_FILTER> <GROUP_FILTER> <PROJECT_PATH> [<KMS_AUTOSTART> <SERVER_PORT> <HTTP_PORT>]"
  exit 1
fi

# TEST_FILTER
[ -n "$1" ] && TEST_FILTER="$1" || exit 1

[ -n "$2" ] && GROUP_FILTER="$2" || exit 1

[ -n "$3" ] && PROJECT_PATH="$3" || exit 1

[ -n "$4" ] && KMS_AUTOSTART="$4" || KMS_AUTOSTART="test"

# SERVER_PORT
[ -n "$5" ] && SERVER_PORT="$5"

# HTTP_PORT
[ -n "$6" ] && HTTP_PORT="$6"

echo "Checking KMS_PORT_8888_TCP_ADDR env variable: $KMS_PORT_8888_TCP_ADDR"
[ -n "$KMS_PORT_8888_TCP_ADDR" ] || KMS_PORT_8888_TCP_ADDR="127.0.0.1"
echo "Checking KMS_PORT_8888_TCP_PORT env variable: $KMS_PORT_8888_TCP_PORT"
[ -n "$KMS_PORT_8888_TCP_PORT" ] || exit 1
echo "Checking KMS_HTTP_PORT env variable: $KMS_HTTP_PORT"
[ -n "$KMS_HTTP_PORT" ] || exit 1

# Check if we want to record session
[ -n "$USE_FFMPEG" ] || USE_FFMPEG="no"
echo "Checking USE_FFMPEG env variable: $USE_FFMPEG"

# Abort if port is not available
if [ -n "$SERVER_PORT" ]; then
  echo "Checking port $SERVER_PORT"
  netstat -tauen | grep $SERVER_PORT && exit 1
fi
if [ -n "$HTTP_PORT" ]; then
  echo "Checking port $HTTP_PORT"
  netstat -tauen | grep $HTTP_PORT && exit 1
fi
if [ ! -n "$KMS_PORT_8888_TCP_ADDR" ]; then
  echo "Checking port $KMS_PORT_8888_TCP_PORT"
  netstat -tauen | grep $KMS_PORT_8888_TCP_PORT && exit 1
fi
echo "Checking port $KMS_HTTP_PORT"
netstat -tauen | grep $KMS_HTTP_PORT && exit 1

#Restart xvfb to avoid problems with corrupted xvfb instances
sudo service xvfb stop

PID=$(pidof /usr/bin/Xvfb)
echo "Killing Xvfb processes: $PID"
sudo kill -9 $PID
wait $PID

sudo service xvfb start

export DISPLAY=:1

if [ "$USE_FFMPEG" == "yes" ] ; then
  echo "***************** Start ffmpeg recording session on display 1"
  ffmpeg -video_size 1280x1024 -framerate 25 -f x11grab -i :1.0+0,0 $(pwd)/session-recording.mp4 &
  echo $! > ffmpeg.pid
fi

mavenOpts="-U -am -pl $PROJECT_PATH"
mavenOpts="$mavenOpts -Dgroups=$GROUP_FILTER"
mavenOpts="$mavenOpts -Dtest=$TEST_FILTER"
mavenOpts="$mavenOpts -DfailIfNoTests=false"
mavenOpts="$mavenOpts -Dkms.autostart=$KMS_AUTOSTART"
mavenOpts="$mavenOpts -Dkms.ws.uri=ws://$KMS_PORT_8888_TCP_ADDR:$KMS_PORT_8888_TCP_PORT/kurento"
mavenOpts="$mavenOpts -Dkms.http.port=$KMS_HTTP_PORT"
if [ -n "$SERVER_PORT" ]; then
  mavenOpts="$mavenOpts -Dserver.port=$SERVER_PORT"
fi
if [ -n "$HTTP_PORT" ]; then
  mavenOpts="$mavenOpts -Dhttp.port=$HTTP_PORT"
fi
mavenOpts="$mavenOpts -Dkms.command=/usr/bin/kurento-media-server"

mavenOpts="$mavenOpts -Dkurento.test.files=/var/lib/jenkins/test-files"
mavenOpts="$mavenOpts -Dkurento.workspace=${WORKSPACE}"
mavenOpts="$mavenOpts -Dproject.path=$WORKSPACE/$PROJECT_PATH"

mvn --settings $MAVEN_SETTINGS clean verify $mavenOpts $MAVEN_OPTS

if [ "$USE_FFMPEG" == "yes" ] ; then
  echo "********************* Stop ffmpeg"
  FFMPEG_PID=$(cat ffmpeg.pid)
  kill $FFMPEG_PID
  wait $FFMPEG_PID
  echo "Looking for ffmpeg process"
  ps -Af | grep ffmpeg
fi
