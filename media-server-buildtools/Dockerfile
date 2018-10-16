# Build tools for Kurento projects - Dockerfile
#
# This image is used to test Debian packaging of Kurento repositories.
#
# Build:
#     docker build --tag kurento-buildtools .
#
# Optional build arguments:
#     --build-arg UBUNTU_VERSION=<Version>
#       <Version> is like "14.04", "16.04", etc. Valid values are listed here:
#       https://hub.docker.com/_/buildpack-deps/
#       Default: "16.04"
#
# Usage:
#     git clone https://github.com/Kurento/adm-scripts.git
#     git clone https://github.com/Kurento/kms-core.git
#     docker run --rm \
#         --mount type=bind,source="$PWD/adm-scripts",target=/adm-scripts \
#         --mount type=bind,source="$PWD/kms-core",target=/build \
#         kurento-buildtools  [Args...]
#
# About [Args...]:
#     It is possible to pass arguments to the build script: just append them
#     to the `docker run` call. 'adm-scripts/kurento-buildpackage.sh' will be
#     run as the Docker's ENTRYPOINT.
#     For information about available arguments, use '--help'.

ARG UBUNTU_VERSION=16.04

FROM buildpack-deps:${UBUNTU_VERSION}-curl

# Configure environment:
# - LANG: Set the default locale for all commands
# - DEBIAN_FRONTEND: Disable user-facing questions and messages
# - PATH: Add the auxiliary scripts to the current PATH
# - PYTHONUNBUFFERED: Disable stdin, stdout, stderr buffering in Python
ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8 \
    PATH="/adm-scripts:${PATH}" \
    PYTHONUNBUFFERED=1

# Configure apt-get:
# - Disable installation of recommended and suggested packages
RUN echo 'APT::Install-Recommends "false";' >/etc/apt/apt.conf.d/00recommends \
 && echo 'APT::Install-Suggests "false";' >>/etc/apt/apt.conf.d/00recommends

# Install a basic set of packages
# * Tools will get installed anyway in most cases:
#   - build-essential, cmake, pkg-config: Used by C/C++ based projects
#   - default-jdk (Java JDK), maven: Used by Java based projects
#   - jshon: Used by some Javascript tools
# * Miscellaneous tools that are used by several jobs in CI
RUN apt-get update \
 && apt-get install -y \
        build-essential \
        cmake \
        pkg-config \
        default-jdk \
        maven \
        jshon \
        \
        gnupg \
        iproute2 \
        zip unzip \
 && apt-get clean && rm -rf /var/lib/apt/lists/*

# Install Node.js 8.x LTS (includes NPM) and Bower
# Used by Javascript based projects
RUN curl -sL https://deb.nodesource.com/setup_8.x | bash - \
 && apt-get update \
 && apt-get install -y \
        nodejs \
 && apt-get clean && rm -rf /var/lib/apt/lists/* \
 && npm -g install bower \
 && npm cache clean --force

# Install dependencies of 'kurento-buildpackage.sh':
# * git-buildpackage
#   - Python 3 (pip, setuptools, wheel)
#   - debuild (package 'devscripts')
#     - dpkg-buildpackage (package 'dpkg-dev')
#     - lintian
#   - git
#     - openssh-client (for SSH access)
# * lsb-release
# * mk-build-deps (package 'devscripts')
#   - equivs
# * nproc (package 'coreutils')
RUN apt-get update \
 && apt-get install -y \
        python3 python3-pip python3-setuptools python3-wheel \
        devscripts \
        dpkg-dev \
        lintian \
        git \
        openssh-client \
        \
        lsb-release \
        equivs \
        coreutils \
 && apt-get clean && rm -rf /var/lib/apt/lists/*

# Install 'git-buildpackage'
RUN PIP_VERSION="$(python3 -c 'import pip; print(pip.__version__)')" \
    dpkg --compare-versions "$PIP_VERSION" ge "1.6.0" && ARGS="--no-cache-dir"; \
    pip3 $ARGS install --upgrade gbp

# Prepare workspace:
# - Configure Git user, which will appear in the Debian Changelog files
# - Create directories where the host will mount its file system
RUN git config --global user.name "Kurento CI" \
 && git config --global user.email "kurento@googlegroups.com" \
 && mkdir /adm-scripts \
 && mkdir /build



# ------------ Runtime command ------------

COPY entrypoint.sh /

ENTRYPOINT ["/entrypoint.sh"]
CMD []
