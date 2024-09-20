#!/bin/bash

PIDOFKMS=$(pidof kurento-media-server)

eu-stack -idvas -p $PIDOFKMS
