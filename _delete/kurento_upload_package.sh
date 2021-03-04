#!/bin/bash -x

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
  if [ "${DEBIAN_PACKAGE_REPOSITORY_HOST}x" == "x" ]
  then
    echo "You need to specify environment variable REPREPRO_URL with the address of your repository"
    exit 1
  else
    REPREPRO_URL=${DEBIAN_PACKAGE_REPOSITORY_HOST}
  fi
fi

if [ "${COMPONENT}x" == "x" ]
then
  if [ "${DEBIAN_PACKAGE_COMPONENT}" == "x" ]
  then
    echo "You did not specified a component, using main by default"
    COMPONENT=main
  else
    COMPONENT=${DEBIAN_PACKAGE_COMPONENT}
  fi
fi

echo "Using component ${COMPONENT}"

if [ -n "$DEBIAN_PACKAGE_REPOSITORY" ]; then
  PARAM_REPO="repo=$DEBIAN_PACKAGE_REPOSITORY\&"
fi

KEY=$ID_RSA_FILE

curl -s -o /dev/stderr -w %{http_code} --insecure --key $KEY --cert $CERT -X POST ${REPREPRO_URL}/upload?${PARAM_REPO}dist=$DIST\&comp=$COMPONENT\&name=$(basename $PACKAGE)\&cmd=add --data-binary @$PACKAGE | grep 200 || exit 1

if [ "${DO_SYNC}x" == "TRUEx" ]
then
curl -s -o /dev/stderr -w %{http_code} --insecure --key $KEY --cert $CERT -X POST ${REPREPRO_URL}/upload?${PARAM_REPO}dist=$DIST\&comp=$COMPONENT\&cmd=publish | grep 200  || exit 1
fi
