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
# docker build [Args...] --tag kurento/kurento-buildpackage:<UbuntuCodename> .
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
#         --install-kurento 6.12.0 \
#         --apt-add-repo \
#         [kurento-buildpackage arguments...]
#
#
# Run arguments
# -------------
#
# It is possible to pass arguments to the `kurento-buildpackage.sh` script in
# this image: just append them to the `docker run` call.
#
# It is strongly recommended to use '--install-kurento' and '--apt-add-repo',
# because this Docker image doesn't include any build dependencies or repos.
#
# Use '--help' to read about arguments accepted by *kurento-buildpackage*.

ARG UBUNTU_CODENAME=xenial

FROM ubuntu:${UBUNTU_CODENAME}

ARG UBUNTU_CODENAME

# Configure environment:
# * DEBIAN_FRONTEND: Disable Apt interactive questions and messages
# * PYTHONUNBUFFERED: Disable Python stdin/stdout/stderr buffering
# * LANG: Set the default locale for all commands
# * PATH: Add the auxiliary scripts to the current PATH
ENV DEBIAN_FRONTEND=noninteractive \
    PYTHONUNBUFFERED=1 \
    LANG=C.UTF-8 \
    PATH="/adm-scripts:${PATH}"

# Configure Apt:
# * Disable installation of recommended and suggested packages
RUN echo 'APT::Install-Recommends "false";' >/etc/apt/apt.conf.d/00recommends \
 && echo 'APT::Install-Suggests "false";' >>/etc/apt/apt.conf.d/00recommends

# Install a basic set of packages
# * gnupg, iproute2, zip: Tools used by various CI scripts.
RUN apt-get update && apt-get install --no-install-recommends --yes \
        gnupg \
        iproute2 \
        zip unzip \
 && rm -rf /var/lib/apt/lists/*

# Install an extra set of packages.
# These are basic build dependencies that end up beind required by most jobs.
RUN apt-get update && apt-get install --no-install-recommends --yes \
        build-essential \
        libglib2.0-dev \
        wget \
 && rm -rf /var/lib/apt/lists/*

# Install dependencies of the `kurento-buildpackage` build script
# (listed in the script header).
RUN apt-get update && apt-get install --no-install-recommends --yes \
        python3 python3-pip python3-setuptools python3-wheel \
        devscripts \
        dpkg-dev \
        lintian \
        git \
        openssh-client \
        equivs \
        coreutils \
 && rm -rf /var/lib/apt/lists/*

# Configure Git user, which will appear in the Debian Changelog files
# (this is required by Git-BuildPackage)
RUN git config --system user.name "Kurento" \
 && git config --system user.email "kurento@openvidu.io" \
 && git config --global --add safe.directory '*'

# Install the Git-BuildPackage tool for creation of .deb files.
RUN if [ "$UBUNTU_CODENAME" = "xenial" ]; then \
        apt-get purge --auto-remove --yes python3-pip \
        && wget -O /tmp/get-pip.py https://bootstrap.pypa.io/pip/3.5/get-pip.py \
        && python3 /tmp/get-pip.py \
        && python3 -m pip install --upgrade nosexcover \
        && python3 -m pip install --upgrade --use-deprecated=legacy-resolver gbp==0.9.10 ; \
    else \
        python3 -m pip install --upgrade gbp ; \
    fi

# APT_KEEP_CACHE
#
# By default, Docker images based on "ubuntu" automatically clean the Apt
# package cache. However, this breaks 'd-devlibdeps' (d-shlibs) < 0.83 so a
# workaround is needed: https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=916856
#
# Also, for this image it makes sense to have a persistent cache of downloaded
# packages between runs. The user might want to set up the cache in an external
# volume or bind mount. Useful if you're doing lots of (re)builds and want to
# avoid downloading the same packages over and over again...
#
# NOTE: If you derive images from this one, you probably want to set this
# env variable again to "false".
ENV APT_KEEP_CACHE="true"

COPY entrypoint.sh /

ENTRYPOINT ["/entrypoint.sh"]
CMD []
