=======================
Writing Kurento Modules
=======================

.. contents:: Table of Contents

[TODO REVIEW]

You can expand the Kurento Media Server developing your own modules. There are two flavors of Kurento modules:

- Modules based on :term:`OpenCV`. This kind of modules are recommended if you would like to develop a filter providing Computer Vision or Augmented Reality features.

- Modules based on :term:`GStreamer`. This kind of modules provide a generic entry point for media processing with the GStreamer framework. Such modules are more powerful but also they are more difficult to develop. Skills in GStreamer development are necessary.

The starting point to develop a filter is to create the filter structure. For this task, you can use the ``kurento-module-scaffold`` tool. This tool is distributed with the ``kurento-media-server-dev`` package. To install this tool run this command:

.. sourcecode:: bash

   sudo apt-get update && sudo apt-get install --yes kurento-media-server-dev

The tool usage is different depending on the chosen flavor:

1. OpenCV module:

   .. sourcecode:: bash

      kurento-module-scaffold.sh <module_name> <output_directory> opencv_filter

2. Gstreamer module:

   .. sourcecode:: bash

      kurento-module-scaffold.sh <module_name> <output_directory>

The tool generates the folder tree, all the needed ``CmakeLists.txt`` files, and example files of Kurento module descriptor files (.kmd). These files contain the description of the modules, the constructor, the methods, the properties, the events
and the complex types defined by the developer.

Once *kmd* files are completed it is time to generate the corresponding code. The tool
``kurento-module-creator`` generates glue code to server-side. Run this from the root directory:

.. sourcecode:: bash

   cd build
   cmake ..

The following sections detail how to create your module depending on the filter type you chose (OpenCV or GStreamer).



OpenCV module
=============

We have four files in ``src/server/implementation/``:

.. sourcecode:: text

   ModuleNameImpl.cpp
   ModuleNameImpl.hpp
   ModuleNameOpenCVImpl.cpp
   ModuleNameOpenCVImpl.hpp

The first two files should not be modified. The last two files will contain the logic of your module.

The file ``ModuleNameOpenCVImpl.cpp`` contains functions to deal with the methods and the parameters (you must implement the logic). Also, this file contains a function called ``process``. This function will be called with each new frame, thus you must implement the logic of your filter inside it.



GStreamer module
================

In this case, we have two directories inside the ``src/`` folder:

- The ``gst-plugins/`` folder contains the implementation of your GStreamer Element (the ``kurento-module-scaffold`` generates a dummy filter).

- Inside the ``server/objects/`` folder you have two files:

  .. sourcecode:: text

     ModuleNameImpl.cpp
     ModuleNameImpl.hpp

In the file ``ModuleNameImpl.cpp`` you have to invoke the methods of your GStreamer element. The module logic will be implemented in the GStreamer Element.



For both kind of modules
========================

If you need extra compilation dependencies you can add compilation rules to the *kurento-module-creator* using the function ``generate_code`` in the ``CmakeLists.txt`` file, located in ``src/server/``.

The following parameters are available:

- ``SERVER_STUB_DESTINATION`` (required)
  The generated code that you may need to modify will be generated on the folder indicated by this parameter.

- ``MODELS`` (required)
  This parameter receives the folders where the models (.kmd files) are located.

- ``INTERFACE_LIB_EXTRA_SOURCES``
  ``INTERFACE_LIB_EXTRA_HEADERS``
  ``INTERFACE_LIB_EXTRA_INCLUDE_DIRS``
  ``INTERFACE_LIB_EXTRA_LIBRARIES``
  These parameters allow to add additional source code to the static library. Files included in ``INTERFACE_LIB_EXTRA_HEADERS`` will be installed in the system as headers for this library. All the parameters accept a list as input.

- ``SERVER_IMPL_LIB_EXTRA_SOURCES``
  ``SERVER_IMPL_LIB_EXTRA_HEADERS``
  ``SERVER_IMPL_LIB_EXTRA_INCLUDE_DIRS``
  ``SERVER_IMPL_LIB_EXTRA_LIBRARIES``
  These parameters allow to add additional source code to the interface library.  Files included in ``SERVER_IMPL_LIB_EXTRA_HEADERS`` will be installed in the system as headers for this library. All the parameters accept a list as input.

- ``MODULE_EXTRA_INCLUDE_DIRS``
  ``MODULE_EXTRA_LIBRARIES``
  These parameters allow to add extra include directories and libraries to the module.

- ``SERVER_IMPL_LIB_FIND_CMAKE_EXTRA_LIBRARIES``
  This parameter receives a list of strings. Each string has this format: ``libname[ libversion range]`` (possible ranges can use symbols ``AND`` ``OR`` ``<`` ``<=`` ``>`` ``>=`` ``^`` and ``~``).

  - ``^`` indicates a version compatible using :term:`Semantic Versioning`.
  - ``~`` Indicates a version similar, that can change just last indicated version character.

Once the module logic is implemented and the compilation process is finished, you need to install your module in your system. You can follow two different ways:

1. You can generate the Debian package (``debuild -us -uc``) and install it
(``dpkg -i``).
2. You can define the following environment variables in the file ``/etc/default/kurento``:

   .. sourcecode:: text

      KURENTO_MODULES_PATH=<module_path>/build/src
      GST_PLUGIN_PATH=<module_path>/build/src

Now, you need to generate code for Java or JavaScript to use your module from the client-side.

- For Java, from the build directory you have to execute ``cmake .. -DGENERATE_JAVA_CLIENT_PROJECT=TRUE`` command, that generates a Java folder with client code. You can run ``make java_install`` and your module will be installed in your Maven local repository. To use the module in your Maven project, you have to add the dependency to the ``pom.xml`` file:

  .. sourcecode:: xml

     <dependency>
       <groupId>org.kurento.module</groupId>
       <artifactId>modulename</artifactId>
       <version>moduleversion</version>
     </dependency>

- For JavaScript, you should run ``cmake .. -DGENERATE_JS_CLIENT_PROJECT=TRUE``. This command generates a ``js/`` folder with client code. Now you can manually add the JavaScript library to use your module in your application. Alternatively, you can use :term:`Bower` (for *Browser JavaScript*) or :term:`NPM` (for *Node.js*). To do that, you should add your JavaScript module as a dependency in your ``bower.json`` or ``package.json`` file respectively, as follows:

  .. sourcecode:: js

     "dependencies": {
       "modulename": "moduleversion"
     }



Examples
========

Simple examples for both kind of modules are available in GitHub:

- `OpenCV module <https://github.com/Kurento/kms-opencv-plugin-sample>`__.
- `GStreamer module <https://github.com/Kurento/kms-plugin-sample>`__.

There are a lot of examples showking how to define methods, parameters or events in
all our public built-in modules:

- `kms-pointerdetector <https://github.com/Kurento/kms-pointerdetector/tree/master/src/server/interface>`__.
- `kms-crowddetector <https://github.com/Kurento/kms-crowddetector/tree/master/src/server/interface>`__.
- `kms-chroma <https://github.com/Kurento/kms-chroma/tree/master/src/server/interface>`__.
- `kms-platedetector <https://github.com/Kurento/kms-platedetector/tree/master/src/server/interface>`__.

Moreover, all our modules are developed using this methodology. For that reason you can take a look to our main modules:

- `kms-core <https://github.com/Kurento/kms-core>`__.
- `kms-elements <https://github.com/Kurento/kms-elements>`__.
- `kms-filters <https://github.com/Kurento/kms-filters>`__.
