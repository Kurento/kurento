=======================
Introduction to Kurento
=======================

What is Kurento?
================

:term:`Kurento` Media Server (**KMS**) is a multimedia server package that can be used to develop advanced video applications for :term:`WebRTC` platforms. It is an Open Source project, with source code released under the terms of `Apache License Version 2.0 <https://www.apache.org/licenses/LICENSE-2.0>`__ and `available on GitHub <https://github.com/Kurento>`__.

The most prominent characteristics of Kurento are these:



Modular Pipelines
-----------------

.. figure:: /images/example-pipeline-browser-recorder.png
   :align: center
   :alt: Simple Example of a Media Pipeline

   *Simple Example of a Media Pipeline*

A Kurento Pipeline is based on composable elements such as WebRTC or RTP receivers, recorders, mixers, etc. that can be mix-and-matched, activated, or deactivated at any point in time, even when the media is already flowing.



Built-in Modules
----------------

Kurento is based on the concept of **Media Elements**: self-contained objects that hold a specific media capability. Several modules are provided for **group communications**, **transcoding** of media formats, **recording**, **mixing**, and **routing** of audiovisual flows.

For example, the *WebRtcEndpoint* element is able to send and receive :term:`WebRTC` media streams; the *RecorderEndpoint* element can store media streams into a file system; the *FaceOverlayFilter* element detects faces on the exchanged video streams and adds a specific overlaid image on top of them; and so on.

Kurento exposes a rich toolbox of media elements as part of its API:

.. figure:: /images/kurento-toolbox-basic.png
   :align: center
   :alt: Some Media Elements provided out of the box by Kurento

   *Some Media Elements provided out of the box by Kurento*

To better understand these concepts it is recommended to take a look to the sections :doc:`/features/kurento_modules` and :doc:`/features/kurento_protocol`. You can also take a look at the API SDK implementations in :doc:`/features/kurento_client`.

Furthermore, Kurento has a plugin API that allows you to :doc:`write your own modules </user/writing_modules>`!



JSON-RPC Protocol
-----------------

KMS exposes all its API features through a JSON-RPC protocol called :doc:`/features/kurento_protocol`, which can be accessed directly through a WebSocket connection. For convenience, Kurento also offers Java and JavaScript SDKs: :doc:`/features/kurento_client`. But you can use **any programming language**, simply writing your code directly against the protocol.

The picture below shows how to use Kurento in three scenarios:

- Using the Kurento JavaScript SDK directly from a WebRTC browser (only recommended for quick tests and development, not for production services).
- Using the Kurento Java SDK in a standalone Java EE Application Server. The web browser is a client of this application for things like HTML, and :term:`WebRTC` signaling, while the application itself is client of KMS (using the Kurento Protocol to control KMS).
- Using the Kurento JavaScript SDK in a Node.js Application Server. Again, the web browser is a client of this application, while the application is client of KMS.

.. figure:: /images/kurento-clients-connection.png
   :align: center
   :alt: Connection of Kurento Java and JavaScript SDKs to Kurento Media Server

   *Connection of Kurento Java and JavaScript SDKs to Kurento Media Server*

Complete examples for the supported SDK technologies are described in the :doc:`Tutorials section </user/tutorials>`.



WebRTC implementation
---------------------

KMS offers a functional implementation of the whole :term:`WebRTC` stack. Use it to send or receive media from web browsers such as Firefox, Safari, or Chrome.



What is a WebRTC media server?
==============================

`WebRTC <https://webrtc.org/>`__ is a set of protocols and APIs that provide web browsers and mobile applications with Real-Time Communications (RTC) capabilities over peer-to-peer connections. It was conceived to allow connecting browsers without intermediate helpers or services, but in practice this P2P model falls short when trying to create more complex applications. For this reason, in most cases a central media server is required.

