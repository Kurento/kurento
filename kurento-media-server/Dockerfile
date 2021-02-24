# Kurento Media Server
#
# This Docker image is used to run an instance of Kurento Media Server.
#
#
# Build command
# -------------
#
# docker build [Args...] --tag kurento/kurento-media-server:latest .
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
# --build-arg KMS_VERSION=<KmsVersion>
#
#   <KmsVersion> is like "6.7.2", "6.9.0", etc.
#   Alternatively, "dev" is used to build a nightly version of KMS.
#
#   Optional. Default: "dev".
#
#
# Run command
# -----------
#
# docker run --name kms -p 8888:8888 kurento/kurento-media-server:latest
#
# Then, you can follow the logs with the `docker logs` command:
#
# docker logs --follow kms >"kms-$(date '+%Y%m%dT%H%M%S').log" 2>&1

ARG UBUNTU_CODENAME="xenial"

FROM ubuntu:${UBUNTU_CODENAME}

MAINTAINER Patxi Gortázar <patxi.gortazar@gmail.com>
MAINTAINER Fede Diaz <nordri@gmail.com>
MAINTAINER Juan Navarro <juan.navarro@gmx.es>

ARG UBUNTU_CODENAME
ARG KMS_VERSION="dev"

# Configure environment:
# * DEBIAN_FRONTEND: Disable Apt interactive questions and messages
# * LANG: Set the default locale for all commands
ENV DEBIAN_FRONTEND="noninteractive" \
    LANG="C.UTF-8"

# Install required tools:
# * curl: For `healthchecker.sh`
# * dnsutils: For `dig` in `getmyip.sh`
# * gnupg: For `apt-key adv` (since Ubuntu 18.04)
RUN apt-get -q update && apt-get -q install --no-install-recommends --yes \
        curl \
        dnsutils \
        gnupg \
 && rm -rf /var/lib/apt/lists/*

# Configure Apt:
# * Disable installation of recommended and suggested packages
# * Add Kurento package repository
RUN echo 'APT::Install-Recommends "false";' >/etc/apt/apt.conf.d/00recommends \
 && echo 'APT::Install-Suggests "false";' >>/etc/apt/apt.conf.d/00recommends \
 && echo "UBUNTU_CODENAME=${UBUNTU_CODENAME}" \
 && echo "KMS_VERSION=${KMS_VERSION}" \
 && apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83 \
 && APT_LIST="deb [arch=amd64] http://ubuntu.openvidu.io/${KMS_VERSION} ${UBUNTU_CODENAME} kms6" \
 && echo "Apt source line: $APT_LIST" \
 && echo "$APT_LIST" >/etc/apt/sources.list.d/kurento.list

# Install Kurento Media Server
RUN apt-get -q update && apt-get -q install --no-install-recommends --yes \
        kurento-media-server \
 && rm -rf /var/lib/apt/lists/*

# Install additional modules
# These might not be available, so allow errors
RUN apt-get -q update && apt-get -q install --no-install-recommends --yes \
        kms-chroma || true \
 && rm -rf /var/lib/apt/lists/*
RUN apt-get -q update && apt-get -q install --no-install-recommends --yes \
        kms-crowddetector || true \
 && rm -rf /var/lib/apt/lists/*
RUN apt-get -q update && apt-get -q install --no-install-recommends --yes \
        kms-platedetector || true \
 && rm -rf /var/lib/apt/lists/*
RUN apt-get -q update && apt-get -q install --no-install-recommends --yes \
        kms-pointerdetector || true \
 && rm -rf /var/lib/apt/lists/*

# Install debug symbols
RUN apt-key adv \
        --keyserver keyserver.ubuntu.com \
        --recv-keys F2EDC64DC5AEE1F6B9C621F0C8CAB6595FDFF622 \
 && echo "deb http://ddebs.ubuntu.com ${UBUNTU_CODENAME} main restricted universe multiverse" >/etc/apt/sources.list.d/ddebs.list \
 && echo "deb http://ddebs.ubuntu.com ${UBUNTU_CODENAME}-updates main restricted universe multiverse" >>/etc/apt/sources.list.d/ddebs.list \
 && apt-get -q update \
 && apt-get -q install --no-install-recommends --yes \
        kurento-dbg \
 && rm -rf /var/lib/apt/lists/*

# Configure environment for KMS. All of these can be overriden with `docker run`.
# * Disable color in debug logs
# * Provide default empty values for all environment variables supported by
#   the entrypoint script. This acts more as documentation than really a need.
ENV GST_DEBUG_NO_COLOR=1 \
    GST_DEBUG="" \
    KMS_MTU="" \
    KMS_EXTERNAL_IPV4="" \
    KMS_EXTERNAL_IPV6="" \
    KMS_NETWORK_INTERFACES="" \
    KMS_STUN_IP="" \
    KMS_STUN_PORT="" \
    KMS_TURN_URL=""

# Expose default Kurento RPC control port
EXPOSE 8888

COPY entrypoint.sh /
COPY getmyip.sh /
COPY healthchecker.sh /

HEALTHCHECK --start-period=15s --interval=30s --timeout=3s --retries=1 CMD /healthchecker.sh

ENTRYPOINT ["/entrypoint.sh"]
