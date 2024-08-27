#!/bin/bash

PIDOFKMS=$(pidof kurento-media-server)

gdb -batch -ex 'thread apply all bt full' -p $PIDOFKMS