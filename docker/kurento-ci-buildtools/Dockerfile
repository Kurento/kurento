# CI build tools for Kurento projects - Dockerfile
#
# This Docker image is used to run all CI jobs related to Kurento projects. It
# contains all tools needed for the different repositories, such as
# [git-buildpackage][1] to generate Debian packages, Maven for Java projects,
# and Node.js for JavaScript projects.
#
# [1]: https://hub.docker.com/r/kurento/kurento-buildpackage
#
#
# Build command
# -------------
#
# docker build [Args...] --tag kurento/kurento-ci-buildtools:<UbuntuCodename> .
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

ARG UBUNTU_CODENAME=noble

FROM kurento/kurento-buildpackage:${UBUNTU_CODENAME}

# Settings from parent image:
# Re-enable the cleaning of Apt package cache
# (in case you end up using the parent's entrypoint.sh)
ENV APT_KEEP_CACHE="false"

# Node.js: Force address resolution using IPv4 before IPv6, regardless of the
# system's preferrence list (which usually uses IPv6 first) [1].
# This is needed until Node v20 which will implement [2] the Happy Eyeballs
# algorithm [3] by default. This is just a kludge and the proper solution moving
# forwards should be to properly configure the system with `/etc/gai.conf` [4].
# [1]: https://nodejs.org/api/cli.html#--dns-result-orderorder
# [2]: https://github.com/nodejs/node/pull/46790
# [3]: https://en.wikipedia.org/wiki/Happy_Eyeballs
# [4]: https://github.com/nodejs/node/issues/40537#issuecomment-1442410604
ENV NODE_OPTIONS="--dns-result-order=ipv4first"

# Install Node, NPM, and Bower (fox JavaScript-based projects).
RUN \
apt-get update && apt-get install --no-install-recommends --yes \
    wget \
&& wget --no-verbose -O - https://deb.nodesource.com/setup_18.x | bash - \
&& apt-get update && apt-get install --no-install-recommends --yes \
    nodejs \
&& rm -rf /var/lib/apt/lists/* \
&& npm config set cache=/.npm \
&& npm config set color=false \
&& npm config set progress=false \
&& npm install -g npm \
&& npm install -g bower \
&& npm cache clean --force \
&& chmod --recursive 0777 /.npm

# Install packages
# * build-essential, cmake: For C/C++ server modules that generate client SDK files.
# * curl: For CI script `deploy_js_http.sh`.
# * default-jdk (Java JDK): For Java projects.
# * jq: For some JavaScript tools and jobs.
# * libnss-wrapper: Used by jobs that perform Git push/pull operations.
# * maven: For Java projects.
# * rsync: For remote copies.
#
# Tools used by test/integration/kurento-test:
# * ffmpeg: For different integration testst that generate an RTP stream.
# * libmediainfo-dev: MediaInfoLibrary class.
# * netcat: KmsService class, to check if a port is already in use.
RUN \
apt-get update && apt-get install --no-install-recommends --yes \
    build-essential cmake \
    curl \
    openjdk-25-jdk \
    ffmpeg \
    jq \
    libmediainfo-dev \
    libnss-wrapper \
    netcat-openbsd \
    rsync \
&& rm -rf /var/lib/apt/lists/*


ENV MAVEN_VERSION=3.9.11
ENV MAVEN_HOME=/usr/share/maven
ENV PATH=$MAVEN_HOME/bin:$PATH
RUN wget -q https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz && \
    tar -xzf apache-maven-${MAVEN_VERSION}-bin.tar.gz -C /opt && \
    ln -s /opt/apache-maven-${MAVEN_VERSION} $MAVEN_HOME && \
    rm apache-maven-${MAVEN_VERSION}-bin.tar.gz
RUN mvn -v


ENTRYPOINT ["/bin/bash"]
CMD []
