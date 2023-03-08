===============
Developer Guide
===============

This section is a comprehensive guide for development of *Kurento itself*. The intended reader of this text is any person who wants to get involved in writing code for the Kurento project, or to understand how the source code of this project is structured.

If you are looking to write applications that make use of Kurento, then you should read :doc:`/user/writing_applications`.

.. contents:: Table of Contents



Introduction
============

This is an overview of the tools and technologies used by Kurento:

- Officially supported platform(s): **Ubuntu 20.04 (Focal)** (64-bits).
- The code is written in C and C++ languages.
- The code style is heavily influenced by that of Gtk and GStreamer projects.
- CMake is the build tool of choice, and is used to build all modules.
- Source code is versioned in several GitHub repositories.
- The GStreamer multimedia framework sits at the heart of Kurento Media Server.
- In addition to GStreamer, Kurento uses lots of other libraries, such as Boost, jsoncpp, libsrtp, or libnice.



.. _dev-code-repos:

Code repositories
=================

Kurento source code belongs to the Kurento organization at https://github.com/Kurento. Additionally, several 3rd-party libraries are also forked under the same organization; each one of these repositories has a specific purpose and usually contains the code required to build a shared library of the same name.

An overview of the relationships between all modules of Kurento Media Server:

.. graphviz:: /images/graphs/dependencies-all.dot
   :align: center
   :caption: Media Server dependency graph

As the dependency graph is not strictly linear, there are multiple possible ways to order all modules into a linear dependency list; this section provides one possible ordered list, which will be consistently used through all Kurento documents.

**Fork repositories**:

Kurento depends on several Open Source libraries, the main one being GStreamer. Sometimes these libraries show specific behaviors that need to be tweaked in order to be useful for Kurento; other times there are bugs that have been fixed but the patch is not accepted at the upstream source for whatever reason. In these situations, while the official path of feature requests and/or patch submit is still tried, we have created a fork of the affected libraries.

- `libsrtp <https://github.com/Kurento/libsrtp>`__
- `openh264 <https://github.com/Kurento/openh264>`__
- `openh264-gst-plugin <https://github.com/Kurento/openh264-gst-plugin>`__
- `gst-plugins-good <https://github.com/Kurento/gst-plugins-good>`__
- `libnice <https://github.com/Kurento/libnice>`__ (produces gstreamer1.0-nice)

**Kurento monorepo**

The bulk of source code resides in the Kurento monorepo: https://github.com/Kurento/kurento. The media server itself exists under the ``server/`` subdir, and contains these modules:

- ``server/module-creator``: A code generation tool for generating code scaffolding for plugins. This code includes Kurento Media Server code, and Kurento client code.
- ``server/cmake-utils``: Contains a set of utilities for building the media server with CMake.
- ``server/jsonrpc``: The Kurento protocol is based on JsonRpc, and makes use of a JsonRpc library contained in this module.
- ``server/module-core``: Core GStreamer code. This is the base plugin library that is needed for other plugins.
- ``server/module-elements``: Main elements offering pipeline capabilities like WebRTC, RTP, media player, media recorder, etc.
- ``server/module-filters``: Basic video filters included with the media server.
- ``server/media-server``: Main entry point of the media server. That is, the ``main()`` function for the server executable code.

**Example plugins**

Kurento Media Server is distributed with some basic GStreamer pipeline elements, but other elements are available in form of example plugins. These showcase the kind of third party modules that could be written and integrated with Kurento, and are just for instructional purposes. Don't use them in production:

.. note::

   These plugins were available for installation with Kurento 6.x; however, they are currently unavailable for Kurento 7.x due to breaking changes in OpenCV 4.0.

- ``server/module-examples/chroma``
- ``server/module-examples/crowddetector``
- ``server/module-examples/datachannelexample``
- ``server/module-examples/markerdetector``
- ``server/module-examples/platedetector``
- ``server/module-examples/pointerdetector``

There are also a couple minimal samples of what can be achieved with the default scaffolding done by Kurento Module Creator (see :doc:`/user/writing_modules`):

