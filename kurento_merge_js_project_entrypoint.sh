#!/bin/bash

PATH=$PATH:$(realpath $(dirname "$0"))

echo "Preparing environment..."

if [ -n "$KEY" ]; then
  echo "Add private key to ssh agent"
  eval $(ssh-agent -s)
  ssh-add $KEY
fi

if [ -f /root/.ssh/config ]; then
  echo "Set correct owner & permissions to /root/.ssh/config file"
  chown root:root /root/.ssh/config
  chown 600 /root/.ssh/config
  ls -la /root/.ssh
  ls -la /opt
fi

kurento_merge_js_project.sh
