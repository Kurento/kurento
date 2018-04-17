FROM phusion/baseimage:0.10.0
LABEL maintainer fede diaz nordri@gmail.com

RUN apt-get update && \
	apt-get install -y coturn net-tools && \
	apt-get -y autoremove && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

ADD entrypoint.sh /entrypoint
ENTRYPOINT [ "/entrypoint" ]