- ``server/module-examples/gstreamer-example``
- ``server/module-examples/opencv-example``

**Clients**

Application Servers can be developed in Java, JavaScript with Node.js, or JavaScript directly in the browser. Each of these languages have their respective client SDK:

- ``clients/java``: For Application Servers written with Java technologies.
- ``clients/javascript``: For Application Servers written with Node.js, or directly with browser JavaScript (not recommended).

This is an overview of the dependency graph for Java packages:

.. graphviz:: /images/graphs/dependencies-java.dot
   :align: center
   :caption: Java dependency graph

**Tutorials and examples**

There are several repositories that contain sample applications for Kurento. Currently these are:

- ``tutorials/java``
- ``tutorials/javascript-node``
- ``tutorials/javascript-browser``

A developer intending to work on Kurento itself must know how to work with the fork and server modules, and understand that each of these have a different development life cycle. Most of the development occurs at the server, while it's unusual to make changes in forks except for updating their upstream versions.



Development 101
===============

Kurento is a C/C++ project developed with an Ubuntu system as main target, which means that its dependency management and distribution is based on the Debian package system.



Libraries
---------

It is not a trivial task to configure the compiler to use a set of libraries because a library can be composed of several *.so* and *.h* files. To make this task easier, `pkg-config <https://www.freedesktop.org/wiki/Software/pkg-config>`__ is used when compiling programs and libraries. In short: when a library is installed in a system, it registers itself in the *pkg-config* database with all its required files, which allows to later query those values in order to compile with the library in question.

For example, if you want to compile a C program which depends on GLib 2.0, you can run:

.. code-block:: shell

   gcc -o program program.c $(pkg-config --libs --cflags glib-2.0)


Debian packages
---------------

In a Debian/Ubuntu system, development libraries are distributed as Debian packages which are made available in public package repositories. When a C or C++ project is developed in these systems, it is usual to distribute it also in Debian packages. It is then possible to install them with *apt-get*, which will handle automatically all the package's dependencies.

When a library is packaged, the result usually consists of several packages. These are some pointers on the most common naming conventions for packages, although they are not always strictly enforced by Debian or Ubuntu maintainers:

- **bin package**: Package containing the binary files for the library itself. Programs are linked against them during development, and they are also loaded in production. The package name starts with *lib*, followed by the name of the library.
- **dev package**: Contains files needed to link with the library during development. The package name starts with *lib* and ends with *-dev*. For example: *libboost-dev* or *libglib2.0-dev*.
- **dbg package**: Contains debug symbols to ease error debugging during development. The package name starts with *lib* and ends with *-dbg*. For example: *libboost-dbg*.
- **doc package**: Contains documentation for the library. Used in development. The package name starts with *lib* and ends with *-doc*. For example: *libboost-doc*.
- **src package**: Package containing the source code for the library. It uses the same package name as the bin version, but it is accessed with the command ``apt-get source`` instead of ``apt-get install``.



Build tools
-----------

There are several tools for building C/C++ projects: Autotools, Make, CMake, Gradle, etc. The most prominent tool for building projects is the Makefile, and all the other tools tend to be simply wrappers around this one. Kurento uses CMake, which generates native Makefiles to build and package the project. There are some IDEs that recognize CMake projects directly, such as `JetBrains CLion <https://www.jetbrains.com/clion/>`__ or `Qt Creator <https://www.qt.io/ide/>`__.

A CMake projects consists of several *CMakeLists.txt* files, which define how to compile and package native code into binaries and shared libraries. These files also contain a list of the libraries (dependencies) needed to build the code.

To specify a dependency it is necessary to know how to configure this library in the compiler. The already mentioned *pkg-config* tool is the standard de-facto for this task, so CMake comes with the ability to use *pkg-config* under the hood. There are also some libraries built with CMake that use some specific CMake-only utilities.



.. _dev-sources:

Build from sources
==================

To build the source code of Kurento Media Server, you have 2 options:

