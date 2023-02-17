=========
Tutorials
=========

.. contents:: Table of Contents

This section contains tutorials showing how to use the Kurento framework to build different types of :term:`WebRTC` and multimedia applications.

.. note::

   These tutorials have been created with **learning purposes**. They don't have comprehensive error handling, or any kind of sophisticated session management. As such, *these tutorials should not be used in production environments*; they only show example code for you to study, in order to achieve what you want with your own code.

   **Use at your own risk!**

These tutorials come in three flavors:

- **Java**: Showing applications where clients interact with *Spring Boot*-based
  applications, that host the logic orchestrating the communication among clients and control Kurento Media Server capabilities.

  To run the Java tutorials, you need to first install the Java JDK and Maven:

  .. code-block:: shell

     sudo apt-get update ; sudo apt-get install --no-install-recommends \
         git \
         default-jdk \
         maven

  Java tutorials are written on top of `Spring Boot <https://spring.io/projects/spring-boot>`__, so they already include most features expected from a full-fledged service, such as a web server or logging support.

  Spring Boot is also able to create a "`fully executable jar <https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html#deployment-install>`__", a standalone executable built out of the application package. This executable comes already with support for commands such as *start*, *stop*, or *restart*, so it can be used as a system service with either *init.d* (System V) and *systemd*. For more info, refer to the `Spring Boot documentation <https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html#deployment-service>`__ and online resources such as `this Stack Overflow answer <https://stackoverflow.com/questions/21503883/spring-boot-application-as-a-service/30497095#30497095>`__.

- **Browser JavaScript**: These show applications executing at the browser and communicating directly with the Kurento Media Server. In these tutorials all logic is directly hosted by the browser. Hence, no application server is necessary.

- **Node.js**: In which clients interact with an application server made with Node.js technology. The application server holds the logic orchestrating the communication among the clients and controlling Kurento Media Server capabilities for them.

.. note::

   These tutorials require *HTTPS* in order to use WebRTC. Following instructions will provide further information about how to enable application security.



Hello World
===========

This is one of the simplest WebRTC applications you can create with Kurento. It implements a :term:`WebRTC` *loopback* (a WebRTC media stream going from client to Kurento Media Server and back to the client)

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-helloworld>
   Browser JavaScript </tutorials/js/tutorial-helloworld>
   Node.js </tutorials/node/tutorial-helloworld>



WebRTC Magic Mirror
===================

This web application consists of a :term:`WebRTC` *loopback* video communication, adding a funny hat over detected faces. This is an example of a Computer Vision and Augmented Reality filter.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-magicmirror>
   Browser JavaScript </tutorials/js/tutorial-magicmirror>
   Node.js </tutorials/node/tutorial-magicmirror>



RTP Receiver
============

This web application showcases reception of an incoming RTP or SRTP stream, and playback via a WebRTC connection.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-rtp-receiver>



WebRTC One-To-Many broadcast
============================

Video broadcasting for :term:`WebRTC`. One peer transmits a video stream and N peers receive it.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-one2many>
   Node.js </tutorials/node/tutorial-one2many>



WebRTC One-To-One video call
============================

This web application is a videophone (call one to one) based on :term:`WebRTC`.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-one2one>
   Node.js </tutorials/node/tutorial-one2one>



WebRTC One-To-One video call with recording and filtering
=========================================================

This is an enhanced version of the the One-To-One application with video recording and Augmented Reality.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-one2one-adv>



WebRTC Many-To-Many video call (Group Call)
===========================================

This tutorial connects several participants to the same video conference. A group call will consist (in the media server side) in N*N WebRTC endpoints, where N is the number of clients connected to that conference.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-groupcall>



Media Elements metadata
=======================

This tutorial detects and draws faces present in the webcam video. It connects filters: KmsDetectFaces and the KmsShowFaces.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-metadata>



WebRTC Media Player
===================

This tutorial reads a file from disk or from any URL, and plays the video to WebRTC.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-player>



WebRTC outgoing Data Channels
=============================

This tutorial injects video into a QR filter and then sends the stream to WebRTC. QR detection events are delivered by means of WebRTC Data Channels, to be displayed in browser.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-datachannel-send-qr>



WebRTC incoming Data Channel
============================

This tutorial shows how text messages sent from browser can be delivered by Data Channels, to be displayed together with loopback video.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-show-datachannel>
   Browser JavaScript </tutorials/js/tutorial-helloworld-datachannels>



WebRTC recording
================

This tutorial has two parts:

1. A :term:`WebRTC` *loopback* records the stream to disk.
2. The stream is played back.

Users can choose which type of media to send and record: audio, video or both.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-recorder>
   Browser JavaScript </tutorials/js/tutorial-recorder>



WebRTC statistics
=================

This tutorial implements a :term:`WebRTC` *loopback* and shows how to collect WebRTC statistics.

.. toctree::
   :maxdepth: 1

   Browser JavaScript </tutorials/js/tutorial-loopback-stats>



Chroma Filter
=============

This web application consists of a :term:`WebRTC` video communication in mirror (*loopback*) with a chroma filter element.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/module-chromafilter>
   Browser JavaScript </tutorials/js/module-chromafilter>
   Node.js </tutorials/node/module-chromafilter>



Crowd Detector Filter
=====================

This web application consists of a :term:`WebRTC` video communication in mirror (*loopback*) with a crowd detector filter. This filter detects people agglomeration in video streams.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/module-crowddetector>
   Browser JavaScript </tutorials/js/module-crowddetector>
   Node.js </tutorials/node/module-crowddetector>



Plate Detector Filter
=====================

This web application consists of a :term:`WebRTC` video communication in mirror (*loopback*) with a plate detector filter element.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/module-platedetector>
   Browser JavaScript </tutorials/js/module-platedetector>
   Node.js </tutorials/node/module-platedetector>



Pointer Detector Filter
=======================

This web application consists of a :term:`WebRTC` video communication in mirror (*loopback*) with a pointer-tracking filter element.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/module-pointerdetector>
   Browser JavaScript </tutorials/js/module-pointerdetector>
   Node.js </tutorials/node/module-pointerdetector>