.. figure:: /images/media-server-intro.png
   :align: center
   :alt: Peer-to-peer WebRTC approach vs. WebRTC through a media server

   *Peer-to-peer WebRTC approach vs. WebRTC through a media server*

Conceptually, a WebRTC media server is just a multimedia middleware where media traffic passes through when moving from source(s) to destination(s).

Media servers are capable of processing incoming media streams and offer different outcomes, such as:

- Group Communications: Distributing among several receivers the media stream that one peer generates, i.e. acting as a Multi-Conference Unit ("MCU").
- Mixing: Transforming several incoming stream into one single composite stream.
- Transcoding: On-the-fly adaptation of codecs and formats between incompatible clients.
- Recording: Storing in a persistent way the media exchanged among peers.

.. figure:: /images/media-server-capabilities.png
   :align: center
   :alt: Typical WebRTC Media Server capabilities

   *Typical WebRTC Media Server capabilities*



Why Kurento Media Server?
=========================

**Kurento Media Server** (KMS) can be used in the *WebRTC Media Server* model, to allow for media transmission, processing, recording, and playback. KMS is built on top of the fantastic :term:`GStreamer` multimedia library, and provides the following features:

*  Networked streaming protocols, including :term:`HTTP`, :term:`RTP` and :term:`WebRTC`.
*  Group communications (*both* MCU *and* SFU functionality) supporting media mixing and media routing/dispatching.
*  Generic support for filters implementing **Computer Vision** and **Augmented Reality** algorithms.
*  Media storage that supports writing operations for :term:`WebM` and :term:`MP4` and playing in all formats supported by *GStreamer*.
*  Automatic media transcoding between any of the codecs supported by GStreamer, including VP8, H.264, H.263, AMR, OPUS, Speex, G.711, and more.

.. figure:: /images/kurento-media-server-intro.png
   :align: center
   :alt: Kurento Media Server capabilities

   *Kurento Media Server capabilities*



Kurento Design Principles
=========================

Kurento is designed based on the following main principles:

    **Distribution of Media and Application Services**
        Kurento Media Server and applications can be deployed, escalated or distributed among different machines.

        A single application can invoke the services of more than one Kurento Media Server. The opposite also applies, that is, a Kurento Media Server can attend the requests of more than one application.

    **Suitable for the Cloud**
        Kurento is suitable to be integrated into cloud environments to act as a PaaS (Platform as a Service) component.

    **Media Pipelines**
        Chaining :term:`Media Elements <Media Element>` via :term:`Media Pipelines <Media Pipeline>` is an intuitive approach to challenge the complexity of multimedia processing.

    **Application development**
        Developers do not need to be aware of internal Kurento Media Server complexities: all the applications can deployed in any technology or framework the developer likes, from client to server. From browsers to cloud services.

    **End-to-End Communication Capability**
        Kurento provides end-to-end communication capabilities so developers do not need to deal with the complexity of transporting, encoding/decoding and rendering media on client devices.

    **Fully Processable Media Streams**
       Kurento enables not only interactive interpersonal communications (e.g. Skype-like with conversational call push/reception capabilities), but also human-to-machine (e.g. Video on Demand through real-time streaming) and machine-to-machine (e.g. remote video recording, multisensory data exchange) communications.

    **Modular Processing of Media**
       Modularization achieved through :term:`media elements <Media Element>` and :term:`pipelines <Media Pipeline>` allows defining the media processing functionality of an application through a "graph-oriented" language, where the application developer is able to create the desired logic by chaining the appropriate functionalities.

    **Auditable Processing**
        Kurento is able to generate rich and detailed information for QoS monitoring, billing and auditing.

    **Seamless IMS integration**
        Kurento is designed to support seamless integration into the :term:`IMS` infrastructure of Telephony Carriers.

    **Transparent Media Adaptation Layer**
        Kurento provides a transparent media adaptation layer to make the convergence among different devices having different requirements in terms of screen size, power consumption, transmission rate, etc. possible.