* Build absolutely everything from scratch. Keeping in mind the dependency graph from :ref:`dev-code-repos`, you will need to start from the leftmost part and progress towards the right, building all projects one by one.

* Start from an intermediate point. For example if you only want to build Kurento Media Server itself, and not its dependencies, you can leverage the packages that are already built in the **Kurento packages repository** (see instructions for either the :ref:`Release repo <installation-local>` or :ref:`Development repo <installation-dev-local>`).

In all cases, the workflow is the same. Follow these steps to end up with an environment that is appropriate for hacking on the Kurento source code:

1. Install required tools.
2. Install build dependencies.
3. Download the source code.
4. Build and run Kurento Media Server.
5. Build and run Kurento tests.



Install required tools
----------------------

This command installs the basic set of tools that are needed for the next steps:

.. code-block:: shell

   sudo apt-get update ; sudo apt-get install --no-install-recommends \
       build-essential \
       ca-certificates \
       cmake \
       git \
       gnupg \
       pkg-config



Install build dependencies
--------------------------

**Option 1: Quick setup**

If you install the ``kurento-media-server-dev`` package, all build dependencies will get installed too. This is a quick and easy way to get all the dependencies, if you don't care about building them from scratch.

First add the Kurento repos to your system, by following either of :ref:`release install <installation-local>` or :ref:`development install <installation-dev-local>`. Then, run this command:

.. code-block:: shell

   sudo apt-get update ; sudo apt-get install --no-install-recommends \
       kurento-media-server-dev

If you *do care* about building everything from scratch, keep reading.

**Option 2: Build everything**

All repositories that form the Kurento Media Server codebase are prepared to be packaged with Debian packaging tools. For every one of the fork libraries and modules of Kurento, you should have a look at its ``debian/control`` file, and make sure the dependencies listed in ``Build-Depends`` are installed in your system. This can be automated with ``mk-build-deps`` (which is part of the ``devscripts`` package).

For example, to build the Kurento *core* module:

.. code-block:: shell

   sudo apt-get update ; sudo apt-get install --no-install-recommends \
       devscripts equivs

   cd server/module-core/

   # Get DISTRIB_* env vars.
   source /etc/upstream-release/lsb-release 2>/dev/null || source /etc/lsb-release

   sudo apt-get update ; sudo mk-build-deps --install --remove \
       --tool="apt-get -o Debug::pkgProblemResolver=yes --target-release 'a=${DISTRIB_CODENAME}-backports' --no-install-recommends --no-remove" \
       ./debian/control



Download the source code
------------------------

Run:

.. code-block:: shell

   git clone https://github.com/Kurento/kurento.git
   cd kurento/

   git submodule update --init --recursive



Build and run Kurento Media Server
----------------------------------

Change into the ``server/`` directory, then run this command:

.. code-block:: shell

   export MAKEFLAGS="-j$(nproc)"

   bin/build-run.sh

By default, the script `build-run.sh <https://github.com/Kurento/kurento/blob/main/server/bin/build-run.sh>`__ will set up the environment and settings to make a Debug build of Kurento Media Server. You can inspect that script to learn about all the other options it offers, including builds for `AddressSanitizer <https://github.com/google/sanitizers/wiki/AddressSanitizer>`__, selection between GCC and Clang compilers, and other modes.

You can also set the logging level of specific categories by exporting the environment variable *GST_DEBUG* before running this script (see :doc:`/features/logging`).

After the build has been completed, you can change into the build directory and run the unit tests. For more info, see :ref:`dev-unit-tests`.



Clean up your system
--------------------

To leave the system in a clean state, remove all Kurento packages and related development libraries. All Kurento packages contain the word "*kurento*" in the version number, so Aptitude makes it very easy to uninstall them all:

.. code-block:: shell

   sudo aptitude remove ~Vkurento

Use *purge* instead of *remove* to also delete any leftover configuration files in ``/etc/``.



.. _dev-dbg:

Install debug symbols
=====================

To work with Kurento source code itself or analyze a crash in either the server or any 3rd-party library, you'll want to have debug symbols installed. These provide for full information about the source file name and line where problems are happening; this information is paramount for a successful debug session, and you'll also need to provide these details when requesting support or :ref:`filing a bug report <support-community>`.

