#!/bin/bash

pushd . > /dev/null
SCRIPT_PATH=$(dirname `which $0`)
cd $SCRIPT_PATH
SCRIPT_PATH="`pwd`"
popd /dev/null

LOGS_DIR=$SCRIPT_PATH/memory_leaks

export GST_TRACE=live

function process_logs {
  DIR=$1
  make check
  mkdir -p $DIR
  pushd $LOGS_DIR
  for f in *.log; do
    cat $f | grep "GstClockEntry\|GstMiniObject\|GstObject" > $DIR/$f
  done
  popd
}

export ITERATIONS=1
DIR_A=$LOGS_DIR/$ITERATIONS"_it"
process_logs $DIR_A

export ITERATIONS=2
DIR_B=$LOGS_DIR/$ITERATIONS"_it"
process_logs $DIR_B

MEMORY_LEAKS="NO"
pushd $DIR_A
for f in *.log; do
  DIFF=$(diff $f $DIR_B/$f)
  if [ $? -ne 0 ]; then
    echo "Memory leaks in $f";
    MEMORY_LEAKS="YES"
  else
    echo "$f OK";
  fi
done
popd

if [ $MEMORY_LEAKS == "YES" ]; then
  exit -1
fi

echo "Memory leaks not detected"
