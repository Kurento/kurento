.. _What_is_Kurento:

.. image:: images/kurento-rect-logo3.png
   :alt:    Creating client applications through Kurento APIs
   :align:  center

%%%%%%%%%%%%%
About Kurento
%%%%%%%%%%%%%

**Kurento** is a WebRTC media server and a set of client APIs simplifying the
development of advanced video applications for web and smartphone platforms.
Kurento features include group communications, transcoding, recording, mixing,
broadcasting and routing of audiovisual flows.

Kurento also provides advanced media processing capabilities involving computer
vision, video indexing, augmented reality and speech analysis. Kurento modular
architecture eases the integration of third party media processing algorithms
(eg. speech recognition, sentiment analysis, face recognition, etc.), which can
be transparently used by application developers as the rest of Kurento built-in
features.

Kurento´s core element is the **Kurento Media Server**, responsible for media
transmission, processing, loading and recording. It is implemented in low level
technologies based on :term:`GStreamer` to optimize the resource consumption.
It provides the following features:

-  Networked streaming protocols, including :term:`HTTP`, :term:`RTP` and
   :term:`WebRTC`.
-  Group communications (MCU and SFU functionality) supporting both media
   mixing and media routing/dispatching.
-  Generic support for filters implementing Computer Vision and Augmented
   Reality algorithms.
-  Media storage that supports writing operations for :term:`WebM` and
   :term:`MP4` and playing in all formats supported by *GStreamer*.
-  Automatic media transcoding between any of the codecs supported by
   GStreamer, including VP8, H.264, H.263, AMR, OPUS, Speex, G.711 and more.

There is a :term:`Kurento Client` library available in
`Java <http://www.java.com/>`__ and
`Javascript <http://www.w3.org/standards/webdesign/script>`__ languages to
control Kurento Media Server from applications. If you prefer another programming
language, a custom Kurento Client library can be written by following the
specification of the :term:`Kurento Protocol`, based on :term:`WebSocket`
and :term:`JSON-RPC`.

Kurento is open source, released under the terms of
`Apache License, Version 2.0 <http://www.apache.org/licenses/LICENSE-2.0>`__.
Its source code is hosted on `GitHub <https://github.com/Kurento>`__.

For a quick start guide, check the guide at
:doc:`installing the Kurento Media Server<installation_guide>` and take a look
at our :doc:`tutorials<tutorials>` which showcase some demo applications. You
can choose your favorite technology to build multimedia applications: **Java**,
**Browser JavaScript** or **Node.js**.

If you want to make the most of Kurento, please take a look to the
:doc:`advanced documentation<mastering_kurento>`.
