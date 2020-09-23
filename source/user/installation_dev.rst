=========================
Installing Nightly Builds
=========================

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

Kurento's Docker Hub contains images built from each KMS nightly version. Just head to the `kurento-media-server-dev Docker Hub page <https://hub.docker.com/r/kurento/kurento-media-server-dev>`__, and follow the instructions you'll find there. The nightly images work exactly the same as `Kurento Docker release images <https://hub.docker.com/r/kurento/kurento-media-server>`__, but just using nightly builds instead of release ones.



Local Installation
------------------

The steps to install a nightly version of Kurento Media Server are pretty much the same as those explained in :ref:`installation-local` -- with the only change of using *dev* instead of a version number, in the file ``/etc/apt/sources.list.d/kurento.list``.

Open a terminal and run these commands:

1. Make sure that GnuPG is installed.

   .. code-block:: console

      sudo apt-get update && sudo apt-get install --no-install-recommends --yes \
          gnupg

2. Add the Kurento repository to your system configuration.

   Run these commands:

   .. code-block:: console

      # Import the Kurento repository signing key
      sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83

      # Get Ubuntu version definitions
      source /etc/upstream-release/lsb-release 2>/dev/null || source /etc/lsb-release

      # Add the repository to Apt
      sudo tee "/etc/apt/sources.list.d/kurento.list" >/dev/null <<EOF
      # Kurento Media Server - Nightly packages
      deb [arch=amd64] http://ubuntu.openvidu.io/dev $DISTRIB_CODENAME kms6
      EOF

3. Install KMS:

   .. note::

      This step applies **only for a first time installation**. If you already have installed Kurento and want to upgrade it, follow instead the steps described here: :ref:`installation-local-upgrade`.

   .. code-block:: console

      sudo apt-get update && sudo apt-get install --no-install-recommends --yes \
          kurento-media-server

   This will install the nightly version of Kurento Media Server.



Kurento Java Client
===================

The development builds of the Kurento Java Client are made available for Maven in https://maven.openvidu.io/
To use these, you need to add first this repository to your Maven configuration.

Adding a repository to Maven can be done at three scope levels:

- **Project level**.

  This will add access to development builds only for the project where the configuration is done. Open the project's *pom.xml* and include this:

  .. code-block:: xml

     <project>
       ...
       <repositories>
         <repository>
           <id>kurento-snapshots</id>
           <name>Kurento Snapshots</name>
           <url>https://maven.openvidu.io/repository/snapshots/</url>
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
           <name>Kurento Snapshots</name>
           <url>https://maven.openvidu.io/repository/snapshots/</url>
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

  After this is included, there are two ways to use the updated versions:

  1. In the same *pom.xml*, look for the desired *<dependency>* and change its version. For example:

     .. code-block:: xml

        <dependency>
          <groupId>org.kurento</groupId>
          <artifactId>kurento-client</artifactId>
          <version>|VERSION_CLIENT_JAVA|-SNAPSHOT</version>
        </dependency>

  2. If you have not specified a dependency version, use the ``-U`` switch in your next Maven run to force updating all dependencies.

- **User and System levels**.

  The file *settings.xml* provides configuration for all projects, but its contents have a different reach depending on where it is located:

  - At ``$HOME/.m2/settings.xml``, it defines the settings that will be applied for a single user.
  - At ``/etc/maven/settings.xml``, it defines the settings that will be applied for all Maven users on a machine.

  To use this method, first edit the settings file at one of the mentioned locations, and include this:

  .. code-block:: xml

     <settings>
       ...
       <profiles>
         <profile>
           <id>kurento</id>
           <repositories>
             <repository>
               <id>kurento-snapshots</id>
               <name>Kurento Snapshots</name>
               <url>https://maven.openvidu.io/repository/snapshots/</url>
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
               <name>Kurento Snapshots</name>
               <url>https://maven.openvidu.io/repository/snapshots/</url>
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

  After this is included, use the ``-Pkurento`` switch in your next Maven run to enable the new profile, so all artifacts get downloaded into you local repository. Once in your local repository, Maven can successfully resolve the dependencies and the profile no longer needs to be activated in future runs.

For more information about adding snapshot repositories to Maven, check their official documentation: `Guide to Testing Development Versions of Plugins <https://maven.apache.org/guides/development/guide-testing-development-plugins.html>`__.



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
