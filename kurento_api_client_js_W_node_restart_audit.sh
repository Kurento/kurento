#!/bin/bash

if [ -z KMS_PORT ]
then
  echo "Need a KMS_PORT env variable with a kms port to connect to"
  exit 1
fi

# Prepare js project
echo "Preparing js project"
npm install

echo "Running tests"
rm -f node_modules/kurento-client && ln -s .. node_modules/kurento-client
cd test_reconnect
node index.js --scope=docker --ws_port=$KMS_PORT
