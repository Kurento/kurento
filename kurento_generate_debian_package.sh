#!/bin/bash -x

if [ "${GNUPG_KEY_ID}x" == "x" ]
then
  echo "You should indentify an gnupg key in environment variable GNUPG_KEY_ID, use one from gpg -K"
fi

# Path information
BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
PATH="${BASEPATH}:${BASEPATH}/kms:${PATH}"

PROJECT_NAME=$1
BRANCH=$2

[ -z "$PROJECT_NAME" ] && PROJECT_NAME=$KURENTO_PROJECT
if [ -z "$BRANCH" ]
then
  if [ -z "$GERRIT_NEWREV" ]
  then
    BRANCH=$GERRIT_REFNAME
  else
    BRANCH=$GERRIT_NEWREV
  fi
fi

if [ ! -d $PROJECT_NAME ]
then
  . kurento_clone_repo.sh || exit 1
else
  echo "Project already cloned, using existing one"
  cd $PROJECT_NAME
  git remote update
  git checkout $BRANCH || exit 1
  git pull origin $BRANCH || exit 1
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

DEBIAN_FRONTEND=noninteractive sudo apt-get install --force-yes -y lsb-release python-yaml python-simplejson  || exit 1

DIST=$(lsb_release -c)
DIST=$(echo ${DIST##*:} | tr -d ' ' | tr -d '\t')

. kurento_install_deb_dependencies.sh || exit 1

echo ${CUSTOM_PRE_COMMAND}
${CUSTOM_PRE_COMMAND} || exit 1

PROJECT_VERSION="$(kurento_get_version.sh)" || {
  echo "[kurento_generate_debian_package] ERROR: Command failed: kurento_get_version"
  exit 1
}

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
  sed -i "1 s/${debver}/${ver}.$(date +"%Y%m%d%H%M%S").${DIST}/g" debian/changelog
  export orig_ver=${ver}
fi

export ver=$(head -1 debian/changelog | sed -e "s@.* (\(.*\)) .*@\1@")

echo Version is: ${ver}

if [ "${GNUPG_KEY_ID}x" == "x" ]
then
 echo "Generating packages without signing them"
 build_args="-uc -us"
else
 echo "Using key ${GNUPG_KEY_ID}"
 build_args="-k${GNUPG_KEY_ID}"
fi

#dpkg-buildpackage -S -sa $build_args || echo "Warning, source package not created"
dpkg-buildpackage $build_args || exit 1

#Check if repossitory is public or private
PRIVATE=`get_config_value.py --key private`
if [ "x$PRIVATE" == "xTrue" ]
then
  export DEBIAN_PACKAGE_REPOSITORY=ubuntu-priv
else
  export DEBIAN_PACKAGE_REPOSITORY=ubuntu-pub
fi

list=(../*${ver}_*.deb)
length=${#list[@]}
current=0

#sudo dpkg -i ../*${ver}_*.deb || { echo "Packages cannot be installed"; exit 1 }

for i in ../*${ver}_*.deb
do
  current=$((current + 1))

  if [[ "$current" -eq "$length" ]]
  then
     export DO_SYNC=TRUE
  fi

  kurento_upload_package.sh $DIST-dev $i || { echo "Failed to upload package $i"; exit 1; }

  if [ $rc = 0 ] || [ "${FORCE_RELEASE}" = "yes" ]
  then
    kurento_upload_package.sh $DIST $i || { echo "Failed to upload package $i"; exit 1; }
  fi
done
