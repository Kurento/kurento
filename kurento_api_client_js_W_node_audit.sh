#!/bin/bash

if [ -z KMS_PORT ]
then
  echo "Need a KMS_PORT env variable with a kms port to connect to"
  exit 1
fi

# Prepare js project
echo "Preparing js project"
npm install
node_modules/.bin/grunt || true
node_modules/.bin/grunt sync:bower

# Execute test with NPM
rm -f node_modules/kurento-client && ln -s .. node_modules/kurento-client
npm test -- --scope=docker --ws_port=$KMS_PORT --timeout_factor=3
