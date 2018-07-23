#
# Kurento CI Dockerfile for Ubuntu 14.04 (Trusty)
#

FROM buildpack-deps:trusty-scm

# Default to UTF-8 file encoding
ENV LANG C.UTF-8

#
# Configure Apt:
# - General apt-get settings
# - Kurento package proxy
# - Kurento package repository
#
ENV DEBIAN_FRONTEND noninteractive
RUN echo 'APT::Install-Recommends "false";' > /etc/apt/apt.conf.d/00recommends \
 && echo 'APT::Install-Suggests "false";' >> /etc/apt/apt.conf.d/00recommends \
 && echo 'Acquire::http::Proxy "http://proxy.openvidu.io:3142";' > /etc/apt/apt.conf.d/01proxy \
 && echo 'Acquire::HTTP::Proxy::deb.nodesource.com "DIRECT";' >> /etc/apt/apt.conf.d/01proxy \
 && echo 'deb http://ubuntu.openvidu.io/dev trusty kms6' > /etc/apt/sources.list.d/kurento.list \
 && apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83

#
# Install additional packages
# This list is divided into 3 sections:
# 1. Miscellaneous tools that are used by several jobs in CI.
# 2. Dependencies of our build tools and packaging scripts.
#    Source: https://doc-kurento.readthedocs.io/en/latest/dev/dev_guide.html#generating-debian-packages
# 3. Extra build tools that are used by some CI scripts.
#    - *maven-debian-helper* installs an old Maven version in Ubuntu 14.04,
#      and it may end up being installed instead of *maven*. By adding '-' to
#      its name we ensure that this doesn't happen and we get Maven 3.0.5.
#    - *jshon*: used by mavenize_js_project, npm_publish.
#
RUN apt-get update \
  && apt-get install -y \
    gnupg \
    iproute2 \
    zip unzip \
    \
    build-essential \
    debhelper \
    curl \
    fakeroot \
    flex \
    git \
    libcommons-validator-java \
    python \
    python-apt \
    python-debian \
    python-git \
    python-requests \
    python-yaml \
    subversion \
    wget \
    \
    cmake \
    maven \
    maven-debian-helper- \
    openjdk-7-jdk \
    pkg-config \
    jshon

# Install Node.js 8.x LTS + NPM + Bower
RUN curl -sL https://deb.nodesource.com/setup_8.x | bash - \
 && apt-get install -y nodejs \
 && npm -g install bower

# Clean the house
RUN apt-get clean \
 && rm -rf /tmp/* \
 && rm -rf /var/lib/apt/lists/*