**Installing the debug symbols does not impose any extra load to the system**. So, it doesn't really hurt at all to have them installed even in production setups, where they will prove useful whenever an unexpected crash happens to bring the system down and a postmortem stack trace is automatically generated.

After having :doc:`installed Kurento </user/installation>`, first thing to do is to enable the Ubuntu's official **Debug Symbol Packages** repository:

.. code-block:: shell

   # Get DISTRIB_* env vars.
   source /etc/upstream-release/lsb-release 2>/dev/null || source /etc/lsb-release

   # Add Ubuntu debug repository key for apt-get.
   apt-get update ; apt-get install --yes ubuntu-dbgsym-keyring \
   || apt-key adv \
      --keyserver keyserver.ubuntu.com \
      --recv-keys F2EDC64DC5AEE1F6B9C621F0C8CAB6595FDFF622

   # Add Ubuntu debug repository line for apt-get.
   sudo tee "/etc/apt/sources.list.d/ddebs.list" >/dev/null <<EOF
   deb http://ddebs.ubuntu.com $DISTRIB_CODENAME main restricted universe multiverse
   deb http://ddebs.ubuntu.com ${DISTRIB_CODENAME}-updates main restricted universe multiverse
   EOF

Now, install all debug packages that are relevant to Kurento:

.. code-block:: shell

   # Install debug packages.
   # The debug packages repository fails very often due to bad server state.
   # Try to update, and only if it works install debug symbols.
   sudo apt-get update && sudo apt-get install --no-install-recommends --yes \
       kurento-dbg



Why are debug symbols useful?
-----------------------------

Let's see a couple examples that show the difference between the same stack trace, as generated *before* installing the debug symbols, and *after* installing them. **Don't report a stack trace that looks like the first one in this example**:

**NOT USEFUL**: WITHOUT debug symbols:

.. code-block:: shell-session

   $ cat /var/log/kurento-media-server/errors.log
   Segmentation fault (thread 139667051341568, pid 14132)
   Stack trace:
   [kurento::MediaElementImpl::mediaFlowInStateChanged(int, char*, KmsElementPadType)]
   /usr/lib/x86_64-linux-gnu/libkmscoreimpl.so.6:0x1025E0
   [g_signal_emit]
   /usr/lib/x86_64-linux-gnu/libgobject-2.0.so.0:0x2B08F
   [check_if_flow_media]
   /usr/lib/x86_64-linux-gnu/libkmsgstcommons.so.6:0x1F9E4
   [g_hook_list_marshal]
   /lib/x86_64-linux-gnu/libglib-2.0.so.0:0x3A904

**USEFUL** WITH debug symbols:

.. code-block:: shell-session

   $ cat /var/log/kurento-media-server/errors.log
   Segmentation fault (thread 140672899761920, pid 15217)
   Stack trace:
   [kurento::MediaElementImpl::mediaFlowInStateChanged(int, char*, KmsElementPadType)]
   /home/kurento/server/module-core/src/server/implementation/objects/MediaElementImpl.cpp:479
   [g_signal_emit]
   /build/glib2.0-prJhLS/glib2.0-2.48.2/./gobject/gsignal.c:3443
   [cb_buffer_received]
   /home/kurento/server/module-core/src/gst-plugins/commons/kmselement.c:578
   [g_hook_list_marshal]
   /build/glib2.0-prJhLS/glib2.0-2.48.2/./glib/ghook.c:673

The second stack trace is much more helpful, because it indicates the exact file names and line numbers where the crash happened. With these, a developer will at least have a starting point where to start looking for any potential bug.

It's important to note that stack traces, while helpful, are not a 100% replacement of actually running the software under a debugger (**GDB**) or memory analyzer (**Valgrind**). Most crashes will need further investigation before they can be fixed.



.. _dev-gdb:

Run and debug with GDB
======================

