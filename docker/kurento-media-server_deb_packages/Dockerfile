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
#   <UbuntuCodename> is like "focal".
#
#   Optional. Default: "focal".
#
# --build-arg KMS_VERSION=<KmsVersion>
#
#   <KmsVersion> is like "7.0.0".
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
#   Doc: http://manpages.ubuntu.com/manpages/man1/apt-transport-http.1.html#options
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
ARG UBUNTU_CODENAME="noble"



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
# Run apt-get/dpkg without interactive dialogue.
ARG DEBIAN_FRONTEND=noninteractive
# Set the default locale for all commands.
ENV LANG="C.UTF-8"

# Copy .deb artifacts for deploying
RUN mkdir /kurento-deb

COPY kurento-cmake-utils_7.0.2-1kurento1.24.04~20240627105042.gitd9d81c2eb.24.04~20240628163822.24.04~20240628163903_all.deb /kurento-deb/kurento-cmake-utils_7.0.2-1kurento1.24.04.deb
COPY kurento-jsonrpc_7.0.2-1kurento1.24.04~20240628170923_amd64.deb /kurento-deb/kurento-jsonrpc_7.0.2-1kurento1.24.04.deb
COPY kurento-jsonrpc-dbgsym_7.0.2-1kurento1.24.04~20240628170923_amd64.ddeb /kurento-deb/kurento-jsonrpc-dbgsym_7.0.2-1kurento1.24.04.ddeb
COPY kurento-module-creator_7.0.2-1kurento1.24.04~20240628171120_all.deb /kurento-deb/kurento-module-creator_7.0.2-1kurento1.24.04.deb
COPY kurento-module-core_7.0.2-1kurento1.24.04~20240628171316_amd64.deb /kurento-deb/kurento-module-core_7.0.2-1kurento1.24.04.deb
COPY kurento-module-core-dbgsym_7.0.2-1kurento1.24.04~20240628171316_amd64.ddeb /kurento-deb/kurento-module-core-dbgsym_7.0.2-1kurento1.24.04.ddeb
COPY kurento-module-elements_7.0.2-1kurento1.24.04~20240628171709.24.04~20240628172907.24.04~20240628173507.24.04~20240628173909.24.04~20240629111357_amd64.deb /kurento-deb/kurento-module-elements_7.0.2-1kurento1.24.04.deb
COPY kurento-module-elements-dbgsym_7.0.2-1kurento1.24.04~20240628171709.24.04~20240628172907.24.04~20240628173507.24.04~20240628173909.24.04~20240629111357_amd64.ddeb /kurento-deb/kurento-module-elements-dbgsym_7.0.2-1kurento1.24.04.ddeb
COPY kurento-module-filters_7.0.2-1kurento1.24.04~20240629111717_amd64.deb /kurento-deb/kurento-module-filters_7.0.2-1kurento1.24.04.deb
COPY kurento-module-filters-dbgsym_7.0.2-1kurento1.24.04~20240629111717_amd64.ddeb /kurento-deb/kurento-module-filters-dbgsym_7.0.2-1kurento1.24.04.ddeb
COPY kurento-module-chroma_7.0.2-1kurento1.24.04~20240629111937_amd64.deb /kurento-deb/kurento-module-chroma_7.0.2-1kurento1.24.04.deb
COPY kurento-module-chroma-dbgsym_7.0.2-1kurento1.24.04~20240629111937_amd64.ddeb /kurento-deb/kurento-module-chroma-dbgsym_7.0.2-1kurento1.24.04.ddeb
COPY kurento-module-gstreamer-example_7.0.2-1kurento1.24.04~20240629112122_amd64.deb /kurento-deb/kurento-module-gstreamer-example_7.0.2-1kurento1.24.04.deb
COPY kurento-module-gstreamer-example-dbgsym_7.0.2-1kurento1.24.04~20240629112122_amd64.ddeb /kurento-deb/kurento-module-gstreamer-example-dbgsym_7.0.2-1kurento1.24.04.ddeb
COPY kurento-module-opencv-example_7.0.2-1kurento1.24.04~20240629112305_amd64.deb /kurento-deb/kurento-module-opencv-example_7.0.2-1kurento1.24.04.deb
COPY kurento-module-opencv-example-dbgsym_7.0.2-1kurento1.24.04~20240629112305_amd64.ddeb /kurento-deb/kurento-module-opencv-example-dbgsym_7.0.2-1kurento1.24.04.ddeb
COPY kurento-media-server_7.0.2-1kurento1.24.04~20240629112508_amd64.deb /kurento-deb/kurento-media-server_7.0.2-1kurento1.24.04.deb
COPY kurento-media-server-dbgsym_7.0.2-1kurento1.24.04~20240629112508_amd64.ddeb /kurento-deb/kurento-media-server-dbgsym_7.0.2-1kurento1.24.04.ddeb
COPY kurento-dbg_7.0.2-1kurento1.24.04~20240629112508_amd64.deb /kurento-deb/kurento-dbg_7.0.2-1kurento1.24.04.deb






