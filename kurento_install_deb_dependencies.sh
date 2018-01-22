#!/bin/bash -x

echo "##################### EXECUTE: kurento_install_deb_dependencies #####################"

if [ ! -s debian/control ] ; then
  echo "Warning, no debian/control file found"
  exit 1
fi

DEBIAN_FRONTEND=noninteractive apt-get update || echo "Error updating packages"

dpkg -i *deb
apt-get install -y -f

pkgs=$(cat debian/control | sed -e "s/$/\!\!/g" | tr -d '\n' | sed "s/\!\![[:space:]]/ /g" | sed "s/\!\!/\n/g" | grep "Build-Depends" | sed "s/Build-Depends://g" | sed "s/Build-Depends-Indep://g" | sed "s/([^)]*)//g" | sed "s/,\([^,^|]*\)[^,]*/\1/g" | sed 's/\[[^]]*\]//g' | sed "s/[[:space:]]+/ /g")
echo "Installing packages: $pkgs"
DEBIAN_FRONTEND=noninteractive apt-get install --force-yes -y $pkgs || exit 1
