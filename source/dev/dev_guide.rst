===============
Developer Guide
===============

This section is a comprehensive guide for development of *Kurento itself*. The intended reader of this text is any person who wants to get involved in writing code for the Kurento project, or to understand how the source code of this project is structured.

If you are looking to write applications that make use of Kurento, then you should read :doc:`/user/writing_applications`.

.. contents:: Table of Contents



Introduction
============

This is an overview of the tools and technologies used by KMS:

- The code is written in C and C++ languages.
- The code style is heavily influenced by that of Gtk and GStreamer projects.
- CMake is the build tool of choice, and is used to build all modules.
- Source code is versioned in several GitHub repositories.
- The officially supported platforms are Long-Term Support (*LTS*) versions of Ubuntu: **Ubuntu 16.04 (Xenial)** and **Ubuntu 18.04 (Bionic)** (64-bits only).
- The GStreamer multimedia framework sits at the heart of Kurento Media Server.
- In addition to GStreamer, KMS uses other libraries like boost, jsoncpp, libnice, etc.



.. _dev-code-repos:

Code repositories
=================

Kurento source code is stored in several GitHub repositories at https://github.com/Kurento. Each one of these repositories has a specific purpose and usually contains the code required to build a shared library of the same name.

An overview of the relationships between all repos forming the Kurento Media Server:

.. graphviz:: /images/graphs/dependencies-all.dot
   :align: center
   :caption: All dependency relationships

As the dependency graph is not strictly linear, there are multiple possible ways to order all modules into a linear dependency list; this section provides one possible ordered list, which will be consistently used through all Kurento documents.

**Fork repositories**:

KMS depends on several open source libraries, the main one being GStreamer. Sometimes these libraries show specific behaviors that need to be tweaked in order to be useful for KMS; other times there are bugs that have been fixed but the patch is not accepted at the upstream source for whatever reason. In these situations, while the official path of feature requests and/or patch submit is still tried, we have created a fork of the affected libraries.

- `jsoncpp <https://github.com/Kurento/jsoncpp>`__
- `libsrtp <https://github.com/Kurento/libsrtp>`__
- `openh264 <https://github.com/Kurento/openh264>`__
- `usrsctp <https://github.com/Kurento/usrsctp>`__
- `gstreamer <https://github.com/Kurento/gstreamer>`__ (produces libgstreamer1.5)
- `gst-plugins-base <https://github.com/Kurento/gst-plugins-base>`__
- `gst-plugins-good <https://github.com/Kurento/gst-plugins-good>`__
- `gst-plugins-bad <https://github.com/Kurento/gst-plugins-bad>`__
- `gst-plugins-ugly <https://github.com/Kurento/gst-plugins-ugly>`__
- `gst-libav <https://github.com/Kurento/gst-libav>`__
- `openwebrtc-gst-plugins <https://github.com/Kurento/openwebrtc-gst-plugins>`__
- `libnice <https://github.com/Kurento/libnice>`__ (produces gstreamer1.0-nice, gstreamer1.5-nice)

**Main repositories**

- `kurento-module-creator <https://github.com/Kurento/kurento-module-creator>`__: It is a code generation tool for generating code scaffolding for plugins. This code includes KMS code and Kurento client code. It has mainly Java code.
- `kms-cmake-utils <https://github.com/Kurento/kms-cmake-utils>`__: Contains a set of utilities for building KMS with CMake.
- `kms-jsonrpc <https://github.com/Kurento/kms-jsonrpc>`__: Kurento protocol is based on JsonRpc, and makes use of a JsonRpc library contained in this repository. It has C++ code.
- `kms-core <https://github.com/Kurento/kms-core>`__: Contains the core GStreamer code. This is the base library that is needed for other libraries. It has 80% C code and a 20% C++ code.
- `kms-elements <https://github.com/Kurento/kms-elements>`__: Contains the main elements offering pipeline capabilities like WebRtc, Rtp, Player, Recorder, etc. It has 80% C code and a 20% C++ code.
- `kms-filters <https://github.com/Kurento/kms-filters>`__: Contains the basic video filters included in KMS. It has 65% C code and a 35% C++ code.
- `kurento-media-server <https://github.com/Kurento/kurento-media-server>`__: Contains the main entry point of KMS. That is, the main() function for the server executable code. This program depends on libraries located in the above repositories. It has mainly C++ code.

