========================
About Kurento and WebRTC
========================

:term:`Kurento` is a :term:`WebRTC` Media Server and a set of client APIs that simplify the development of advanced video applications for web and smartphone platforms. Its features include group communications, transcoding, recording, mixing, broadcasting and routing of audiovisual flows.

The code is open source, released under the terms of `Apache License Version 2.0`_ and `available on GitHub`_.

.. _Apache License Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
.. _available on GitHub: https://github.com/Kurento

Kurento follows an architecture based on composable modules that can be mix-and-matched, activated, or deactivated at any point in time. Developers can create additional modules to add new functionalities that will be able to be plugged-in dynamically.

With Kurento, it's an easy task to add third-party media processing algorithms to any WebRTC application, like integrating Computer Vision, Augmented Reality, video indexing, and speech analysis. For example, features such as speech recognition, sentiment analysis or face recognition can be developed by specialized teams, and then seamlessly added to Kurento as new modules.



WebRTC media servers
====================

`WebRTC <https://webrtc.org/>`_ is a set of protocols, mechanisms and APIs that provide browsers and mobile applications with Real-Time Communications (RTC) capabilities over peer-to-peer connections. It has been conceived as a technology that allows browsers to communicate directly without the mediation of any kind of infrastructure. However, this model is only enough for creating basic web applications; features such as group communications, media stream recording, media broadcasting, or media transcoding are difficult to implement on top of it. For this reason, many applications end up requiring an intermediate media server.

.. figure:: /images/media-server-intro.png
   :align: center
   :alt: Peer-to-peer WebRTC approach vs. WebRTC through a media server

   *Peer-to-peer WebRTC approach vs. WebRTC through a media server*

Conceptually, a WebRTC media server is just a multimedia middleware where media traffic passes through when moving from source to destinations.

Media servers are capable of processing incoming media streams and offer different outcomes, such as:

- Group Communications: Distributing among several receivers the media stream that one peer generates, i.e. acting as a Multi-Conference Unit ("MCU").
- Mixing: Transforming several incoming stream into one single composite stream.
- Transcoding: On-the-fly adaptation of codecs and formats between incompatible clients.
- Recording: Storing in a persistent way the media exchanged among peers.

.. figure:: /images/media-server-capabilities.png
   :align: center
   :alt: Typical WebRTC Media Server capabilities

   *Typical WebRTC Media Server capabilities*



Kurento Media Server
====================

Kurento's main component is the **Kurento Media Server** (KMS), responsible for media transmission, processing, recording, and playback. KMS is built on top of the fantastic :term:`GStreamer` multimedia library, and provides the following features:

-  Networked streaming protocols, including :term:`HTTP`, :term:`RTP` and :term:`WebRTC`.
-  Group communications (MCU and SFU functionality) supporting both media mixing and media routing/dispatching.
-  Generic support for filters implementing Computer Vision and Augmented Reality algorithms.
-  Media storage that supports writing operations for :term:`WebM` and :term:`MP4` and playing in all formats supported by *GStreamer*.
-  Automatic media transcoding between any of the codecs supported by GStreamer, including VP8, H.264, H.263, AMR, OPUS, Speex, G.711, and more.

.. figure:: /images/kurento-media-server-intro.png
   :align: center
   :alt: Kurento Media Server capabilities

   *Kurento Media Server capabilities*



Kurento Design Principles
=========================

Kurento is designed based on the following main principles:

    **Separate Media and Signaling Planes**
        :term:`Signaling <signaling plane>` and :term:`Media <media plane>` are two separate planes and Kurento is designed so that applications can handle separately those facets of multimedia processing.

    **Distribution of Media and Application Services**
        Kurento Media Server and applications can be collocated, escalated or distributed among different machines.

        A single application can invoke the services of more than one Kurento Media Server. The opposite also applies, that is, a  Kurento Media Server can attend the requests of more than one application.

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
