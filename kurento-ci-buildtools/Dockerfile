# CI build tools for Kurento projects - Dockerfile
#
# This Docker image is used to run all CI jobs related to Kurento projects. It
# contains all tools needed for the different repositories, such as
# [git-buildpackage][1] to generate Debian packages, Maven for Java projects,
# and Node.js for JavaScript projects.
#
# [1]: https://hub.docker.com/r/kurento/kurento-buildpackage
#
#
# Build command
# -------------
#
# docker build [Args...] --tag kurento/kurento-ci-buildtools:<UbuntuCodename> .
#
#
# Build arguments
# ---------------
#
# --build-arg UBUNTU_CODENAME=<UbuntuCodename>
#
#   <UbuntuCodename> is like "xenial", "bionic", etc.
#
#   Optional. Default: "xenial".

ARG UBUNTU_CODENAME=xenial

FROM kurento/kurento-buildpackage:${UBUNTU_CODENAME}

# Settings from parent image:
# Re-enable the cleaning of Apt package cache
# (in case you end up using the parent's entrypoint.sh)
ENV APT_KEEP_CACHE="false"

# Install Node, NPM, and Bower (fox JavaScript-based projects)
# - Tested Node version: 8.x (10.x might introduce breaking changes)
# - Tested NPM version: 6.14.5 (newer versions should probably work fine)
RUN apt-get update && apt-get install --no-install-recommends --yes \
        wget \
 && wget --no-verbose -O - https://deb.nodesource.com/setup_8.x | bash - \
 && apt-get update && apt-get install --no-install-recommends --yes \
        nodejs \
 && rm -rf /var/lib/apt/lists/* \
 && npm install -g npm@6.14.5 \
 && npm install -g bower \
 && npm cache clean --force

# Install packages
# * build-essential: For C/C++ Kurento modules that generate client SDK files
# * curl: For CI script kurento_http_publish.sh
# * default-jdk (Java JDK): For Java projects
# * maven: For Java projects
# * jq, jshon: For some JavaScript tools and jobs
# * rsync: For remote copies
#
# Tools used by kurento-java/kurento-integration-tests/kurento-test:
# * ffmpeg: For different integration testst that generate an RTP stream
# * libmediainfo-dev: MediaInfoLibrary class
# * netcat: KmsService class, to check if KMS port is already in use
RUN apt-get update && apt-get install --no-install-recommends --yes \
        build-essential \
        curl \
        default-jdk \
        maven \
        jq jshon \
        rsync \
        ffmpeg \
        libmediainfo-dev \
        netcat-openbsd \
 && rm -rf /var/lib/apt/lists/*

ENTRYPOINT ["/bin/bash"]
CMD []
