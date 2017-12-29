===================
Introducing Kurento
===================

WebRTC media servers
====================

:term:`WebRTC` is an open source technology that enables web browsers with Real-Time Communications (RTC) capabilities via JavaScript APIs. It has been conceived as a peer-to-peer technology where browsers can directly communicate without the mediation of any kind of infrastructure. This model is enough for creating basic applications but features such as group communications, media stream recording, media broadcasting or media transcoding are difficult to implement on top of it. For this reason, many applications require using an intermediate media server.

.. figure:: /images/media-server-intro.png
   :align:   center
   :alt:     Peer-to-peer WebRTC approach vs. WebRTC through a media server

   *Peer-to-peer WebRTC approach vs. WebRTC through a media server*

Conceptually, a WebRTC media server is just a multimedia middleware where media traffic passes through when moving from source to destinations.
Media servers are capable of processing incoming media streams and offer different outcomes, such as:

- Group Communications: Distributing among several receivers the media stream that one peer generates, i.e. acting as a Multi-Conference Unit (“MCU“).
- Mixing: Transforming several incoming stream into one single composite stream.
- Transcoding: On-the-fly adaptation of codecs and formats between incompatible clients.
- Recording: Storing in a persistent way the media exchanged among peers.

.. figure:: /images/media-server-capabilities.png
   :align:  center
   :alt:    Typical WebRTC Media Server capabilities

   *Typical WebRTC Media Server capabilities*



Kurento Media Server
====================

At the heart of the Kurento architecture there is a media server called **Kurento Media Server (KMS)**. KMS is based on pluggable media processing capabilities meaning that its features are provided by composable modules that can be activated or deactivated at any point in time. Moreover, developers can seamlessly create additional modules, thus extending Kurento Media Server with new functionalities which can be plugged in dynamically.

Out of the box, KMS provides a multitude of features, like group communications, mixing, transcoding, recording, and playing. In addition, it also provides advanced modules for media processing including Computer Vision, Augmented Reality, alpha blending, and much more.

.. figure:: /images/kurento-media-server-intro.png
   :align:  center
   :alt:    Kurento Media Server capabilities

   *Kurento Media Server capabilities*



Kurento API, Clients, and Protocol
==================================

KMS is controlled by means of an API, for which the Kurento project provides several :term:`Kurento Client` libraries: for `Java`_, `Browser Javascript`_, and `Node.js`_. If you prefer another programming language, a custom Kurento Client library can be written by following the specification of the :term:`Kurento Protocol`, based on :term:`WebSocket` and :term:`JSON-RPC`.

.. _Java: http://www.java.com
.. _Browser Javascript: http://www.w3.org/standards/webdesign/script
.. _Node.js: https://nodejs.org

The picture below shows how to use Kurento Clients in three scenarios:

- Using the Kurento JavaScript Client directly in a compliant :term:`WebRTC` browser.
- Using the Kurento Java Client in a Java EE Application Server.
- Using the Kurento JavaScript Client in a Node.js server.

.. figure:: /images/kurento-clients-connection.png
   :align:  center
   :alt:    Connection of Kurento Clients (Java and JavaScript) to Kuento Media Server

   *Connection of Kurento Clients (Java and JavaScript) to Kuento Media Server*

Complete examples for these three technologies is described in the :doc:`tutorials </tutorials>` section.

The Kurento Client API is based on the concept of **Media Element**s. A Media Element holds a specific media capability. For example, the media element called *WebRtcEndpoint* holds the capability of sending and receiving WebRTC media streams; the media element called *RecorderEndpoint* has the capability of recording into the file system any media streams it receives; the *FaceOverlayFilter* detects faces on the exchanged video streams and adds a specific overlaid image on top of them, etc. Kurento exposes a rich toolbox of media elements as part of its APIs.

.. figure:: /images/kurento-basic-toolbox.png
   :align:  center
   :alt:    Some Media Elements provided out of the box by Kurento

   *Some Media Elements provided out of the box by Kurento*

To better understand theses concepts it is recommended to take a look to :doc:`Kurento API </mastering/kurento_API>` and :doc:`Kurento Protocol </mastering/kurento_protocol>` sections. You can also take a look at the JavaDoc and JsDoc:

- `kurento-client-java </langdoc/javadoc/index.html>`_ : JavaDoc of Kurento Java Client.
- `kurento-client-js </langdoc/jsdoc/kurento-client-js/index.html>`_ : JsDoc of Kurento JavaScript Client.
- `kurento-utils-js </langdoc/jsdoc/kurento-utils-js/index.html>`_ : JsDoc of an utility JavaScript library aimed to simplify the development of WebRTC applications.



Creating applications with Kurento
==================================

From the application developer perspective, Media Elements are like *Lego* pieces: you just need to take the elements needed for an application and connect them, following the desired topology. In Kurento jargon, a graph of connected media elements is called a **Media Pipeline**. Hence, when creating a pipeline, developers need to determine the capabilities they want to use (the media elements) and the topology determining which media elements provide media to which other media elements (the connectivity). The connectivity is controlled through the *connect* primitive, exposed on all Kurento Client APIs.

This primitive is always invoked in the element acting as source and takes as argument the sink element following this scheme:

.. sourcecode:: java

   sourceMediaElement.connect(sinkMediaElement)

For example, if you want to create an application recording WebRTC streams into the file system, you'll need two media elements: *WebRtcEndpoint* and *RecorderEndpoint*. When a client connects to the application, you will need to instantiate these media elements making the stream received by the
*WebRtcEndpoint* (which is capable of receiving WebRTC streams) to be fed to the *RecorderEndpoint* (which is capable of recording media streams into the file system). Finally you will need to connect them so that the stream received by the former is transferred into the later:

.. sourcecode:: java

   WebRtcEndpoint.connect(RecorderEndpoint)

To simplify the handling of WebRTC streams in the client-side, Kurento provides an utility called *WebRtcPeer*. Nevertheless, the standard WebRTC API (*getUserMedia*, *RTCPeerConnection*, and so on) can also be used to connect to *WebRtcEndpoints*. For further information please visit the :doc:`tutorials </tutorials>` section.

.. figure:: /images/media-pipeline-sample.png
   :align:  center
   :alt:    Simple Example of a Media Pipeline

   *Simple Example of a Media Pipeline*
