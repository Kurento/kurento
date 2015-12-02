#!/bin/bash

# Prepare js project
echo "Preparing js project"
npm install

echo "Running tests"
rm -f node_modules/kurento-client && ln -s .. node_modules/kurento-client
cd test_reconnect
node index.js --scope=docker
