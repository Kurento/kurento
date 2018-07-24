#!/bin/bash

cd ..

git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git
DEPOT_PATH="$PWD/depot_tools"
export PATH=$DEPOT_PATH:$PATH

echo "solutions = [
  {
    \"managed\": False,
    \"name\": \"src\",
    \"url\": \"ssh://code.kurento.org:12345/libjingle\",
    \"custom_deps\": {},
    \"deps_file\": \"DEPS\",
    \"safesync_url\": \"\",
  },
]" > .gclient

mv libjingle src

gclient sync

cd src

#yes | sudo postpone -d -f ./build/install-build-deps.sh || exit 1

DEBIAN_FRONTEND=noninteractive sudo postpone -d -f apt-get install --force-yes -y libasound2:i386 libcap2:i386 libelf-dev:i386 libexif12:i386 libfontconfig1:i386 libgconf-2-4:i386 libgl1-mesa-glx:i386 libglib2.0-0:i386 libgpm2:i386 libgtk2.0-0:i386 libncurses5:i386 libnss3:i386 libpango1.0-0:i386 libssl1.0.0:i386 libtinfo-dev:i386 libudev1:i386 libxcomposite1:i386 libxcursor1:i386 libxdamage1:i386 libxi6:i386 libxrandr2:i386 libxss1:i386 libxtst6:i386 linux-libc-dev:i386 ant apache2.2-bin autoconf bison cdbs cmake curl devscripts dpkg-dev elfutils fakeroot flex fonts-thai-tlwg g++ g++-4.8-multilib g++-4.8-multilib-arm-linux-gnueabihf g++-arm-linux-gnueabihf gawk gcc-4.8-multilib-arm-linux-gnueabihf git-core git-svn g++-mingw-w64-i686 gperf intltool language-pack-da language-pack-fr language-pack-he language-pack-zh-hant lib32gcc1 lib32ncurses5-dev lib32stdc++6 lib32z1-dev libapache2-mod-php5 libasound2 libasound2-dev libatk1.0-0 libatk1.0-dbg libav-tools libbluetooth-dev libbrlapi0.6 libbrlapi-dev libbz2-1.0 libbz2-dev libc6 libc6-dbg libc6-dev-armhf-cross libc6-i386 libcairo2 libcairo2-dbg libcairo2-dev libcap2 libcap-dev libcups2 libcups2-dev libcurl4-gnutls-dev libdrm-dev libelf-dev libexif12 libexif-dev libexpat1 libfontconfig1 libfontconfig1-dbg libfreetype6 libgbm-dev libgconf2-dev libgl1-mesa-dev libgles2-mesa-dev libglib2.0-0 libglib2.0-0-dbg libglib2.0-dev libglu1-mesa-dev libgnome-keyring0 libgnome-keyring-dev libgtk2.0-0 libgtk2.0-0-dbg libgtk2.0-dev libjpeg-dev libkrb5-dev libnspr4 libnspr4-dbg libnspr4-dev libnss3 libnss3-dbg libnss3-dev libpam0g libpam0g-dev libpango1.0-0 libpango1.0-0-dbg libpci3 libpci-dev libpcre3 libpcre3-dbg libpixman-1-0 libpixman-1-0-dbg libpng12-0 libpulse0 libpulse-dev libsctp-dev libspeechd2 libspeechd-dev libsqlite3-0 libsqlite3-0-dbg libsqlite3-dev libssl-dev libstdc++6 libstdc++6-4.8-dbg libtinfo-dev libtool libudev1 libudev-dev libwww-perl libx11-6 libx11-6-dbg libxau6 libxau6-dbg libxcb1 libxcb1-dbg libxcomposite1 libxcomposite1-dbg libxcursor1 libxcursor1-dbg libxdamage1 libxdamage1-dbg libxdmcp6 libxdmcp6-dbg libxext6 libxext6-dbg libxfixes3 libxfixes3-dbg libxi6 libxi6-dbg libxinerama1 libxinerama1-dbg libxkbcommon-dev libxrandr2 libxrandr2-dbg libxrender1 libxrender1-dbg libxslt1-dev libxss-dev libxt-dev libxtst6 libxtst6-dbg libxtst-dev linux-libc-dev-armhf-cross mesa-common-dev openbox patch perl php5-cgi pkg-config python python-cherrypy3 python-crypto python-dev python-numpy python-opencv python-openssl python-psutil realpath rpm ruby subversion texinfo ttf-dejavu-core ttf-indic-fonts ttf-kochi-gothic ttf-kochi-mincho wdiff xfonts-mathml xsltproc xutils-dev xvfb zip zlib1g zlib1g-dbg || exit 1

# Removed package from istall: msttcorefonts

ninja -C out/Release

cd ..
mv src libjingle
