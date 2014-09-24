%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
JavaScript Tutorial 2 - Magic Mirror
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application extends :doc:`Tutorial 1 <./tutorial-1-helloworld>` adding
media processing to the basic `WebRTC`:term: loopback.

For the impatient: running this example
=======================================

You need to have installed the Kurento Media Server before running this example
read the :doc:`installation guide <../../installation_guide>` for further
information.

Be sure to have installed `Node.js`:term: and `Bower`:term: in your system. In
an Ubuntu machine, you can install both as follows:

.. sourcecode:: sh

   sudo add-apt-repository ppa:chris-lea/node.js
   sudo apt-get update
   sudo apt-get install nodejs
   sudo npm install -g bower

Due to `Same-origin policy`:term:, this demo has to be served by an HTTP server.
A very simple way of doing this is by means of a HTTP Node.js server which can
be installed using `npm`:term: :

.. sourcecode:: sh

   sudo npm install http-server -g

You also need the source code of this demo. You can clone it from GitHub. Then
start the HTTP server:

.. sourcecode:: sh

    git clone https://github.com/Kurento/kurento-tutorial-js.git
    cd kurento-tutorial-js/kurento-magic-mirror
    bower install
    http-server

Finally access the application connecting to the URL http://localhost:8080/
through a WebRTC capable browser (Chrome, Firefox).

Understanding this example
==========================

This application uses computer vision and augmented reality techniques to add a
funny hat on top of faces. The following picture shows a screenshot of the demo
running in a web browser:

.. figure:: ../../images/kurento-java-tutorial-2-magicmirror-screenshot.png
   :align:   center
   :alt:     Kurento Magic Mirror Screenshot: WebRTC with filter in loopback
   :width: 600px

   *Kurento Magic Mirror Screenshot: WebRTC with filter in loopback*

The interface of the application (an HTML web page) is composed by two HTML5
video tags: one for the video camera stream (the local client-side stream) and
other for the mirror (the remote stream). The video camera stream is sent to
the Kurento Media Server, processed and then is returned to the client as a
remote stream.

To implement this, we need to create a `Media Pipeline`:term: composed by the
following `Media Element`:term: s:

- **WebRtcEndpoint**: Provides full-duplex (bidirectional) `WebRTC`:term:
  capabilities.

- **FaceOverlay filter**: Computer vision filter that detects faces in the
  video stream and puts an image on top of them. In this demo the filter is
  configured to put a
  `Super Mario hat <http://files.kurento.org/imgs/mario-wings.png>`_).

The media pipeline implemented is illustrated in the following picture:

.. figure:: ../../images/kurento-java-tutorial-2-magicmirror-pipeline.png
   :align:   center
   :alt:     WebRTC with filter in loopback Media Pipeline
   :width: 400px

   *WebRTC with filter in loopback Media Pipeline*

The complete source code of this demo can be found in
`GitHub <https://github.com/Kurento/kurento-tutorial-js/tree/master/kurento-magic-mirror>`_.

JavaScript Logic
================

This demo follows a *Single Page Application* architecture (`SPA`:term:). The
interface is the following HTML page:
`index.html <https://github.com/Kurento/kurento-tutorial-js/blob/master/kurento-magic-mirror/index.html>`_.
This web page links two Kurento JavaScript libraries:

* **kurento-client.js** : Implementation of the Kurento JavaScript Client.

* **kurento-utils.js** : Kurento utily library aimed to simplify the WebRTC
  management in the browser.

The specific logic of this demo is coded in the following JavaScript page:
`index.js <https://github.com/Kurento/kurento-tutorial-js/blob/master/kurento-magic-mirror/js/index.js>`_.
In this file, there is an ``start`` function which is called when the green
button labeled as *Start* in the GUI is clicked.

.. sourcecode:: js

   function start() {
      showSpinner(videoInput, videoOutput);
      webRtcPeer = kurentoUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput, onOffer, onError);
   }

As you can see, the function *WebRtcPeer.startSendRecv* of *kurento-utils* is
used to start a WebRTC communication, using the HTML video tag with id
*videoInput* to show the video camera (local stream) and the video tag
*videoOutput* to show the video processed by Kurento Media Server (remote
stream). Then, two callback functions are used:

* ``onOffer`` : Callback executed if the SDP negotiation is carried out
  correctly.

* ``onError`` : Callback executed if something wrong happens.

In ``onOffer`` we can found the most interesting code from a Kurento JavaScript
Client point of view. First, we have create an instance of the *KurentoClient*
class that will manage the connection with the Kurento Media Server. So, we
need to provide the URI of its WebSocket endpoint:

.. sourcecode:: js

   const ws_uri = 'ws://' + location.hostname + ':8888/kurento';

   kurentoClient(ws_uri, function(error, kurentoClient) {
     ...
   };

Once we have an instance of ``kurentoClient``, the following step is to create a
*Media Pipeline*, as follows:

.. sourcecode:: js

   kurentoClient.create("MediaPipeline", function(error, pipeline) {
      ...
   });

If everything works correctly, we have an instance of a media pipeline (variable
``pipeline`` in this example). With this instance, we are able to create
*Media Elements*. In this example we just need a *WebRtcEndpoint* and a
*FaceOverlayFilter*. Then, these media elements are interconnected:

.. sourcecode:: js

   pipeline.create('WebRtcEndpoint', function(error, webRtc) {
      if (error) return onError(error);

      pipeline.create('FaceOverlayFilter', function(error, filter) {
         if (error) return onError(error);

         var offsetXPercent = -0.4;
         var offsetYPercent = -1;
         var widthPercent = 1.5;
         var heightPercent = 1.5;
         filter.setOverlayedImage(hat_uri, offsetXPercent,
            offsetYPercent, widthPercent,
            heightPercent, function(error) {
               if (error) return onError(error);
            });

         webRtc.connect(filter, function(error) {
            if (error) return onError(error);

            filter.connect(webRtc, function(error) {
               if (error) return onError(error);
            });
         });

         ...

      });
   });

In WebRTC, `SDP`:term: (Session Description protocol) is used for negotiating
media interchange between apps. Such negotiation happens based on the SDP offer
and answer exchange mechanism. This negotiation is implemented in the second
part of the method *processSdpAnswer*, using the SDP offer obtained from the
browser client (using *kurentoUtils.WebRtcPeer*), and returning a SDP answer
returned by *WebRtcEndpoint*.

.. sourcecode:: js

   webRtc.processOffer(sdpOffer, function(error, sdpAnswer) {
      if (error) return onError(error);

      webRtcPeer.processSdpAnswer(sdpAnswer);
   });

Dependencies
============

The dependencies of this demo has to be obtained using `Bower`:term:. The
definition of these dependencies are defined in the
`bower.json <https://github.com/Kurento/kurento-tutorial-js/blob/master/kurento-magic-mirror/bower.json>`_
file, as follows:

.. sourcecode:: js

   "dependencies": {
      "kurento-client": "^5.0.0",
      "kurento-utils": "^5.0.0"
   }

.. note::

   We are in active development. Be sure that you have the latest version of
   Kurento Java Client in your bower.json. You can find it at
   `Bower <http://bower.io/search/?q=kurento-client>`_ searching for
   ``kurento-client``.
