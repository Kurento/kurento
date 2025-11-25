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

ARG KMS_VERSION="7.1.2"


# Stage: Kurento Compiling environment configuration
# ================================

FROM ubuntu:${UBUNTU_CODENAME} AS compile_env

ENV DEBIAN_FRONTEND=noninteractive

# [Optional] Uncomment this section to install additional OS packages.
# RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
#     && apt-get -y install --no-install-recommends <your-package-list-here>
RUN apt update && apt install -y --no-install-recommends \
    build-essential \
    ca-certificates \
    cmake \
    git \
    gnupg \
    gdb

RUN apt update && apt install -y --no-install-recommends \
    maven pkg-config libjsoncpp-dev libboost-dev libboost-test-dev libboost-filesystem-dev libboost-system-dev libboost-thread-dev gstreamer1.0-x libgstreamer1.0-0 libgstreamer1.0-dev  libvpx-dev \
    gstreamer1.0-libav gstreamer1.0-nice gstreamer1.0-plugins-ugly gstreamer1.0-plugins-bad  \
     gstreamer1.0-plugins-good  gstreamer1.0-tools libgstreamer-gl1.0-0 libgstreamer-opencv1.0-0 libgstreamer-plugins-bad1.0-0 libgstreamer-plugins-bad1.0-dev libgstreamer-plugins-base1.0-0 libgstreamer-plugins-base1.0-dev libgstreamer-plugins-good1.0-0 libgstreamer-plugins-good1.0-dev  \
     libsigc++-2.0-dev libglibmm-2.4-dev libsoup2.4-dev libsoup2.4-1 libnice-dev libnice10 libboost-program-options-dev libboost-log-dev libevent-dev  libevent-2.1-7 libevent-core-2.1-7 libevent-extra-2.1-7 libevent-openssl-2.1-7 libevent-pthreads-2.1-7 \
    libwebsocketpp-dev ffmpeg

RUN apt install -y devscripts git-buildpackage equivs

RUN wget https://download.oracle.com/java/25/latest/jdk-25_linux-x64_bin.tar.gz

RUN tar xvf jdk-25_linux-x64_bin.tar.gz

RUN mv jdk-25 /usr/lib/jvm

RUN update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/jdk-25/bin/javac 2211
RUN update-alternatives --install /usr/bin/java java /usr/lib/jvm/jdk-25/bin/java 2211

RUN rm jdk-25_linux-x64_bin.tar.gz

# Stage: GStreamer-Rust Compiling environment configuration
# ================================

FROM compile_env AS compile_gst_rust


RUN apt update && apt install -y curl git build-essential pkg-config libssl-dev

COPY --from=sources ./server/gst-plugins-rs/debian.diff /

RUN \
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y

ENV \
PATH=$PATH:/root/.cargo/bin

RUN \
rustup update

# Install Rust cargo-deb
RUN \
 cargo install cargo-deb \
 && cargo install cargo-c

