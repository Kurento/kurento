#!/bin/bash

if [ $# -lt 3 ]
then
  echo "Usage: $0 <source> <orig-dist> <target-dist>"
  exit 1
fi

exec 3>&1 >/dev/tty || exec 3>&1 >./upload_sources_logs

SOURCE="Source: $1"
ORIG_DIST=$2
TARGET_DIST=$3

# Path information
BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
PATH="${BASEPATH}:${BASEPATH}/kms:${PATH}"

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

if [ "${ORIG_REPREPRO_URL}x" == "x" ]
then
  echo "You need to specify environment variable ORIG_REPREPRO_URL with the address of your repository"
  exit 1
fi

if [ "${TARGET_REPREPRO_URL}x" == "x" ]
then
  echo "You need to specify environment variable TARGET_REPREPRO_URL with the address of your repository"
  exit 1
fi

if [ "${ORIG_COMPONENT}x" == "x" ]
then
  echo "You need to specify environment variable ORIG_COMPONENT with the origin component"
  exit 1
fi

if [ "${DEST_COMPONENT}x" == "x" ]
then
  echo "You need to specify environment variable DEST_COMPONENT with the destination component"
  exit 1
fi

if [ "x${ARCH}" != "xamd64" -a "x${ARCH}" != "xi386" ]
then
  echo "You need to specify environment variable ARCH with i386 or amd64"
  exit 1
fi

KEY=$ID_RSA_FILE

TEMP_DIR=`mktemp -d`

cd $TEMP_DIR

curl --fail -s ${ORIG_REPREPRO_URL}/dists/${ORIG_DIST}/${ORIG_COMPONENT}/binary-${ARCH}/Packages > Packages || curl -s ${ORIG_REPREPRO_URL}/dists/${ORIG_DIST}/${ORIG_COMPONENT}/binary-${ARCH}/Packages.gz | zcat > Packages

export DIST=$TARGET_DIST
export COMPONENT=$DEST_COMPONENT
export REPREPRO_URL=$TARGET_REPREPRO_URL
export DEBIAN_PACKAGE_REPOSITORY=ubuntu-pub

if [ ! -e Packages ]
then
  echo "Cannot get packages index from ${ORIG_REPREPRO_URL}"
  exit 1
fi

for file in $(cat Packages | awk -v RS='' -v p="$SOURCE" '$0 ~ p'  | grep Filename | cut -d':' -f2)
do
  wget ${ORIG_REPREPRO_URL}/$file
done

for file in $(cat Packages | awk -v RS='' -v p="$SOURCE" '$0 ~ p'  | grep Filename | cut -d':' -f2)
do
  PACKAGE=$(basename $file)
  echo "Uploading package $PACKAGE to ${TARGET_REPREPRO_URL}/dists/$DEST_DIST/$DEST_COMPONENT"
  kurento_upload_package.sh $TARGET_DIST $PACKAGE
  #curl --insecure --key $KEY --cert $CERT -X POST ${TARGET_REPREPRO_URL}/upload?dist=$TARGET_DIST\&comp=$DEST_COMPONENT --data-binary @$PACKAGE || exit 1
done

SOURCE="Package: $1"
for file in $(cat Packages | awk -v RS='' -v p="$SOURCE" '$0 ~ p'  | grep Filename | cut -d':' -f2)
do
  wget ${ORIG_REPREPRO_URL}/$file
done

for file in $(cat Packages | awk -v RS='' -v p="$SOURCE" '$0 ~ p'  | grep Filename | cut -d':' -f2)
do
  PACKAGE=$(basename $file)
  echo "Uploading package $PACKAGE to ${TARGET_REPREPRO_URL}/dists/$DEST_DIST/$DEST_COMPONENT"
  kurento_upload_package.sh $TARGET_DIST $PACKAGE
  #curl --insecure --key $KEY --cert $CERT -X POST ${TARGET_REPREPRO_URL}/upload?dist=$TARGET_DIST\&comp=$DEST_COMPONENT --data-binary @$PACKAGE || exit 1
done

cd -
rm -rf $TEMP_DIR

exec >&3-
