#!/bin/bash -x

# It checks if the version specified is a release version

if [ -n "$1" ]
then
  VERSION="$1"
else
  echo "Missing parameter version"
  exit 1
fi

echo "$VERSION" | grep -q -P "^\d+\.\d+\.\d+" || {
  echo "ERROR: Not a valid release version: $VERSION"
  exit 1
}

RE='[^0-9]*\([0-9]*\)[.]\([0-9]*\)[.]\([0-9]*\)\([0-9A-Za-z-]*\)'
MAJOR="$(echo "$VERSION" | sed -e "s#$RE#\1#")"
MINOR="$(echo "$VERSION" | sed -e "s#$RE#\2#")"
PATCH="$(echo "$VERSION" | sed -e "s#$RE#\3#")"
SPECIAL="$(echo "$VERSION" | sed -e "s#$RE#\4#")"

echo "Version: [$MAJOR, $MINOR, $PATCH, $SPECIAL]"

if [ "${SPECIAL}x" != "x" ]; then
  echo "ERROR: Not a release version: $VERSION, special found: $SPECIAL"
  exit 1
fi
