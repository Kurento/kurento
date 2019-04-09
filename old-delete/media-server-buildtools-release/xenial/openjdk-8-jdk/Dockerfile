# Kurento build and packaging tools for Ubuntu 16.04 (Xenial)
#
# This Docker image is used to build and generate Ubuntu packages (*.deb) of
# all repositories related to Kurento Media Server.

FROM buildpack-deps:16.04-curl

# Configure environment:
# * LANG: Set the default locale for all commands
# * DEBIAN_FRONTEND: Disable user-facing questions and messages
# * PYTHONUNBUFFERED: Disable stdin, stdout, stderr buffering in Python
ENV LANG=C.UTF-8 \
    DEBIAN_FRONTEND=noninteractive \
    PYTHONUNBUFFERED=1

# Configure apt-get:
# * Disable installation of recommended and suggested packages
# * Use the Openvidu package proxy
# * Fix issues with Node.js package repo
# * Add Kurento package repository
RUN echo 'APT::Install-Recommends "false";' >/etc/apt/apt.conf.d/00recommends \
 && echo 'APT::Install-Suggests "false";' >>/etc/apt/apt.conf.d/00recommends \
 && echo 'Acquire::http::Proxy "http://proxy.openvidu.io:3142";' >/etc/apt/apt.conf.d/01proxy \
 && echo 'Acquire::HTTP::Proxy::deb.nodesource.com "DIRECT";' >>/etc/apt/apt.conf.d/01proxy \
 && echo 'deb [arch=amd64] http://ubuntu.openvidu.io/6.7.2 xenial kms6' >/etc/apt/sources.list.d/kurento.list \
 && apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83

# Install everything needed:
# * Dependencies of KMS build and packaging scripts:
#   https://doc-kurento.readthedocs.io/en/latest/dev/dev_guide.html#generating-debian-packages
# * Dependencies of other Kurento scripts:
#   - CMake, pkg-config: Used in all C/C++ based repositories
#   - Java JDK, Maven: Used in Kurento Module Creator and other Java modules
#   - Jshon: Used in JavaScript-related modules
# * Miscellaneous tools that are used by several jobs in CI
RUN apt-get update \
 && apt-get install -y \
        build-essential \
        debhelper \
        curl \
        fakeroot \
        flex \
        git openssh-client \
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
        pkg-config \
        openjdk-8-jdk \
        maven \
        jshon \
        \
        gnupg \
        iproute2 \
        zip unzip \
 && apt-get clean && rm -rf /var/lib/apt/lists/*

# Install Node.js 8.x LTS (includes NPM) and Bower
# This is used by all JavaScript-related modules
RUN curl -sL https://deb.nodesource.com/setup_8.x | bash - \
 && apt-get update \
 && apt-get install -y \
        nodejs \
 && apt-get clean && rm -rf /var/lib/apt/lists/* \
 && npm -g install bower \
 && npm cache clean --force
