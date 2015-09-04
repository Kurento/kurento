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
  git remote update
  git checkout $BRANCH
  git pull origin $BRANCH
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

last_release=$(git describe --abbrev=0 --tags || git rev-list --max-parents=0 HEAD)

export rc=$(git log ${last_release}..HEAD --oneline | wc -l)

if [ $(dpkg -l | grep postpone | wc -l) -lt 1 ]
then
  DEBIAN_FRONTEND=noninteractive sudo apt-get install --force-yes -y postpone
fi

DEBIAN_FRONTEND=noninteractive sudo postpone -d -f apt-get install --force-yes -y lsb-release || exit 1

DIST=$(lsb_release -c)
DIST=$(echo ${DIST##*:} | tr -d ' ' | tr -d '\t')

. kurento_install_deb_dependencies.sh || exit 1

echo ${CUSTOM_PRE_COMMAND}
${CUSTOM_PRE_COMMAND} || exit 1

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
 build_args="-uc -us"
else
 echo "Using key ${KEY_ID}"
 build_args="-k${KEY_ID}"
fi

dpkg-buildpackage -S -sa $build_args || echo "Warning, source package not created"
dpkg-buildpackage $build_args || echo "Warning, failure detecten on debian package generation"

for i in ../*${ver}_*.deb
do
  kurento_upload_package.sh $DIST-dev $i || echo "Failed to upload package $i"

  if [ $rc = 0 ] || [ "${FORCE_RELEASE}" = "yes" ]
  then
    kurento_upload_package.sh $DIST $i || echo "Failed to upload package $i"
  fi
done