`GDB <https://www.gnu.org/software/gdb/>`__ is a debugger that helps in understanding why and how a program is crashing. Among several other things, you can use GDB to obtain a **backtrace**, which is a detailed list of all functions that were running when the Kurento process failed.

You can build Kurento Media Server from sources and then use GDB to execute and debug it. Alternatively, you can also use GDB with an already installed version of Kurento.



GDB from sources
----------------

1. Complete the previous instructions on how to build and run from sources: :ref:`dev-sources`.

2. Install debug symbols: :ref:`dev-dbg`.

3. Build and run Kurento with GDB.

   For this step, the easiest method is to use our launch script, *build-run.sh*. It builds all sources, configures the environment, and starts up the debugger:

   .. code-block:: shell

      bin/build-run.sh --gdb
      # [... wait for build ...]
      (gdb)

4. Run GDB commands to *start Kurento Media Server* and then get a *backtrace* (see indications in next section).



GDB from installation
---------------------

You don't *have* to build Kurento from sources in order to run it with the GDB debugger. Using an already existing installation is perfectly fine, too, so it's possible to use GDB in your servers without much addition (apart from installing *gdb* itself, that is):

1. Assuming a machine where Kurento is :doc:`installed </user/installation>`, go ahead and also install ``gdb``.

2. Install debug symbols: :ref:`dev-dbg`.

3. Define the *G_DEBUG* environment variable.

   This helps capturing assertions from 3rd-party libraries used by Kurento, such as *GLib* and *GStreamer*:

   .. code-block:: shell

      export G_DEBUG=fatal-warnings

4. Load your service settings.

   You possibly did some changes in the Kurento service settings file, ``/etc/default/kurento-media-server``. This file contains shell code that can be sourced directly into your current session:

   .. code-block:: shell

      source /etc/default/kurento-media-server

5. Ensure Kurento is not already running as a service.

   .. code-block:: shell

      sudo service kurento-media-server stop

5. Run Kurento with GDB.

   .. code-block:: shell

      gdb /usr/bin/kurento-media-server
      # [ ... GDB starts up ...]
      (gdb)

6. Run GDB commands to *start Kurento Media Server* and then get a *backtrace* (see indications in next section).

**Running Kurento with Docker**

If you are running Kurento from the Docker image, you can also follow the steps above, however a couple extra things must be done:

* Launch the Kurento Docker container with these additional arguments:

  .. code-block:: shell

     docker run -ti --cap-add SYS_PTRACE --security-opt seccomp=unconfined --entrypoint /bin/bash [...]

* Skip steps *4* and *5* from above.



GDB commands
------------

Once you see the ``(gdb)`` command prompt, you're already running a `GDB session <https://www.cprogramming.com/gdb.html>`__, and you can start issuing debug commands. Here, the most useful ones are *backtrace* and *info* variants (`Examining the Stack <https://sourceware.org/gdb/current/onlinedocs/gdb/Stack.html>`__). When you want to finish, stop execution with *Ctrl+C*, then type the *quit* command:

.. code-block:: shell

   # Actually start running the Kurento Media Server process
   (gdb) run

   # At this point, Kurento is running; now try to make the crash happen,
   # which will return you to the "(gdb)" prompt.
   #
   # Or you can press "Ctrl+C" to force an interruption.
   #
   # You can also send the SIGSEGV signal to simulate a segmentation fault:
   # sudo kill -SIGSEGV "$(pgrep -f kurento-media-server)"

   # Obtain an execution backtrace.
   (gdb) backtrace

   # Change to an interesting frame and get all details.
   (gdb) frame 3
   (gdb) info frame
   (gdb) info args
   (gdb) info locals

   # Quit GDB and return to the shell.
   (gdb) quit

Explaining GDB usage is out of scope for this documentation, but just note one thing: in the above text, ``frame 3`` is **just an example**; depending on the case, the backtrace needs to be examined first to decide which frame number is the most interesting. Typically (but not always), the interesting frame is the first one that involves Kurento's own code instead of 3rd-party code.



Work on a forked library
========================

These are the two typical workflows used to work with fork libraries:



