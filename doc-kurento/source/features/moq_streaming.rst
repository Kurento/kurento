.. _features-moq-streaming:

===================
MoQ Media Streaming
===================

Kurento can publish media to and receive media from a Media over QUIC (MoQ)
relay using the ``MoqPublisherEndpoint`` and ``MoqSubscriberEndpoint`` classes.
This allows a Kurento media pipeline to exchange live audio and video with
other MoQ-capable clients and services.

The publisher accepts media from the pipeline, encodes video as H.264 and
audio as AAC, and publishes a broadcast to a relay. The subscriber connects to
a relay broadcast, decodes its media, and makes it available to the pipeline.
Both endpoints use the ``moq-gst`` GStreamer plugin: ``moqsink`` for publishing
and ``moqsrc`` for subscribing.



Requirements
============

Install the ``moq-gst`` GStreamer plugin on the Kurento Media Server host and
make sure GStreamer can load it. The publisher also requires the GStreamer
H.264 and AAC encoders used by the endpoint. For example, on systems where the
plugin package is available, verify it with:

.. code-block:: shell

   gst-inspect-1.0 moq

The selected relay must support the URL and broadcast name supplied to the
endpoint. A publisher and its subscribers must use the same broadcast name.



Publishing a broadcast
======================

Create a ``MoqPublisherEndpoint`` in a media pipeline, connect a media source
to it, and call ``publish()``. The following JavaScript example uses the
Kurento Client callback API:

.. code-block:: javascript

   pipeline.create('MoqPublisherEndpoint', {
     url: 'https://relay.example.com',
     broadcast: 'my-broadcast.hang'
   }, function(error, publisher) {
     if (error) return onError(error);

     webRtcEndpoint.connect(publisher, function(error) {
       if (error) return onError(error);
       publisher.publish(onError);
     });
   });

Call ``unpublish()`` to stop the broadcast. The ``url`` and ``broadcast``
properties report the configuration used when the endpoint was created.



Receiving a broadcast
=====================

Create a ``MoqSubscriberEndpoint`` with the relay URL and broadcast name. Call
``subscribe()`` to start receiving, then connect the endpoint to a media sink:

.. code-block:: javascript

   pipeline.create('MoqSubscriberEndpoint', {
     url: 'https://relay.example.com',
     broadcast: 'my-broadcast.hang'
   }, function(error, subscriber) {
     if (error) return onError(error);

     subscriber.connect(webRtcEndpoint, function(error) {
       if (error) return onError(error);
       subscriber.subscribe(onError);
     });
   });

Call ``unsubscribe()`` to stop receiving. The subscriber emits an
``EndOfStream`` event when the broadcast ends.



Tutorial
========

The :doc:`browser MoQ relay tutorial </tutorials/js/tutorial-moq-relay>`
demonstrates a complete WebRTC-to-MoQ publishing flow and a MoQ-to-WebRTC
receiving flow.
