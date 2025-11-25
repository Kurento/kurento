# dev-integration image to build Java & JS kurento projects with the latest toolchain
#
# VERSION 7.0.0

FROM maven:3.9.11-eclipse-temurin-25

ARG NODE_VERSION=20.x
LABEL maintainer="Kurento <kurento@openvidu.io>"

# Core build dependencies shared by Java and JavaScript projects
RUN apt-get update -y \
  && apt-get install --no-install-recommends -y -q \
    build-essential \
    ca-certificates \
    curl \
    ffmpeg \
    git \
    gnupg \
    jshon \
    libffi-dev \
    libmediainfo-dev \
    libssl-dev \
    mediainfo \
    python3 \
    python3-dev \
    python3-pip \
    rsync \
    xmlstarlet \
  && rm -rf /var/lib/apt/lists/*

# Install Node.js and Bower (version comes from build args for reproducibility)
RUN curl -fsSL "https://deb.nodesource.com/setup_${NODE_VERSION}" | bash - \
  && apt-get update -y \
  && apt-get install --no-install-recommends -y nodejs \
  && npm -g install bower \
  && npm cache clean --force \
  && rm -rf /var/lib/apt/lists/*

# Python tooling used by several CI helper scripts
RUN pip3 install --no-cache-dir \
    awscli \
    boto3 \
    dnspython \
    pyOpenSSL