Full cycle
----------

This workflow has the easiest and fastest setup, however it also is the slowest one. To make a change, you would edit the code in the library, then build it, generate Debian packages, and lastly install those packages over the ones already installed in your system. It would then be possible to run Kurento and see the effect of the changes in the library.

This is of course an extremely cumbersome process to follow during anything more complex than a couple of edits in the library code.



In-place linking
----------------

The other work method consists on changing the system library path so it points to the working copy where the fork library is being modified. Typically, this involves building the fork with its specific tool (which often is Automake), changing the environment variable *LD_LIBRARY_PATH*, and running Kurento Media Server with such configuration that any required shared libraries will load the modified version instead of the one installed in the system.

This allows for the fastest development cycle, however the specific instructions to do this are very project-dependent.

[TODO: Add concrete instructions for every forked library]



.. _dev-packages:

Create Deb packages
===================

You can easily create Debian packages (*.deb* files) for Kurento itself and for any of the forked libraries. Typically, Deb packages can be created directly by using standard system tools such as `dpkg-buildpackage <https://manpages.ubuntu.com/manpages/man1/dpkg-buildpackage.1.html>`__ or `debuild <https://manpages.ubuntu.com/manpages/man1/debuild.1.html>`__.



kurento-buildpackage script
---------------------------

All Kurento packages are normally built in our CI servers, using a script aptly named `kurento-buildpackage <https://github.com/Kurento/kurento/blob/main/ci-scripts/kurento-buildpackage.sh>`__. When running this tool inside any project's directory, it will configure Kurento repositories, install dependencies, and finally use *git-buildpackage* to update the *debian/changelog* file, before actually building new Deb packages.

You can also use *kurento-buildpackage* locally, to build test packages while working on any of the Kurento projects; default options will generally be good enough. However, note that the script assumes all dependencies to either be installable from current Apt repositories, or be already installed in your system. If you want to allow the script to install any Kurento dependencies that you might be missing, run it with ``--install-kurento <KurentoVersion>``, where *<KurentoVersion>* is the version of Kurento against which the project should be built.

For example, say you want to build the development branch of *kurento-module-core* against Kurento 7.0.0. Run these commands:

.. code-block:: shell

   git clone https://github.com/Kurento/kurento.git
   cd kurento/server/module-core/
   ../../ci-scripts/kurento-buildpackage.sh \
       --install-kurento 7.0.0 \
       --apt-add-repo

Run ``kurento-buildpackage.sh --help``, to read about what are the dependencies that you'll have to install to use this tool, and what are the command-line flags that can be used with it.



kurento-buildpackage Docker image
---------------------------------

In an attempt to make it easier than ever to create Deb packages from Kurento repositories, we offer a Docker image that already contains everything needed to run the *kurento-buildpackage* tool. You can use this Docker image as if you were running the script itself, with the advantage that your system won't have to be modified to install any dependencies, your builds will be completely repeatable, and you will be able to create packages for different versions of Ubuntu.

To use the `kurento-buildpackage Docker image <https://hub.docker.com/r/kurento/kurento-buildpackage>`__, you'll need to bind-mount the project directory onto the ``/hostdir`` path inside the container. All other options to *kurento-buildpackage* remain the same.

For example, say you want to build all Kurento packages for *Ubuntu 20.04 (Focal)*, from scratch (i.e. without jump-starting from the *apt-get* repositories), you've been saving them into ``$HOME/packages/``, and now it's the turn of *kurento-module-core*. Run these commands:

.. code-block:: shell

   git clone https://github.com/Kurento/kurento.git

   cd kurento/

   docker run --rm \
       --mount type=bind,src=server/module-core,dst=/hostdir \
       --mount type=bind,src=ci-scripts,dst=/ci-scripts \
       --mount type=bind,src="$HOME/packages",dst=/packages \
       kurento/kurento-buildpackage:focal \
           --install-files /packages \
           --dstdir /packages



.. _dev-unit-tests:

Unit Tests
==========

