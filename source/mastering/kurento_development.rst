.. _Kurento_Development:

%%%%%%%%%%%%%%%%%%%
Kurento Development
%%%%%%%%%%%%%%%%%%%

Kurento is composed by several components. Each component is being developed
with very different technologies.

* **Kurento Media Server:** This is the core component of Kurento. It is
  implemented using C/C++ and GStreamer platform.
* **Kurento Java Client:** This Kurento Client is implemented in Java with
  Maven and Sprint.
* **Kurento JavaScript Client:** This Kurento Client is implemented in
  JavaScript with Node.js and NPM.

In this section, we will see how to use nightly compiled versions of Kurento
code base. This is not the recommended way to use Kurento, but can be useful if
you are testing brand new features.

We'll also explain in detail how Kurento can be built from sources. This is a
complex task because Kurento uses several technologies, although it can be very
funny ;)

.. _using_nightly_versions:

Using development versions
--------------------------

In this section we are going to explain how to use development versions of
Kurento. We build every Kurento component at least once a day as we follow the
*Continuous Integration* principles.

Some components are build nightly, with the code developed that day. Other
components are created automatically when code is merged into source repository.

Using development versions is not the recommended way to use Kurento, but it can
be useful to try brand new features.

.. warning:: You have to use this versions with caution, because them can be
   broken. Usually they have bugs and incomplete functionalities. **Never** use
   development versions in production.

Kurento Media Server
====================

The development builds of Kurento Media Server are .deb packages hosted in
http://ubuntu.kurento.org.

The following procedure is needed to setup Ubuntu to automatically get latest
package from the development repository. This needs to be done only once::

    sudo add-apt-repository ppa:kurento/kurento
    echo "deb http://ubuntu.kurento.org repo/" | sudo tee -a /etc/apt/sources.list > /dev/null

Every time you want to install the latest development version, you have to
execute the following commands::

    sudo apt-get update
    sudo apt-get install kurento-media-server

As you can imagine, it is not possible to have installed at the same time latest
stable version and latest development version of Kurento Media Server.

Older versions can be manually downloaded from http://ubuntu.kurento.org/repo.
Notice dependencies will be downgraded as required by the old package::

    sudo dpkg -i kurento_4.2.5-16-g18d9c6~1.gbp18d9c6_i386.deb
    sudo apt-get -f install

To install 32-bit versions instead of 64-bit ones, the setup of Kurento
repository is different::

    sudo add-apt-repository ppa:kurento/kurento
    echo "deb http://ubuntu.kurento.org repo/" | sudo tee -a /etc/apt/sources.list > /dev/null

Kurento Java Client
===================

The development builds of Kurento Java Client Maven artifacts hosted in
http://maven.kurento.org.

To use development versions, first you have to add this repository in your Maven
installation as a valid snapshot repository. To do this, add following
configuration repository to the repositories section to file
``~/.m2/settings.xml``:

.. sourcecode:: xml

   <repository>
       <id>kurento-snapshots</id>
       <name>Kurento Snapshot Repository</name>
       <url>http://maven.kurento.org/archiva/repository/snapshots/</url>
       <releases>
           <enabled>false</enabled>
       </releases>
       <snapshots>
           <enabled>true</enabled>
       </snapshots>
   </repository>

Then, you have to change the dependency in your application's ``pom.xml`` to
point to a development version. There is no way in Maven to use the latest
development version of an artifact. You have to specify the concrete
development version you want to depend on. To know what is the current Kurento
Java Client development version, you can take a look to its
`pom.xml <https://github.com/Kurento/kurento-java/blob/develop/kurento-client/pom.xml>`_
and search for the version tag. For example, if you see something like:

.. sourcecode:: xml

      <parent>
         <groupId>org.kurento</groupId>
         <artifactId>kurento-java-parent</artifactId>
         <version>|version_dev|</version>
         <relativePath>../kurento-java-parent</relativePath>
      </parent>

The development version is ``|version_dev|`` and you have to include in your
application's pom.xml the following dependency:

.. sourcecode:: xml

   <dependency>
       <groupId>org.kurento</groupId>
       <artifactId>kurento-client</artifactId>
       <version>|version_dev|</version>
   </dependency>

Kurento JavaScript Client
=========================

JavaScript is special because in some cases there is no need to build anything.
JavaScript is a scripting language that can execute directly from sources. But
in some cases, the code have to be "processed" to be used from client
applications.

Node.js development
~~~~~~~~~~~~~~~~~~~

