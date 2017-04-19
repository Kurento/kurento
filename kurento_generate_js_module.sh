#!/bin/bash -x

PATH=$PATH:$(realpath $(dirname "$0"))

PROJECT_NAME=$1
BRANCH=$2

[ -z "$PROJECT_NAME" ] && PROJECT_NAME=$KURENTO_PROJECT
[ -z "$BRANCH" ] && BRANCH=$GERRIT_REFNAME

COMMIT_ID=`git rev-parse HEAD`
mkdir -p build
cd build
cmake .. -DGENERATE_JS_CLIENT_PROJECT=TRUE -DDISABLE_LIBRARIES_GENERATION=TRUE || exit 1
JS_PROJECT_NAME=`cat js_project_name`
echo Project name $JS_PROJECT_NAME
rm -rf js
git clone ${KURENTO_GIT_REPOSITORY}/${JS_PROJECT_NAME}-js.git js || exit 1
cd js || (echo "Cannot clone repository, may not be public" && exit 0)
# Remove /refs/heads/ before use
BRANCH=$(echo $BRANCH | sed 's|refs/heads/||g')
git checkout ${BRANCH} || git checkout -b ${BRANCH}
git reset --hard origin/${BRANCH} || echo
rm -rf *
cd ..
cmake .. -DGENERATE_JS_CLIENT_PROJECT=TRUE || exit 1
cd js
for i in `find . | grep -v  "^./.git" | grep -v "^.$"`
do
  git add $i
done
if ! git diff-index --quiet HEAD
then
  git commit -a -m "Generated code from ${PROJECT_NAME} $COMMIT_ID"
  git push origin ${BRANCH} || exit 1
fi

PROJECT_VERSION=`kurento_get_version.sh`

# If release version, create tag
if [[ ${PROJECT_VERSION} != *-dev ]]; then
  echo "Creating tag ${JS_PROJECT_NAME}-${PROJECT_VERSION}"
  git tag "${PROJECT_VERSION}"
  git push --tags || exit 1
fi
