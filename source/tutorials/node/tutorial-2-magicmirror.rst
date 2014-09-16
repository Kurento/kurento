%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Node.js Tutorial 2 - WebRTC magic mirror
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application extends :doc:`Tutorial 1 <./tutorial-1-helloworld>` adding
media processing to the basic `WebRTC`:term: loopback.

For the impatient: running this example
=======================================

First of all, you should install Kurento Media Server to run this demo. Please
visit the `installation guide <../../Installation_Guide.rst>`_ for further
information.

Be sure to have installed `Node.js`:term: and `Bower`:term: in your system. In
an Ubuntu machine, you can install both as follows:

.. sourcecode:: sh

   sudo add-apt-repository ppa:chris-lea/node.js
   sudo apt-get update
   sudo apt-get install nodejs
   sudo npm install -g bower

To launch the application you need to clone the GitHub project where this demo
is hosted and then install and run it, as follows:

.. sourcecode:: shell

    git clone https://github.com/Kurento/kurento-tutorial-node.git
    cd kurento-magic-mirror
    npm install
    cd static
    bower install
    cd ..
    node app.js

Access the application connecting to the URL http://localhost:8080/ through a
WebRTC capable browser (Chrome, Firefox).

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
Kurento Media Server, which processes and sends it back to the client as a
remote stream. To implement this, we need to create a `Media Pipeline`:term:
composed by the following `Media Element`:term: s:

- **WebRtcEndpoint**: Provides full-duplex (bidirectional) `WebRTC`:term:
  capabilities.

- **FaceOverlay filter**: Computer vision filter that detects faces in the
  video stream and puts an image on top of them. In this demo the filter is
  configured to put a
  `Super Mario hat <http://files.kurento.org/imgs/mario-wings.png>`_).

.. figure:: ../../images/kurento-java-tutorial-2-magicmirror-pipeline.png
   :align:   center
   :alt:     WebRTC with filter in loopback Media Pipeline
   :width: 400px

   *WebRTC with filter in loopback Media Pipeline*

This is a web application, and therefore it follows a client-server
architecture. At the client-side, the logic is implemented in **JavaScript**.
At the server-side we use a Node.js application server consuming the
**Kurento JavaScript Client** API to control **Kurento Media Server**
capabilities. All in all, the high level architecture of this demo is
three-tier. To communicate these entities, two WebSockets are used. First, a
WebSocket is created between client and application server to implement a
custom signaling protocol. Second, another WebSocket is used to perform the
communication between the Kurento JavaScript Client and the Kurento Media
Server. This communication takes place using the **Kurento Protocol**. For
further information on it, please see this
:doc:`page <../../mastering/kurento_protocol>` of the documentation.

To communicate the client with the Node.js application server we have designed a
simple signaling protocol based on `JSON`:term: messages over `WebSocket`:term:
's. The normal sequence between client and server is as follows: i) Client
starts the Magic Mirror. ii) Client stops the Magic Mirror.

If any exception happens, server sends an error message to the client. The
detailed message sequence between client and application server is depicted in
the following picture:

.. figure:: ../../images/kurento-java-tutorial-2-magicmirror-signaling.png
   :align:   center
   :alt:     One to one video call signaling protocol
   :width: 600px

   *One to one video call signaling protocol*

As you can see in the diagram, an `SDP`:term: needs to be exchanged between
client and server to establish the `WebRTC`:term: session between the browser
and Kurento. Specifically, the SDP negotiation connects the WebRtcPeer at the
browser with the WebRtcEndpoint at the server. The complete source code of this
demo can be found in
`GitHub <https://github.com/Kurento/kurento-tutorial-node/tree/master/kurento-magic-mirror>`_.

Application Server Side
=======================

This demo has been developed using the **express** framework for Node.js, but
express is not a requirement for Kurento.

The main script of this demo is
`app.js <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-magic-mirror/app.js>`_.

Once the *Kurento Client* has been instantiated, you are ready for communicating
with Kurento Media Server and controlling its multimedia capabilities.

.. sourcecode:: js

   var kurento = require('kurento-client');

   //...

   const ws_uri = "ws://localhost:8888/kurento";

   //...

   kurento(ws_uri, function(error, _kurentoClient) {
      if (error) {
         console.log("Could not find media server at address " + ws_uri);
         return callback("Could not find media server at address" + ws_uri
            + ". Exiting with error " + error);
      }

      kurentoClient = _kurentoClient;
      callback(null, kurentoClient);
   });


This web application follows *Single Page Application* architecture
(`SPA`:term:) and uses a `WebSocket`:term: in the path ``/magicmirror`` to
communicate client with application server by means of requests and responses.

The following code snippet implements the server part of the signaling protocol
depicted in the previous sequence diagram.

