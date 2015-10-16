#!/bin/bash -x

cleanup() {
  echo "Clean capability test before exit"
  if [ "$RECORD_TEST" == "true" ] ; then
    echo "********************* Stop recording"
    FFMPEG_PID=$(cat ffmpeg.pid 2>/dev/null)
    [ -n "$FFMPEG_PID" ] && kill $FFMPEG_PID && wait $FFMPEG_PID
  fi
}

trap cleanup EXIT
echo "##################### EXECUTE: capability-test #####################"
# PROJECT_PATH string
#    Identifies the module to execute within a reactor project
#
# RECORD_TEST [ true | false ]
#    Activates session recording in case ffmpeg is available
#    DEFAULT: false
#
# Parameter management

if [ $# -lt 1 ]; then
  echo "Usage: $0 <PROJECT_PATH>"
  exit 1
fi

# TEST_FILTER
[ -n "$1" ] && PROJECT_PATH="$1" || exit 1

# Check if we want to record session
[ -n "$RECORD_TEST" ] || RECORD_TEST="false"

export DISPLAY=:1
if [ "$USE_FFMPEG" == "true" ] ; then
  echo "***************** Recording session to  $WORKSPACE/session-recording.mp4"
  ffmpeg -video_size 800x600 -framerate 2 -f x11grab -i :1.0+0,0 $WORKSPACE/session-recording.mp4 &
  echo $! > ffmpeg.pid
fi

# Compile kurento-java if directory is present
[ -d $WORKSPACE/kurento-java ] &&  \
  (cd kurento-java &&  mvn --settings $MAVEN_SETTINGS clean install -Pdeploy -U -Dmaven.test.skip=true)

mavenOpts="-U -am -pl $PROJECT_PATH"
mavenOpts="$mavenOpts -DfailIfNoTests=false"
mavenOpts="$mavenOpts -Dkurento.workspace=$WORKSPACE"
mavenOpts="$mavenOpts -Dproject.path=$WORKSPACE/$PROJECT_PATH"

mvn --settings $MAVEN_SETTINGS clean verify $mavenOpts $MAVEN_OPTS
