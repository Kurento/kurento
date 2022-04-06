=========================
Installing Nightly Builds
=========================

.. contents:: Table of Contents

Some components of KMS are built nightly, with the code developed during that same day. Other components are built immediately when code is merged into the source repositories.

These builds end up being uploaded to *Development* repositories so they can be installed by anyone. Use these if you want to develop *Kurento itself*, or if you want to try the latest changes before they are officially released.

.. warning::

   Nightly builds always represent the current state on the software development; 99% of the time this is stable code, very close to what will end up being released.

   However, it's also possible (although unlikely) that these builds might include undocumented changes, regressions, bugs or deprecations. It's safer to be conservative and avoid using nightly builds in a production environment, unless you have a strong reason to do it.

.. note::

   If you are looking to build KMS from the source code, then you should check the section aimed at development of *KMS itself*: :ref:`dev-sources`.



Kurento Media Server
====================

Docker image
------------

While official release Docker images are published as `kurento/kurento-media-server <https://hub.docker.com/r/kurento/kurento-media-server>`__, nightly builds with the latest development progress are available as `kurento/kurento-media-server-dev <https://hub.docker.com/r/kurento/kurento-media-server-dev>`__ (notice the ``-dev``). Other than that, these images behave exactly like the release ones. For usage instructions check out this section: :ref:`installation-docker`.



Local Installation
------------------

The steps to install a nightly version of Kurento Media Server are pretty much the same as those explained in :ref:`installation-local` -- with the only change of using *dev* instead of a version number, in the file ``/etc/apt/sources.list.d/kurento.list``.

Open a terminal and run these commands:

1. Make sure that GnuPG is installed.

   .. code-block:: shell

      sudo apt-get update ; sudo apt-get install --no-install-recommends \
          gnupg

2. Add the Kurento repository to your system configuration.

   Run these commands:

   .. code-block:: shell

      # Import the Kurento repository signing key
      sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83

      # Get Ubuntu version definitions
      source /etc/lsb-release

      # Add the repository to Apt
      sudo tee "/etc/apt/sources.list.d/kurento.list" >/dev/null <<EOF
      # Kurento Media Server - Nightly packages
      deb [arch=amd64] http://ubuntu.openvidu.io/dev $DISTRIB_CODENAME kms6
      EOF

3. Install KMS:

   .. note::

      This step applies **only for a first time installation**. If you already have installed Kurento and want to upgrade it, follow instead the steps described here: :ref:`installation-local-upgrade`.

   .. code-block:: shell

      sudo apt-get update ; sudo apt-get install --no-install-recommends \
          kurento-media-server

   This will install the nightly version of Kurento Media Server.



Kurento Java Client
===================

Development builds of Kurento Java packages are uploaded to the `GitHub Maven Repository <https://github.com/orgs/Kurento/packages>`__.

This repository can be added to the Maven configuration at the **Project**, **User**, or **System** levels.

For more information about adding a snapshots repository to Maven, check the official documentation: `Guide to Testing Development Versions of Plugins <https://maven.apache.org/guides/development/guide-testing-development-plugins.html>`__.



Project level
-------------

This adds access to development builds only for a single project. Open the project's ``pom.xml`` and include this:

.. code-block:: xml

   <project>
     ...
     <repositories>
       <repository>
         <id>kurento-github-public</id>
         <name>Kurento GitHub Maven packages (public access)</name>
         <url>https://public:&#103;hp_tFHDdd4Nh9GqKSaoPjnFIXrb0PFsUh258gzV@maven.pkg.github.com/kurento/*</url>
         <releases>
           <enabled>false</enabled>
         </releases>
         <snapshots>
           <enabled>true</enabled>
         </snapshots>
       </repository>
     </repositories>
     ...
   </project>

Afterwards, in the same ``pom.xml``, look for the desired dependency and change its version to a snapshot one. For example:

.. code-block:: xml

   <dependency>
     <groupId>org.kurento</groupId>
     <artifactId>kurento-client</artifactId>
     <version>|VERSION_CLIENT_JAVA|-SNAPSHOT</version>
   </dependency>



User and System levels
----------------------

Add the snapshots repository to either of your *User* or *System* ``settings.xml`` file:

- At ``$HOME/.m2/settings.xml``, the configuration applies only to the current user.
- At ``/etc/maven/settings.xml``, the configuration applies to all users on the machine.

Edit one of the mentioned settings files, and include this:

.. code-block:: xml

   <settings>
     ...
     <profiles>
       <profile>
         <id>snapshots</id>
         <repositories>
           <repository>
             <id>kurento-github-public</id>
             <name>Kurento GitHub Maven packages (public access)</name>
             <url>https://public:&#103;hp_tFHDdd4Nh9GqKSaoPjnFIXrb0PFsUh258gzV@maven.pkg.github.com/kurento/*</url>
             <releases>
               <enabled>false</enabled>
             </releases>
             <snapshots>
               <enabled>true</enabled>
             </snapshots>
           </repository>
         </repositories>
       </profile>
     </profiles>
     ...
   </settings>

..
   NOTE FOR EDITORS:
   The <url> does basic auth via GitHub Access Token with the `read:packages` scope.
   Generated with `docker run ghcr.io/jcansdale/gpr encode <Token>`.
   This is provided to work around the GitHub limitation of not allowing
   anonymous downloads from their Maven package registry.
   More details here: https://github.community/t/download-from-github-package-registry-without-authentication/14407/111

Then use the ``-Psnapshots`` argument in your next Maven run, to enable the new profile. For example:

.. code-block:: shell

   mvn -Psnapshots clean package

.. code-block:: shell

   mvn dependency:get -Psnapshots -Dartifact='org.kurento:kurento-client:6.16.4-SNAPSHOT'



Kurento JavaScript Client
=========================

Node.js
-------

If you are using the Kurento JavaScript Client from a Node.js application and want to use the latest development version of this library, you have to change the *dependencies* section in the application's *package.json*. This way, NPM will point directly to the development repository:

.. code-block:: js

   "dependencies": {
       "kurento-client": "Kurento/kurento-client-js#master",
   }


Browser JavaScript
------------------

If you are using the Kurento JavaScript Client from a browser application, with Bower to handle JS dependencies, and want to use the latest development version of this library, you have to change the *dependencies* section in the application's *bower.json*. This way, Bower will point directly to the development repository:

.. code-block:: js

   "dependencies": {
       "kurento-client": "master",
       "kurento-utils": "master",
   }

Alternatively, if your browser application is pointing directly to JavaScript libraries from HTML resources, then you have to change to development URLs:

.. code-block:: html

   <script type="text/javascript"
       src="http://builds.openvidu.io/dev/master/latest/js/kurento-client.min.js">
   </script>
