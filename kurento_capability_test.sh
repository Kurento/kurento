#!/bin/bash -x

echo "##################### EXECUTE: capability-test #####################"
# Parameter management

if [ $# -lt 2 ]; then
  echo "Usage: $0 <TEST_FILTER> <GROUP_FILTER> [<SERVER_PORT> <HTTP_PORT> <USE_FFMPEG>]"
  exit 1
fi

# TEST_FILTER
[ -n "$1" ] && TEST_FILTER="$1" || exit 1

[ -n "$2" ] && GROUP_FILTER="$2" || exit 1

# SERVER_PORT
[ -n "$2" ] && SERVER_PORT="$2"

# HTTP_PORT
[ -n "$3" ] && HTTP_PORT="$3"

# Use ffmpeg
[ -n "$4" ] && USE_FFMPEG="$4" || USE_FFMPEG="no"

echo "Checking KMS_WS_ADDR env variable"
[ -n "$KMS_WS_ADDR" ] || KMS_WS_ADDR="127.0.0.1"
echo "Checking KMS_WS_PORT env variable"
[ -n "$KMS_WS_PORT" ] || exit 1
echo "Checking KMS_HTTP_PORT env variable"
[ -n "$KMS_HTTP_PORT" ] || exit 1

# Abort if port is not available
if [ -n "$SERVER_PORT" ]; then
  echo "Checking port $SERVER_PORT"
  netstat -tauen | grep $SERVER_PORT && exit 1
fi
if [ -n "$HTTP_PORT" ]; then
  echo "Checking port $HTTP_PORT"
  netstat -tauen | grep $HTTP_PORT && exit 1
fi
if [ ! -n "$KMS_WS_ADDR" ]; then
  echo "Checking port $KMS_WS_PORT"
  netstat -tauen | grep $KMS_WS_PORT && exit 1
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

mavenOpts="-pl kurento-integration-tests/kurento-test"
mavenOpts="$mavenOpts -Dgroups=$GROUP_FILTER"
mavenOpts="$mavenOpts -Dtest=$TEST_FILTER*"
mavenOpts="$mavenOpts -DfailIfNoTests=false"
if [ "$KMS_WS_ADDR" == "127.0.0.1" ]; then
  mavenOpts="$mavenOpts -Dkms.autostart=test"
else
  mavenOpts="$mavenOpts -Dkms.autostart=false"
fi
mavenOpts="$mavenOpts -Dkms.ws.uri=ws://$KMS_WS_ADDR:$KMS_WS_PORT/kurento"
mavenOpts="$mavenOpts -Dkms.http.port=$KMS_HTTP_PORT"
if [ -n "$SERVER_PORT" ]; then
  mavenOpts="$mavenOpts -Dserver.port=$SERVER_PORT"
fi
if [ -n "$HTTP_PORT" ]; then
  mavenOpts="$mavenOpts -Dhttp.port=$HTTP_PORT"
fi
mavenOpts="$mavenOpts -Dkms.command=/usr/bin/kurento-media-server"

# This is no longer needed
#mavenOpts="$mavenOpts -Dkms.gst.plugins=/usr/share/gst-kurento-plugins"
mavenOpts="$mavenOpts -Dkurento.test.files=/var/lib/jenkins/test-files"
mavenOpts="$mavenOpts -Dkurento.workspace=${WORKSPACE}"
mavenOpts="$mavenOpts -Dproject.path=$WORKSPACE/kurento-integration-tests/kurento-test"

mvn --settings $MAVEN_SETTINGS clean verify -U -am $mavenOpts

if [ "$USE_FFMPEG" == "yes" ] ; then
  echo "********************* Stop ffmpeg"
  FFMPEG_PID=$(cat ffmpeg.pid)
  kill $FFMPEG_PID
  wait $FFMPEG_PID
  echo "Looking for ffmpeg process"
  ps -Af | grep ffmpeg
fi
