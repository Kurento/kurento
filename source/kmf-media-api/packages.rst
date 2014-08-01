.. _kmf-media-api:

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Kurento Media API Documentation
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

Introduction
============

The :doc:`KMF Media API </kmf-media-api/packages>` (`javadoc <../javadoc/index.html?com/kurento/kmf/media/package-summary.html>`__)
is a Java API exposing a toolbox of Media Elements that
can be chained for creating complex multimedia processing pipelines.
The abstraction and modularity of the Media API makes possible for
non-expert developers to create complex and interoperable multimedia
applications.

The main java objects in the API are:

* :java:type:`MediaPipelines <MediaPipeline>` , which are chains of processing.
  A pipeline connects two :java:type:`Endpoints <Endpoint>`, the :java:type:`MediaSource`
  of one with the :java:type:`MediaSink` of other `Endpoint`.
* :java:type:`MediaElements <MediaElement>` are processing elements composing the pipeline.
  A `MediaElement`, depending of its role in the pipeline, can be a :java:type:`Filter` or
  an :java:type:`Endpoint`

    * A :java:type:`Filter` processes media injected through its :java:type:`MediaSink`,
      and delivers the outcome through its :java:type:`MediaSource`.
    * An :java:type:`Endpoint` is a `MediaElement` enabling KMS to interchange media contents with
      external systems, supporting different transport protocols and mechanisms (RTP, WebRTC, HTTP, FILE).
      An Endpoint may contain both sources and sinks for different media types, to provide bidirectional
      communication.

  :java:type:`MediaElements <MediaElement>` (to be more precise :java:type:`MediaObjects <MediaObject>`)
  have :java:type:`MediaPads <MediaPad>`. Pads can be :java:type:`MediaSources <MediaSource>` or
  :java:type:`MediaSinks <MediaSink>`.

    * A :java:type:`MediaSource` is a Pad that generates a media stream to process
    * A :java:type:`MediaSink` is a Pad that *consumes* (receives) a media stream after processing.

The different capabilities of the Kurento Media Server are exposed typically as `Filters` or `Endpoints`
in the Media API.

API Javadoc
===========

This is the standard `javadoc of the Kurento Media API  <../javadoc/index.html?com/kurento/kmf/media/package-summary.html>`__
classes.

API Classes
===========

.. toctree::
   :maxdepth: 2

   inheritance
   com/kurento/kmf/media/package-index
   com/kurento/kmf/media/events/package-index


API Jsdoc
=========

This is the standard `jsdoc of the JavaScript Kurento Media API  <../jsdoc/kws-media-api/index.html>`__.
