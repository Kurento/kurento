#!/bin/bash

sleep 5s

ip addr show|grep 192.168
while [ $? != 0 ];
do
  sleep 5s
  echo "Looking for a 192.168 IP"
  ip addr show|grep 192.168
done

exec /opt/bin/entry_point.sh
