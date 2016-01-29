#!/bin/bash -x

if [ ! -s debian/control ] ; then
  echo "Warning, no debian/control file found"
  exit 1
fi

echo "deb http://ubuntu.kurento.org trusty-dev kms6" | tee /etc/apt/sources.list.d/kurento.list
wget -O - http://ubuntu.kurento.org/kurento.gpg.key | apt-key add -
DEBIAN_FRONTEND=noninteractive apt-get update

pkgs=$(cat debian/control | sed -e "s/$/\!\!/g" | tr -d '\n' | sed "s/\!\![[:space:]]/ /g" | sed "s/\!\!/\n/g" | grep "Build-Depends" | sed "s/Build-Depends://g" | sed "s/Build-Depends-Indep://g" | sed "s/([^)]*)//g" | sed "s/,\([^,^|]*\)[^,]*/\1/g" | sed 's/\[[^]]*\]//g' | sed "s/[[:space:]]+/ /g")
echo "Installing packages: $pkgs"
DEBIAN_FRONTEND=noninteractive apt-get install --force-yes --fix-broken -y $pkgs || exit 1

if [ "${EXTRA_PACKAGES}x" != "x" ]; then
  DEBIAN_FRONTEND=noninteractive apt-get install --force-yes -y $EXTRA_PACKAGES || exit 1
fi
