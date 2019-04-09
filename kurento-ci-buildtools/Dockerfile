# CI build tools for Kurento projects - Dockerfile
#
# This image is used to run all CI jobs related to Kurento projects. It contains
# all tools needed for the different repositories, such as *git-buildpackage*
# to generate Debian packages, Maven for Java projects, and Node.js for JS.
#
#
# Build command
# -------------
#
# docker build [Args...] --tag kurento/kurento-ci-buildtools:<UbuntuVersion> .
#
#
# Build arguments
# ---------------
#
# --build-arg UBUNTU_VERSION=<UbuntuVersion>
#
#   <UbuntuVersion> is like "xenial", "bionic", etc.
#   Valid values are listed here: https://hub.docker.com/_/buildpack-deps/
#
#   Optional. Default: "xenial".



ARG UBUNTU_VERSION=xenial

FROM kurento/kurento-buildpackage:${UBUNTU_VERSION}

# Install a basic set of packages
# * default-jdk (Java JDK), maven: Used by Java based projects
# * jshon: Used by some JavaScript tools
RUN apt-get update \
 && apt-get install --yes \
        default-jdk \
        maven \
        jshon \
 && apt-get clean && rm -rf /var/lib/apt/lists/*

# Install Node.js 8.x LTS (includes NPM) and Bower
# Used by JavaScript-based projects
RUN curl -sL https://deb.nodesource.com/setup_8.x | bash - \
 && apt-get update \
 && apt-get install --yes \
        nodejs \
 && apt-get clean && rm -rf /var/lib/apt/lists/* \
 && npm -g install bower \
 && npm cache clean --force



# ------------ Runtime command ------------

ENTRYPOINT ["/bin/bash"]
CMD []
