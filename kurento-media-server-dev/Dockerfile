FROM ubuntu:16.04
MAINTAINER kurento team

RUN apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83 && \
    echo "deb [arch=amd64] http://ubuntu.openvidu.io/dev xenial kms6" > /etc/apt/sources.list.d/kurento.list && \
    apt-get update && \
    apt-get install -y kurento-media-server \
    kms-chroma \
    kms-crowddetector \
    kms-platedetector \
    kms-pointerdetector

EXPOSE 8888

COPY ./entrypoint.sh /entrypoint.sh

ENV GST_DEBUG=Kurento*:5

ENTRYPOINT ["/entrypoint.sh"]
