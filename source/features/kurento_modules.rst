===============
Kurento Modules
===============

Kurento is a pluggable framework. Each plugin in Kurento is called a *module*.

If you are interested in writing our own modules, please read the section about :doc:`Writing Kurento Modules </user/writing_modules>`.

We classify Kurento modules into three groups, namely:

- **Main modules**. Incorporated out of the box with Kurento Media Server:

  - **kms-core**: Main components of Kurento Media Server.
  - **kms-elements**: Implementation of Kurento Media Elements (*WebRtcEndpoint*, *PlayerEndpoint*, etc.)
  - **kms-filters**: Implementation of Kurento Filters (**FaceOverlayFilter**, **ZBarFilter**, etc.)

- **Built-in modules**. Extra modules developed by the Kurento team to enhance the basic capabilities of Kurento Media Server. So far, there are four built-in modules, namely:

  - **kms-pointerdetector**: Filter that detects pointers in video streams, based on color tracking. Install command:

    .. sourcecode:: bash

       sudo apt-get install kms-pointerdetector

  - **kms-chroma**: Filter that takes a color range in the top layer and makes it transparent, revealing another image behind. Install command:

    .. sourcecode:: bash

       sudo apt-get install kms-chroma

  - **kms-crowddetector**: Filter that detects people agglomeration in video streams. Install command:

    .. sourcecode:: bash

       sudo apt-get install kms-crowddetector

  - **kms-platedetector**: Filter that detects vehicle plates in video streams. Install command:

    .. sourcecode:: bash

       sudo apt-get install kms-platedetector

    .. warning::

       The plate detector module is a prototype and its results are not always accurate. Consider this if you are planning to use this module in a production environment.

- **Custom modules**. Extensions to Kurento Media Server which provides new media capabilities.

The following picture shows an schematic view of the Kurento Media Server with its different modules:

.. figure:: ../images/kurento-modules02.png
   :align: center
   :alt: Kurento modules architecture

   **Kurento modules architecture**
   *Kurento Media Server can be extended with built-in modules (crowddetector, pointerdetector, chroma, platedetector) and also with other custom modules.*

Taking into account the built-in modules, the Kurento toolbox is extended as follows:

.. figure:: ../images/kurento-extended-toolbox.png
   :align: center
   :alt: Extended Kurento Toolbox

   **Extended Kurento Toolbox**
   *The basic Kurento toolbox (left side of the picture) is extended with more Computer Vision and Augmented Reality filters (right side of the picture) provided by the built-in modules.*

The remainder of this page is structured in four sections in which the built-in modules (*kms-pointerdetector*, *kms-chroma*, *kms-crowddetector*, *kms-platedetector*) are used to develop simple applications (tutorials) aimed to show how to use them.



Module Tutorial - Pointer Detector Filter
=========================================

This web application consists of a :term:`WebRTC` video communication in mirror (*loopback*) with a pointer-tracking filter element.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/module-pointerdetector>
   Browser JavaScript </tutorials/js/module-pointerdetector>
   Node.js </tutorials/node/module-pointerdetector>



Module Tutorial - Chroma Filter
===============================

This web application consists of a :term:`WebRTC` video communication in mirror (*loopback*) with a chroma filter element.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/module-chromafilter>
   Browser JavaScript </tutorials/js/module-chromafilter>
   Node.js </tutorials/node/module-chromafilter>



Module Tutorial - Crowd Detector Filter
=======================================

This web application consists of a :term:`WebRTC` video communication in mirror (*loopback*) with a crowd detector filter. This filter detects people agglomeration in video streams.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/module-crowddetector>
   Browser JavaScript </tutorials/js/module-crowddetector>
   Node.js </tutorials/node/module-crowddetector>



Module Tutorial - Plate Detector Filter
=======================================

This web application consists of a :term:`WebRTC` video communication in mirror (*loopback*) with a plate detector filter element.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/module-platedetector>
   Browser JavaScript </tutorials/js/module-platedetector>
   Node.js </tutorials/node/module-platedetector>
