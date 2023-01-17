FROM buildpack-deps:xenial-scm

# Configure Kurento's apt proxy
RUN echo 'Acquire::http::Proxy "http://proxy.openvidu.io:3142";' > /etc/apt/apt.conf.d/01proxy \
  && echo 'Acquire::HTTP::Proxy::deb.nodesource.com "DIRECT";' >> /etc/apt/apt.conf.d/01proxy

RUN apt-get update \
  && apt-get install -y \
    git-review \
    python \
    python-configobj \
    realpath \
    unzip \
    wget \
    zip \
  && rm -rf /var/lib/apt/lists/*

# Default to UTF-8 file.encoding
ENV LANG C.UTF-8

RUN gpg --keyserver keyserver.ubuntu.com --recv EB9B1D8886F44E2A \
	&& gpg --export --armor EB9B1D8886F44E2A | apt-key add - \
	&& echo 'deb http://ppa.launchpad.net/openjdk-r/ppa/ubuntu xenial main ' > /etc/apt/sources.list.d/openjdk-8.list \
  && apt-get update \
	&& apt-get install -y --force-yes openjdk-8-jdk \
	&& rm -rf /var/lib/apt/lists/*

ENV MAVEN_VERSION 3.3.9

RUN curl -fsSL https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar xzf - -C /usr/share \
	  && mv /usr/share/apache-maven-$MAVEN_VERSION /usr/share/maven \
	  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME /usr/share/maven

# Installing nodejs
RUN curl -sL https://deb.nodesource.com/setup_6.x -o nodesource_setup.sh \
  && bash nodesource_setup.sh \
  && apt-get install -y nodejs \
  && rm nodesource_setup.sh

RUN npm -g install bower

RUN apt-get update \
  && apt-get install -y \
    git \
    graphviz \
    libxml2-dev \
    libxslt-dev \
    python \
    python-dev \
    python-pip \
    python-setuptools \
    rsync \
    zlib1g-dev \
    cmake

RUN pip install \
  javasphinx \
  lxml \
  javalang \
  docutils\
  sphinx==1.2.3

RUN apt-get install -y texlive-full