**Extra repositories**

KMS is distributed with some basic GStreamer pipeline elements, but other elements are available in form of modules.
These modules are *demos* of what third party modules could be written and integrated into Kurento. These are just for instructional purposes, and shouldn't be used in production servers.

- `kms-chroma <https://github.com/Kurento/kms-chroma>`__
- `kms-crowddetector <https://github.com/Kurento/kms-crowddetector>`__
- `kms-platedetector <https://github.com/Kurento/kms-platedetector>`__
- `kms-pointerdetector <https://github.com/Kurento/kms-pointerdetector>`__

**Omni-Build repository**

This repository is a special project because it is designed to build all KMS Main repositories from a single entry point. This repo brings the other KMS Main repositories as Git submodules: it makes KMS development easier because if you build this project, you don’t need to manually install the libraries of the other KMS Main repositories. However, all other development and support libraries must still be installed manually.

- `kms-omni-build <https://github.com/Kurento/kms-omni-build>`__

**Client repositories**

Application Servers can be developed in Java, JavaScript with Node.js, or JavaScript directly in the browser. Each of these languages have their support tools made available in their respective repositories.

- `kurento-client-js <https://github.com/Kurento/kurento-client-js>`__ (Node.js Application Servers, browser JavaScript)
- `kurento-java <https://github.com/Kurento/kurento-java>`__ (Java Application Servers)

**Tutorial or demo repositories**

There are several repositories that contain sample code for developers that use Kurento or want to develop a custom Kurento module. Currently these are:

- `kms-datachannelexample <https://github.com/Kurento/kms-datachannelexample>`__
- `kms-opencv-plugin-sample <https://github.com/Kurento/kms-opencv-plugin-sample>`__
- `kms-plugin-sample <https://github.com/Kurento/kms-plugin-sample>`__
- `kurento-tutorial-java <https://github.com/Kurento/kurento-tutorial-java>`__
- `kurento-tutorial-js <https://github.com/Kurento/kurento-tutorial-js>`__
- `kurento-tutorial-node <https://github.com/Kurento/kurento-tutorial-node>`__

A KMS developer must know how to work with KMS Fork and Main repositories and understand that each of these have a different development life cycle. The majority of development for KMS will occur at theK MS Main repositories, while it's unusual to make changes in Fork repositories except for updating their upstream versions.



Development 101
===============

KMS is a C/C++ project developed with an Ubuntu system as main target, which means that its dependency management and distribution is based on the Debian package system.



Libraries
---------

It is not a trivial task to configure the compiler to use a set of libraries because a library can be composed of several *.so* and *.h* files. To make this task easier, `pkg-config <https://www.freedesktop.org/wiki/Software/pkg-config>`__ is used when compiling programs and libraries. In short: when a library is installed in a system, it registers itself in the ``pkg-config`` database with all its required files, which allows to later query those values in order to compile with the library in question.

For example, if you want to compile a C program which depends on GLib 2.0, you can run:

.. code-block:: console

   gcc -o program program.c $(pkg-config --libs --cflags glib-2.0)



Debian packages
---------------

In a Debian/Ubuntu system, development libraries are distributed as Debian packages which are made available in public package repositories. When a C or C++ project is developed in these systems, it is usual to distribute it also in Debian packages. It is then possible to install them with the command ``apt-get install``, which will handle automatically all the package's dependencies.

When a library is packaged, the result usually consists of several packages. These are some pointers on the most common naming conventions for packages, although they are not always strictly enforced by Debian or Ubuntu maintainers:

