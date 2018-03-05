#!/bin/bash

echo "##################### EXECUTE: kurento_python_extra #####################"

PACKAGES=(
  # Our Python-based build system requires these packages
  python
  python-apt
  python-debian
  python-git
  python-requests
  python-yaml

  # Temp solution to sudo issue
  sudo
)

DEBIAN_FRONTEND=noninteractive apt-get update
DEBIAN_FRONTEND=noninteractive apt-get install --yes "${PACKAGES[@]}"
