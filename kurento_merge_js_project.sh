#!/bin/bash -x
echo "##################### EXECUTE: merge_js_project #####################"

# KURENTO_PROJECT string
#   Name of the project to be merged
#
# KURENTO_GIT_REPOSITORY_SERVER string
#   URL of kurento code repository

# Verify mandatory parameters
[ -z "$KURENTO_PROJECT" ] && exit 1
[ -z "$KURENTO_GIT_REPOSITORY_SERVER" ] && exit 1

# Verify project structure
[ -f package.json ] || exit 1

# Deploy to maven repository
kurento_check_version.sh true
kurento_mavenize_js_project.sh $KURENTO_PROJECT
kurento_maven_deploy.sh $MAVEN_SETTINGS

# Deploy to bower repository
[ -z "$BASE_NAME" ] && BASE_NAME=$KURENTO_PROJECT

# Verify Bower repository.
if git ls-remote ssh://jenkins@$KURENTO_GIT_REPOSITORY_SERVER/$BASE_NAME-bower.git; then
   BOWER_REPOSITORY=ssh://jenkins@$KURENTO_GIT_REPOSITORY_SERVER/$BASE_NAME-bower.git
else
   BOWER_REPOSITORY=git@github.com:Kurento/${BASE_NAME}-bower.git
fi

# Select files to be moved to bower repository
FILES=""
FILES="$FILES dist/$BASE_NAME.js:js/$BASE_NAME.js"
FILES="$FILES dist/$BASE_NAME.min.js:js/$BASE_NAME.min.js"
FILES="$FILES dist/$BASE_NAME.map:js/$BASE_NAME.map"
# README_bower.md is optional
[ -f README_bower.md ] && FILES="$FILES README_bower.md:README.md"
# bower.json is optional
[ -f bower.json ] && FILES="$FILES bower.json:bower.json"
# LICENSE is optional
[ -f LICENSE ] && FILES="$FILES LICENSE:LICENSE"

export BOWER_REPOSITORY
export FILES
kurento_bower_publish.sh
