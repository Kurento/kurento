=======================
Writing Kurento Modules
=======================

.. contents:: Table of Contents



Scaffolding and development
===========================

You can develop your own modules to expand the features of Kurento Media Server. There are two main flavors of Kurento modules:

* Modules based on :term:`OpenCV`. These are recommended if you would like to add features such as **Computer Vision** or **Augmented Reality**.

* Modules based on :term:`GStreamer`. This kind of modules provide a generic entry point for media processing within the GStreamer framework. Such modules are more powerful, but also they are more difficult to develop. It is necessary to have good knowledge of GStreamer development.

The starting point to develop a filter is to create a basic structure for the source code, what we'll call the *scaffolding*. This is done with the *kurento-module-scaffold* tool, which comes included with the *kurento-media-server-dev* package. To install it, run this command:

.. code-block:: shell

   sudo apt-get update ; sudo apt-get install --no-install-recommends \
       kurento-media-server-dev

Now use the scaffold tool to generate code for your new module:

.. code-block:: shell

   kurento-module-scaffold <CamelCaseName> <SnakeCaseName> [IsOpenCV]

For example:

* For an OpenCV module:

  .. code-block:: shell

     kurento-module-scaffold MyOpenCVModule my-opencv-module true

* For a GStreamer module:

  .. code-block:: shell

     kurento-module-scaffold MyGstModule my-gst-module false

The scaffolding tool generates a complete folder tree, with all the needed *CMakeLists.txt* files to build with CMake. You'll also find empty Kurento Module Descriptor files (``*.kmd.json``), which must contain a complete description of the module: constructor, methods, properties, events, and the complex types defined by the developer.

Once your *.kmd* files have been filled with a complete description of the module, it is time to generate the corresponding server stub code with *kurento-module-creator*. Run this from the root directory of your module:

.. code-block:: shell

   mkdir build/ ; cd build/
   cmake ..
   make

If working with a GStreamer module, now you can verify that the new module can be successfully loaded by GStreamer, with these commands:

.. code-block:: shell

   # To check if the plugin is found and loaded:
   gst-inspect-1.0 --gst-plugin-path="$PWD/src/gst-plugins" | grep -i {modulename}

   # To inspect all metadata exported by the plugin:
   gst-inspect-1.0 --gst-plugin-path="$PWD/src/gst-plugins" {modulename}

   # To test the plugin directly with some video input
   # (this is just a sample for raw video; adapt as necessary!)
   gst-launch-1.0 --gst-plugin-path="$PWD/src/gst-plugins" \
       uridecodebin uri='file:///path/to/video.mp4' ! videoconvert \
       ! {modulename} \
       ! videoconvert ! autovideosink

Note that ``{modulename}`` is the name of your module in all lowercase.

The following sections detail how to create your module, depending on the filter type you chose (OpenCV or GStreamer).



OpenCV module
-------------

There are several files in ``src/server/implementation/objects/``:

.. code-block:: text

   {ModuleName}Impl.cpp
   {ModuleName}Impl.hpp
   {ModuleName}OpenCVImpl.cpp
   {ModuleName}OpenCVImpl.hpp

The first two files contain the server-side implementation of the JSON-RPC API, and normally you won't need to modify them. The last two files will contain the logic of your module.

The file ``{ModuleName}OpenCVImpl.cpp`` contains functions to deal with the methods and the parameters (you must implement the logic). Also, this file contains a class method called **process**. This function will be called with each new frame, so you must implement the logic of your filter in there.



GStreamer module
----------------

In this case, these are the files that you'll find under ``src/``:

* ``src/gst-plugins/`` contains the implementation of your GStreamer Element:

  .. code-block:: text

     gst{modulename}.cpp
     gst{modulename}.h
     {modulename}.c

* ``src/server/implementation/objects/`` contains the server-side implementation of the JSON-RPC API:

  .. code-block:: text

     {ModuleName}Impl.cpp
     {ModuleName}Impl.hpp

