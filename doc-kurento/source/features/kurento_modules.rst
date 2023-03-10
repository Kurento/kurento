===============
Kurento Modules
===============

.. contents:: Table of Contents

**Kurento Media Server** is controlled through the API it exposes, so application developers can use high level languages to interact with it. The Kurento project already provides SDK implementations of this API for several platforms: :doc:`/features/kurento_client`.

If you prefer a programming language different from the supported ones, you can implement your own Kurento Client by using the :doc:`/features/kurento_protocol`, which is based on :term:`WebSocket` and :term:`JSON-RPC`.

In the following sections we will describe the Kurento API from a high-level point of view, showing the media capabilities exposed by Kurento Media Server to clients. If you want to see working demos using Kurento, please refer to the :doc:`Tutorials section </user/tutorials>`.



Media Elements and Media Pipelines
==================================

Kurento is based on two concepts that act as building blocks for application developers:

* **Media Elements**. A Media Element is a functional unit performing a specific action on a media stream. Media Elements are a way of every capability is represented as a self-contained "black box" (the Media Element) to the application developer, who does not need to understand the low-level details of the element for using it. Media Elements are capable of *receiving* media from other elements (through media sources) and of *sending* media to other elements (through media sinks). Depending on their function, Media Elements can be split into different groups:

  - **Input Endpoints**: Media Elements capable of receiving media and injecting it into a pipeline. There are several types of input endpoints. File input endpoints take the media from a file, Network input endpoints take the media from the network, and Capture input endpoints are capable of capturing the media stream directly from a camera or other kind of hardware resource.

  - **Filters**: Media Elements in charge of transforming or analyzing media. Hence there are filters for performing operations such as mixing, muxing, analyzing, augmenting, etc.

  - **Hubs**: Media Objects in charge of managing multiple media flows in a pipeline. A *Hub* contains a different *HubPort* for each one of the Media Elements that are connected. Depending on the Hub type, there are different ways to control the media. For example, there is a Hub called *Composite* that merges all input video streams in a unique output video stream, with all inputs arranged in a grid.

  - **Output Endpoints**: Media Elements capable of taking a media stream out of the Media Pipeline. Again, there are several types of output endpoints, specialized in files, network, screen, etc.

* **Media Pipeline**: A Media Pipeline is a chain of Media Elements, where the output stream generated by a source element is fed into one or more sink elements. Hence, the pipeline represents a "pipe" capable of performing a sequence of operations over a stream.

  .. figure:: /images/kurento-java-tutorial-2-magicmirror-pipeline.png
     :align:  center
     :alt:    Media Pipeline example

     *Example of a Media Pipeline implementing an interactive multimedia application receiving media from a WebRtcEndpoint, overlaying an image on the detected faces and sending back the resulting stream*

The Kurento API is :wikipedia:`Object-Oriented <Object-oriented programming>`. This means that it is based on Classes that can be instantiated in the form of Objects; these Objects provide *properties* that are a representation of the internal state of the Kurento server, and *methods* that expose the operations that can be performed by the server.

The following class diagram shows part of the main classes in the Kurento API:

.. graphviz:: /images/graphs/mediaobjects.dot
   :align: center
   :caption: Class diagram of main classes in Kurento API



Endpoints
=========

**WebRtcEndpoint**: Input/output endpoint that provides media streaming for Real Time Communications (RTC) through the web. It implements :term:`WebRTC` technology to communicate with browsers.

.. image:: /images/toolbox/WebRtcEndpoint.png
   :align:  center

**RtpEndpoint**: Input/output endpoint that provides bidirectional content delivery capabilities with remote networked peers, through the :term:`RTP` protocol. It uses :term:`SDP` for media negotiation.

.. image:: /images/toolbox/RtpEndpoint.png
   :align:  center

**HttpPostEndpoint**: Input endpoint that accepts media using HTTP POST requests like HTTP file upload function.

.. image:: /images/toolbox/HttpPostEndpoint.png
   :align:  center

**PlayerEndpoint**: Input endpoint that retrieves content from file system, HTTP URL or RTSP URL and injects it into the Media Pipeline.

