==================
Release Procedures
==================

[WORK-IN-PROGRESS]

.. contents:: Table of Contents



Introduction
============

Kurento is a project composed of multiple kinds of modules, spanning a multitude of different technologies, languages and sets of *best practices*. Each one of those modules have their own unique style of administration, and the procedures needed to publish a new release can vary a lot.

This document aims to summarize all release procedures that apply to each one of the modules the Kurento project is composed of. The main form of categorization is by technology type: C/C++ based modules, Java modules, JavaScript modules, and others.


General considerations
======================

- Kurento components to be released must use development versions.
- Dependencies to Kurento libraries can use release or development versions, as needed.

  - In Maven (Java), development versions are indicated by the suffix ``-SNAPSHOT`` after the version number. Example: ``6.7.0-SNAPSHOT``.
  - In CMake (C/C++), development versions are indicated by the suffix ``-dev`` after the version number. Example: ``6.7.0-dev``.

- All dependencies to development versions will be changed to a release version during the release procedure. Concerning people will be asked to choose an appropriate release version for each development dependency.
- Kurento uses semantic versioning. Please refer to www.semver.org for more information.
- Tags will be named with the version number of the release. Example: ``6.7.0``.



Project Inventory
=================

Where applicable, these lists are presented in the order given by their module dependencies, such as the one indicated in :ref:`development-dependency-list`.



Media Server
------------

KMS main components
~~~~~~~~~~~~~~~~~~~

- kurento-module-creator
- kms-cmake-utils
- kms-jsonrpc
- kms-core
- kms-elements
- kms-filters
- kurento-media-server



KMS extra modules
~~~~~~~~~~~~~~~~~

- kms-chroma
- kms-crowddetector
- kms-datachannelexample
- kms-platedetector
- kms-pointerdetector



Kurento external libraries
~~~~~~~~~~~~~~~~~~~~~~~~~~

- jsoncpp
- libsrtp
- openh264
- usrsctp
- gstreamer
- gst-plugins-base
- gst-plugins-good
- gst-plugins-bad
- gst-plugins-ugly
- gst-libav
- openwebrtc-gst-plugins
- libnice


Java
Public
kurento-qa-pom
kurento-java
kurento-room
kurento-tutorial-java
kurento-maven-plugin
doc-kurento
Private
kurento-sfu
kurento-tree
kurento-demo
kurento-tutorial-test
Javascript
Public
kurento-client-js
kurento-jsonrpc-js
kurento-utils-js
kurento-tutorial-js
kurento-tutorial-node
KurentoForks/demo-console
Private
kurento-demo-js
kurento-demo-node
kurento-developer-portal



C/C++ modules
=============



Java modules
============

