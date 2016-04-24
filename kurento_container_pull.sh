#!/bin/bash

pullAndTag() {
	echo "Pulling $KURENTO_REGISTRY_URI/$1"
	docker pull $KURENTO_REGISTRY_URI/$1
	echo "Tagging $KURENTO_REGISTRY_URI/$1 as $1"
	docker tag $KURENTO_REGISTRY_URI/$1 $1
}

# Internal (private) images
[ -n "$SELENIUM_VERSION" ] || SELENIUM_VERSION="2.47.1"
[ -n "$KURENTO_REGISTRY_URI" ] || KURENTO_REGISTRY_URI="$KURENTO_REGISTRY_URI"

# dev-integration images (for Java & JS)
NODE_VERSIONS="0.12 4.x 5.x"
for NODE_VERSION in $NODE_VERSIONS
do
	dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/dev-integration:jdk-7-node-$NODE_VERSION
	dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/dev-integration:jdk-8-node-$NODE_VERSION
done
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/dev-integration-browser:$SELENIUM_VERSION-node-0.12
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/dev-integration-browser:$SELENIUM_VERSION-node-4.x

# kurento-media-server development version with core dump & public modules
docker pull $KURENTO_REGISTRY_URI/kurento/kurento-media-server-dev:latest
docker tag $KURENTO_REGISTRY_URI/kurento/kurento-media-server-dev:latest kurento/kurento-media-server-dev:latest

# coturn image
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/coturn:1.1.0

# svn-client to extract files from svn into a docker host
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/svn-client:1.0.0

# dev-documentation images (for documentation projects)
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/dev-documentation:1.0.0-jdk-7
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/dev-documentation:1.0.0-jdk-8

# dev-media-server images (for media server projects)
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/dev-media-server:trusty-jdk-7
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/dev-media-server:trusty-jdk-8
docker pull $KURENTO_REGISTRY_URI/kurento/dev-media-server:wily-jdk-7
docker pull $KURENTO_REGISTRY_URI/kurento/dev-media-server:wily-jdk-8

# dev-chef image
dogestry pull s3://kurento-docker/?region=eu-west-1 kurento/dev-chef:1.0.0

# Selenium images
echo "Pulling images for selenium version $SELENIUM_VERSION"
docker pull selenium/hub:$SELENIUM_VERSION
for image in (node-chrome node-firefox node-chrome-beta node-chrome-dev node-firefox-beta)
do
	pullAndTag kurento/$image:$SELENIUM_VERSION
	pullAndTag kurento/$image-debug:$SELENIUM_VERSION
	pullAndTag kurento/$image-debug:$SELENIUM_VERSION-dnat
done

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