- **bin package**: Package containing the binary files for the library itself. Programs are linked against them during development, and they are also loaded in production. The package name starts with *lib*, followed by the name of the library.
- **dev package**: Contains files needed to link with the library during development. The package name starts with *lib* and ends with *-dev*. For example: *libboost-dev* or *libglib2.0-dev*.
- **dbg package**: Contains debug symbols to ease error debugging during development. The package name starts with *lib* and ends with *-dbg*. For example: *libboost-dbg*.
- **doc package**: Contains documentation for the library. Used in development. The package name starts with *lib* and ends with *-doc*. For example: *libboost-doc*.
- **src package**: Package containing the source code for the library. It uses the same package name as the bin version, but it is accessed with the command ``apt-get source`` instead of ``apt-get install``.



Build tools
-----------

There are several tools for building C/C++ projects: Autotools, Make, CMake, Gradle, etc. The most prominent tool for building projects is the Makefile, and all the other tools tend to be simply wrappers around this one. KMS uses CMake, which generates native Makefiles to build and package the project. There are some IDEs that recognize CMake projects directly, such as `JetBrains CLion <https://www.jetbrains.com/clion/>`__ or `Qt Creator <https://www.qt.io/ide/>`__.

A CMake projects consists of several *CMakeLists.txt* files, which define how to compile and package native code into binaries and shared libraries. These files also contain a list of the libraries (dependencies) needed to build the code.

To specify a dependency it is necessary to know how to configure this library in the compiler. The already mentioned ``pkg-config`` tool is the standard de-facto for this task, so CMake comes with the ability to use ``pkg-config`` under the hood. There are also some libraries built with CMake that use some specific CMake-only utilities.



.. _dev-sources:

Build from sources
==================

To work directly with KMS source code, or to just build KMS from sources, the easiest way is using the module **kms-omni-build**. Just follow these steps:

1. Install required tools, like Git.
2. Add the Kurento repository to your system configuration.
3. Clone **kms-omni-build**.
4. Install build dependencies: tools like GCC, CMake, etc., and KMS development libraries.
5. Build with CMake and Make.
6. Run the newly compiled KMS.
7. Run KMS tests.



Install required tools
----------------------

This command will install the basic set of tools that are needed for the next steps:

.. code-block:: console

   sudo apt-get update && sudo apt-get install --no-install-recommends --yes \
       build-essential \
       ca-certificates \
       cmake \
       git \
       gnupg



Add Kurento repository
----------------------

Run these commands to add the Kurento repository to your system configuration:

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

   sudo apt-get update



Install build dependencies
--------------------------

Run:

.. code-block:: console

   sudo apt-get update && sudo apt-get install --no-install-recommends --yes \
       kurento-media-server-dev



Download KMS source code
------------------------

Run:

.. code-block:: console

   git clone https://github.com/Kurento/kms-omni-build.git
   cd kms-omni-build
   git submodule update --init --recursive
   git submodule update --remote

.. note::

   ``--recursive`` and ``--remote`` are not used together, because each individual submodule may have their own submodules that might be expected to check out some specific commit, and we don't want to update those.

*OPTIONAL*: Change to the *master* branch of each submodule, if you will be working with the latest version of the code:

.. code-block:: console

   REF=master
   git checkout "$REF" || true
   git submodule foreach "git checkout $REF || true"

You can also set ``REF`` to any other branch or tag, such as ``REF=6.12.0``. This will bring the code to the state it had in that version release.



Build and run KMS
-----------------

Make sure your current directory is already *kms-omni-build*, then run this command:

.. code-block:: console

   export MAKEFLAGS="-j$(nproc)"
   ./bin/kms-build-run.sh

By default, the script `kms-build-run.sh <https://github.com/Kurento/kms-omni-build/blob/master/bin/kms-build-run.sh>`__ will set up the environment and settings to make a Debug build of KMS. You can inspect that script to learn about all the other options it offers, including builds for `AddressSanitizer <https://github.com/google/sanitizers/wiki/AddressSanitizer>`__, selection between GCC and Clang compilers, and other modes.

You can set the logging level of specific categories by exporting the environment variable ``GST_DEBUG`` before running this script (see :doc:`/features/logging`).



Clean up your system
--------------------