In the file ``{ModuleName}Impl.cpp`` you have to invoke the methods of your GStreamer element. The actual module logic should be implemented in the GStreamer Element.



For both kind of modules
------------------------

If you need extra compilation dependencies, you can add compilation rules to the *kurento-module-creator* using the function *generate_code* in the ``src/server/CMakeLists.txt`` file.

The following parameters are available:

* *SERVER_STUB_DESTINATION* (required)

  The generated code that you may need to modify will be generated on the folder indicated by this parameter.

* *MODELS* (required)

  This parameter receives the folders where the models (*.kmd* files) are located.

* *INTERFACE_LIB_EXTRA_SOURCES*, *INTERFACE_LIB_EXTRA_HEADERS*, *INTERFACE_LIB_EXTRA_INCLUDE_DIRS*, *INTERFACE_LIB_EXTRA_LIBRARIES*

  These parameters allow to add additional source code to the static library. Files included in *INTERFACE_LIB_EXTRA_HEADERS* will be installed in the system as headers for this library. All the parameters accept a list as input.

* *SERVER_IMPL_LIB_EXTRA_SOURCES*, *SERVER_IMPL_LIB_EXTRA_HEADERS*, *SERVER_IMPL_LIB_EXTRA_INCLUDE_DIRS*, *SERVER_IMPL_LIB_EXTRA_LIBRARIES*

  These parameters allow to add additional source code to the interface library.  Files included in *SERVER_IMPL_LIB_EXTRA_HEADERS* will be installed in the system as headers for this library. All the parameters accept a list as input.

* *MODULE_EXTRA_INCLUDE_DIRS*, *MODULE_EXTRA_LIBRARIES*

  These parameters allow to add extra include directories and libraries to the module.

* *SERVER_IMPL_LIB_FIND_CMAKE_EXTRA_LIBRARIES*

  This parameter receives a list of strings. Each string has this format:

  .. code-block:: text

     libname [VersionRange]

  where *[VersionRange]* can use these symbols: ``AND``, ``OR``, ``<``, ``<=``, ``>``, ``>=``, ``^``, and ``~``.

  .. note::

     * ``^`` indicates a "compatible" version, under the definition of :term:`Semantic Versioning`.
     * ``~`` indicates a "similar" version, again according to the definition of SemVer.



Installation and usage
======================

Before being able to use your new module, its binary files must be installed to the host where Kurento Media Server is running. Using a module with Kurento comprises two sides of the same coin:

1. Install the module. This allows KMS to know about the module, and allows the media server to use it when clients attempt to instantiate a new object that is exported by the module code.

2. Use the module from client applications. Technically this step is optional, but unless your application directly implements the :doc:`Kurento Protocol </features/kurento_protocol>`, you will want to use the client-side module API. This is done by using client code that gets automatically generated from the Kurento Module Descriptor files (``*.kmd.json``).



Installing locally
------------------

The recommended way to distribute a module is to build it into a Debian package file (``*.deb``). This is the easiest and most convenient method for end users of the module, as they will just have to perform a simple package installation on any system where KMS is already running. Besides, this doesn't require the user to know anything about plugin paths or how the module files must be laid out on disk.

To build a Debian package file, you can either use the **kurento-buildpackage** tool as described in :ref:`dev-packages`, or do it manually by installing and running the appropriate tools:

