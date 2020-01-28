# Kurento Media Server - AddressSanitizer
#
# This Docker image is used to run an instance of Kurento Media Server that
# has AddressSanitizer instrumentation code injected into the binary. This is
# useful to run KMS and detect memory access errors.
#
#
# Build command
# -------------
#
# docker build [Args...] --tag kurento/kurento-media-server:latest-asan .
#
#
# Build arguments
# ---------------
#
# --build-arg UBUNTU_VERSION=<UbuntuVersion>
#
#   <UbuntuVersion> is like "xenial", "bionic", etc.
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
# --build-arg KMS_IMAGE=<DockerTag>
#
#   <DockerTag> is:
#   * "kurento/kurento-media-server:<KmsVersion>" for release,
#   * "kurento/kurento-media-server-dev:latest" for nightly.
#
#   Optional. Default: "kurento/kurento-media-server-dev:latest".
#
#
# Run command
# -----------
#
# docker run --name kms -p 8888:8888 kurento/kurento-media-server:latest-asan
#
# Then, you can follow the logs with the `docker logs` command:
#
# docker logs --follow kms >"kms-$(date '+%Y%m%dT%H%M%S').log" 2>&1

ARG UBUNTU_VERSION="xenial"
ARG KMS_IMAGE="kurento/kurento-media-server-dev:latest"

FROM ubuntu:${UBUNTU_VERSION} AS builder

ARG UBUNTU_VERSION="xenial"
ARG KMS_VERSION="dev"
ARG GCC_VERSION="9"

# Configure environment:
# * DEBIAN_FRONTEND: Disable interactive questions and messages in `apt-get`
# * LANG: Set the default locale for all commands
ENV DEBIAN_FRONTEND="noninteractive" \
    LANG="C.UTF-8"

# Install required tools:
# * git: For `git clone`
# * gnupg: For `apt-key adv` (since Ubuntu 18.04)
# * software-properties-common: For `add-apt-repository`
RUN apt-get update && apt-get install --no-install-recommends --yes \
        git \
        gnupg \
        software-properties-common \
 && echo "deb http://ubuntu.openvidu.io/$KMS_VERSION $UBUNTU_VERSION kms6" >/etc/apt/sources.list.d/kurento.list \
 && apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83 \
 && apt-get update && apt-get install --no-install-recommends --yes \
        build-essential \
        kurento-media-server-dev \
        maven-debian-helper \
 && rm -rf /var/lib/apt/lists/*

# Install latest GCC (GCC 9 in Ubuntu 16.04 "Xenial")
# This is done to use the latest AddressSanitizer, which is always a good idea
# because it will have better detection capabilities and bugs resolved
RUN add-apt-repository --yes ppa:ubuntu-toolchain-r/test \
 && apt-get update && apt-get install --no-install-recommends --yes \
        "gcc-${GCC_VERSION}" \
        "g++-${GCC_VERSION}" \
        "cpp-${GCC_VERSION}" \
 && rm -rf /var/lib/apt/lists/* \
 && update-alternatives --remove-all gcc || true \
 && update-alternatives --remove-all g++ || true \
 && update-alternatives --remove-all cpp || true \
 && update-alternatives --install /usr/bin/gcc gcc "/usr/bin/gcc-${GCC_VERSION}" 60 \
        --slave /usr/bin/g++ g++ "/usr/bin/g++-${GCC_VERSION}" \
        --slave /usr/bin/cpp cpp "/usr/bin/cpp-${GCC_VERSION}" \
 && update-alternatives --set gcc "/usr/bin/gcc-${GCC_VERSION}"

# Download, build, and dist KMS
COPY ./build.sh /
RUN /build.sh

# Final Docker stage with only the final files
FROM ${KMS_IMAGE}

# Install latest GCC runtime (GCC 9 in Ubuntu 16.04 "Xenial")
COPY --from=builder /etc/apt/sources.list.d/ubuntu-toolchain-r-ubuntu-test-*.list /etc/apt/sources.list.d/
COPY --from=builder /etc/apt/trusted.gpg.d/ubuntu-toolchain-r_ubuntu_test.gpg /etc/apt/trusted.gpg.d/
RUN apt-get update && apt-get install --no-install-recommends --yes \
        libgcc1 \
        libstdc++6 \
 && rm -rf /var/lib/apt/lists/*

# Get the KMS built in previous stage
COPY --from=builder /kurento-asan /kurento-asan

# Get and run the custom entrypoint
COPY ./entrypoint.sh /
WORKDIR /kurento-asan
ENTRYPOINT ["/entrypoint.sh"]