# Install tools:
# * ca-certificates: For HTTPS requests (e.g. with PlayerEndpoint).
# * curl: For `healthchecker.sh`.
# * dnsutils: For `dig` in `getmyip.sh`.
# * gnupg: For `apt-key adv` (Since Ubuntu 18.04).
# * nano: Not required but useful for edits while troubleshooting.
RUN \
apt-get $APT_ARGS update && apt-get $APT_ARGS install --no-install-recommends --yes \
    ca-certificates \
    curl \
    dnsutils \
    gnupg \
    nano \
&& rm -rf /var/lib/apt/lists/*



# Install Kurento Media Server.
#RUN apt install /kurento-deb/kurento-cmake-utils_7.0.2-1kurento1.24.04.deb
RUN apt-get update 

RUN apt install --yes /kurento-deb/kurento-jsonrpc_7.0.2-1kurento1.24.04.deb
RUN apt install --yes /kurento-deb/kurento-jsonrpc-dbgsym_7.0.2-1kurento1.24.04.ddeb
RUN apt install --yes /kurento-deb/kurento-module-creator_7.0.2-1kurento1.24.04.deb
RUN apt install --yes /kurento-deb/kurento-module-core_7.0.2-1kurento1.24.04.deb
RUN apt install --yes /kurento-deb/kurento-module-core-dbgsym_7.0.2-1kurento1.24.04.ddeb
RUN apt install --yes /kurento-deb/kurento-module-elements_7.0.2-1kurento1.24.04.deb
RUN apt install --yes /kurento-deb/kurento-module-elements-dbgsym_7.0.2-1kurento1.24.04.ddeb
RUN apt install --yes /kurento-deb/kurento-module-filters_7.0.2-1kurento1.24.04.deb
RUN apt install --yes /kurento-deb/kurento-module-filters-dbgsym_7.0.2-1kurento1.24.04.ddeb
RUN apt install --yes /kurento-deb/kurento-media-server_7.0.2-1kurento1.24.04.deb
RUN apt install --yes /kurento-deb/kurento-media-server-dbgsym_7.0.2-1kurento1.24.04.ddeb


# Install additional modules.
# These might not be all available, so install separately and allow errors.
RUN apt install --yes /kurento-deb/kurento-module-chroma_7.0.2-1kurento1.24.04.deb
RUN apt install --yes /kurento-deb/kurento-module-chroma-dbgsym_7.0.2-1kurento1.24.04.ddeb
RUN apt install --yes /kurento-deb/kurento-module-gstreamer-example_7.0.2-1kurento1.24.04.deb
RUN apt install --yes /kurento-deb/kurento-module-opencv-example_7.0.2-1kurento1.24.04.deb
RUN apt install --yes /kurento-deb/kurento-module-opencv-example-dbgsym_7.0.2-1kurento1.24.04.ddeb



# Install additional tools that are indirectly used by some GStreamer plugins:
# * gstreamer-tools: Allows running the command `gst-inspect`.
#   Useful for troubleshooting and debugging installed GStreamer plugins.
# * gstreamer-x: Video rendering plugins for X11 and Pango.
#   Needed by some overlay elements like "textoverlay".
RUN \
apt-get $APT_ARGS update && apt-get $APT_ARGS install --no-install-recommends --yes \
    gstreamer1.0-tools \
    gstreamer1.0-x \
&& rm -rf /var/lib/apt/lists/*

# Add Ubuntu debug repository key for apt-get.
RUN \
apt-get $APT_ARGS update && apt-get $APT_ARGS install --no-install-recommends --yes \
    ubuntu-dbgsym-keyring \
|| apt-key adv \
    --keyserver hkp://keyserver.ubuntu.com:80 \
    --recv-keys F2EDC64DC5AEE1F6B9C621F0C8CAB6595FDFF622 \
&& rm -rf /var/lib/apt/lists/*

# Add Ubuntu debug repository line for apt-get.
RUN \
echo "deb http://ddebs.ubuntu.com $UBUNTU_CODENAME main restricted universe multiverse" >/etc/apt/sources.list.d/ddebs.list \
&& echo "deb http://ddebs.ubuntu.com ${UBUNTU_CODENAME}-updates main restricted universe multiverse" >>/etc/apt/sources.list.d/ddebs.list

# Install debug packages.
# The debug packages repository fails very often due to bad server state.
# Try to update, and only if it works install debug symbols.
#RUN apt-get update 
#RUN apt install --yes /kurento-deb/kurento-dbg_7.0.2-1kurento1.24.04.deb

RUN rm -rf /kurento-deb

# Expose default Kurento RPC control port.
EXPOSE 8888

COPY entrypoint.sh /
COPY getmyip.sh /
COPY healthchecker.sh /

HEALTHCHECK --start-period=15s --interval=30s --timeout=3s --retries=1 CMD /healthchecker.sh

ENTRYPOINT ["/entrypoint.sh"]
