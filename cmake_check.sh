#!/bin/bash

# export DISPLAY=:1
mkdir build && cd build && cmake .. -DCMAKE_INSTALL_PREFIX=/usr

CHECK=$(make -qp | awk -F':' '/^[a-zA-Z0-9][^$#\/\t=]*:([^=]|$)/ {split($1,A,/ /);for(i in A)print A[i]}' | grep ^check$)
if [ "${CHECK}x" = "x" ]; then
  make -j4 || exit 1
else
  make -j4 || exit 1
  make check ARGS=-j4 -j4 || exit 1
fi
