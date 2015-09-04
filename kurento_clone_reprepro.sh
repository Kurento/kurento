#!/bin/bash

# Argument = -t test -r server -p password -v

usage()
{
cat << EOF
usage: $0 options

This script clones packages from one repo to other

OPTIONS:
   -h      Show this message
   -u      Original reprepro url (mandatory)
   -d      Original dist (mandatory)
   -c      Original component (mandatory)
   -r      Id rsa file for remote reprepro (mandatory)
   -t      Certificate for remove reprepro (mandatory)
   -l      Target URL (mandatory)
   -i      Target dist (optional)
   -o      Target component (optional)
   -s      Source package (optional)
   -v      Verbose
EOF
}

VERBOSE=0
ORIG_REPO_URL=
DIST=
ORIG_COMPONENT=
TARGET_DIST=
export COMPONENT=
SOURCE=
export ID_RSA_FILE=
export CERT=
export REPREPRO_URL=

while getopts “hu:d:c:r:t:l:i:o:s:v” OPTION
do
  case $OPTION in
    h)
      usage
      exit 1
      ;;
    u)
      ORIG_REPO_URL=$OPTARG
      ;;
    d)
      DIST=$OPTARG
      if [ x$TARGET_DIST = "x" ]
      then
        TARGET_DIST=$DIST
      fi
      ;;
    c)
      ORIG_COMPONENT=$OPTARG
      if [ x$TARGET_COMPONENT = "x" ]
      then
        COMPONENT=$ORIG_COMPONENT
      fi
      ;;
    r)
      ID_RSA_FILE=$OPTARG
      ;;
    t)
      CERT=$OPTARG
      ;;
    l)
      REPREPRO_URL=$OPTARG
      ;;
    i)
      TARGET_DIST=$OPTARG
      ;;
    o)
      COMPONENT=$OPTARG
      ;;
    s)
      SOURCE=$OPTARG
      ;;
    v)
      VERBOSE=1
      ;;
    ?)
      usage
      exit
      ;;
  esac
done

print () {
  if [ $VERBOSE = 1 ]
  then
  echo $@
  fi
}

check_argument () {
  if [ "x$@" = "x" ]
  then
    usage
    exit 1
  fi
}

check_argument $ORIG_REPO_URL
check_argument $DIST
check_argument $ORIG_COMPONENT
check_argument $ID_RSA_FILE
check_argument $CERT
check_argument $REPREPRO_URL

tmp_dir=$(mktemp -d)

PATH=$PATH:$(realpath $(dirname "$0"))

cd $tmp_dir

wget --no-verbose ${ORIG_REPO_URL}/dists/${DIST}/${ORIG_COMPONENT}/binary-amd64/Packages
if [ $? != 0 ]
then
  echo "Cannot get Packages file, bad repo url, dist or component"
  rm -rf ${tmp_dir}
  exit 1
fi
cd -

if [ "x$SOURCE" = "x" ]
then
  files=$(cat ${tmp_dir}/Packages | grep Filename | cut -d':' -f2)
else
  files=$(cat ${tmp_dir}/Packages | awk -v RS='' -v p="(Source|Package): $SOURCE\n" '$0 ~ p'  | grep Filename | cut -d':' -f2)
fi

print Files are: $files

for i in $files
do
  print "Cloning package $i"
  cd $tmp_dir
  wget --no-verbose ${ORIG_REPO_URL}/$i -O package.deb
  cd -
  kurento_upload_package.sh $TARGET_DIST $tmp_dir/package.deb
  if [ $? != 0 ]
  then
    print "Failed to upload package $i"
    rm -rf ${tmp_dir}
    exit 1
  fi
done

rm -rf ${tmp_dir}
