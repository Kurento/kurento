#!/bin/bash

PATH=$PATH:$(realpath $(dirname "$0"))

config_files="pom.xml package.json bower.json CMakeLists.txt Makefile configure.ac configure.in"
dev_version_suffixes="dev|SNAPSHOT|master|KurentoForks"

for config_file in $config_files
do
  files=$(find . -path ./node_modules -prune -o -name "$config_file")
  for file in $files
  do
    if [ -f $file ]; then
      dev_versions=$(cat $file | grep -E --regexp="$dev_version_suffixes")
      if [ "${dev_versions}x" != "x" ]; then
        echo "There are development versions/dependencies. File $file, lines: $dev_versions"
      fi
    fi
  done
done
