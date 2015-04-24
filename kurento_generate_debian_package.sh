#!/bin/bash

if [ $# -lt 2 ]
then
  echo "Usage: $0 <project_name> <branch>"
  exit 1
fi

if [ "${KEY_ID}x" == "x" ]
then
  echo "You should indentify an gnupg key in environment variable KEY_ID, use one from gpg -K"
fi

PATH=$PATH:$(realpath $(dirname "$0"))

PROJECT_NAME=$1
BRANCH=$2

if [ ! -d $PROJECT_NAME ]
then
  . kurento_clone_repo.sh || exit 1
else
  echo "Project already cloned, using existing one"
  cd $PROJECT_NAME
  git checkout $BRANCH
fi

if [ ! -s debian/changelog ]
then
  exit 0
fi

name=$(grep Source debian/control | head -1 | sed -e "s@.*: @@")

if [ "${name}x" = "x" ]; then
  name=$(grep Package debian/control | head -1 | sed -e "s@.*: @@")
fi

echo Compiling package: ${name}

last_release=$(git rev-list --tags --max-count=1 || git rev-list HEAD | tail -n 1)

export rc=$(git log ${last_release}..HEAD --oneline | wc -l)

if [ $(dpkg -l | grep postpone | wc -l) -lt 1 ]
then
  DEBIAN_FRONTEND=noninteractive sudo apt-get install --force-yes -y postpone
fi

DEBIAN_FRONTEND=noninteractive sudo postpone -d -f apt-get install --force-yes -y lsb-release || exit 1

DIST=$(lsb_release -c)
DIST=$(echo ${DIST##*:} | tr -d ' ' | tr -d '\t')

. kurento_install_deb_dependencies.sh || exit 1

${CUSTOM_PRE_COMMAND}

PROJECT_VERSION=`kurento_get_version.sh`

if [ "${PROJECT_VERSION}x" = "x" ]; then
  exit 1
fi

export ver="${PROJECT_VERSION%-*}"
export debver=$(head -1 debian/changelog | sed -e "s@.* (\(.*\)) .*@\1@")

if [ $rc -gt 0 ]
then
  sep="~"
  sed -i "1 s/${debver}/${ver}${sep}$(date +"%Y%m%d%H%M%S").${rc}.g$(git rev-parse --short HEAD).${DIST}/g" debian/changelog
  export orig_ver=${ver}
else
  sed -i "1 s/${debver}/${ver}.${DIST}/g" debian/changelog
  export orig_ver=${ver}
fi

export ver=$(head -1 debian/changelog | sed -e "s@.* (\(.*\)) .*@\1@")

echo Version is: ${ver}

if [ "${KEY_ID}x" == "x" ]
then
 echo "Generating packages without signing them"
 build_args=-uc -us
else
 build_args=-k${KEY_ID}
fi

dpkg-buildpackage -S -sa $build_args || echo "Warning, source package not created"
dpkg-buildpackage $build_args || exit 1

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
  echo "You need to specify environment variable REPREPRO_URL with the address of your repository"
  exit 1
fi

KEY=$ID_RSA_FILE

for i in ../*${ver}_*.deb
do
  curl --insecure --key $KEY --cert $CERT -X POST ${REPREPRO_URL}/upload?dist=$DIST --data-binary @$i || echo "Failed to upload package $i"

  if [ $rc = 0 ]
  then
    curl --insecure --key $KEY --cert $CERT -X POST ${REPREPRO_URL}/upload?dist=$DIST-releases --data-binary @$i || echo "Failed to upload package $i"
  fi
done
