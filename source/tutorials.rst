%%%%%%%%%%%%%%%%%
Kurento Tutorials
%%%%%%%%%%%%%%%%%

This section contains tutorials showing how to use Kurento framework to build
different types of `WebRTC`:term: and multimedia applications. Tutorials come
in three flavors:

- **Java**: These show applications where clients interact with Spring Boot -based
  applications, that host the logic orchestrating the communication among
  clients and controlling Kurento Media Server capabilities.

- **Browser JavaScript**: These show applications executing at the browser and
  communicating directly with the Kurento Media Server. In these tutorials all
  logic is directly hosted by the browser. Hence, no application server is
  necessary.

- **Node.js**: These show applications where clients interact with an
  application server based on Node.js technology. The application server holds
  the logic orchestrating the communication among the clients and controlling
  Kurento Media Server capabilities for them.

.. note::

   The tutorials have been created with learning objectives. They are not
   intended to be used in production environments where different unmanaged error
   conditions may emerge. Use at your own risk!

.. note::

   These tutorials require ``https`` in order to use WebRTC. Following
   `instructions <mastering/securing-kurento-applications.html>`_ will provide
   further information about how to enable application security.


Hello world
===========

This is one of the simplest WebRTC applications you can create with Kurento. It
implements a `WebRTC`:term: *loopback* (a WebRTC media stream going from client
to Kurento Media Server and back to the client)

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-helloworld>
   Browser JavaScript </tutorials/js/tutorial-helloworld>
   Node.js </tutorials/node/tutorial-helloworld>

WebRTC magic mirror
===================

This web application consists on a `WebRTC`:term: video communication in loopback,
adding a funny hat over detected faces. This is an example of a computer vision and
augmented reality filter.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-magicmirror>
   Browser JavaScript </tutorials/js/tutorial-magicmirror>
   Node.js </tutorials/node/tutorial-magicmirror>

WebRTC one-to-many broadcast
============================

Video broadcasting for `WebRTC`:term:. One peer transmits a video stream and N
peers receive it.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-one2many>
   Node.js </tutorials/node/tutorial-one2many>

WebRTC one-to-one video call
============================

This web application is a videophone (call one to one) based on `WebRTC`:term:.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-one2one>
   Node.js </tutorials/node/tutorial-one2one>

WebRTC one-to-one video call with recording and filtering
=========================================================

This is an enhanced version of the the one-to-one application with video recording
and augmented reality.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-one2one-adv>

WebRTC many-to-many video call (Group call)
===========================================

This tutorial connects several participants to the same video conference. A group
call will consist -in the media server side- in N*N WebRTC endpoints, where N is
the number of clients connected to that conference.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-groupcall>

Media Elements metadata
=======================

This tutorial detects and draws faces present in the webcam video. It connects
filters: KmsDetectFaces and the KmsShowFaces.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-metadata>

Play media to WebRTC
====================

This tutorial reads a file from disk and plays the video to WebRTC.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-player>

WebRTC outgoing data channels
=============================

This tutorial injects video to a QR filter and then sends the stream to WebRTC.
QR detection events are delivered by means of WebRTC data channels, to be
displayed in browser.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-send-datachannel>

WebRTC incoming data channel
============================

This tutorial shows how text messages sent from browser can be delivered by data
channels, to be displayed together with loopback video.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-show-datachannel>
   Browser JavaScript </tutorials/js/tutorial-helloworld-datachannels>

WebRTC recording
================

This tutorial has two parts. First, it implements a `WebRTC`:term: *loopback* and
records the stream to disk. Second, it plays back the recorded stream. Users
can choose which type of media to send and record: audio, video or both.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-recorder>
   Browser Javascript </tutorials/js/tutorial-recorder>

WebRTC repository
=================

This is similar to the recording tutorial, but using the repository to store
metadata.

.. toctree::
   :maxdepth: 1

   Java </tutorials/java/tutorial-repository>


WebRTC statistics
=================

This tutorial implements a `WebRTC`:term: *loopback* and shows how to collect
WebRTC statistics.

.. toctree::
   :maxdepth: 1

   Browser JavaScript </tutorials/js/tutorial-loopback-stats>