Kurento uses the Check unit testing framework for C (https://libcheck.github.io/check/). If you are working on the source code and :ref:`building from sources <dev-sources>`, you can build and run unit tests manually: just ``cd`` to the build directory and run ``make check``. All available tests will run, and a summary report will be shown at the end.

.. note::

   It is recommended to first disable GStreamer log colors, that way the resulting log files won't contain extraneous escape sequences such as ``^[[31;01m ^[[00m``. Also, it will be useful to specify a higher logging level than the default; set the environment variable *GST_DEBUG*, as explained in :ref:`logging-levels`.

   The complete command could look like this:

   .. code-block:: shell

      export GST_DEBUG_NO_COLOR=1
      export GST_DEBUG="3,check:5,test_base:5"

      make check

The log output of the whole test suite will get saved into the file *./Testing/Temporary/LastTest.log*. To find the starting point of each individual test inside this log file, search for the words "**test start**". For the start of a specific test, search for "**<TestName>: test start**". For example:

.. code-block:: text

   webrtcendpoint.c:1848:test_vp8_sendrecv: test start

To build and run one specific test, use ``make test_<TestName>.check``. For example:

.. code-block:: shell

   make test_agnosticbin.check

If you had Valgrind installed (to analyze memory usage), a ``.valgrind`` target will have been generated too. For example:

.. code-block:: shell

   make test_agnosticbin.valgrind



How to disable tests
--------------------

Debian tools will automatically run unit tests as part of the :ref:`package creation <dev-packages>` process. However, for special situations during development, we might want to temporarily disable testing before creating an experimental package. For example, say you are investigating an issue, and want to see what happens if you force a crash in some point of the code; or maybe you want to temporarily change a module's behavior but it breaks some unit test.

It is possible to skip building and running unit tests automatically, by editing the file ``debian/rules`` and changing the *auto_configure* rule from ``-DGENERATE_TESTS=TRUE`` to ``-DGENERATE_TESTS=FALSE -DDISABLE_TESTS=TRUE``.

Some tests require an IPv6-enabled network. These can be disabled temporarily in order to run all remaining tests on a machine that only has an IPv4 interface. Just use ``-DDISABLE_IPV6_TESTS=TRUE`` in either your CMake call, or in the Debian files where this property is mentioned, if you want to create *.deb* packages (currently, only ``server/module-elements/debian/rules``).



How-To
======

How to add or update external libraries
---------------------------------------

Add or change it in these files:

- *debian/control*.
- *CMakeLists.txt*.



How to add new fork libraries
-----------------------------

1. Fork the repository.
2. Create a *.build.yaml* file in this repository, listing its project dependencies (if any).
3. Add dependency to *debian/control* in the project that uses it.
4. Add dependency to *CMakeLists.txt* in the project that uses it.



How to work with API changes
----------------------------

What to do when you are developing a new feature that spans across the media server and the public API? This is a summary of the actions done in CI by ``ci-scripts/kurento_generate_java_module.sh`` and ``ci-scripts/kurento_maven_deploy.sh``:

1. Work on your changes, which may include changing files where the Kurento API is defined.

2. Generate client SDK dependencies:

   .. code-block:: shell

      cd server/<module>/  # E.g. server/module-filters/
      mkdir build ; cd build/
      cmake -DGENERATE_JAVA_CLIENT_PROJECT=TRUE -DDISABLE_LIBRARIES_GENERATION=TRUE ..
      cd java/
      mvn -DskipTests=true clean install

3. Generate client SDK:

   .. code-block:: shell

      cd clients/java/
      mvn -DskipTests=true clean install

4. At this point, the new Java packages have been generated and installed *in the local Maven cache*. Your Java application can now make use of any changes that were introduced in the API.



Known problems
--------------

- Some unit tests can fail, especially if the storage server (which contains some required input files) is having connectivity issues. If tests fail, packages are not generated. To skip tests, edit the file *debian/rules* and change ``-DGENERATE_TESTS=TRUE`` to ``-DGENERATE_TESTS=FALSE -DDISABLE_TESTS=TRUE``.
