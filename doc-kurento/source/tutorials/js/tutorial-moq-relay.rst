%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
JavaScript - WebRTC and MoQ relay
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This browser tutorial demonstrates publishing a camera and microphone stream
from a ``WebRtcEndpoint`` to a MoQ relay and receiving the same broadcast back
through a ``MoqSubscriberEndpoint``. The browser communicates directly with
Kurento Media Server using the Kurento JavaScript Client; no application server
is needed.

For an overview of the endpoints and their requirements, see
:doc:`/features/moq_streaming`.



Requirements
============

* Install and start Kurento Media Server with the ``MoqPublisherEndpoint`` and
  ``MoqSubscriberEndpoint`` classes available.
* Install the ``moq-gst`` GStreamer plugin and the H.264/AAC encoders on the
  Kurento Media Server host.
* Have access to a MoQ relay that accepts publishing and subscriptions at the
  URL you configure in the tutorial.
* Use a WebRTC-capable browser and serve the tutorial over HTTPS.

.. warning::

   This browser tutorial still uses Bower, which is deprecated. Its
   dependencies have not been migrated for Kurento 7, so ``bower install`` may
   not be able to download everything needed to run the example.



Running the tutorial
====================

Install Bower and the static HTTPS server if they are not already available,
then install the tutorial's dependencies:

.. code-block:: shell

   npm install --global bower http-server
   cd tutorials/javascript-browser/moq-relay
   bower install

Start the HTTPS server using the example certificate and key:

.. code-block:: shell

   http-server -p 8443 -S \
     -C ../hello-world/keys/server.crt \
     -K ../hello-world/keys/server.key

Open ``https://localhost:8443/`` in a WebRTC-capable browser. Enter the Kurento
WebSocket URL, MoQ relay URL, and broadcast name. Start publishing to send the
local camera and microphone stream to the relay; start viewing to subscribe to
that broadcast through Kurento.

The supplied certificate is self-signed and will cause a browser warning. For
a remote Kurento Media Server, configure the correct WebSocket URL and ensure
the KMS WebSocket transport is reachable from the browser.



How it works
============

The publishing side creates a ``WebRtcEndpoint`` and a
``MoqPublisherEndpoint``, connects them, and starts publishing. On the
receiving side, the tutorial creates a ``MoqSubscriberEndpoint``, subscribes
to the same relay broadcast, and connects it to a ``WebRtcEndpoint`` for
playback in the browser. The tutorial uses independent pipelines for the
publisher and subscriber.

The complete source code is available in the
`Kurento repository <https://github.com/Kurento/kurento/tree/main/tutorials/javascript-browser/moq-relay>`__.