.. sourcecode:: js

   ws.on('message', function(_message) {
      var message = JSON.parse(_message); switch (message.id) {

      case 'start':
         start(sessionId, message.sdpOffer, function(error, sdpAnswer) {
            if (error) {
               return ws.send(JSON.stringify({
                  id : 'error', message : error
               }));
            }
            ws.send(JSON.stringify({
               id : 'startResponse', sdpAnswer : sdpAnswer
            }));
         });
         break;

      case 'stop':
         stop(sessionId); break;

      //...
   });

In the designed protocol there are three different kinds of incoming messages to
the *Server* : ``start`` and ``stop``. These messages are treated in the
*switch* clause, taking the proper steps in each case.

In the following snippet, we can see the ``start`` method. It creates a Media
Pipeline, then creates the Media Elements (``WebRtcEndpoint`` and
``FaceOverlayFilter``) and make the connections among them. A ``startResponse``
message is sent back to the client with the SDP answer.

.. sourcecode:: js

   function start(sessionId, sdpOffer, callback) {
     getKurentoClient(function(error, kurentoClient) {
       kurentoClient.create('MediaPipeline', function(error, pipeline) {
         createMediaElements(pipeline, function(error, webRtcEndpoint, faceOverlayFilter) {
           connectMediaElements(webRtcEndpoint, faceOverlayFilter, function(error) {
             webRtcEndpoint.processOffer(sdpOffer, function(error, sdpAnswer) {
               pipelines[sessionId] = pipeline; return callback(null, sdpAnswer);
             });
           });
         });
       });
     });
   }

   function createMediaElements(pipeline, callback) {
     pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint) {
       pipeline.create('FaceOverlayFilter', function(error, faceOverlayFilter) {
         faceOverlayFilter.setOverlayedImage(
             "http://files.kurento.org/imgs/mario-wings.png",
             -0.35, -1.2, 1.6, 1.6, function(error) {
           return callback(null, webRtcEndpoint, faceOverlayFilter);
         });
       });
     });
   }

   function connectMediaElements(webRtcEndpoint, faceOverlayFilter, callback) {
     webRtcEndpoint.connect(faceOverlayFilter, function(error) {
       faceOverlayFilter.connect(webRtcEndpoint, function(error) {
         return callback(null);
       });
     });
   }

Client-Side
===========

Let's move now to the client-side of the application. To call the previously
created WebSocket service in the server-side, we use the JavaScript class
``WebSocket``. We use an specific Kurento JavaScript library called
**kurento-utils.js** to simplify the WebRTC interaction with the server. These
libraries are linked in the
`index.html <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-magic-mirror/static/index.html>`_
web page, and are used in the
`index.js <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-magic-mirror/static/js/index.js>`_.
In the following snippet we can see the creation of the WebSocket (variable
``ws``) in the path ``/magicmirror``. Then, the ``onmessage`` listener of the
WebSocket is used to implement the JSON signaling protocol in the client-side.
Notice that there are four incoming messages to client: ``startResponse`` and
``error``. Convenient actions are taken to implement each step in the
communication. For example, in functions ``start`` the function
``WebRtcPeer.startSendRecv`` of *kurento-utils.js* is used to start a WebRTC
communication.

.. sourcecode:: javascript

   var ws = new WebSocket('ws://' + location.host + '/magicmirror');
   
   ws.onmessage = function(message) {
      var parsedMessage = JSON.parse(message.data);
      console.info('Received message: ' + message.data);
   
      switch (parsedMessage.id) {
      case 'startResponse':
         startResponse(parsedMessage);
         break;
      case 'error':
         if (state == I_AM_STARTING) {
            setState(I_CAN_START);
         }
         console.error("Error message from server: " + parsedMessage.message);
         break;
      default:
         if (state == I_AM_STARTING) {
            setState(I_CAN_START);
         }
         console.error('Unrecognized message', parsedMessage);
      }
   }

   function start() {
      console.log("Starting video call ...")
      // Disable start button
      setState(I_AM_STARTING);
      showSpinner(videoInput, videoOutput);
   
      console.log("Creating WebRtcPeer and generating local sdp offer ...");
      webRtcPeer = kurentoUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput, onOffer, onError);
   }

   function onOffer(offerSdp) {
      console.info('Invoking SDP offer callback function ' + location.host);
      var message = {
         id : 'start',
         sdpOffer : offerSdp
      }
      sendMessage(message);
   }

   function onError(error) {
      console.error(error);
   }

Dependencies
============

Dependencies of this demo are managed using npm. Our main dependency is the
Kurento Client JavaScript (*kurento-client*). The relevant part of the
`package.json <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-magic-mirror/package.json>`_
file for managing this dependency is:

.. sourcecode:: json

   "dependencies": {
     ...
     "kurento-client" : "|version|"
   }

At the client side, dependencies are managed using Bower. Take a look to the
`bower.json <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-magic-mirror/static/js/bower.js>`_
file and pay attention to the following section:

.. sourcecode:: json

   "dependencies": {
     "kurento-utils" : "|version|"
   }

.. note::

   We are in active development. Be sure that you have the latest version of
   Kurento Java Client in your bower.json. You can find it at `Bower <http://bower.io/search/?q=kurento-client>`_
   searching for ``kurento-client``.
