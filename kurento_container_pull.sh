#!/bin/bash

# Internal (private) images

# dev-integration images (for Java & JS)
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/dev-integration:jdk-7-node-0.12
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/dev-integration:jdk-8-node-0.12
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/dev-integration-browser:1.0.0

# kurento-media-server development version with core dump & public modules
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/kurento-media-server-dev:latest

# coturn image
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/coturn:1.0.0

# svn-client to extract files from svn into a docker host
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/svn-client:1.0.0

# dev-documentation images (for documentation projects)
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/dev-documentation:1.0.0-jdk-7
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/dev-documentation:1.0.0-jdk-8

# dev-media-server images (for media server projects)
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/dev-media-server:trusty-jdk-7
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/dev-media-server:trusty-jdk-8
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/dev-media-server:wily-jdk-7
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/dev-media-server:wily-jdk-8

# Selenium images
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/node-chrome-debug:1.0.0
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/node-firefox-debug:1.0.0

# DNAT images
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/node-chrome-beta-debug:1.0.0-dnat
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/node-chrome-dev-debug:1.0.0-dnat
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/node-firefox-beta-debug:1.0.0-dnat

# dev-chef image
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/dev-chef:1.0.0

# Selenium images
[ -n "$SELENIUM_VERSION" ] || SELENIUM_VERSION="2.48.2"
echo "Pulling images for selenium version $SELENIUM_VERSION"
docker pull selenium/hub:$SELENIUM_VERSION
docker pull selenium/node-chrome:$SELENIUM_VERSION
docker pull selenium/node-firefox:$SELENIUM_VERSION
docker pull selenium/node-chrome-debug:$SELENIUM_VERSION
docker pull selenium/node-firefox-debug:$SELENIUM_VERSION
docker pull kurento/node-chrome-beta:$SELENIUM_VERSION
docker pull kurento/node-chrome-beta-debug:$SELENIUM_VERSION
docker pull kurento/node-chrome-dev:$SELENIUM_VERSION
docker pull kurento/node-chrome-dev-debug:$SELENIUM_VERSION
docker pull selenium/node-firefox-beta:$SELENIUM_VERSION
docker pull selenium/node-firefox-beta-debug:$SELENIUM_VERSION

# Image to record vnc sessions
docker pull softsam/vncrecorder:latest

# Mongo image
docker pull mongo:2.6.11

docker images > container_images.txt

# Keep just KEEP_IMAGES last kms dev images
KEEP_IMAGES=3
NUM_IMAGES=$(docker images | grep kurento-media-server-dev | awk '{print $2}' | sort | wc -l)
if [ $NUM_IMAGES -gt $KEEP_IMAGES ]; then
	NUM_REMOVE_IMAGES=$[$NUM_IMAGES-$KEEP_IMAGES]
	REMOVE_IMAGES=$(docker images | grep kurento-media-server-dev | awk '{print $2}' | sort | head -$NUM_REMOVE_IMAGES)
    status=0
    for image in $REMOVE_IMAGES
    do
    	echo "Removing image $image"
        docker rmi kurento/kurento-media-server-dev:$image || $status=$[$status || $?]
    done
fi

exit $status
