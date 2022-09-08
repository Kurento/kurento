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

While official Kurento releases are published as Docker images and tagged with a release number, the latest development progress is tagged with `dev`: `kurento/kurento-media-server <https://hub.docker.com/r/kurento/kurento-media-server/tags>`__ (notice the ``dev-*`` tags). Other than that, these images behave exactly like the release ones. For usage instructions check out this section: :ref:`installation-docker`.



.. _installation-dev-local:

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

This repo can be configured once per-User (by editing Maven's global ``settings.xml``), or it can be added per-Project, to every ``pom.xml``. We recommend using the first method.

For more information about adding a snapshots repository to Maven, check the official documentation: `Guide to Testing Development Versions of Plugins <https://maven.apache.org/guides/development/guide-testing-development-plugins.html>`__.



Per-User config
---------------

Add the snapshots repository to your Maven settings file: ``$HOME/.m2/settings.xml``. If this file doesn't exist yet, you can copy it from ``/etc/maven/settings.xml``, which offers a nice default template to get you started.

Edit the settings file to include this:

.. code-block:: xml

   <settings>
       ...
       <profiles>
           <profile>
               <id>snapshot</id>
               <repositories>
                   <repository>
                       <id>kurento-github-public</id>
                       <name>Kurento GitHub Maven packages (public access)</name>
                       <url>https://public:&#103;hp_fW4yqnUBB4LZvk8DE6VEbsu6XdnSBZ466WEJ@maven.pkg.github.com/kurento/*</url>
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
                       <id>kurento-github-public</id>
                       <name>Kurento GitHub Maven packages (public access)</name>
                       <url>https://public:&#103;hp_fW4yqnUBB4LZvk8DE6VEbsu6XdnSBZ466WEJ@maven.pkg.github.com/kurento/*</url>
                       <releases>
                           <enabled>false</enabled>
                       </releases>
                       <snapshots>
                           <enabled>true</enabled>
                       </snapshots>
                   </pluginRepository>
               </pluginRepositories>
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

Then use the ``-Psnapshot`` argument in your Maven commands, to enable the new profile. For example:

.. code-block:: shell

   mvn -Psnapshot clean package

.. code-block:: shell

   mvn dependency:get -Psnapshot -Dartifact='org.kurento:kurento-client:6.12.0-SNAPSHOT'

If you don't want to change all your Maven commands, it is possible to mark the profile as active by default. This way, a ``-Psnapshot`` argument will always be implicitly added, so all calls to Maven will already use the profile:

.. code-block:: xml

   <settings>
       ...
       <profiles>
           <profile>
               <id>snapshot</id>
               ...
           </profile>
       </profiles>
       <activeProfiles>
           <activeProfile>snapshot</activeProfile>
       </activeProfiles>
       ...
   </settings>



Per-Project config
------------------

This method consists on explicitly adding access to the snapshots repository, for a specific project. Open the project's ``pom.xml`` and include this:

.. code-block:: xml

   <project>
       ...
       <repositories>
           <repository>
               <id>kurento-github-public</id>
               <name>Kurento GitHub Maven packages (public access)</name>
               <url>https://public:&#103;hp_fW4yqnUBB4LZvk8DE6VEbsu6XdnSBZ466WEJ@maven.pkg.github.com/kurento/*</url>
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
               <id>kurento-github-public</id>
               <name>Kurento GitHub Maven packages (public access)</name>
               <url>https://public:&#103;hp_fW4yqnUBB4LZvk8DE6VEbsu6XdnSBZ466WEJ@maven.pkg.github.com/kurento/*</url>
               <releases>
                   <enabled>false</enabled>
               </releases>
               <snapshots>
                   <enabled>true</enabled>
               </snapshots>
           </pluginRepository>
       </pluginRepositories>
       ...
   </project>

Afterwards, in the same ``pom.xml``, look for the desired dependency and change its version to a snapshot one. For example:

.. code-block:: xml

   <dependency>
       <groupId>org.kurento</groupId>
       <artifactId>kurento-client</artifactId>
       <version>6.12.0-SNAPSHOT</version>
   </dependency>



Kurento JavaScript Client
=========================

Node.js
-------

If you are using the Kurento JavaScript Client from a Node.js application and want to use the latest development version of this library, you have to change the *dependencies* section in the application's *package.json*. This way, NPM will point directly to the development repository:

.. code-block:: js

   "dependencies": {
     "kurento-client": "git+https://github.com/Kurento/kurento-client-js.git#master"
   }


Browser JavaScript
------------------

If you are using the Kurento JavaScript Client from a browser application, with Bower to handle JS dependencies, and want to use the latest development version of this library, you have to change the *dependencies* section in the application's *bower.json*. This way, Bower will point directly to the development repository:

.. code-block:: js

   "dependencies": {
     "kurento-client": "git+https://github.com/Kurento/kurento-client-bower.git#master",
     "kurento-utils": "git+https://github.com/Kurento/kurento-utils-bower.git#master"
   }

Alternatively, if your browser application is pointing directly to JavaScript libraries from HTML resources, then you have to change to development URLs:

.. code-block:: html

   <script type="text/javascript"
       src="http://builds.openvidu.io/dev/master/latest/js/kurento-client.min.js">
   </script>
