#!/bin/bash

if [ $# -lt 3 ]
then
  echo "Usage: $0 <source> <orig-dist> <target-dist>"
  exit 1
fi

exec 3>&1 >/dev/tty || exec 3>&1 >./upload_sources_logs

SOURCE="Source: $1"
ORIG_DIST=$2
DEST_DIST=$3

if [ "${ID_RSA_FILE}x" == "x" ]
then
  echo "You need to specify environment variable ID_RSA_FILE with the public key to upload packages to the repository"
  exit 1
fi

if [ "${CERT}x" == "x" ]
then
  echo "You need to specify environment variable CERT with the certificate to upload packages to the repository via https"
  exit 1
fi

if [ "${REPREPRO_URL}x" == "x" ]
then
  echo "You need to specify environment variable REPREPRO_URL with the hostname of your repository. Omit the http:// part, just hostname."
  exit 1
fi

if [ "${COMPONENT}x" == "x" ]
then
  echo "You need to specify environment variable COMPONENT with the component"
  exit 1
fi

KEY=$ID_RSA_FILE

mkdir tmp
cd tmp

for file in $(curl -s http://${REPREPRO_URL}/dists/${ORIG_DIST}/${COMPONENT}/binary-amd64/Packages | awk -v RS='' -v p=$SOURCE '$0 ~ p'  | grep Filename | cut -d':' -f2)
do
  wget ${REPREPRO_URL}/$file
done

for file in $(curl -s http://${REPREPRO_URL}/dists/${ORIG_DIST}/${COMPONENT}/binary-amd64/Packages | awk -v RS='' -v p=$SOURCE '$0 ~ p'  | grep Filename | cut -d':' -f2)
do
  PACKAGE=$(basename $file)
  echo "Uploading package $PACKAGE to ${REPREPRO_URL}/dists/$DEST_DIST/$COMPONENT"
  curl --insecure --key $KEY --cert $CERT -X POST https://${REPREPRO_URL}/upload?dist=$DEST_DIST\&comp=$COMPONENT --data-binary @$PACKAGE || exit 1
done

cd ..
rm -rf tmp

exec >&3-
