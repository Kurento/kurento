# Coturn
#
# VERSION               4.4

FROM      ubuntu:14.04
MAINTAINER Patxi Gortázar <patxi.gortazar@gmail.com>

RUN apt-get update && apt-get install -y \
  curl \
  libevent-core-2.0-5 \
  libevent-extra-2.0-5 \
  libevent-openssl-2.0-5 \
  libevent-pthreads-2.0-5 \
  libhiredis0.10 \
  libmysqlclient18 \
  libpq5 \
  telnet \
  wget

RUN wget http://turnserver.open-sys.org/downloads/v4.4.2.2/turnserver-4.4.2.2-debian-wheezy-ubuntu-mint-x86-64bits.tar.gz \
  && tar xzf turnserver-4.4.2.2-debian-wheezy-ubuntu-mint-x86-64bits.tar.gz \
  && dpkg -i coturn_4.4.2.2-1_amd64.deb

COPY ./turnserver.sh /turnserver.sh

ENV TURN_USERNAME kurento
ENV TURN_PASSWORD kurento
ENV REALM kurento.org
ENV NAT false

EXPOSE 3478 3478/udp

ENTRYPOINT ["/turnserver.sh"]
