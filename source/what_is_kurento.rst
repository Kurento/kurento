.. _What_is_Kurento:

.. image:: images/kurento-rect-logo3.png
   :alt:    Creating client applications through Kurento APIs
   :align:  center
   :scale: 50 %

%%%%%%%%%%%%%%%
What's Kurento?
%%%%%%%%%%%%%%%

**Kurento** is a WebRTC media server and a set of client APIs making simple the
development of advanced video applications for WWW and smartphone platforms.
Kurento features include group communications, transcoding, recording, mixing,
broadcasting and routing of audiovisual flows.

Kurento also provides advanced media processing capabilities involving computer
vision, video indexing, augmented reality and speech analysis. Kurento modular
architecture makes simple the integration of third party media processing
algorithms (i.e. speech recognition, sentiment analysis, face recognition,
etc.), which can be transparently used by application developers as the rest of
Kurento built-in features.

Kurento's core element is **Kurento Media Server**, responsible for media
transmission, processing, loading and recording. It is implemented in low level
technologies based on :term:`GStreamer` to optimize the resource consumption.
It provides the following features:

-  Networked streaming protocols, including :term:`HTTP` (working as client
   and server), :term:`RTP` and :term:`WebRTC`.
-  Group communications (MCUs and SFUs functionality) supporting both media
   mixing and media routing/dispatching.
-  Generic support for computational vision and augmented reality filters.
-  Media storage supporting writing operations for :term:`WebM` and
   :term:`MP4` and playing in all formats supported by *GStreamer*.
-  Automatic media transcodification between any of the codecs supported by
   GStreamer including VP8, H.264, H.263, AMR, OPUS, Speex, G.711, etc.

There are available :term:`Kurento Client` libraries in
`Java <http://www.java.com/>`__ and
`Javascript <http://www.w3.org/standards/webdesign/script>`__ to control
Kurento Server from applications. If you prefer another programming language,
you can use the :term:`Kurento Protocol`, based on :term:`WebSocket` and
:term:`JSON-RPC`.

Kurento is open source, released under the terms of
`LGPL version 2.1 <http://www.gnu.org/licenses/lgpl-2.1.html>`__ license. Its
source code is hosted on `GitHub <https://github.com/Kurento>`__.

If you want to put your hands on quickly, the best way is
:doc:`installing the Kurento Server<installation_guide>` and take a look to our
:doc:`tutorials<tutorials>` in form of working demo applications. You can
choose your favorite technology to build multimedia applications: **Java**,
**Browser JavaScript** or **Node.js**.

If you want to make the most of Kurento, please take a look to the
:doc:`advanced documentation<mastering_kurento>`.
