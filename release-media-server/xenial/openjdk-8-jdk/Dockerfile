FROM buildpack-deps:xenial-scm

# A few problems with compiling Java from source:
#  1. Oracle.  Licensing prevents us from redistributing the official JDK.
#  2. Compiling OpenJDK also requires the JDK to be installed, and it gets
#       really hairy.

# Fix for "no matching key exchange method found" error (http://www.openssh.com/legacy.html)
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

RUN apt-get update && apt-get install -y openjdk-8-jdk gnupg maven && rm -rf /var/lib/apt/lists/*

RUN mkdir /nodejs \
  && curl -sL https://deb.nodesource.com/setup_4.x | bash - \
  && apt-get install -y nodejs

ENV PATH $PATH:/nodejs/bin

RUN npm -g install bower

#RUN echo "deb http://ubuntuci.kurento.org xenial kms6" | tee /etc/apt/sources.list.d/kurento.list 

# Configure Kurento's apt proxy
RUN echo 'Acquire::http::Proxy "http://proxy.kurento.org:3142";' > /etc/apt/apt.conf.d/01proxy \
  && echo 'Acquire::HTTP::Proxy::deb.nodesource.com "DIRECT";' >> /etc/apt/apt.conf.d/01proxy