To leave the system in a clean state, remove all KMS packages and related development libraries. Run this command and, for each prompted question, visualize the packages that are going to be uninstalled and press Enter if you agree. This command is used on a daily basis by the development team at Kurento with the option ``--yes`` (which makes the process automatic and unattended), so it should be fairly safe to use. However we don't know what is the configuration of your particular system, and running in manual mode is the safest bet in order to avoid uninstalling any unexpected package.

Run:

.. code-block:: console

    PACKAGES=(
        # KMS main components + extra modules
        '^(kms|kurento).*'

        # Kurento external libraries
        ffmpeg
        '^gir1.2-gst.*1.5'
        gir1.2-nice-0.1
        '^(lib)?gstreamer.*1.5.*'
        '^lib(nice|s3-2|srtp|usrsctp).*'
        '^srtp-.*'
        '^openh264(-gst-plugins-bad-1.5)?'
        '^openwebrtc-gst-plugins.*'

        # System development libraries
        '^libboost-?(filesystem|log|program-options|regex|system|test|thread)?-dev'
        '^lib(glib2.0|glibmm-2.4|opencv|sigc++-2.0|soup2.4|ssl|tesseract|vpx)-dev'
        uuid-dev
    )

    # Run a loop over all package names and uninstall them.
    for PACKAGE in "${PACKAGES[@]}"; do
        sudo apt-get purge --auto-remove "$PACKAGE" || { echo "Skip unknown package"; }
    done



.. _dev-dbg:

Install debug symbols
=====================

Whenever working with KMS source code itself, of during any analysis of crash in either the server or any 3rd-party library, you'll want to have debug symbols installed. These provide for full information about the source file name and line where problems are happening; this information is paramount for a successful debug session, and you'll also need to provide these details when requesting support or :ref:`filing a bug report <support-community>`.

**Installing the debug symbols does not impose any extra load to the system**. So, it doesn't really hurt at all to have them installed even in production setups, where they will prove useful whenever an unexpected crash happens to bring the system down and a postmortem stack trace is automatically generated.

After having :doc:`installed Kurento </user/installation>`, first thing to do is to enable the Ubuntu's official **Debug Symbol Packages** repository:

.. code-block:: console

   # Import the Ubuntu debug repository signing key
   sudo apt-key adv \
       --keyserver keyserver.ubuntu.com \
       --recv-keys F2EDC64DC5AEE1F6B9C621F0C8CAB6595FDFF622

   # Get Ubuntu version definitions
   source /etc/upstream-release/lsb-release 2>/dev/null || source /etc/lsb-release

   # Add the repository to Apt
   sudo tee "/etc/apt/sources.list.d/ddebs.list" >/dev/null <<EOF
   # Official Ubuntu repos with debug packages
   deb http://ddebs.ubuntu.com ${DISTRIB_CODENAME} main restricted universe multiverse
   deb http://ddebs.ubuntu.com ${DISTRIB_CODENAME}-updates main restricted universe multiverse
   EOF

Now, install all debug symbols that are relevant to KMS:

.. code-block:: console

   sudo apt-get update && sudo apt-get install --no-install-recommends --yes \
       kurento-dbg



Why are debug symbols useful?
-----------------------------

Let's see a couple examples that show the difference between the same stack trace, as generated *before* installing the debug symbols, and *after* installing them. **Don't report a stack trace that looks like the first one in this example**:

.. code-block:: text

   # ==== NOT USEFUL: WITHOUT debug symbols ====

   $ cat /var/log/kurento-media-server/errors.log
   Segmentation fault (thread 139667051341568, pid 14132)
   Stack trace:
   [kurento::MediaElementImpl::mediaFlowInStateChange(int, char*, KmsElementPadType)]
   /usr/lib/x86_64-linux-gnu/libkmscoreimpl.so.6:0x1025E0
   [g_signal_emit]
   /usr/lib/x86_64-linux-gnu/libgobject-2.0.so.0:0x2B08F
   [check_if_flow_media]
   /usr/lib/x86_64-linux-gnu/libkmsgstcommons.so.6:0x1F9E4
   [g_hook_list_marshal]
   /lib/x86_64-linux-gnu/libglib-2.0.so.0:0x3A904

