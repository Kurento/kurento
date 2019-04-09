FROM buildpack-deps:trusty-scm

# A few problems with compiling Java from source:
#  1. Oracle.  Licensing prevents us from redistributing the official JDK.
#  2. Compiling OpenJDK also requires the JDK to be installed, and it gets
#       really hairy.

# Configure Kurento's apt proxy
RUN echo 'Acquire::http::Proxy "http://proxy.kurento.org:3142";' > /etc/apt/apt.conf.d/01proxy \
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
	&& gpg --export --armor EB9B1D8886F44E2A | sudo apt-key add - \
	&& echo 'deb http://ppa.launchpad.net/openjdk-r/ppa/ubuntu trusty main ' > /etc/apt/sources.list.d/openjdk-8.list \
  && apt-get update \
	&& apt-get install -y --force-yes openjdk-8-jdk \
	&& rm -rf /var/lib/apt/lists/*

ENV MAVEN_VERSION 3.3.9

RUN curl -fsSL https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar xzf - -C /usr/share \
	  && mv /usr/share/apache-maven-$MAVEN_VERSION /usr/share/maven \
	  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn

ENV MAVEN_HOME /usr/share/maven

RUN mkdir /nodejs \
	  && curl -sL https://deb.nodesource.com/setup_0.12 | bash - \
	  && apt-get install -y nodejs

ENV PATH $PATH:/nodejs/bin

RUN npm -g install bower

RUN echo "deb http://ubuntuci.kurento.org trusty kms6" | tee /etc/apt/sources.list.d/kurento.list 
