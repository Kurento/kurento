# =======================================
# Kurento Media Server - AddressSanitizer
# =======================================
#
# This Docker image is used to run an instance of Kurento Media Server that
# has AddressSanitizer instrumentation code injected into the binary. This is
# useful to run Kurento and detect memory access errors.
#
#
#
# Build Command
# =============
#
# Run:
#
#     docker build [Args...] --tag kurento/kurento-media-server:latest-asan .
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
# --build-arg KMS_IMAGE=<KmsImage>
#
#   <KmsImage> is:
#   * "kurento/kurento-media-server:<KmsVersion>" for releases,
#   * "kurento/kurento-media-server:dev" for development.
#
#   Optional. Default: "kurento/kurento-media-server:dev".
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
#     docker run --name kurento --network host kurento/kurento-media-server:latest-asan
#
# Then, you can follow the logs with the `docker logs` command:
#
#     docker logs --follow kms >"kms-$(date '+%Y%m%dT%H%M%S').log" 2>&1



# Global arguments for FROM.
ARG UBUNTU_CODENAME="xenial"
ARG KMS_IMAGE="kurento/kurento-media-server:dev"



# Stage: Base system configuration
# ================================

FROM ubuntu:${UBUNTU_CODENAME} AS ubuntu_base

ARG APT_ARGS=""



# Stage: Build from sources
# =========================

FROM ubuntu_base AS builder

ARG UBUNTU_CODENAME

ARG KMS_VERSION="dev"
ARG GCC_VERSION="9"

# Configure environment:
# * LANG: Set the default locale for all commands.
ENV LANG="C.UTF-8"

# Install tools:
# * git: For `git clone` in `build.sh`.
# * gnupg: For `apt-key adv` (since Ubuntu 18.04).
RUN apt-get $APT_ARGS update && apt-get $APT_ARGS install --no-install-recommends --yes \
        git \
        gnupg \
 && rm -rf /var/lib/apt/lists/*

# Add Kurento Apt package repository.
RUN apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 234821A61B67740F89BFD669FC8A16625AFA7A83 \
 && echo "deb [arch=amd64] http://ubuntu.openvidu.io/${KMS_VERSION} ${UBUNTU_CODENAME} kms6" >/etc/apt/sources.list.d/kurento.list

# Install development tools.
RUN apt-get $APT_ARGS update && apt-get $APT_ARGS install --no-install-recommends --yes \
        build-essential \
        kurento-media-server-dev \
        maven-debian-helper \
 && rm -rf /var/lib/apt/lists/*

# Install latest GCC (GCC 9 in Ubuntu 16.04 "Xenial").
# This is done to use the latest AddressSanitizer, which is always a good idea
# because it will have better detection capabilities and bugs resolved.
RUN echo "deb http://ppa.launchpad.net/ubuntu-toolchain-r/test/ubuntu $UBUNTU_CODENAME main" \
        >/etc/apt/sources.list.d/ubuntu-toolchain.list \
 && apt-key adv --keyserver keyserver.ubuntu.com --recv-keys BA9EF27F \
 && apt-get $APT_ARGS update && apt-get $APT_ARGS install --no-install-recommends --yes \
        "gcc-${GCC_VERSION}" \
        "g++-${GCC_VERSION}" \
        "cpp-${GCC_VERSION}" \
 && rm -rf /var/lib/apt/lists/*

# Configure GCC version.
RUN update-alternatives --remove-all gcc || true \
 && update-alternatives --remove-all g++ || true \
 && update-alternatives --remove-all cpp || true \
 && update-alternatives --install /usr/bin/gcc gcc "/usr/bin/gcc-${GCC_VERSION}" 60 \
        --slave /usr/bin/g++ g++ "/usr/bin/g++-${GCC_VERSION}" \
        --slave /usr/bin/cpp cpp "/usr/bin/cpp-${GCC_VERSION}" \
 && update-alternatives --set gcc "/usr/bin/gcc-${GCC_VERSION}"

# Download, build, and dist Kurento.
COPY build.sh /
RUN /build.sh



# Stage: Copy artifacts
# =====================

FROM ${KMS_IMAGE}

ARG UBUNTU_CODENAME

# Install latest GCC runtime (GCC 9 in Ubuntu 16.04 "Xenial").
RUN apt-get $APT_ARGS update && apt-get $APT_ARGS install --no-install-recommends --yes \
        gnupg \
 && echo "deb http://ppa.launchpad.net/ubuntu-toolchain-r/test/ubuntu $UBUNTU_CODENAME main" \
        >/etc/apt/sources.list.d/ubuntu-toolchain.list \
 && apt-key adv --keyserver keyserver.ubuntu.com --recv-keys BA9EF27F \
 && apt-get $APT_ARGS update && apt-get $APT_ARGS install --no-install-recommends --yes \
        libgcc1 \
        libstdc++6 \
 && rm -rf /var/lib/apt/lists/*

# Get the Kurento binaries built in previous stage.
COPY --from=builder /kurento-asan /kurento-asan

COPY entrypoint.sh /
COPY getmyip.sh /
WORKDIR /kurento-asan
ENTRYPOINT ["/entrypoint.sh"]