If you are using Kurento JavaScript Client from a Node.js application and want
to use the latest development version of this library, you have to change the
``dependencies`` section in the application's ``package.json``. You have to
point directly to the development repository, that is:

.. sourcecode:: js

   "dependencies": {
       "kurento-client": "https://github.com/Kurento/kurento-client-js#develop"
   }

Browser JavaScript development
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If you are using Kurento JavaScript Client from a browser application with Bower
and want to use the latest development version of this library, you have to
change the ``dependencies`` section in the application's ``bower.json``. You
have to point directly to the development bower repository, that is:

.. sourcecode:: js

   "dependencies": {
       "kurento-client": "https://github.com/Kurento/kurento-client-js-bower#develop"
       "kurento-utils": "https://github.com/Kurento/kurento-utils-js-bower#develop"
   }

Alternatively, if your browser application is pointing directly to JavaScript
libraries from HTML resources, then, you have to change to development URLs:

.. sourcecode:: html

   <script type="text/javascript" src="http://builds.kurento.org/dev/latest/js/kurento-client-js.min.js"></script>

Build Kurento from sources
--------------------------

As Kurento has several components implemented with very different technologies,
build it from source can be a challenging task. In the following subsections we
will explain the building process in detail for each component.

.. todo:: We need to explain in some place how to generate code from Kurento
   Modules and how to build the Kurento Module Creator. If we don't do so, we
   are explaining only the half of the history to build from sources.

Kurento Media Server
====================

To build Kurento Media Server you need an **Ubuntu 14.04 LTS** operating system.

First you have to install all needed development tools and dependencies
executing the following commands::

     sudo add-apt-repository ppa:kurento/kurento
     sudo apt-get update
     sudo apt-get install git
     sudo apt-get install libthrift-dev thrift-compiler libjsoncpp-dev
     sudo apt-get install gstreamer1.0* libgstreamer1.0-dev
     sudo apt-get install libgstreamer-plugins-base1.0-dev libnice-dev gtk-doc-tools
     sudo apt-get install cmake libglibmm-2.4-dev uuid-dev libevent-dev libboost-dev
     sudo apt-get install libboost-system-dev libboost-filesystem-dev
     sudo apt-get install libboost-test-dev libsctp-dev
     sudo apt-get install libopencv-dev autoconf git libjsoncpp-dev
     sudo apt-get install libtool libsoup2.4-dev tesseract-ocr-dev tesseract-ocr-eng
     sudo apt-get install libgnutls28-dev gnutls-bin libvpx-dev

Plase take care that the list of dependencies changes as dependencies change and
new features are added, you can check the actual packages that the Ubuntu PPA
needs for building from sources in the ``Build-Depends`` of the latest release
`debian/control file <https://github.com/Kurento/kurento-media-server/blob/master/debian/control>`__.

Second you have to get the source code clonning git repository::

     git clone https://github.com/Kurento/kurento-media-server.git

Then you can compile Kurento Media Server::

    mkdir build
    cd build
    cmake ..
    make

Finally, install it into the system as a service::

    git submodule update --recursive --init
    dpkg-buildpackage -us -uc
    sudo dpkg -i ../kurento_<version>_<arch>.deb

And start and stop it::

    sudo service kurento-media-server start
    sudo service kurento-media-server stop

Kurento Java Client
===================

To build Kurento Java Client, be sure you have Java, Maven and Git installed. If
not, install it with::

    sudo apt-get install openjdk-7-jdk
    sudo apt-get install git
    sudo apt-get install maven

First, you have to get the source code clonning git repository::

    git clone https://github.com/Kurento/kurento-java.git

Then, you compile, package and install the project ``kurento-client``::

    cd kurento-java/kurento-client
    mvn install -DskipTests

You are ready to use the just compiled Kurento Java Client in your application.
Remember that you have to change the application's ``pom.xml`` to point to the
newly created client. To know this version, just take a look to
``kurento-java/kurento-client/pom.xml`` file.

Kurento JavaScript Client
=========================

To build Kurento JavaScript Client, be sure you have Node.js and Git installed.
If not, install it with::

   sudo add-apt-repository ppa:chris-lea/node.js
   sudo apt-get update
   sudo apt-get install nodejs
   sudo apt-get install git

First, you have to get the source code clonning git repository::

   git clone https://github.com/Kurento/kurento-client-js

Then, you compile, package and install the project ``kurento-client``::

   cd kurento-client-js
   npm install
