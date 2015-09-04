#!/bin/bash

cd ..

git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git
DEPOT_PATH="$PWD/depot_tools"
export PATH=$PATH:$DEPOT_PATH

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

mv libjingle src

gclient sync

mv src libjingle

cd libjingle

yes | sudo postpone -d -f ./build/install-build-deps.sh || exit 1

sudo ninja -C out/Debug