.. code-block:: shell

   # Install dpkg-buildpackage, the Debian package builder
   sudo apt-get update ; sudo apt-get install --no-install-recommends \
       dpkg-dev

   # Run dpkg-buildpackage to build Debian packages
   dpkg-buildpackage -us -uc

   # Copy the generated packages to their final destination
   cp ../*.*deb /path/to/destination/

The Debian builder tool ends up generating one or more *.deb* package files **in the parent directory** from where it was called, together with some additional files that can be ignored. For example:

.. code-block:: shell-session

   $ ls -1 ../*.*deb
   ../my-gst-module-dev_0.0.1~rc1_amd64.deb
   ../my-gst-module_0.0.1~rc1_amd64.deb

Depending on the contents of the module project, the Debian package builder can generate multiple *.deb* files:

* The file without any suffix contains the shared library code that has been compiled from our source code. This is the file that end users of the module will need to install in their systems.
* *-dev* packages contain header files and are used by *other developers* to build their software upon the module's code. This is not needed by end users.
* *-doc* packages usually contain *manpages* and other documentation, if the module contained any.
* *-dbg* and *-dbgsym* packages contain the debug symbols that have been extracted from the compilation process. It can be used by other developers to troubleshoot crashes and provide bug reports.

Now copy and install the package(s) into any Debian or Ubuntu based system where KMS is already installed:

.. code-block:: shell

   sudo dpkg -i my-gst-module_0.0.1~rc1_amd64.deb

For more information about the process of creating Debian packages, check these resources:

* `Debian Building Tutorial <https://wiki.debian.org/BuildingTutorial>`__
* `Debian Policy Manual <https://www.debian.org/doc/debian-policy/index.html>`__

**Alternatively**, it is also possible to just build the module and manually copy its binary files to the destination system. You can then define the following environment variables in the file ``/etc/default/kurento``, to instruct KMS about where the plugin files have been copied:

.. code-block:: shell

   KURENTO_MODULES_PATH="$KURENTO_MODULES_PATH /path/to/module"
   GST_PLUGIN_PATH="$GST_PLUGIN_PATH /path/to/module"

KMS will then add these paths to the path lookup it performs at startup, when looking for all available plugins.

When ready, you should **verify the module installation**. Run KMS twice, with the ``--version`` and ``--list`` arguments. The former shows a list of all installed modules and their versions, while the latter prints a list of all the actual *MediaObject* Factories that clients can invoke with the JSON-RPC API. Your own module should show up in both lists:

.. code-block:: shell-session
   :emphasize-lines: 7,12,13

   $ /usr/bin/kurento-media-server --version
   Kurento Media Server version: 6.12.0
   Found modules:
       'core' version 6.12.0
       'elements' version 6.12.0
       'filters' version 6.12.0
       'mygstmodule' version 0.0.1~0.gd61e201

   $ /usr/bin/kurento-media-server --list
   Available factories:
       [...]
       MyGstModule
       mygstmodule.MyGstModule



Installing in Docker
--------------------

It is perfectly possible to install and use additional Kurento modules with Docker-based deployments of Kurento Media Server. To do so, first follow any of the installation methods described above, but then instead of copying files to a host server you would add them into a Docker image or container.

Our recommendation is to leverage the `FROM <https://docs.docker.com/engine/reference/builder/#from>`__ feature of *Dockerfiles*, to derive directly from a `Kurento Docker image <https://hub.docker.com/r/kurento/kurento-media-server>`__, and create your own fully customized image.

A ``Dockerfile`` such as this one would be a good enough starting point:

.. code-block:: docker

   FROM kurento/kurento-media-server:latest
   COPY my-gst-module_0.0.1~rc1_amd64.deb /
   RUN dpkg -i /my-gst-module_0.0.1~rc1_amd64.deb

Now build the new image:

.. code-block:: shell-session

   $ docker build --tag kms-with-my-gst-module:latest .
   Step 1/3 : FROM kurento/kurento-media-server:latest
   Step 2/3 : COPY my-gst-module_0.0.1~rc1_amd64.deb /
   Step 3/3 : RUN dpkg -i /my-gst-module_0.0.1~rc1_amd64.deb
   Successfully built d10d3b4a8202
   Successfully tagged kms-with-my-gst-module:latest

And verify your module is correctly loaded by KMS:

.. code-block:: shell-session
   :emphasize-lines: 7,12,13

   $ docker run --rm kms-with-my-gst-module:latest --version
   Kurento Media Server version: 6.12.0
   Found modules:
       'core' version 6.12.0
       'elements' version 6.12.0
       'filters' version 6.12.0
       'mygstmodule' version 0.0.1~0.gd61e201

   $ docker run --rm kms-with-my-gst-module:latest --list
   Available factories:
       [...]
       MyGstModule
       mygstmodule.MyGstModule



Java client code
----------------

Run this from the root directory of your module:

.. code-block:: shell

   mkdir build/ ; cd build/
   cmake .. -DGENERATE_JAVA_CLIENT_PROJECT=TRUE

This generates a ``build/java/`` directory, containing all the client code. You can now run either of these commands:

* ``make java`` (equivalent to ``mvn clean package``) to build the Maven package.
* ``make java_install`` (equivalent to ``mvn clean install``) to build the Maven package and install it into the local repository (typically located at *$HOME/.m2/*).

Finally, to actually use the module in your Maven project, you have to add the dependency to the *pom.xml* file:

.. code-block:: xml

   <project>
     <dependencies>
       <dependency>
         <groupId>org.kurento.module</groupId>
         <artifactId>{modulename}</artifactId>
         <version>0.0.1-SNAPSHOT</version>
       </dependency>
     </dependencies>
   </project>

Note that ``{modulename}`` is the name of your module in all lowercase.

Then you will be able to instantiate and use the new module in your Java code. For example, Kurento's `OpenCV plugin sample <https://github.com/Kurento/kms-opencv-plugin-sample>`__ is used like this:

.. code-block:: java

   import org.kurento.module.opencvpluginsample.OpenCVPluginSample;
   [...]
   final OpenCVPluginSample myFilter =
     new OpenCVPluginSample.Builder(pipeline).build();
   myFilter.setFilterType(0);
   [...]
   myWebRtcEndpoint1.connect(myFilter);
   myFilter.connect(myWebRtcEndpoint2);

The result is, as expected, that the OpenCV plugin sample applies a :wikipedia:`Canny edge detector` to the original image:

.. figure:: ../images/kms-opencv-plugin-sample.png
   :align: center
   :alt:   Kurento's OpenCV plugin sample, applying a Canny edge detector

   *Kurento's OpenCV plugin sample, applying a Canny edge detector*



JavaScript client code
----------------------

Run this from the root directory of your module:

.. code-block:: shell

   mkdir build/ ; cd build/
   cmake .. -DGENERATE_JS_CLIENT_PROJECT=TRUE

This generates a ``build/js/`` directory, containing all the client code. You can now manually copy this code to your application. Alternatively, you can use :term:`Bower` (for *Browser JavaScript*) or :term:`NPM` (for *Node.js*). To do that, you should add your JavaScript module as a dependency in your *bower.json* or *package.json* file, respectively:

.. code-block:: json

   "dependencies": {
     "{modulename}": "0.0.1"
   }

Note that ``{modulename}`` is the name of your module in all lowercase.



Examples
========

Simple examples for both kinds of modules are available in GitHub:

* `GStreamer module <https://github.com/Kurento/kms-gstreamer-plugin-sample>`__.
* `OpenCV module <https://github.com/Kurento/kms-opencv-plugin-sample>`__.

There are a lot of examples showing how to define methods, parameters or events in the "extra" modules that Kurento provides for demonstration purposes:

* `kms-pointerdetector <https://github.com/Kurento/kms-pointerdetector/tree/master/src/server/interface>`__.
* `kms-crowddetector <https://github.com/Kurento/kms-crowddetector/tree/master/src/server/interface>`__.
* `kms-chroma <https://github.com/Kurento/kms-chroma/tree/master/src/server/interface>`__.
* `kms-platedetector <https://github.com/Kurento/kms-platedetector/tree/master/src/server/interface>`__.

Besides that, all of the Kurento main modules are developed using this methodology, so you can also have a look in these:

* `kms-core <https://github.com/Kurento/kms-core>`__.
* `kms-elements <https://github.com/Kurento/kms-elements>`__.
* `kms-filters <https://github.com/Kurento/kms-filters>`__.
