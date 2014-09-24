.. _Kurento_Development:

%%%%%%%%%%%%%%%%%%%%%%%%%%%
Working with nightly builds
%%%%%%%%%%%%%%%%%%%%%%%%%%%

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
http://ubuntu.kurento.org. You can find current development version at
http://ubuntu.kurento.org/pool/main/k/kurento-media-server/.

To install packages from unstable repository you need to execute::

    sudo add-apt-repository ppa:kurento/kurento
    sudo apt-add-repository http://ubuntu.kurento.org
    wget -O - http://ubuntu.kurento.org/kurento.gpg.key | sudo apt-key add -
    sudo apt-get update
    sudo apt-get install kurento-media-server

As you can imagine, it is not possible to have installed at the same time latest
stable version and latest development version of Kurento Media Server.

Older versions can be manually downloaded from http://ubuntu.kurento.org/repo.
Notice dependencies will be downgraded as required by the old package. For
example::

    sudo dpkg -i kurento_4.2.5-16-g18d9c6~1.gbp18d9c6_i386.deb
    sudo apt-get -f install



Kurento Java Client
===================

The development builds of Kurento Java Client Maven artifacts hosted in
http://maven.kurento.org.

To use development versions, first you have to add this repository in your Maven
installation as a valid snapshot repository. To do this, add following
configuration repository to the repositories section to file
``~/.m2/settings.xml``:

.. sourcecode:: xml

   <repositories>
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
   </repositories>
   <pluginRepositories>
      <pluginRepository>
         <id>kurento-snapshots</id>
         <name>Kurento Snapshot Repository</name>
         <url>http://repository.kurento.com/archiva/repository/snapshots/</url>
         <releases>
            <enabled>false</enabled>
         </releases>
         <snapshots>
            <enabled>true</enabled>
         </snapshots>
      </pluginRepository>
   </pluginRepositories>

Then, you have to change the dependency in your application's ``pom.xml`` to
point to a development version. There is no way in Maven to use the latest
development version of an artifact. You have to specify the concrete
development version you want to depend on. To know what is the current Kurento
Java Client development version, you can take a look to the
`internal Kurento Maven repository <https://repository.kurento.com/archiva/browse/org.kurento/kurento-client>`_
and search for the latest version. Then, you have to include in your
application's pom.xml the following dependency:

.. sourcecode:: xml

   <dependency>
       <groupId>org.kurento</groupId>
       <artifactId>kurento-client</artifactId>
       <version>latest-version-SNAPSHOT</version>
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
