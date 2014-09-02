.. _What_is_Kurento:

.. image:: images/kurento-rect-logo3.png
   :alt:    Creating client applications through Kurento APIs
   :align:  center
   :scale: 50 %

%%%%%%%%%%%%%%%
What's Kurento?
%%%%%%%%%%%%%%%

**Kurento** is a development framework aimed to create rich multimedia
applications in Internet with real time audio and video.

The core element is **Kurento Server**, responsible for media transmission,
processing, loading and recording. It is implemented in low level technologies
based on :term:`GStreamer` to optimize the resource consumption. It provides
the following features:

-  Networked streaming protocols, including :term:`HTTP` (working as client
   and server), :term:`RTP` and :term:`WebRTC`.
-  Generic support for computational vision and augmented reality filters.
-  Media storage supporting writing operations for :term:`WebM` and
   :term:`MP4` and reading operations for any of *Gstreamer's* muxers.
-  Automatic media transcodification between any of the codecs supported by
   Gstreamer.

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

If you want to want to make the most of Kurento, please take a look to the
:doc:`advanced documentation<mastering_kurento>`. There you find information
about the :doc:`Kurento Archicture<mastering/kurento_architecture>`,
:doc:`Kurento Media Elements<mastering/kurento_media_elements>`,
:doc:`Kurento Protocol<mastering/kurento_protocol>`,
:doc:`Kurento Server Advanced Installation Guide<mastering/advanced_installation_guide>`,
:doc:`Kurento API<mastering/kurento_API>`,
:doc:`how to develop Kurento Modules<mastering/kurento_server>`, and
:doc:`how to get nightly Kurento nersions<mastering/kurento_development>`.

Finally, if you would like to contribute to Kurento take a look to the
:doc:`repository structure<contribute/repository>` and also
:doc:`how to build Kurento Media Server<contribute/building_kms>`.
