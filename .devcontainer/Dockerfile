ARG VARIANT="noble"
FROM mcr.microsoft.com/devcontainers/base:${VARIANT}
#FROM mcr.microsoft.com/vscode/devcontainers/base:0-${VARIANT}

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

#RUN apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83

#COPY repo_cfg /tmp

#RUN bash -c "source /etc/upstream-release/lsb-release 2>/dev/null || source /etc/lsb-release "
#RUN tee "/etc/apt/sources.list.d/kurento.list" >/dev/null < /tmp/repo_cfg \
#    && rm /tmp/repo_cfg && apt update

#RUN apt install -y --no-install-recommends \
#    kurento-media-server-dev

RUN apt update && apt install -y --no-install-recommends \
    maven pkg-config libjsoncpp-dev libboost-dev libboost-test-dev libboost-filesystem-dev libboost-system-dev libboost-thread-dev gstreamer1.0-x libgstreamer1.0-0 libgstreamer1.0-dev  libvpx-dev \
    gstreamer1.0-libav gstreamer1.0-nice gstreamer1.0-plugins-ugly gstreamer1.0-plugins-bad  \
     gstreamer1.0-plugins-good  gstreamer1.0-tools libgstreamer-gl1.0-0 libgstreamer-opencv1.0-0 libgstreamer-plugins-bad1.0-0 libgstreamer-plugins-bad1.0-dev libgstreamer-plugins-base1.0-0 libgstreamer-plugins-base1.0-dev libgstreamer-plugins-good1.0-0 libgstreamer-plugins-good1.0-dev  \
     libsigc++-2.0-dev libglibmm-2.4-dev libsoup2.4-dev libsoup2.4-1 libnice-dev libnice10 libboost-program-options-dev libboost-log-dev libevent-dev  libevent-2.1-7 libevent-core-2.1-7 libevent-extra-2.1-7 libevent-openssl-2.1-7 libevent-pthreads-2.1-7 \
    libwebsocketpp-dev ffmpeg

RUN apt install -y devscripts git-buildpackage equivs

RUN wget https://download.java.net/java/GA/jdk22.0.1/c7ec1332f7bb44aeba2eb341ae18aca4/8/GPL/openjdk-22.0.1_linux-x64_bin.tar.gz

RUN tar xvf openjdk-22.0.1_linux-x64_bin.tar.gz

RUN mv jdk-22.0.1 /usr/lib/jvm

RUN update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/jdk-22.0.1/bin/javac 2211
RUN update-alternatives --install /usr/bin/java java /usr/lib/jvm/jdk-22.0.1/bin/java 2211

RUN rm openjdk-22.0.1_linux-x64_bin.tar.gz

RUN curl -sL https://deb.nodesource.com/setup_18.x | bash -

RUN apt install -y nodejs

RUN node --dns-result-order=ipv4first /usr/bin/npm  install -g bower


# Ensure sudo group users are not 
# asked for a password when using 
# sudo command by ammending sudoers file
RUN echo '%sudo ALL=(ALL) NOPASSWD:ALL' >> \
/etc/sudoers