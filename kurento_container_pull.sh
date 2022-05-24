#!/usr/bin/env bash

# This script is run every night by the Ansible job in Jenkins,
# and its purpose is to check that all mentioned Docker images do actually
# exist and are available for being pulled from the Hub.



# Shell setup
# -----------

BASEPATH="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"  # Absolute canonical path
# shellcheck source=bash.conf.sh
source "$BASEPATH/bash.conf.sh" || exit 1

log "==================== BEGIN ===================="

# Trace all commands
set -o xtrace



# Internal (private) images
[ -n "$SELENIUM_VERSION" ] || SELENIUM_VERSION="3.14.0"

#  Remove dangling docker images
RMI_IMAGES="$(docker images --quiet --filter "dangling=true")"
[[ -n "$RMI_IMAGES" ]] && docker rmi $RMI_IMAGES

# dev-integration images (for Java & JS)
NODE_VERSIONS="0.12 4.x 6.x"
for NODE_VERSION in $NODE_VERSIONS; do
    docker pull kurento/dev-integration:jdk-8-node-$NODE_VERSION
done

docker pull kurento/dev-integration-browser:$SELENIUM_VERSION-node-8.x

# kurento-media-server development version with core dump & public modules
docker pull kurento/kurento-media-server:dev

# coturn image
docker pull kurento/coturn:latest

# svn-client to extract files from svn into a docker host
docker pull kurento/svn-client:1.0.0

# dev-documentation images (for documentation projects)
docker pull kurento/dev-documentation:1.0.0-jdk-7
docker pull kurento/dev-documentation:1.0.0-jdk-8

# Selenium images
echo "Pulling images for selenium version $SELENIUM_VERSION"
docker pull selenium/base:$SELENIUM_VERSION
docker pull selenium/node-base:$SELENIUM_VERSION
docker pull selenium/hub:$SELENIUM_VERSION

for image in node-chrome node-firefox node-chrome-beta node-chrome-dev node-firefox-beta; do
    docker pull kurento/$image:latest
    docker pull kurento/$image-debug:latest
done

# Image to record vnc sessions
docker pull softsam/vncrecorder:latest

# Mongo image
docker pull mongo:2.6.11

echo "Generating report"
docker images > container_images.txt

# Keep just KEEP_IMAGES last kms dev images
KEEP_IMAGES=3
NUM_IMAGES=$(docker images | grep kurento-media-server:dev | awk '{print $2}' | sort | uniq | wc -l)
if [ $NUM_IMAGES -gt $KEEP_IMAGES ]; then
    NUM_REMOVE_IMAGES=$[$NUM_IMAGES-$KEEP_IMAGES]
    REMOVE_IMAGES=$(docker images | grep kurento-media-server:dev | awk '{print $2}' | sort | uniq | head -$NUM_REMOVE_IMAGES)
    status=0
    for image in $REMOVE_IMAGES; do
        for repo_name in $(docker images | grep kurento-media-server:dev | grep -P "\s$image\s" | awk '{print $1}'); do
            echo "Removing image $image"
            docker rmi $repo_name:$image || status=$[$status || $?]
        done
    done
fi



log "==================== END ===================="

exit ${status:-0}
