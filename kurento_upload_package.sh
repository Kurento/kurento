#!/bin/bash

if [ $# -lt 2 ]
then
  echo "Usage: $0 <target-dist> <package>"
  exit 1
fi

DIST=$1
PACKAGE=$2

if [ "${ID_RSA_FILE}x" == "x" ]
then
  if [ "${HTTP_KEY}x" == "x" ]
  then
    echo "You need to specify environment variable ID_RSA_FILE with the public key to upload packages to the repository"
    exit 1
  else
    ID_RSA_FILE=${HTTP_KEY}
  fi
fi

echo "Using http key ${ID_RSA_FILE}"

if [ "${CERT}x" == "x" ]
then
  if [ "${HTTP_CERT}x" == "x" ]
  then
    echo "You need to specify environment variable CERT with the certificate to upload packages to the repository via https"
    exit 1
  else
    CERT=${HTTP_CERT}
  fi
fi

echo "Using http cert ${CERT}"

if [ "${REPREPRO_URL}x" == "x" ]
then
  echo "You need to specify environment variable REPREPRO_URL with the address of your repository"
  exit 1
fi

if [ "${COMPONENT}x" == "x" ]
then
  echo "You did not specified a component, using main by default"
  COMPONENT=main
fi

KEY=$ID_RSA_FILE

curl --insecure --key $KEY --cert $CERT -X POST ${REPREPRO_URL}/upload?dist=$DIST\&comp=$COMPONENT\&name=$(basename $PACKAGE) --data-binary @$PACKAGE || exit 1
