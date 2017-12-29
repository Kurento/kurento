================
What is Kurento?
================

`Kurento`_ is a `WebRTC`_ Media Server and a set of client APIs that simplify the development of advanced video applications for web and smartphone platforms.
Its features include group communications, transcoding, recording, mixing, broadcasting and routing of audiovisual flows.

The code is open source, released under the terms of `Apache License Version 2.0`_ and `available on GitHub`_.

.. _Kurento: http://www.kurento.org
.. _WebRTC: https://webrtc.org
.. _Apache License Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
.. _available on GitHub: https://github.com/Kurento

Kurento modular architecture eases the integration of third-party media
processing algorithms, allowing to provide advanced capabilities such as Computer Vision, Augmented Reality, video indexing and speech analysis. This means that features such as speech recognition, sentiment analysis or face recognition can be transparently used by application developers, exactly as if those were part of the core built-in features.

Kurento´s main component is the **Kurento Media Server (KMS)**, responsible for media transmission, processing, recording, and playback. It is implemented in low level technologies based on :term:`GStreamer` to optimize the usage of resources.

It provides the following features:

-  Networked streaming protocols, including :term:`HTTP`, :term:`RTP` and :term:`WebRTC`.
-  Group communications (MCU and SFU functionality) supporting both media mixing and media routing/dispatching.
-  Generic support for filters implementing Computer Vision and Augmented Reality algorithms.
-  Media storage that supports writing operations for :term:`WebM` and :term:`MP4` and playing in all formats supported by *GStreamer*.
-  Automatic media transcoding between any of the codecs supported by GStreamer, including VP8, H.264, H.263, AMR, OPUS, Speex, G.711 and more.

KMS is controlled by means of an API, for which the Kurento project provides several :term:`Kurento Client` libraries: for `Java`_, `Browser Javascript`_, and `Node.js`_. If you prefer another programming language, a custom Kurento Client library can be written by following the specification of the :term:`Kurento Protocol`, based on :term:`WebSocket` and :term:`JSON-RPC`.

.. _Java: http://www.java.com
.. _Browser Javascript: http://www.w3.org/standards/webdesign/script
.. _Node.js: https://nodejs.org

Check out how to :doc:`get started </getting_started>` with Kurento, and take a look at our :doc:`tutorials </tutorials>` which showcase some demo applications. You can choose your favorite technology to build multimedia applications: **Java**, **Browser JavaScript** or **Node.js**.