.. code-block:: text

   # ==== USEFUL: WITH debug symbols ====

   $ cat /var/log/kurento-media-server/errors.log
   Segmentation fault (thread 140672899761920, pid 15217)
   Stack trace:
   [kurento::MediaElementImpl::mediaFlowInStateChange(int, char*, KmsElementPadType)]
   /home/kurento/kms-omni-build/kms-core/src/server/implementation/objects/MediaElementImpl.cpp:479
   [g_signal_emit]
   /build/glib2.0-prJhLS/glib2.0-2.48.2/./gobject/gsignal.c:3443
   [cb_buffer_received]
   /home/kurento/kms-omni-build/kms-core/src/gst-plugins/commons/kmselement.c:578
   [g_hook_list_marshal]
   /build/glib2.0-prJhLS/glib2.0-2.48.2/./glib/ghook.c:673

The second stack trace is much more helpful, because it indicates the exact file names and line numbers where the crash happened. With these, a developer will at least have a starting point where to start looking for any potential bug.

It's important to note that stack traces, while helpful, are not a 100% replacement of actually running the software under a debugger (**GDB**) or memory analyzer (**Valgrind**). Most crashes will need further investigation before they can be fixed.



.. _dev-gdb:

Run and debug with GDB
======================

`GDB <https://www.gnu.org/software/gdb/>`__ is a debugger that helps in understanding why and how a program is crashing. Among several other things, you can use GDB to obtain a **backtrace**, which is a detailed list of all functions that were running when the Kurento process failed.

You can build KMS from sources and then use GDB to execute and debug it. Alternatively, you can also use GDB with an already installed version of KMS.



From sources
------------

1. Complete the previous instructions on how to build and run from sources: :ref:`dev-sources`.

2. Install debug symbols: :ref:`dev-dbg`.

3. Build and run KMS with GDB.

   For this step, the easiest method is to use our launch script, *kms-build-run.sh*. It builds all sources, configures the environment, and starts up the debugger:

   .. code-block:: console

      ./bin/kms-build-run.sh --gdb
      # [... wait for build ...]
      (gdb)

4. Run GDB commands to *start KMS* and then get a *backtrace* (see indications in next section).



From installation
-----------------

You don't *have* to build KMS from sources in order to run it with the GDB debugger. Using an already existing installation is perfectly fine, too, so it's possible to use GDB in your servers without much addition (apart from installing *gdb* itself, that is):

1. Assuming a machine where KMS is :doc:`installed </user/installation>`, go ahead and also install *gdb*.

2. Install debug symbols: :ref:`dev-dbg`.

3. Define the ``G_DEBUG`` environment variable.

   This helps capturing assertions from 3rd-party libraries used by Kurento, such as *GLib* and *GStreamer*:

   .. code-block:: console

      export G_DEBUG=fatal-warnings

4. Load your service settings.

   You possibly did some changes in the KMS service settings file, */etc/default/kurento-media-server*. This file contains shell code that can be sourced directly into your current session:

   .. code-block:: console

      source /etc/default/kurento-media-server

5. Ensure KMS is not already running as a service, and run it with GDB.

   .. code-block:: console

      sudo service kurento-media-server stop

      gdb /usr/bin/kurento-media-server
      # [ ... GDB starts up ...]
      (gdb)

6. Run GDB commands to *start KMS* and then get a *backtrace* (see indications in next section).



GDB commands
------------

Once you see the ``(gdb)`` command prompt, you're already running a `GDB session <https://www.cprogramming.com/gdb.html>`__, and you can start issuing debug commands. Here, the most useful ones are ``backtrace`` and ``info`` variants (`Examining the Stack <https://sourceware.org/gdb/current/onlinedocs/gdb/Stack.html>`__). When you want to finish, stop execution with *Ctrl+C*, then type the ``quit`` command:

