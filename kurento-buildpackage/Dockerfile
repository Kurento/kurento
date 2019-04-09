# Debian package builder for Kurento projects - Dockerfile
#
# This image is used to generate Debian packages from Kurento repositories.
# It runs *kurento-buildpackage* [1] from a properly configured system.
#
# [1]: https://github.com/Kurento/adm-scripts/blob/master/kurento-buildpackage.sh
#
#
# Build command
# -------------
#
# docker build [Args...] --tag kurento/kurento-buildpackage:<UbuntuVersion> .
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
#
#
# Run command
# -----------
#
# git clone https://github.com/Kurento/kms-core.git
# cd kms-core/
# docker run --rm \
#     --mount type=bind,src="$PWD",dst=/hostdir \
#     kurento/kurento-buildpackage:xenial \
#     --install-kurento nightly \
#     [RunArguments...]
#
#
# Run arguments
# -------------
#
# It is possible to pass arguments to the *kurento-buildpackage* script: just
# append them to the `docker run` call.
#
# The argument '--install-kurento <Version>' is strongly recommended, because
# this Docker image doesn't include any build dependencies.
#
# Use '--help' to read about arguments accepted by *kurento-buildpackage*.



ARG UBUNTU_VERSION=xenial

FROM buildpack-deps:${UBUNTU_VERSION}-curl

# Configure environment:
# - DEBIAN_FRONTEND: Disable user-facing questions and messages
# - LANG: Set the default locale for all commands
# - PATH: Add the auxiliary scripts to the current PATH
# - PYTHONUNBUFFERED: Disable stdin, stdout, stderr buffering in Python
ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8 \
    PATH="/adm-scripts:${PATH}" \
    PYTHONUNBUFFERED=1

# Configure apt-get: disable installation of recommended and suggested packages
RUN echo 'APT::Install-Recommends "false";' >/etc/apt/apt.conf.d/00recommends \
 && echo 'APT::Install-Suggests "false";' >>/etc/apt/apt.conf.d/00recommends

# Install a basic set of packages
# * build-essential, cmake, pkg-config: Used by C/C++ based projects
# * Miscellaneous tools that are used by CI scripts
RUN apt-get update \
 && apt-get install --yes \
        build-essential \
        cmake \
        pkg-config \
        \
        gnupg \
        iproute2 \
        zip unzip \
 && apt-get clean && rm -rf /var/lib/apt/lists/*

# Install dependencies of 'kurento-buildpackage'
# (listed in the tool's help/header)
RUN apt-get update \
 && apt-get install --yes \
        python3 python3-pip python3-setuptools python3-wheel \
        devscripts \
        dpkg-dev \
        lintian \
        git \
        openssh-client \
        lsb-release \
        equivs \
        coreutils \
 && apt-get clean && rm -rf /var/lib/apt/lists/*

# Install 'git-buildpackage'
RUN PIP_VERSION="$(python3 -c 'import pip; print(pip.__version__)')" \
    dpkg --compare-versions "$PIP_VERSION" ge "1.6.0" && ARGS="--no-cache-dir"; \
    pip3 $ARGS install --upgrade gbp==0.9.10

# Prepare workspace:
# - Configure Git user, which will appear in the Debian Changelog files
# - Create directories where the host will mount its file system
RUN git config --global user.name "Kurento CI" \
 && git config --global user.email "kurento@googlegroups.com"



# ------------ Final configuration ------------

# Configure apt-get: enable caching of downloaded packages
# Allows setting up a persistent cache in an external Docker volume
RUN rm -f /etc/apt/apt.conf.d/docker-clean



# ------------ Runtime command ------------

COPY entrypoint.sh /

ENTRYPOINT ["/entrypoint.sh"]
CMD []
