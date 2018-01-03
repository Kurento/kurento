.. _Kurento_Development:

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Installing Pre-Release Builds
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

[TODO full review]
[TODO move "pre-release install" from /user/installation to here]
[TODO check links]

Kurento is composed by several components. Each component is being developed
with very different technologies.

* **Kurento Media Server:** This is the core component of Kurento. It is
  implemented using C/C++ languages, and the GStreamer Framework.
* **Kurento Java Client:** This is implemented in Java with Maven and Spring.
* **Kurento JavaScript Client:** This component is implemented in JavaScript
  with Node.js and NPM.

In this section, we will see how to use nightly compiled versions of Kurento
code base. This is not the recommended way to use Kurento, but can be useful if
you are testing brand new features.

We'll also explain in detail how Kurento can be built from sources.


.. _using_nightly_versions:

Using development versions
--------------------------

Some components of KMS are built nightly, with the code developed during that
same day. Other components are built immediately when code is merged into
the source repositories.

.. warning:: You have to use these versions with caution, because they can be
   broken. Usually they have bugs and incomplete functionalities. **Never** use
   development versions in production.


Kurento Media Server
====================

Documentation for developers is being slowly added directly to the source
repositories of Kurento. If you are a developer and you'd like to read further
about the organization of Kurento components, together with instructions for
installing all required development requirements, please check this document:

https://github.com/Kurento/doc-kurento/blob/master/static/kms_development_guide.md

Also, there is a document which outlines all the Events that can be raised by
KMS through the WebRTCEndpoint, and received by a Client application:

https://github.com/Kurento/doc-kurento/blob/master/static/kms_webrtc_endpoint_events.md


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
          <url>http://maven.kurento.org/snapshots/</url>
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
         <url>http://maven.kurento.org/snapshots/</url>
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
`internal Kurento Maven repository <http://maven.kurento.org/archiva/browse/org.kurento/kurento-client>`_
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
in some cases, the code has to be "processed" to be used by client applications.

Node.js development
~~~~~~~~~~~~~~~~~~~

If you are using Kurento JavaScript Client from a Node.js application and want
to use the latest development version of this library, you have to change the
``dependencies`` section in the application's ``package.json``. You have to
point directly to the development repository, that is:

.. sourcecode:: js

   "dependencies": {
       "kurento-client": "Kurento/kurento-client-js#master"
   }

Browser JavaScript development
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

If you are using Kurento JavaScript Client from a browser application with Bower
and want to use the latest development version of this library, you have to
change the ``dependencies`` section in the application's ``bower.json``. You
have to point directly to the development bower repository, that is:

.. sourcecode:: js

   "dependencies": {
       "kurento-client": "master"
       "kurento-utils": "master"
   }

Alternatively, if your browser application is pointing directly to JavaScript
libraries from HTML resources, then, you have to change to development URLs:

.. sourcecode:: html

   <script type="text/javascript"
       src="http://builds.kurento.org/dev/master/latest/js/kurento-client.min.js"></script>
