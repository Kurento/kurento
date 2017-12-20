#!/bin/bash -x
echo "##################### EXECUTE: kurento_ci_container_entrypoint #####################"

[ -n "$1" ] || { echo "No script to run specified. Need one to run after preparing the environment"; exit 1; }
BUILD_COMMAND=$@

PATH=$(realpath $(dirname "$0")):$(realpath $(dirname "$0"))/kms:$PATH

echo "Preparing environment..."

DIST=$(lsb_release -c)
DIST=$(echo ${DIST##*:} | tr -d ' ' | tr -d '\t')
export DEBIAN_FRONTEND=noninteractive

echo "Network configuration"
ip addr list

for CMD in $BUILD_COMMAND; do
  echo "Running command: $CMD"
  $CMD || exit 1
done
