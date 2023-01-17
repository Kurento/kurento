#!/bin/bash -x
echo "##################### EXECUTE: kurento_http_publish #####################"
# BUILDS_HOST url
#   URL where files will be uploaded
#
# FILES string
#   List of files to be uploaded. It consist of a of tuplas
#   SRC_FILE:DST_FILE:UNCOMPRESS separated by white space.
#
# HTTP_KEY path
#   Path to key file used to authenticate
#
# HTTP_CERT path
#   Path to the certificate file if authentication is requried.
#

# Functions
# Make Collection
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
[ -z "$BUILDS_HOST" ] && BUILDS_HOST=builds.openvidu.io
[ -z "$FILES" ] && exit 1
if [ -n "$HTTP_KEY$HTTP_CERT" ]; then
  export CURL="curl --insecure --key $HTTP_KEY --cert $HTTP_CERT"
else
  CURL="curl"
fi

# Copy deployed files
for FILE in $FILES
do
	SRC_FILE=$(echo $FILE|cut -d":" -f 1)
	DST_FILE=$(echo $FILE|cut -d":" -f 2)
  UNCOMPRESS=$(echo $FILE|cut -d":" -f 3)
  [ -z "$UNCOMPRESS" ] &&  UNCOMPRESS=0
  [ -f $SRC_FILE ] && $CURL -X POST https://$BUILDS_HOST/$DST_FILE --data-binary @$SRC_FILE
done
