#!/bin/bash -x

echo "##################### EXECUTE: update_test_files.sh #####################"

# This script must be started in the test files root


if [ ! -d .svn ]; then
	svn checkout http://files.openvidu.io/svn/kurento . || exit 1
fi

for i in {1..3}
do
	svn update && exit 0
	sleep $((5*$i))
done
