#!/bin/bash

echo "solutions = [
  {
    \"managed\": False,
    \"name\": \"src\",
    \"url\": \"ssh://code.kurento.org:12345/libjingle\",
    \"custom_deps\": {},
    \"deps_file\": \"DEPS\",
    \"safesync_url\": \"\",
  },
]" > .gclient

gclient sync

cd src

sudo postpone -d -f ./build/install-build-deps.sh

ninja -C out/Debug
