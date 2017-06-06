FROM selenium/node-base:2.53.1-rc1

USER root

ARG NODE_VERSION

# Configure Kurento's apt proxy
#RUN echo 'Acquire::http::Proxy "http://proxy.kurento.org:3142";' > /etc/apt/apt.conf.d/01proxy
# HTTPS repos must connect directly as apt-cacher does not support it
RUN echo 'Acquire::http::Proxy::deb.nodesource.com "DIRECT";' >> /etc/apt/apt.conf.d/01proxy

#===============
# Google Chrome
#===============
RUN wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - \
  && echo "deb http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google-chrome.list \
  && apt-get update -qqy \
  && apt-get -qqy install \
    google-chrome-stable \
  && rm /etc/apt/sources.list.d/google-chrome.list

#=========
# Firefox
#=========
RUN apt-key adv --keyserver keyserver.ubuntu.com --recv-keys AF316E81A155146718A6FBD7A6DCF7707EBC211F  \
  && echo "deb http://ppa.launchpad.net/ubuntu-mozilla-security/ppa/ubuntu trusty main" >> /etc/apt/sources.list.d/firefox.list \
  && apt-get update -qqy \
  && apt-get -qqy --no-install-recommends install \
    firefox \
  && rm -rf /etc/apt/sources.list.d/firefox.list

RUN apt-get update -y \
  && apt-get install -y -q \
    ca-certificates \
    curl \
    git

RUN curl -sL https://deb.nodesource.com/setup_$NODE_VERSION | bash - \
  && apt-get install -y nodejs


ENV PATH $PATH:/nodejs/bin

RUN npm -g install bower

RUN apt-get update \
  && apt-get install -y \
    ffmpeg

RUN apt-get update \
  && apt-get install -y \
    python \
    python-pip

USER seluser
