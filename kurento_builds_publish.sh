#!/bin/bash -x
echo "##################### EXECUTE: builds-publish #####################"

# Functions
mkcol () {
	if [ $(dirname $1) != "." -a $(dirname $1) != "/" ]; then
		mkcol $(dirname $1)
	fi
$CURL -X MKCOL https://$BUILDS_HOST/$1
}

upload () {
	REMAINDER=${1#$SRC_FILE}
    DST_REAL=${DST_FILE}${REMAINDER}
	DST_DIR=$(dirname $DST_REAL)
	# Create collection
	mkcol $DST_DIR
	# Upload file
	echo "Upload file $1 to $DST_REAL"
	$CURL --upload-file $1 https://$BUILDS_HOST/$DST_REAL || exit 1
	# Verify file
	curl http://$BUILDS_HOST/$DST_REAL > file.tmp || exit 1
	md5sum file.tmp > file.md5 || exit 1
	cp $1 file.tmp || exit 1
	md5sum -c file.md5 || exit 1
}

export -f upload
export -f mkcol

# Params management
# Files
[ -n "$1" ] && FILES=$1 || exit 1
[ -n "$2" ] && BUILDS_HOST=$2 || BUILDS_HOST=builds.kurento.org

export BUILDS_HOST
export KEY=$JENKINS_HOME/.ssh/id_rsa
export CERT=$JENKINS_HOME/.ssh/jenkins.crt
export CURL="curl --insecure --key $KEY --cert $CERT"

# Copy deployed files
for FILE in $FILES
do
	export SRC_FILE=$(echo $FILE|cut -d":" -f 1)
	export DST_FILE=$(echo $FILE|cut -d":" -f 2)
    UNCOMPRESS=$(echo $FILE|cut -d":" -f 3)

    if [ -z $UNCOMPRESS ]; then
        UNCOMPRESS=0
    fi

    $CURL -X POST https://$BUILDS_HOST/upload/$DST_FILE?uncompress=$UNCOMPRESS --data-binary @$SRC_FILE

    #export DST_DIR=$(dirname $DST_FILE)
    #find $SRC_FILE -type f -exec bash -c 'upload "$0"' {} \;

done