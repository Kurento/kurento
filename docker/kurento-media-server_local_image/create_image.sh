#!/bin/bash
#===============================================================================
#
#          FILE:  create_image.sh
# 
#         USAGE:  ./create_image.sh
# 
#   DESCRIPTION:  Kurento Media Server docker image creation script
# 
#===============================================================================

KMS_VERSION=7.1.2-dev
KMS_VERSION_BUILD_ARG=$(echo $KMS_VERSION | sed 's/-dev$//')

docker build --build-context sources=../../ --build-arg KMS_VERSION=$KMS_VERSION_BULD_ARG -t kurento/kurento-media-server:$KMS_VERSION .