.. image:: /images/toolbox/PlayerEndpoint.png
   :align:  center

**RecorderEndpoint**: Output endpoint that provides function to store contents in reliable mode (doesn't discard data). It contains *Media Sink* pads for audio and video.

.. image:: /images/toolbox/RecorderEndpoint.png
   :align:  center

The following class diagram shows the main endpoint classes:

.. graphviz:: /images/graphs/endpoints.dot
   :align: center
   :caption: Class diagram of Kurento Endpoints. In blue, the classes that a final API client will actually use.



Filters
=======

Filters are MediaElements that perform media processing, Computer Vision, Augmented Reality, and so on.

**ZBarFilter**: Detects QR and bar codes in a video stream. When a code is found, the filter raises a *CodeFoundEvent*. Clients can add a listener to this event to execute some action.

.. image:: /images/toolbox/ZBarFilter.png
   :align:  center

**FaceOverlayFilter**: Detects faces in a video stream and overlays them with a configurable image.

.. image:: /images/toolbox/FaceOverlayFilter.png
   :align:  center

**GStreamerFilter**: Generic filter interface that allows injecting any GStreamer element into a Kurento Media Pipeline. Note however that the current implementation of GStreamerFilter only allows single elements to be injected; one cannot indicate more than one at the same time. Use several GStreamerFilters if you need to inject more than one element at the same time.

.. image:: /images/toolbox/GStreamerFilter.png
   :align:  center

Usage of some popular GStreamer elements requires installation of additional packages. For example, overlay elements such as *timeoverlay* or *textoverlay* require installation of the **gstreamer1.0-x** package, which will also install the *Pango* rendering library.

The following class diagram shows the main filter classes:

.. graphviz:: /images/graphs/filters.dot
   :align: center
   :caption: Class diagram of Kurento Filters. In blue, the classes that a final API client will actually use.



Hubs
====

Hubs are media objects in charge of managing multiple media flows in a pipeline. A Hub has several hub ports where other Media Elements are connected.

**Composite**: Mixes the audio stream of its connected inputs and constructs a grid with the video streams of them.

.. image:: /images/toolbox/Composite.png
   :align:  center

**DispatcherOneToMany**: Sends a given input to all the connected output HubPorts.

.. image:: /images/toolbox/DispatcherOneToMany.png
   :align:  center

**Dispatcher**: Routes between arbitrary input-output HubPort pairs.

.. image:: /images/toolbox/Dispatcher.png
   :align:  center

The following class diagram shows the Hub classes:

.. graphviz:: /images/graphs/hubs.dot
   :align: center
   :caption: Class diagram of Kurento Hubs. In blue, the classes that a final API client will actually use.



Example Modules
===============

In addition to the base features, there are some additional example modules provided **for demonstration purposes**:

.. figure:: ../images/kurento-modules.png
   :align:  center
   :alt:    Kurento modules architecture

   **Kurento modules architecture**
   *Kurento Media Server can be extended with example modules (chroma, crowddetector, platedetector, pointerdetector) and also with other custom modules.*

These example modules are provided to show how to extend the base features of Kurento Media Server:

* **Chroma**: Takes a color range from the top-left area of the video, and makes it transparent, revealing another background image.
* **CrowdDetector**: Detects groups of people in video streams.
* **PlateDetector**: Detects vehicle license plates in video streams.
* **PointerDetector**: Detects pointers in video streams, based on color tracking.

.. warning::

   These example modules **are just prototypes** and their results are not necessarily accurate or reliable. You can use them as programming guideline, but we strongly discourage anyone from using them in production environments.

All example modules come already preinstalled in the Kurento Docker images. For local installations, they can be installed separately with *apt-get*.

Taking into account these extra modules, the complete Kurento toolbox is extended as follows:

.. figure:: ../images/kurento-toolbox-extra.png
   :align: center
   :alt: Extended Kurento Toolbox

   **Extended Kurento Toolbox**
   *The basic Kurento toolbox (left side of the picture) is extended with more Computer Vision and Augmented Reality filters (right side of the picture) provided by the example modules.*

If you want to write your own modules, please read the section about :doc:`Writing Kurento Modules </user/writing_modules>`.
