# Kurento Media Server / Pre-Release / Ubuntu 16.04 (Xenial)
#
# This Docker image is used to run a Pre-Release instance of Kurento Media Server.
# JSON-RPC interface is exposed on port 8888.

FROM ubuntu:16.04

MAINTAINER Patxi Gortázar <patxi.gortazar@gmail.com>
MAINTAINER Fede Diaz <nordri@gmail.com>
MAINTAINER Juan Navarro <juan.navarro@gmx.es>

# Configure environment
# * LANG: Set the default locale for all commands
# * DEBIAN_FRONTEND: Disable user-facing questions and messages
ENV LANG=C.UTF-8 \
    DEBIAN_FRONTEND=noninteractive

# Configure apt-get
# * Disable installation of recommended and suggested packages
# * Add Kurento package repository
RUN echo 'APT::Install-Recommends "false";' >/etc/apt/apt.conf.d/00recommends \
 && echo 'APT::Install-Suggests "false";' >>/etc/apt/apt.conf.d/00recommends \
 && echo 'deb [arch=amd64] http://ubuntu.openvidu.io/dev xenial kms6' >/etc/apt/sources.list.d/kurento.list \
 && apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83

# Install everything needed
# * Kurento Media Server core packages
# * Additional modules
# * [FIXME] OpenH264 dependency: wget
#   Remove this when the openh264 package is released with a fix
RUN apt-get update \
 && apt-get install -y \
        kurento-media-server \
        \
        kms-chroma \
        kms-crowddetector \
        kms-platedetector \
        kms-pointerdetector \
        \
        wget \
 && apt-get clean && rm -rf /var/lib/apt/lists/*

EXPOSE 8888

# Configure environment for KMS
# * Use default suggested logging levels:
#   https://doc-kurento.readthedocs.io/en/latest/features/logging.html#suggested-levels
# * Disable colors in debug logs
ENV GST_DEBUG="3,Kurento*:4,kms*:4,sdp*:4,webrtc*:4,*rtpendpoint:4,rtp*handler:4,rtpsynchronizer:4,agnosticbin:4" \
    GST_DEBUG_NO_COLOR=1

COPY ./entrypoint.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
