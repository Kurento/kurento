#!/bin/bash

# Python needs those packages
echo "Intalling python packages..."
DEBIAN_FRONTEND=noninteractive apt-get install -y python-git python-requests python-apt python-debian python-yaml

