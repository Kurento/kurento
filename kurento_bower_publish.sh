#!/bin/bash -x

echo "##################### EXECUTE: bower-publish #####################"
# Parameter management
# BOWER_REPOSITORY string
#   URL of repository where bower code is located. This is regarded as binary
#
# FILES string
#   List of files to be placed in bower repo. It consist of a of pairs
#   SRC_FILE:DST_FILE separated by white space. All paths are relative to
#   project root
#
# REFSPEC string
#   Git reference in both: code and bower repositories where changes are
#   commited. Default value is master
#
# CREATE_TAG true | false
#   Whether a TAG must be created in bower repository. Default value is false

# Validate mandatory parameters
[ -z "$BOWER_REPOSITORY" || exit 1
[ -z "$FILES" || exit 1
[ -z "$REFSPEC" || REFSPEC=master
[ -z "$CREATE_TAG" || CREATE_TAG=false

# Checkout to reference in source code repository
git checkout $REFSPEC || git checkout -b $REFSPEC origin/$REFSPEC || exit 1

# Get information from source code
COMMIT_LOG=$(git log -1 --oneline)
COMMIT_ID=$(echo $COMMIT_LOG|cut -d' ' -f1)
COMMIT_MSG=$(echo $COMMIT_LOG|cut -d' ' -f2-)
MESSAGE="Generated code from $KURENTO_PROJECT:$COMMIT_ID"
VERSION=$(xmlstarlet sel -N x=http://maven.apache.org/POM/4.0.0 -t -v "/x:project/x:version" pom.xml)

# Checkout bower repo
BOWER_DIR="bower_code"
[ -d $BOWER_DIR ] && rm -rf $BOWER_DIR
# Do not declare error if bower repository does not exists
git clone $BOWER_REPOSITORY $BOWER_DIR || exit 0
cd $BOWER_DIR || exit 1
git checkout $REFSPEC || git checkout -b $REFSPEC || exit 1
git pull origin $REFSPEC || exit 1
# Remove all files checked out except git ones

REMOVE_FILES=$(ls -1)
for FILE in $REMOVE_FILES
do
  echo "Removing file $FILE"
  rm -r $FILE
done

# Copy files
for FILE in $FILES
do
  SRC_FILE=../$(echo $FILE|cut -d":" -f 1)
  DST_FILE=$(echo $FILE|cut -d":" -f 2)
  DST_DIR=$(dirname $DST_FILE)
  mkdir -p $DST_DIR
  echo "Moving file: $SRC_FILE ==> $DST_FILE"
  cp $SRC_FILE $DST_FILE || exit 1
done

# Add files
git add .
git status

# Check if a commit is required
if [ $(git status --porcelain|wc -l) -gt 0 ]; then
	echo "Commit changes to bower: $MESSAGE"
	git commit -a -m "$MESSAGE" || exit 1
	git push origin $REFSPEC || exit 1
fi

# Check if a version has to be generated
if [ ${CREATE_TAG} = true ]; then
  echo "Add Tag version: $VERSION"

  # If tag already exists terminate silently
  git ls-remote --tags|grep -q "$VERSION" && exit 0

  # Add tag
  git tag $VERSION || exit 1
  git push --tags || exit 1
fi
