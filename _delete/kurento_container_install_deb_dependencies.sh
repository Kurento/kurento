#!/bin/bash -x

echo "##################### EXECUTE: kurento_container_install_deb_dependencies.sh #####################"

if [ ! -s debian/control ] ; then
  echo "Warning, no debian/control file found"
  exit 1
fi

if [ ! -f /etc/apt/sources.list.d/kurento.list ]; then
  echo "deb http://ubuntuci.kurento.org trusty-dev kms6" | tee /etc/apt/sources.list.d/kurento.list
  if [ -n "$UBUNTU_PRIV_S3_ACCESS_KEY_ID" ] && [ -n "$UBUNTU_PRIV_S3_SECRET_ACCESS_KEY_ID" ]; then
    echo "deb s3://ubuntu-priv.kurento.org.s3.amazonaws.com trusty-dev kms6" | tee /etc/apt/sources.list.d/kurento-priv.list
  fi
fi

if [ -n "$UBUNTU_PRIV_S3_ACCESS_KEY_ID" ] && [ -n "$UBUNTU_PRIV_S3_SECRET_ACCESS_KEY_ID" ]; then
  cat >/etc/apt/s3auth.conf  <<-EOF
  AccessKeyId = $UBUNTU_PRIV_S3_ACCESS_KEY_ID
  SecretAccessKey = $UBUNTU_PRIV_S3_SECRET_ACCESS_KEY_ID
  Token = ''
EOF
fi

apt-key adv --keyserver keyserver.ubuntu.com --recv 2F819BC0
DEBIAN_FRONTEND=noninteractive apt-get update

pkgs=$(cat debian/control | sed -e "s/$/\!\!/g" | tr -d '\n' | sed "s/\!\![[:space:]]/ /g" | sed "s/\!\!/\n/g" | grep "Build-Depends" | sed "s/Build-Depends://g" | sed "s/Build-Depends-Indep://g" | sed "s/([^)]*)//g" | sed "s/,\([^,^|]*\)[^,]*/\1/g" | sed 's/\[[^]]*\]//g' | sed "s/[[:space:]]+/ /g")
echo "Installing packages: $pkgs"
DEBIAN_FRONTEND=noninteractive apt-get install --force-yes --fix-broken -y $pkgs || exit 1

if [ "${EXTRA_PACKAGES}x" != "x" ]; then
  DEBIAN_FRONTEND=noninteractive apt-get install --force-yes -y $EXTRA_PACKAGES || exit 1
fi
