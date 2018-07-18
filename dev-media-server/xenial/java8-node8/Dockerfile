#
# Kurento CI Dockerfile for Ubuntu 16.04 (Xenial)
#

FROM buildpack-deps:xenial-scm

# Configure default locale to UTF-8
ENV LANG C.UTF-8

# Configure system
COPY files /

# Configure Apt keys
RUN apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83

#
# Install required packages
# This list is divided into 3 sections:
# 1. Miscellaneous tools that are used by several jobs in CI.
# 2. Dependencies of our build tools and packaging scripts.
#    Source: https://doc-kurento.readthedocs.io/en/latest/dev/dev_guide.html#generating-debian-packages
# 3. Extra build tools that are used by some CI scripts:
#    - jshon: used by mavenize_js_project, npm_publish.
#
RUN apt-get update && apt-get install -y \
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
    openjdk-8-jdk \
    pkg-config \
    jshon

# Install Node.js 8.x LTS + NPM + Bower
RUN curl -sL https://deb.nodesource.com/setup_8.x | bash - \
 && apt-get install -y nodejs \
 && npm -g install bower

#
# Clean the house
#
# Note: "Best practices for writing Dockerfiles"
# states that official base images already do an apt-get cleanup:
# https://docs.docker.com/v17.12/develop/develop-images/dockerfile_best-practices/#run
#
#   > Official Debian and Ubuntu images automatically run `apt-get clean`, so
#   > explicit invocation is not required.
#
# We still do our own cleanup, just to be sure.
#
RUN apt-get clean \
 && rm -rf \
    /var/lib/apt/lists/* \
    /var/cache/apt/archives/*.deb \
    /var/cache/apt/archives/partial/*.deb \
    /var/cache/apt/*.bin \
    /tmp/*
