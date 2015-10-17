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
# This tool uses a set of variables expected to be exported by tester
# PROJECT_PATH string
#    Identifies the module to execute within a reactor project.
#
# WORKSPACE path
#    Jenkins workspace path. This variable is expected to be exported by
#    script caller.
#
# MAVEN_SETTINGS path
#     Location of the settings.xml file used by maven
#
# MAVEN_OPTS string
#     All settings defined in this varible will be added to mvn command line
#
# RECORD_TEST [ true | false ]
#    Activates session recording in case ffmpeg is available
#    DEFAULT: false
#

# Get CLI parameter for backward compatibility
[ -n "$1" ] && PROJECT_PATH=$1

# Set default environment if required
export DISPLAY=:1
mavenOpts=""
[ -z "$WORKSPACE" ] && WORKSPACE="."
if [ -n "$PROJECT_PATH" ]; then
  mavenOpts="$mavenOpts -am -pl $PROJECT_PATH"
  mavenOpts="$mavenOpts -Dproject.path=$WORKSPACE/$PROJECT_PATH"
fi
mavenOpts="$mavenOpts -Dkurento.workspace=$WORKSPACE"
mavenOpts="$mavenOpts -DfailIfNoTests=false"
mavenOpts="$mavenOpts -U"

if [ "$RECORD_TEST" = "true" ]; then
  echo "***************** Recording session to  $WORKSPACE/session-recording.mp4"
  ffmpeg -video_size 800x600 -framerate 2 -f x11grab -i :1.0+0,0 $WORKSPACE/session-recording.mp4 &
  echo $! > ffmpeg.pid
fi

# Compile kurento-java if directory is present
[ -d $WORKSPACE/kurento-java ] &&  \
  (cd kurento-java &&  mvn --settings $MAVEN_SETTINGS clean install -Pdeploy -U -Dmaven.test.skip=true)

# Execute capability test
mvn --settings $MAVEN_SETTINGS clean verify $mavenOpts $MAVEN_OPTS
