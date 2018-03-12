FROM      ubuntu:16.04
MAINTAINER Patxi Gortázar <patxi.gortazar@gmail.com>
MAINTAINER Fede Diaz <nordri@gmail.com>

RUN apt-get update \
  && apt-get -y dist-upgrade \
  && apt-get install -y software-properties-common curl

RUN	\
 	apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83 && \
	add-apt-repository "deb http://ubuntu.openvidu.io/6.7.0 xenial kms6" && \
	apt-get update && \
	apt-get -y install kurento-media-server && \
	apt-get clean && \
	rm -rf /var/lib/apt/lists/*

EXPOSE 8888

COPY ./entrypoint.sh /entrypoint.sh
COPY ./healthchecker.sh /healthchecker.sh

HEALTHCHECK --start-period=15s --interval=30s --timeout=3s --retries=1 CMD /healthchecker.sh

ENV GST_DEBUG=Kurento*:5

ENTRYPOINT ["/entrypoint.sh"]

