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
#   Alternatively, "dev" is used to build a nightly version of KMS.
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
#     docker run --name kms -p 8888:8888 kurento/kurento-media-server:latest
#
# Then, you can follow the logs with the `docker logs` command:
#
#     docker logs --follow kms >"kms-$(date '+%Y%m%dT%H%M%S').log" 2>&1



# Global arguments for FROM
ARG UBUNTU_CODENAME="xenial"



# Stage: Base system configuration
# ================================

FROM ubuntu:${UBUNTU_CODENAME} AS ubuntu_base

ARG APT_ARGS=""

# Configure Apt:
# * DEBIAN_FRONTEND: Disable Apt interactive questions and messages
# * --quiet: Hide progress bars.
# * --yes: Assume "Yes" for all confirmation requests.
# * APT::Install-Recommends=false: Avoid installing non-essential stuff.
RUN touch /etc/profile \
 && touch "$HOME"/.bash_profile \
 && printf "\
apt-get-install() {\n\
    (\n\
        export DEBIAN_FRONTEND=noninteractive\n\
        /usr/bin/apt-get --quiet $APT_ARGS update \\\\\\n\
        && /usr/bin/apt-get --quiet --yes -oAPT::Install-Recommends=false $APT_ARGS install \"\$@\"\n\
    )\n\
    local RC=\$?\n\
    rm -rf /var/lib/apt/lists/*\n\
    return \$RC\n\
}\n" >>/etc/profile

# FIXME: Workaround for the network errors introduced by our OpenStack CI.
RUN printf "\
apt-get-install-fix() {\n\
    (\n\
        export DEBIAN_FRONTEND=noninteractive\n\
        /usr/bin/apt-get --quiet update \\\\\\n\
        && until /usr/bin/apt-get --quiet --yes -oAPT::Install-Recommends=false install \"\$@\"; do echo FIXME RETRY; done\n\
    )\n\
    local RC=\$?\n\
    rm -rf /var/lib/apt/lists/*\n\
    return \$RC\n\
}\n" >>/etc/profile

SHELL ["/bin/bash", "--login", "-x", "-c"]



# Stage: Install and prepare
# ==========================

FROM ubuntu_base

LABEL maintainer="Juan Navarro <juan.navarro@gmx.es>"

ARG UBUNTU_CODENAME

ARG KMS_VERSION="dev"

# Configure environment:
# * LANG: Set the default locale for all commands
ENV LANG="C.UTF-8"

# Install tools:
# * ca-certificates: For HTTPS requests (e.g. with PlayerEndpoint)
# * curl: For `healthchecker.sh`
# * dnsutils: For `dig` in `getmyip.sh`
# * gnupg: For `apt-key adv` (since Ubuntu 18.04)
# * nano: Not required but useful for edits while troubleshooting
RUN apt-get-install \
        ca-certificates \
        curl \
        dnsutils \
        gnupg \
        \
        nano

# Add Kurento Apt package repository.
RUN apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 234821A61B67740F89BFD669FC8A16625AFA7A83 \
 && echo "deb [arch=amd64] http://ubuntu.openvidu.io/${KMS_VERSION} ${UBUNTU_CODENAME} kms6" >/etc/apt/sources.list.d/kurento.list

# Install Kurento Media Server
#RUN apt-get-install kurento-media-server
#
# FIXME: apt-get-install-fix is a workaround for the network errors introduced by our OpenStack CI.
RUN apt-get-install-fix kurento-media-server

# Install additional modules
# These might not be all available, so install separately and allow errors
RUN apt-get-install kms-chroma || true
RUN apt-get-install kms-crowddetector || true
RUN apt-get-install kms-datachannelexample || true
RUN apt-get-install kms-markerdetector || true
RUN apt-get-install kms-platedetector || true
RUN apt-get-install kms-pointerdetector || true

# Install additional tools that are indirectly used by some GStreamer plugins:
# * gstreamer-tools: Allows running the command `gst-inspect`.
#   Useful for troubleshooting and debugging installed GStreamer plugins.
# * gstreamer-x: Video rendering plugins for X11 and Pango.
#   Needed by some overlay elements like "textoverlay".
RUN apt-get-install \
        gstreamer1.5-tools \
        gstreamer1.5-x \
 || apt-get-install \
        gstreamer1.0-tools \
        gstreamer1.0-x

# Install debug symbols
RUN apt-get-install ubuntu-dbgsym-keyring \
 || apt-key adv \
        --keyserver keyserver.ubuntu.com \
        --recv-keys F2EDC64DC5AEE1F6B9C621F0C8CAB6595FDFF622 \
 && echo "deb http://ddebs.ubuntu.com ${UBUNTU_CODENAME} main restricted universe multiverse" >/etc/apt/sources.list.d/ddebs.list \
 && echo "deb http://ddebs.ubuntu.com ${UBUNTU_CODENAME}-updates main restricted universe multiverse" >>/etc/apt/sources.list.d/ddebs.list
 # FIXME: Waiting for a fix in Ubuntu repos:
 # https://bugs.launchpad.net/ubuntu/+source/glib2.0/+bug/1989572
 #&& apt-get-install-fix kurento-dbg
#
# FIXME: apt-get-install-fix is a workaround for the network errors introduced by our OpenStack CI.

# Expose default Kurento RPC control port
EXPOSE 8888

COPY entrypoint.sh /
COPY getmyip.sh /
COPY healthchecker.sh /

HEALTHCHECK --start-period=15s --interval=30s --timeout=3s --retries=1 CMD /healthchecker.sh

ENTRYPOINT ["/entrypoint.sh"]
