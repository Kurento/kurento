#!/bin/bash

apt-get update
# Python needs those packages
echo "Intalling python packages..."
DEBIAN_FRONTEND=noninteractive apt-get install --yes --no-install-recommends python-git python-requests python-apt python-debian python-yaml

# Temp solution to sudo issue
DEBIAN_FRONTEND=noninteractive apt-get install --yes --no-install-recommends sudo
