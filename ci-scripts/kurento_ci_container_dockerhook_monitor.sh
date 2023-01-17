#!/bin/bash

while true; do
  ${DOCKERHOOK_HOME}/dockerhook ${KURENTO_SCRIPTS_HOME}/kurento_ci_container_dnat_hook_handler.sh > dockerhook.log 2>&1
  sleep 1
done
