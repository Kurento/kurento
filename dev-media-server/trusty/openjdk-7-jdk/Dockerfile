FROM buildpack-deps:trusty-scm

# A few problems with compiling Java from source:
#  1. Oracle.  Licensing prevents us from redistributing the official JDK.
#  2. Compiling OpenJDK also requires the JDK to be installed, and it gets
#       really hairy.

RUN apt-get update \
  && apt-get install -y \
    git-review \
    python \
    python-configobj \
    realpath \
    unzip \
    wget \
    zip \
    openjdk-7-jdk \
    gnupg \
    build-essential \
    iproute

# Cleaning the house
RUN apt-get clean && \
    apt-get autoclean && \
    rm -Rf /tmp/* && \
    rm -Rf /var/lib/apt/lists/*

# Default to UTF-8 file.encoding
ENV LANG C.UTF-8

ENV MAVEN_VERSION 3.3.9

RUN curl -fsSL https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar xzf - -C /usr/share \
  && mv /usr/share/apache-maven-$MAVEN_VERSION /usr/share/maven \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME /usr/share/maven

RUN mkdir /nodejs \
  && curl -sL https://deb.nodesource.com/setup_8.x | bash - \
  && apt-get install -y nodejs

ENV PATH $PATH:/nodejs/bin

RUN npm -g install bower

# Configure Kurento's apt proxy
RUN echo 'Acquire::http::Proxy "http://proxy.kurento.org:3142";' > /etc/apt/apt.conf.d/01proxy \
  && echo 'Acquire::HTTP::Proxy::deb.nodesource.com "DIRECT";' >> /etc/apt/apt.conf.d/01proxy

# Install Kurento Media Server (KMS) 
RUN echo "deb http://ubuntu.openvidu.io/dev trusty kms6" | tee /etc/apt/sources.list.d/kurento.list \
  && apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83
