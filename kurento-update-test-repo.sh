#!/bin/bash -x

echo '##################### EXECUTE: kurento_ci_container_job_setup #####################'

echo "deb http://ubuntu.kurento.org xenial-test kms6" | tee /etc/apt/sources.list.d/kurento-test.list
wget -O - http://ubuntu.kurento.org/kurento.gpg.key | apt-key add -
apt-get update
