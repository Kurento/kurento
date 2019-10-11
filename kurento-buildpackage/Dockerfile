# Package creation tool for Kurento projects - Dockerfile
#
# This Docker image is used to generate Debian packages from Kurento projects.
# It runs [kurento-buildpackage.sh][1] from a properly configured system.
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
# It is possible to pass arguments to the `kurento-buildpackage.sh` script in
# this image: just append them to the `docker run` call.
#
# The argument '--install-kurento <Version>' is strongly recommended, because
# this Docker image doesn't include any build dependencies.
#
# Use '--help' to read about arguments accepted by *kurento-buildpackage*.



ARG UBUNTU_VERSION=xenial

FROM buildpack-deps:${UBUNTU_VERSION}-curl

# Configure environment:
# * DEBIAN_FRONTEND: Disable interactive questions and messages
# * LANG: Set the default locale for all commands
# * PATH: Add the auxiliary scripts to the current PATH
# * PYTHONUNBUFFERED: Disable stdin/stdout/stderr buffering in Python
ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8 \
    PATH="/adm-scripts:${PATH}" \
    PYTHONUNBUFFERED=1

# Configure `apt-get`:
# * Disable installation of recommended and suggested packages
RUN echo 'APT::Install-Recommends "false";' >/etc/apt/apt.conf.d/00recommends \
 && echo 'APT::Install-Suggests "false";' >>/etc/apt/apt.conf.d/00recommends

# Install a basic set of packages
# * build-essential, cmake, pkg-config: For C/C++ based projects
# * Miscellaneous tools that are used by CI scripts
RUN apt-get update && apt-get install --yes \
        build-essential \
        cmake \
        pkg-config \
        \
        gnupg \
        iproute2 \
        zip unzip \
 && rm -rf /var/lib/apt/lists/*

# Install dependencies of 'kurento-buildpackage'
# (listed in the tool's help/header)
RUN apt-get update && apt-get install --yes \
        python3 python3-pip python3-setuptools python3-wheel \
        devscripts \
        dpkg-dev \
        lintian \
        git \
        openssh-client \
        lsb-release \
        equivs \
        coreutils \
 && rm -rf /var/lib/apt/lists/*

# Install 'git-buildpackage'
RUN PIP_VERSION="$(python3 -c 'import pip; print(pip.__version__)')" \
    dpkg --compare-versions "$PIP_VERSION" ge "1.6.0" && ARGS="--no-cache-dir"; \
    pip3 $ARGS install --upgrade gbp==0.9.10

# Configure Git user, which will appear in the Debian Changelog files
# (this is needed by git-buildpackage)
RUN git config --system user.name "kurento-buildpackage" \
 && git config --system user.email "kurento@googlegroups.com"



# ------------ Final configuration ------------

# Configure "apt-get" to keep the cache.
#
# Automatically cleaning the apt cache breaks 'd-devlibdeps' (d-shlibs) < 0.83
# so this workaround is needed:
# https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=916856
#
# Also, this enables persistently caching downloaded packages. This allows
# setting up a persistent cache in an external Docker volume. Useful if you're
# doing lots of (re)builds and want to avoid downloading the same packages
# over and over again...
#
# WARNING:
# If you derive images from this one, you probably want to begin
# your Dockerfile with re-enabling this config file.
RUN sed -i 's|^|//|' /etc/apt/apt.conf.d/docker-clean



# ------------ Runtime command ------------

COPY entrypoint.sh /

ENTRYPOINT ["/entrypoint.sh"]
CMD []
