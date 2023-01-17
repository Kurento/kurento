# dev-integration image to build Java & JS kurento projects
#
# VERSION	6.0.0

FROM	maven:3.3.3-jdk-8
ARG NODE_VERSION
MAINTAINER Patxi Gortázar <patxi.gortazar@gmail.com>

RUN apt-get update -y \
  && apt-get install --no-install-recommends -y -q \
    build-essential \
    ca-certificates \
    curl \
    git \
    mediainfo \
    libmediainfo-dev \
    python \
    python-dev \
    gnupg \
    xmlstarlet \
    jshon \
    python-pip \
    libffi-dev \
    libssl-dev

RUN curl -sL https://deb.nodesource.com/setup_$NODE_VERSION | bash - \
  && apt-get install -y nodejs

ENV PATH $PATH:/nodejs/bin

RUN npm -g install bower

# Install ffmpeg on debian jessie (https://www.deb-multimedia.org/)
# It is not available on official repositories (https://wiki.debian.org/ffmpeg)
RUN echo "deb http://www.deb-multimedia.org jessie main non-free" > /etc/apt/sources.list.d/deb-multimedia.list \
  && apt-get update \
  && apt-get install -y --force-yes deb-multimedia-keyring \
  && apt-get update \
  && apt-get install -y ffmpeg
