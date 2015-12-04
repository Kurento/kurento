#!/bin/bash

[ -n "$1" ] || { echo "No script to run specified. Need one to run after preparing the environment"; exit 1; }
BUILD_COMMAND=$1

PATH=$PATH:$(realpath $(dirname "$0"))

echo "Preparing environment..."

#if [ -n "$KEY" ]; then
#  echo "Add private key to ssh agent"
#  eval $(ssh-agent -s)
#  ssh-add $KEY
#  ls -la /opt
#fi

if [ -f $SSH_CONFIG ]; then
  echo "Set correct owner & permissions to /root/.ssh/config file"
  chown root:root $SSH_CONFIG
  chmod 600 $SSH_CONFIG
  chown root:root $KEY
  chmod 600 $KEY
  ls -la /root
  ls -la /root/.ssh
fi

echo "Running command $BUILD_COMMAND"
$BUILD_COMMAND
