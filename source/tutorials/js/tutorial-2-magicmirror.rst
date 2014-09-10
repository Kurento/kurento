%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
JavaScript Tutorial 2 - Magic Mirror
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application extends Tutorial 1 adding media processing to a basic
`WebRTC`:term: loopback. This processing uses computer vision and augmented reality
techniques to add a funny hat on top of faces. The following picture shows a 
screenshot of the demo running in a web browser:

.. figure:: ../../images/kurento-js-tutorial-2-magicmirror-screenshot.png
   :align:   center
   :alt:     Loopback video call with filtering screenshot :width: 600px
   :width: 600px

The interface of the application (an HTML web page) is composed by two HTML5
video tags: one for the video camera stream (the local client-side stream) and
other for the mirror (the remote stream). The video camera stream is sent to
the Kurento Media Server, processed and then is returned to the client as a
remote stream.

To implement this, we need to create a `Media Pipeline`:term: composed
by the following `Media Element`:term: s:

- **WebRtcEndpoint**: Provides full-duplex (bidirectional) `WebRTC`:term:
capabilities.

- **FaceOverlay filter**: Computer vision filter that detects faces in the
  video stream and puts an image on top of them. In this demo 
  the filter is configured to put a
  `Super Mario hat <http://files.kurento.org/imgs/mario-wings.png>`_).

The media pipeline implemented is illustrated in the following picture:

.. figure:: ../../images/kurento-java-tutorial-2-magicmirror-pipeline.png
   :align:   center
   :alt:     Loopback video call with filtering media pipeline

The complete source code of this demo can be found in
`GitHub <https://github.com/Kurento/kurento-tutorial-js/tree/develop/kurento-magic-mirror>`_.

JavaScript Logic
================

This demo follows a *Single Page Application* architecture (`SPA`:term:). The
interface is the following HTML page:
`index.html <https://github.com/Kurento/kurento-tutorial-js/blob/develop/kurento-magic-mirror/index.html>`_.
This web page links two Kurento JavaScript libraries:

* **kurento-client.js** : Implementation of the Kurento JavaScript Client.

* **kurento-utils.js** : Kurento utily library aimed to simplify the WebRTC
  management in the browser.

The specific logic of this demo is coded in the following JavaScript page:
`index.js <https://github.com/Kurento/kurento-tutorial-js/blob/develop/kurento-magic-mirror/js/index.js>`_.
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
*videoOutput* to show the video processed by Kurento server (remote stream).
Then, two callback functions are used:

* ``onOffer`` : Callback executed if the SDP negotiation is carried out
  correctly.

* ``onError`` : Callback executed if something wrong happens.

In ``onOffer`` we can found the most interesting code from a Kurento JavaScript
Client point of view. First, we have create an instance of the *KurentoClient*
class that will manage the connection with the Kurento Server. So, we need to
provide the URI of its WebSocket endpoint:

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
`bower.json <https://github.com/Kurento/kurento-tutorial-js/blob/develop/kurento-magic-mirror/bower.json>`_
file, as follows:

.. sourcecode:: json

   "dependencies": {
      "kurento-client": "develop",
      "kurento-utils": "develop"
   }


How to run this application
===========================

To run this application, first you need to install Bower, and so you also need
to install `npm`:term:. The following snippet shows how to install npm (by
installing `Node.js`:term: package) and Bower in an Ubuntu machine:

.. sourcecode:: sh

   sudo add-apt-repository ppa:chris-lea/node.js
   sudo apt-get update
   sudo apt-get install nodejs
   sudo npm install -g bower

Once Bower is installed, you need to clone the GitHub project where this demo is
hosted. Then you have to resolve the dependencies using Bower, as follows:

.. sourcecode:: sh

    git clone https://github.com/Kurento/kurento-tutorial-js.git
    cd kurento-magic-mirror
    bower install

Due to `Same-origin policy`:term:, this demo has to be served by an HTTP server.
A very simple way of doing this is by means of a HTTP Node.js server which can
be installed using npm. Then, this HTTP has to be started in the folder where
the demo is located:

.. sourcecode:: sh

   sudo npm install http-server -g
   http-server

The web application starts on port 8080 in the localhost. Therefore, to run the
demo, open the URL http://localhost:8080/demo.html in a WebRTC compliant
browser (Chrome, Firefox).