.. code-block:: console

   # Actually start running the KMS process
   (gdb) run

   # At this point, KMS is running; wait until the crash happens,
   # which will return you to the "(gdb)" prompt.
   #
   # Or you can press "Ctrl+C" to force an interruption.
   #
   # You can also send the SIGSEGV signal to simulate a segmentation fault:
   # sudo kill -SIGSEGV "$(pgrep -f kurento-media-server)"

   # Obtain an execution backtrace
   (gdb) backtrace

   # Change to an interesting frame and get all details
   (gdb) frame 3
   (gdb) info frame
   (gdb) info args
   (gdb) info locals

   # Quit GDB and return to the shell
   (gdb) quit

Explaining GDB usage is out of scope for this documentation, but just note one thing: in the above text, ``frame 3`` is **just an example**; depending on the case, the backtrace needs to be examined first to decide which frame number is the most interesting. Typically (but not always), the interesting frame is the first one that involves Kurento's own code instead of 3rd-party code.



Work on a forked library
========================

These are the two typical workflows used to work with fork libraries:



Full cycle
----------

This workflow has the easiest and fastest setup, however it also is the slowest one. To make a change, you would edit the code in the library, then build it, generate Debian packages, and lastly install those packages over the ones already installed in your system. It would then be possible to run KMS and see the effect of the changes in the library.

This is of course an extremely cumbersome process to follow during anything more complex than a couple of edits in the library code.



In-place linking
----------------

The other work method consists on changing the system library path so it points to the working copy where the fork library is being modified. Typically, this involves building the fork with its specific tool (which often is Automake), changing the environment variable ``LD_LIBRARY_PATH``, and running KMS with such configuration that any required shared libraries will load the modified version instead of the one installed in the system.

