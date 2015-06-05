#!/bin/bash

if [ $# -lt 2 ]
then
  echo "Usage: $0 <orig_repo_url> <dist> [<target_dist>]"
  exit 1
fi

tmp_dir=$(mktemp -d)

ORIG_REPO_URL=$1
DIST=$2

if [ $3 ]
then
  TARGET_DIST=$3
else
  echo "Using $DIST as target dist"
  TARGET_DIST=$DIST
fi

PATH=$PATH:$(realpath $(dirname "$0"))

cd $tmp_dir
wget ${ORIG_REPO_URL}/dists/$DIST/main/binary-amd64/Packages
if [ $? != 0 ]
then
  echo "Cannot get Packages file, bad repo url or dist"
  rm -rf ${tmp_dir}
  exit 1
fi
cd -

for i in $(cat ${tmp_dir}/Packages | grep Filename: | cut -c 11-)
do
  echo "Cloning package $i"
  cd $tmp_dir
  wget ${ORIG_REPO_URL}/$i -O package.deb
  cd -
  kurento_upload_package.sh $DIST $tmp_dir/package.deb
  if [ $? != 0 ]
  then
    echo "Failed to upload package $i"
    rm -rf ${tmp_dir}
    exit 1
  fi
done

rm -rf ${tmp_dir}
