FROM ubuntu:14.04

RUN apt-get update \
  && apt-get install -y \
     subversion

RUN mkdir -p /var/lib/test-files
RUN svn checkout http://files.openvidu.io/svn/kurento
