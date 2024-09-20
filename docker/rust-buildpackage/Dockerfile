# Package creation tool for Kurento projects - Dockerfile
#
# This Docker image is used to generate Debian packages from Kurento projects.
# It runs [kurento-buildpackage.sh][1] from a properly configured system.
#
# [1]: https://github.com/Kurento/kurento/blob/main/ci-scripts/kurento-buildpackage.sh
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
#   <UbuntuCodename> is like "focal".
#
#   Optional. Default: "focal".
#
#
# Run command
# -----------
#
# git clone https://github.com/Kurento/kurento.git
# cd kurento/server/module-core/
# docker run --rm \
#     --mount type=bind,src="$PWD",dst=/hostdir \
#     kurento/kurento-buildpackage:focal \
#         --install-kurento 7.0.0 \
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

ARG UBUNTU_CODENAME=noble

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
    PATH="/ci-scripts:${PATH}"

# Configure Apt:
# * Disable installation of recommended and suggested packages
RUN \
echo 'APT::Install-Recommends "false";' >/etc/apt/apt.conf.d/00recommends \
&& echo 'APT::Install-Suggests "false";' >>/etc/apt/apt.conf.d/00recommends

# Install a basic set of packages
# * gnupg, iproute2, zip: Tools used by various CI scripts.
RUN \
apt-get update && apt-get install --no-install-recommends --yes \
    gnupg \
    iproute2 \
    zip unzip \
    git \
&& rm -rf /var/lib/apt/lists/*

# Install an extra set of packages.
# These are basic build dependencies that end up beind required by most jobs.
RUN \
apt-get update && apt-get install --no-install-recommends --yes \
    curl \
    openssl \
    ca-certificates \
    libsrt-openssl-dev \
    librust-openssl-dev librust-openssl-sys-dev \
    librust-pango-dev librust-pango-sys-dev \
    gstreamer1.0-plugins-bad gstreamer1.0-plugins-good gstreamer1.0-plugins-ugly libgstreamer-plugins-bad1.0-0 libgstreamer-plugins-good1.0-0 \
    libgstreamer1.0-dev libgstreamer-plugins-base1.0-dev libgstreamer-plugins-bad1.0-dev gstreamer1.0-plugins-base gstreamer1.0-plugins-good \
    gstreamer1.0-plugins-bad gstreamer1.0-plugins-ugly gstreamer1.0-libav gstreamer1.0-tools gstreamer1.0-x gstreamer1.0-alsa gstreamer1.0-gl \
    gstreamer1.0-gtk3 gstreamer1.0-qt5 gstreamer1.0-pulseaudio \
    libdav1d-dev \
&& rm -rf /var/lib/apt/lists/*

# Install Rust compile environment
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

COPY entrypoint.sh /

ENTRYPOINT ["/entrypoint.sh"]
CMD []