RUN cd / && git clone https://github.com/GStreamer/gst-plugins-rs.git && \
    cd gst-plugins-rs  && \
    git checkout 0.12.7 && \
    git apply /debian.diff && \
    cargo build --package gst-plugin-rtp --release && \
    mkdir -p /build/deb-source  && \ 
    cp -a ./target/release/* /build/deb-source  && \
    cargo deb --separate-debug-symbols  -v --no-build --package=gst-plugin-rtp


FROM compile_env AS compile_kurento

COPY --from=sources ./server /server
COPY --from=sources ./ci-scripts /ci-scripts

RUN cd /server/cmake-utils && \
    /ci-scripts/kurento-buildpackage.sh --release && \
    apt install ./kurento-cmake-utils_7.1.2-1kurento1_all.deb && \
    cd ..

RUN cd /server/jsonrpc && \
    /ci-scripts/kurento-buildpackage.sh --release && \
    apt install ./kurento-jsonrpc_7.1.2-1kurento1_amd64.deb && \
    apt install ./kurento-jsonrpc-dev_7.1.2-1kurento1_amd64.deb && \
    cd ..    

RUN cd /server/module-creator && \
    /ci-scripts/kurento-buildpackage.sh --release && \
    apt install ./kurento-module-creator_7.1.2-1kurento1_all.deb && \
    cd ..    

COPY --from=compile_gst_rust /gst-plugins-rs/target/debian/gst-plugin-rtp_0.12.7-1_amd64.deb /gst-plugin-rtp_0.12.7-1_amd64.deb
RUN apt install /gst-plugin-rtp_0.12.7-1_amd64.deb

RUN cd /server/module-core && \
    /ci-scripts/kurento-buildpackage.sh --release && \
    apt install ./kurento-module-core_7.1.2-1kurento1_amd64.deb && \
    apt install ./kurento-module-core-dev_7.1.2-1kurento1_amd64.deb && \
    cd ..    

RUN cd /server/module-elements && \
    /ci-scripts/kurento-buildpackage.sh --release && \
    apt install ./kurento-module-elements_7.1.2-1kurento1_amd64.deb && \
    apt install ./kurento-module-elements-dev_7.1.2-1kurento1_amd64.deb && \
    cd ..    

RUN cd /server/module-filters && \
    /ci-scripts/kurento-buildpackage.sh --release && \
    apt install ./kurento-module-filters_7.1.2-1kurento1_amd64.deb && \
    apt install ./kurento-module-filters-dev_7.1.2-1kurento1_amd64.deb && \
    cd ..    

RUN cd /server/module-examples/chroma && \
    /ci-scripts/kurento-buildpackage.sh --release && \
    cd ..    

RUN cd /server/module-examples/gstreamer-example && \
    /ci-scripts/kurento-buildpackage.sh --release && \
    cd ..    

RUN cd /server/module-examples/opencv-example && \
    /ci-scripts/kurento-buildpackage.sh --release && \
    cd ..    

RUN cd /server/module-examples/datachannelexample && \
    /ci-scripts/kurento-buildpackage.sh --release && \
    cd ..    

RUN cd /server/module-examples/platedetector && \
    /ci-scripts/kurento-buildpackage.sh --release && \
    cd ..    

RUN cd /server/module-examples/pointerdetector && \
    /ci-scripts/kurento-buildpackage.sh --release && \
    cd ../..

RUN cd /server/media-server && \
    /ci-scripts/kurento-buildpackage.sh --release && \
    cd ../..



# Stage: Base system configuration
# ================================

FROM ubuntu:${UBUNTU_CODENAME} AS ubuntu_base

ARG APT_ARGS=""



# Stage: Install and prepare
# ==========================

FROM ubuntu_base

LABEL maintainer="Saúl Pablo Labajo Izquierdo <slabajo@naevatec.com>"

ARG UBUNTU_CODENAME

ARG KMS_VERSION="dev"

# Configure environment:
# Run apt-get/dpkg without interactive dialogue.
ARG DEBIAN_FRONTEND=noninteractive
# Set the default locale for all commands.
ENV LANG="C.UTF-8"

# Copy .deb artifacts for deploying
RUN mkdir /kurento-deb

COPY --from=compile_gst_rust /gst-plugins-rs/target/debian/gst-plugin-rtp_0.12.7-1_amd64.deb /kurento-deb/gst-plugin-rtp_0.12.7-1_amd64.deb
COPY --from=compile_kurento /server/cmake-utils/kurento-cmake-utils_7.1.2-1kurento1_all.deb /kurento-deb/
COPY --from=compile_kurento /server/jsonrpc/kurento-jsonrpc_7.1.2-1kurento1_amd64.deb /kurento-deb/
COPY --from=compile_kurento /server/jsonrpc/kurento-jsonrpc-dbgsym_7.1.2-1kurento1_amd64.ddeb /kurento-deb/
COPY --from=compile_kurento /server/module-creator/kurento-module-creator_7.1.2-1kurento1_all.deb /kurento-deb/
COPY --from=compile_kurento /server/module-core/kurento-module-core_7.1.2-1kurento1_amd64.deb /kurento-deb/
COPY --from=compile_kurento /server/module-core/kurento-module-core-dbgsym_7.1.2-1kurento1_amd64.ddeb /kurento-deb/
COPY --from=compile_kurento /server/module-elements/kurento-module-elements_7.1.2-1kurento1_amd64.deb /kurento-deb/
COPY --from=compile_kurento /server/module-elements/kurento-module-elements-dbgsym_7.1.2-1kurento1_amd64.ddeb /kurento-deb/
COPY --from=compile_kurento /server/module-filters/kurento-module-filters_7.1.2-1kurento1_amd64.deb /kurento-deb/
COPY --from=compile_kurento /server/module-filters/kurento-module-filters-dbgsym_7.1.2-1kurento1_amd64.ddeb /kurento-deb/
COPY --from=compile_kurento /server/module-examples/chroma/kurento-module-chroma_7.1.2-1kurento1_amd64.deb /kurento-deb/
COPY --from=compile_kurento /server/module-examples/chroma/kurento-module-chroma-dbgsym_7.1.2-1kurento1_amd64.ddeb /kurento-deb/
COPY --from=compile_kurento /server/module-examples/gstreamer-example/kurento-module-gstreamer-example_7.1.2-1kurento1_amd64.deb /kurento-deb/
COPY --from=compile_kurento /server/module-examples/gstreamer-example/kurento-module-gstreamer-example-dbgsym_7.1.2-1kurento1_amd64.ddeb /kurento-deb/
COPY --from=compile_kurento /server/module-examples/opencv-example/kurento-module-opencv-example_7.1.2-1kurento1_amd64.deb /kurento-deb/
COPY --from=compile_kurento /server/module-examples/opencv-example/kurento-module-opencv-example-dbgsym_7.1.2-1kurento1_amd64.ddeb /kurento-deb/
COPY --from=compile_kurento /server/module-examples/datachannelexample/kurento-module-datachannelexample_7.1.2-1kurento1_amd64.deb /kurento-deb/
COPY --from=compile_kurento /server/module-examples/datachannelexample/kurento-module-datachannelexample-dbgsym_7.1.2-1kurento1_amd64.ddeb /kurento-deb/
COPY --from=compile_kurento /server/module-examples/platedetector/kurento-module-platedetector_7.1.2-1kurento1_amd64.deb /kurento-deb/
COPY --from=compile_kurento /server/module-examples/platedetector/kurento-module-platedetector-dbgsym_7.1.2-1kurento1_amd64.ddeb /kurento-deb/
COPY --from=compile_kurento /server/module-examples/pointerdetector/kurento-module-pointerdetector_7.1.2-1kurento1_amd64.deb /kurento-deb/
COPY --from=compile_kurento /server/module-examples/pointerdetector/kurento-module-pointerdetector-dbgsym_7.1.2-1kurento1_amd64.ddeb /kurento-deb/
COPY --from=compile_kurento /server/media-server/kurento-media-server_7.1.2-1kurento1_amd64.deb /kurento-deb/
COPY --from=compile_kurento /server/media-server/kurento-media-server-dbgsym_7.1.2-1kurento1_amd64.ddeb /kurento-deb/
COPY --from=compile_kurento /server/media-server/kurento-dbg_7.1.2-1kurento1_amd64.deb /kurento-deb/


# For debugging purposes
COPY tracedump_gdb.sh /
COPY tracedump_eu.sh /
COPY extended_entrypoint.sh /


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
#RUN apt install /kurento-deb/kurento-cmake-utils_7.1.2-1kurento1_all.deb
RUN apt-get update 

RUN apt install --yes /kurento-deb/kurento-jsonrpc_7.1.2-1kurento1_amd64.deb
RUN apt install --yes /kurento-deb/kurento-jsonrpc-dbgsym_7.1.2-1kurento1_amd64.ddeb
RUN apt install --yes /kurento-deb/kurento-module-creator_7.1.2-1kurento1_all.deb
RUN apt install --yes /kurento-deb/kurento-module-core_7.1.2-1kurento1_amd64.deb
RUN apt install --yes /kurento-deb/kurento-module-core-dbgsym_7.1.2-1kurento1_amd64.ddeb
RUN apt install --yes /kurento-deb/kurento-module-elements_7.1.2-1kurento1_amd64.deb
RUN apt install --yes /kurento-deb/kurento-module-elements-dbgsym_7.1.2-1kurento1_amd64.ddeb
RUN apt install --yes /kurento-deb/kurento-module-filters_7.1.2-1kurento1_amd64.deb
RUN apt install --yes /kurento-deb/kurento-module-filters-dbgsym_7.1.2-1kurento1_amd64.ddeb
RUN apt install --yes /kurento-deb/kurento-media-server_7.1.2-1kurento1_amd64.deb
RUN apt install --yes /kurento-deb/kurento-media-server-dbgsym_7.1.2-1kurento1_amd64.ddeb


# Install additional modules.
# These might not be all available, so install separately and allow errors.
RUN apt install --yes /kurento-deb/kurento-module-chroma_7.1.2-1kurento1_amd64.deb
RUN apt install --yes /kurento-deb/kurento-module-chroma-dbgsym_7.1.2-1kurento1_amd64.ddeb
RUN apt install --yes /kurento-deb/kurento-module-gstreamer-example_7.1.2-1kurento1_amd64.deb
RUN apt install --yes /kurento-deb/kurento-module-gstreamer-example-dbgsym_7.1.2-1kurento1_amd64.ddeb
RUN apt install --yes /kurento-deb/kurento-module-opencv-example_7.1.2-1kurento1_amd64.deb
RUN apt install --yes /kurento-deb/kurento-module-opencv-example-dbgsym_7.1.2-1kurento1_amd64.ddeb
RUN apt install --yes /kurento-deb/kurento-module-datachannelexample_7.1.2-1kurento1_amd64.deb
RUN apt install --yes /kurento-deb/kurento-module-datachannelexample-dbgsym_7.1.2-1kurento1_amd64.ddeb
RUN apt install --yes /kurento-deb/kurento-module-platedetector_7.1.2-1kurento1_amd64.deb
RUN apt install --yes /kurento-deb/kurento-module-platedetector-dbgsym_7.1.2-1kurento1_amd64.ddeb
RUN apt install --yes /kurento-deb/kurento-module-pointerdetector_7.1.2-1kurento1_amd64.deb
RUN apt install --yes /kurento-deb/kurento-module-pointerdetector-dbgsym_7.1.2-1kurento1_amd64.ddeb


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

# Install Gstreamer rust rtp plugins for AV1 support
RUN apt install --yes /kurento-deb/gst-plugin-rtp_0.12.7-1_amd64.deb


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

# for debugging purposes
RUN apt clean && apt update  && apt install --no-install-recommends --yes \
    gdb elfutils


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

ENTRYPOINT ["/extended_entrypoint.sh"]
