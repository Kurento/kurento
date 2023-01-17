FROM ubuntu:14.04
MAINTAINER Kurento <kurento@openvidu.io>

RUN apt-get update \
  && apt-get install -y \
    wget

RUN wget https://opscode-omnibus-packages.s3.amazonaws.com/ubuntu/12.04/x86_64/chefdk_0.3.6-1_amd64.deb \
  && dpkg -i chefdk_0.3.6-1_amd64.deb

RUN apt-get update \
  && apt-get install -y \
    build-essential \
    ruby1.9.3 \
    ruby1.9.1-dev

RUN apt-get update \
  && apt-get install -y \
  zlib1g-dev

RUN gem install --no-ri --no-rdoc foodcritic -v '~>4.0.0'
