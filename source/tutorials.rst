%%%%%%%%%%%%%%%%%
Kurento Tutorials
%%%%%%%%%%%%%%%%%

This section contains tutorials showing how to use Kurento framework to build
different types of `WebRTC`:term: and multimedia applications. Turorials come
in three flavors:

- **Java**: These show applications where clients interact with an application
  server based on Java EE technology. The application server hold the logic
  orchestrating the communication among the clients and controlling Kurento
  Server capabilities for them.

- **Browser JavaScript**: These show applications executing at the browser and
  communicating directly with the Kurento Media Server. In these tutorial, all
  the application logic is hold by the browser. Hence, no application server is
  necessary. For these reasons, these applications need to be simple.

- **Node.js**: These show applications where clients interact with an
  application server based on Node.js technology. The application server holds
  the logic orchestrating the communication among the clients and controlling
  Kurento Media Server capabilities for them.

.. note::

   These tutorials have been created with learning objectives. They are not
   intended to be used in production environments where different unmanaged error
   conditions may emerge.


Tutorial 1 - Hello world
========================

This is one of the simplest WebRTC application you can create with Kurento. It
implements a `WebRTC`:term: *loopback* (a WebRTC media stream going from client
to Kurento and back to the client)

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-1-helloworld>
   Browser JavaScript </tutorials/js/tutorial-1-helloworld>
   Node.js </tutorials/node/tutorial-1-helloworld>

Tutorial 2 - WebRTC magic mirror
================================

This web application consists on a `WebRTC`:term: video communication in mirror
adding a funny hat over your face. This is an example of computer vision and
augmented reality using a filter.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-2-magicmirror>
   Browser JavaScript </tutorials/js/tutorial-2-magicmirror>
   Node.js </tutorials/node/tutorial-2-magicmirror>

Tutorial 3 - WebRTC one-to-many broadcast
=========================================

Video broadcasting for `WebRTC`:term:. One peer transmits a video stream and N
peers receives it.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-3-one2many>
   Node.js </tutorials/node/tutorial-3-one2many>

Tutorial 4 - WebRTC one-to-one video call
=========================================

This web application is a videophone (call one to one) based on `WebRTC`:term:.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-4-one2one>
   Node.js </tutorials/node/tutorial-4-one2one>

Tutorial 5 - WebRTC one-to-one video call with recording and filtering
======================================================================

This is an enhanced version of the previous application recording of the video
communication, and also integration with an augmented reality filter.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-5-one2one-adv>
   
Tutorial 6 - WebRTC many to many video call (Group call)
========================================================

This tutorial allows connect several participants in the same session and see each of them.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-6-groupcall>

Tutorial 7 - WebRTC in loopback with two filters which using metadata
=====================================================================

This tutorial detects and draws faces into the webcam video. The tutorial connects two filters, 
the KmsDetectFaces and the KmsShowFaces.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-7-metadata>

Tutorial 8 - Play of a video through WebRTC
===========================================

This tutorial plays of a video through WebRTC.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-8-player>

Tutorial 9 - WebRTC to send data about qr codes through data channel
====================================================================

Player connected to a filter and a WebRTC to send data about qr codes through data channel.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-9-send-datachannel>

Tutorial 10 - WebRTC in loopback with filter to show data received through data channel
=======================================================================================

This demo allows sending text from browser to the media server through data channels. 
That text will be shown in the loopback video.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-10-show-datachannel>
   Browser JavaScript </tutorials/js/tutorial-10-helloworld-datachannels>
   
Tutorial 11 - WebRTC in loopback with recorder
==============================================

This demo has two parts, in the first part implements a `WebRTC`:term: *loopback* and 
records the media in the Kurento Media Server. And in the second part plays the media was recorded 
in the Kurento Media Server.

.. toctree::
   :maxdepth: 1

   Browser JavaScript </tutorials/js/tutorial-11-recorder>

   
Tutorial 12 - WebRTC in loopback with WebRTC statistics
=======================================================

This demo implements a `WebRTC`:term: *loopback* and shows in the browser several statistics about WebRTC.

.. toctree::
   :maxdepth: 1

   Browser JavaScript </tutorials/js/tutorial-12-loopback-stats>
   

