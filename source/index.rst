.. _What_is_Kurento:

.. image:: images/kurento-rect-logo3.png
   :alt:    Creating client applications through Kurento APIs
   :align:  center
   :scale: 50 %

%%%%%%%%%%%%%%%
What's Kurento?
%%%%%%%%%%%%%%%

.. highlight:: java

**Kurento** is a development framework to develop rich multimedia applications
in Internet with real time audio and video.

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

There are available :doc:`Kurento Client <Kurento_Clients>` libraries in
`Java <http://www.java.com/>`__ and
`Javascript <http://www.w3.org/standards/webdesign/script>`__ to control
Kurento Server from applications. If you prefer another programming language,
you can use the :doc:`Kurento Protocol<Kurento_Protocol>`, based on
:term:`WebSocket` and :term:`Json-RPC`.

Kurento is released under
`LGPL version 2.1 <http://www.gnu.org/licenses/lgpl-2.1.html>`__ license and
all its source code is hosted on `GitHub <https://github.com/Kurento>`__.

Let's a try
===========

If you want to put your hands on quickly, the best way is
:doc:`install Kurento Server<Installation_Guide>` and take a look to our
:doc:`tutorials<Tutorials>` in form of working demo applications. You can
choose your favorite technology to build multimedia applications: **Java**,
**Node.js** or **Browser JavaScript**.

.. toctree::
   :maxdepth: 2

   Installation_Guide.rst
   Tutorials.rst
 
If you prefer a more complete guide about all Kurento programming details, you
can take a look to the following sections:

.. toctree::
   :maxdepth: 1

   Kurento_API.rst
   Kurento_Clients.rst
   Developing Java apps with Kurento
   Developing Node.js apps with Kurento
   Developing Browser JS apps with Kurento

Advanced documentation
======================

.. toctree::
   :maxdepth: 2

   Kurento_Protocol.rst
   Architecture.rst
   Basic_media_concepts.rst
   Kurento Server.rst
   Advanced_Installation_Guide.rst
   Kurento_Development.rst
   glossary.rst
   faq.rst

Indices and tables
==================

* :ref:`genindex`
