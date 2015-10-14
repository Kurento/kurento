#!/bin/bash -x

echo "##################### EXECUTE: capability-test #####################"
# Parameter management

if [ $# -lt 1 ]; then
  echo "Usage: $0 <PROJECT_PATH>"
  exit 1
fi

# TEST_FILTER
[ -n "$1" ] && PROJECT_PATH="$1" || exit 1

# Check if we want to record session
[ -n "$USE_FFMPEG" ] || USE_FFMPEG="no"
echo "Checking USE_FFMPEG env variable: $USE_FFMPEG"

export DISPLAY=:1

if [ "$USE_FFMPEG" == "yes" ] ; then
  echo "***************** Start ffmpeg recording session on display 1"
  ffmpeg -video_size 1280x1024 -framerate 25 -f x11grab -i :1.0+0,0 $(pwd)/session-recording.mp4 &
  echo $! > ffmpeg.pid
fi

mavenOpts="-U -am -pl $PROJECT_PATH"
mavenOpts="$mavenOpts -DfailIfNoTests=false"
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
