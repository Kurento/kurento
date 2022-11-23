# ====================
# Kurento Media Server
# ====================
#
# This Docker image is used to run an instance of Kurento Media Server.
#
#
#
# Build Command
# =============
#
# Run:
#
#     docker build [Args...] --tag kurento/kurento-media-server:latest .
#
#
#
# Build Arguments
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
#   <KmsVersion> is like "6.12.0".
#   Alternatively, "dev" is used to build a nightly version of Kurento.
#
#   Optional. Default: "dev".
#
# --build-arg APT_ARGS=<AptArguments>
#
#   <AptArguments> is a string with arguments that will be passed to all
#   executions of `apt-get`.
#
#   Example: To Use an Apt package proxy
#   Doc: http://manpages.ubuntu.com/manpages/bionic/en/man1/apt-transport-http.1.html#options
#
#       APT_ARGS='-o Acquire::http::Proxy=http://user:pass@host:port/'
#
#   Optional. Default: None.
#
#
#
# Run Command
# ===========
#
# Run:
#
#     docker run --name kurento --network host kurento/kurento-media-server:latest
#
# Then, you can follow the logs with the `docker logs` command:
#
#     docker logs --follow kms >"kms-$(date '+%Y%m%dT%H%M%S').log" 2>&1



# Global arguments for FROM.
ARG UBUNTU_CODENAME="xenial"



# Stage: Base system configuration
# ================================

FROM ubuntu:${UBUNTU_CODENAME} AS ubuntu_base

ARG APT_ARGS=""



# Stage: Install and prepare
# ==========================

FROM ubuntu_base

LABEL maintainer="Juan Navarro <juan.navarro@gmx.es>"

ARG UBUNTU_CODENAME

ARG KMS_VERSION="dev"

# Configure environment:
# * LANG: Set the default locale for all commands.
ENV LANG="C.UTF-8"

# Install tools:
# * ca-certificates: For HTTPS requests (e.g. with PlayerEndpoint).
# * curl: For `healthchecker.sh`.
# * dnsutils: For `dig` in `getmyip.sh`.
# * gnupg: For `apt-key adv` (since Ubuntu 18.04).
# * nano: Not required but useful for edits while troubleshooting.
RUN apt-get $APT_ARGS update && apt-get $APT_ARGS install --no-install-recommends --yes \
        ca-certificates \
        curl \
        dnsutils \
        gnupg \
        nano \
 && rm -rf /var/lib/apt/lists/*

# Add Kurento Apt package repository.
RUN apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 234821A61B67740F89BFD669FC8A16625AFA7A83 \
 && echo "deb [arch=amd64] http://ubuntu.openvidu.io/${KMS_VERSION} ${UBUNTU_CODENAME} kms6" >/etc/apt/sources.list.d/kurento.list

# Install Kurento Media Server.
RUN apt-get $APT_ARGS update && apt-get $APT_ARGS install --no-install-recommends --yes \
        kurento-media-server \
 && rm -rf /var/lib/apt/lists/*

# Install additional modules.
# These might not be all available, so install separately and allow errors.
RUN apt-get $APT_ARGS update && \
    for PACKAGE in \
        kms-chroma \
        kms-crowddetector \
        kms-datachannelexample \
        kms-markerdetector \
        kms-platedetector \
        kms-pointerdetector ; \
    do \
        apt-get $APT_ARGS install --no-install-recommends --yes "$PACKAGE" || true ; \
    done \
 && rm -rf /var/lib/apt/lists/*

# Install additional tools that are indirectly used by some GStreamer plugins:
# * gstreamer-tools: Allows running the command `gst-inspect`.
#   Useful for troubleshooting and debugging installed GStreamer plugins.
# * gstreamer-x: Video rendering plugins for X11 and Pango.
#   Needed by some overlay elements like "textoverlay".
RUN apt-get $APT_ARGS update \
 && apt-get $APT_ARGS install --no-install-recommends --yes \
        gstreamer1.5-tools \
        gstreamer1.5-x \
 || apt-get $APT_ARGS install --no-install-recommends --yes \
        gstreamer1.0-tools \
        gstreamer1.0-x \
 && rm -rf /var/lib/apt/lists/*

# Install debug symbols.
RUN apt-get $APT_ARGS update && apt-get $APT_ARGS install --no-install-recommends --yes \
        ubuntu-dbgsym-keyring \
 || { \
    apt-key adv --keyserver keyserver.ubuntu.com --recv-keys F2EDC64DC5AEE1F6B9C621F0C8CAB6595FDFF622 \
    && echo "deb http://ddebs.ubuntu.com ${UBUNTU_CODENAME} main restricted universe multiverse" >/etc/apt/sources.list.d/ddebs.list \
    && echo "deb http://ddebs.ubuntu.com ${UBUNTU_CODENAME}-updates main restricted universe multiverse" >>/etc/apt/sources.list.d/ddebs.list \
    && apt-get $APT_ARGS update && apt-get $APT_ARGS install --no-install-recommends --yes \
        kurento-dbg ; \
 } \
 && rm -rf /var/lib/apt/lists/*

# Expose default Kurento RPC control port.
EXPOSE 8888

COPY entrypoint.sh /
COPY getmyip.sh /
COPY healthchecker.sh /

HEALTHCHECK --start-period=15s --interval=30s --timeout=3s --retries=1 CMD /healthchecker.sh

ENTRYPOINT ["/entrypoint.sh"]
