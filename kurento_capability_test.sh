#!/bin/bash -x

echo "##################### EXECUTE: capability-test #####################"
# Parameter management

if [ $# -lt 1 ]; then
  echo "Usage: $0 <PROJECT_PATH>"
  exit 1
fi

# TEST_FILTER
[ -n "$1" ] && PROJECT_PATH="$1" || exit 1

echo "Checking KMS_PORT_8888_TCP_ADDR env variable: $KMS_PORT_8888_TCP_ADDR"
[ -n "$KMS_PORT_8888_TCP_ADDR" ] || KMS_PORT_8888_TCP_ADDR="127.0.0.1"
echo "Checking KMS_PORT_8888_TCP_PORT env variable: $KMS_PORT_8888_TCP_PORT"
[ -n "$KMS_PORT_8888_TCP_PORT" ] || exit 1

# Check if we want to record session
[ -n "$USE_FFMPEG" ] || USE_FFMPEG="no"
echo "Checking USE_FFMPEG env variable: $USE_FFMPEG"

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
mavenOpts="$mavenOpts -DfailIfNoTests=false"
#mavenOpts="$mavenOpts -Dkms.ws.uri=ws://$KMS_PORT_8888_TCP_ADDR:$KMS_PORT_8888_TCP_PORT/kurento"
mavenOpts="$mavenOpts -Dkurento.workspace=$WORKSPACE"
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