This allows for the fastest development cycle, however the specific instructions to do this are very project-dependent. For example, when working on the GStreamer fork, maybe you want to run GStreamer without using any of the libraries installed in the system (see https://cgit.freedesktop.org/gstreamer/gstreamer/tree/scripts/gst-uninstalled).

[TODO: Add concrete instructions for every forked library]



.. _dev-packages:

Create Deb packages
===================

You can easily create Debian packages (``.deb`` files) for KMS itself and for any of the forked libraries. Typically, Deb packages can be created directly by using standard system tools such as `dpkg-buildpackage <https://manpages.ubuntu.com/manpages/bionic/en/man1/dpkg-buildpackage.1.html>`__ or `debuild <https://manpages.ubuntu.com/manpages/bionic/en/man1/debuild.1.html>`__, but in order to integrate the build process with Git, we based our tooling on `gbp <https://manpages.ubuntu.com/manpages/bionic/en/man1/gbp.1.html>`__ (`git-buildpackage <https://honk.sigxcpu.org/piki/projects/git-buildpackage/>`__).



kurento-buildpackage script
---------------------------

All Kurento packages are normally built in our CI servers, using a script aptly named `kurento-buildpackage <https://github.com/Kurento/adm-scripts/blob/master/kurento-buildpackage.sh>`__. When running this tool inside any project's directory, it will configure Kurento repositories, install dependencies, and finally use *git-buildpackage* to update the *debian/changelog* file, before actually building new Deb packages.

You can also use *kurento-buildpackage* locally, to build test packages while working on any of the Kurento projects; default options will generally be good enough. However, note that the script assumes all dependencies to either be installable from current Apt repositories, or be already installed in your system. If you want to allow the script to install any Kurento dependencies that you might be missing, run it with ``--install-kurento <KurentoVersion>``, where *<KurentoVersion>* is the version of Kurento against which the project should be built.

For example, say you want to build the current *kms-core* development branch against Kurento 6.12.0. Run these commands:

.. code-block:: console

   git clone https://github.com/Kurento/adm-scripts.git
   git clone https://github.com/Kurento/kms-core.git
   cd kms-core/
   ../adm-scripts/kurento-buildpackage.sh --install-kurento 6.12.0

Run ``kurento-buildpackage.sh --help``, to read about what are the dependencies that you'll have to install to use this tool, and what are the command-line flags that can be used with it.



kurento-buildpackage Docker image
---------------------------------

In an attempt to make it easier than ever to create Deb packages from Kurento repositories, we offer a Docker image that already contains everything needed to run the *kurento-buildpackage* tool. You can use this Docker image as if you were running the script itself, with the advantage that your system won't have to be modified to install any dependencies, your builds will be completely repeatable, and you will be able to create packages for different versions of Ubuntu.

To use the `kurento-buildpackage Docker image <https://hub.docker.com/r/kurento/kurento-buildpackage>`__, you'll need to bind-mount the project directory onto the ``/hostdir`` path inside the container. All other options to *kurento-buildpackage* remain the same.

For example, say you want to build the current *kms-core* development branch against Kurento 6.12.0, for *Ubuntu 16.04 (Xenial)* systems. Run these commands:

.. code-block:: console

   git clone https://github.com/Kurento/kms-core.git
   cd kms-core/
   docker run --rm \
       --mount type=bind,src="$PWD",dst=/hostdir \
       kurento/kurento-buildpackage:xenial \
       --install-kurento 6.12.0



Unit Tests
==========

KMS uses the Check unit testing framework for C (https://libcheck.github.io/check/). If you are developing KMS and :ref:`building from sources <dev-sources>`, you can build and run unit tests manually: just change the last one of the build commands from ``make`` to ``make check``. All available tests will run, and a summary report will be shown at the end.

.. note::

   It is recommended to first disable GStreamer log colors, that way the resulting log files won't contain extraneous escape sequences such as ``^[[31;01m ^[[00m``. Also, it could be useful to specify a higher logging level than the default; set the environment variable *GST_DEBUG*, as explained in :ref:`logging-levels`.

   The complete command would look like this:

   .. code-block:: console

      export GST_DEBUG_NO_COLOR=1
      export GST_DEBUG="3,check:5"
      make check

The log output of the whole test suite will get saved into the file *./Testing/Temporary/LastTest.log*. To find the starting point of each individual test inside this log file, search for the words "**test start**". For the start of a specific test, search for "**{TestName}: test start**". For example:

.. code-block:: text

   webrtcendpoint.c:1848:test_vp8_sendrecv: test start

To build and run one specific test, use ``make {TestName}.check``. For example:

.. code-block:: console

   make test_agnosticbin.check

If you want to analyze memory usage with Valgrind, use ``make {TestName}.valgrind``. For example:

.. code-block:: console

   make test_agnosticbin.valgrind



How to disable tests
--------------------

Debian tools will automatically run unit tests as part of the :ref:`package creation <dev-packages>` process. However, for special situations during development, we might want to temporarily disable testing before creating an experimental package. For example, say you are investigating an issue, and want to see what happens if you force a crash in some point of the code; or maybe you want to temporarily change a module's behavior but it breaks some unit test.

It is possible to skip building and running unit tests automatically, by editing the file *debian/rules* and changing the *auto_configure* rule from ``-DGENERATE_TESTS=TRUE`` to ``-DGENERATE_TESTS=FALSE -DDISABLE_TESTS=TRUE``.



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

What to do when you are developing a new feature that spans across KMS and the public API? This is a summary of the actions done in CI by ``adm-scripts/kurento_generate_java_module.sh`` and ``adm-scripts/kurento_maven_deploy.sh``:

1. Work on your changes, which may include changing the KMS files where the Kurento API is defined.

2. Generate client SDK dependencies:

   .. code-block:: console

      cd <module>  # E.g. kms-filters
      rm -rf build
      mkdir build && cd build
      cmake .. -DGENERATE_JAVA_CLIENT_PROJECT=TRUE -DDISABLE_LIBRARIES_GENERATION=TRUE
      cd java
      mvn clean install

3. Generate client SDK:

   .. code-block:: console

      cd kurento-java
      mvn clean install

4. At this point, the new Java packages have been generated and installed *in the local repository*. Your Java application can now make use of any changes that were introduced in the API.



Known problems
--------------

- Some unit tests can fail, especially if the storage server (which contains some required input files) is having connectivity issues. If tests fail, packages are not generated. To skip tests, edit the file *debian/rules* and change ``-DGENERATE_TESTS=TRUE`` to ``-DGENERATE_TESTS=FALSE -DDISABLE_TESTS=TRUE``.
