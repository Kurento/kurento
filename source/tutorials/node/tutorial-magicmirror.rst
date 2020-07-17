%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Node.js - WebRTC magic mirror
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application extends the :doc:`Hello World Tutorial<./tutorial-helloworld>`, adding
media processing to the basic `WebRTC`:term: loopback.

.. note::

   Web browsers require using *HTTPS* to enable WebRTC, so the web server must use SSL and a certificate file. For instructions, check :ref:`features-security-node-https`.

   For convenience, this tutorial already provides dummy self-signed certificates (which will cause a security warning in the browser).

For the impatient: running this example
=======================================

First of all, you should install Kurento Media Server to run this demo. Please
visit the :doc:`installation guide </user/installation>` for further
information.

Be sure to have installed `Node.js`:term: and `Bower`:term: in your system. In
an Ubuntu machine, you can install both as follows:

.. sourcecode:: bash

   curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
   sudo apt-get install -y nodejs
   sudo npm install -g bower

To launch the application, you need to clone the GitHub project where this demo
is hosted, install it and run it:

.. sourcecode:: bash

    git clone https://github.com/Kurento/kurento-tutorial-node.git
    cd kurento-tutorial-node/kurento-magic-mirror
    git checkout |VERSION_TUTORIAL_NODE|
    npm install
    npm start

If you have problems installing any of the dependencies, please remove them and
clean the npm cache, and try to install them again:

.. sourcecode:: bash

    rm -r node_modules
    npm cache clean

Access the application connecting to the URL https://localhost:8443/ in a
WebRTC capable browser (Chrome, Firefox).

.. note::

   These instructions work only if Kurento Media Server is up and running in the same machine
   as the tutorial. However, it is possible to connect to a remote KMS in other machine, simply adding
   the argument ``ws_uri`` to the npm execution command, as follows:

   .. sourcecode:: bash

      npm start -- --ws_uri=ws://{KMS_HOST}:8888/kurento

   In this case you need to use npm version 2. To update it you can use this command:

   .. sourcecode:: bash

      sudo npm install npm -g

Understanding this example
==========================

This application uses computer vision and augmented reality techniques to add a
funny hat on top of faces. The following picture shows a screenshot of the demo
running in a web browser:

.. figure:: ../../images/kurento-java-tutorial-2-magicmirror-screenshot.png
   :align:   center
   :alt:     Kurento Magic Mirror Screenshot: WebRTC with filter in loopback

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
  `Super Mario hat <http://files.openvidu.io/img/mario-wings.png>`_).

.. figure:: ../../images/kurento-java-tutorial-2-magicmirror-pipeline.png
   :align:   center
   :alt:     WebRTC with filter in loopback Media Pipeline

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
:doc:`page </features/kurento_protocol>` of the documentation.

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

   *One to one video call signaling protocol*

As you can see in the diagram, an :term:`SDP` and :term:`ICE` candidates needs
to be exchanged between client and server to establish the :term:`WebRTC`
session between the Kurento client and server. Specifically, the SDP
negotiation connects the WebRtcPeer at the browser with the WebRtcEndpoint at
the server. The complete source code of this demo can be found in
`GitHub <https://github.com/Kurento/kurento-tutorial-node/tree/master/kurento-magic-mirror>`_.

Application Server Logic
========================

This demo has been developed using the **express** framework for Node.js, but
express is not a requirement for Kurento. The main script of this demo is
`server.js <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-magic-mirror/server.js>`_.

In order to communicate the JavaScript client and the Node application server a
WebSocket is used. The incoming messages to this WebSocket (variable ``ws`` in
the code) are conveniently handled to implemented the signaling protocol
depicted in the figure before (i.e. messages ``start``, ``stop``,
``onIceCandidate``).

.. sourcecode:: js

   var ws = require('ws');

   [...]

   var wss = new ws.Server({
       server : server,
       path : '/magicmirror'
   });

   /*
    * Management of WebSocket messages
    */
   wss.on('connection', function(ws, req) {
       var sessionId = null;
       var request = req;
       var response = {
           writeHead : {}
       };

       sessionHandler(request, response, function(err) {
           sessionId = request.session.id;
           console.log('Connection received with sessionId ' + sessionId);
       });

       ws.on('error', function(error) {
           console.log('Connection ' + sessionId + ' error');
           stop(sessionId);
       });

       ws.on('close', function() {
           console.log('Connection ' + sessionId + ' closed');
           stop(sessionId);
       });

       ws.on('message', function(_message) {
           var message = JSON.parse(_message);
           console.log('Connection ' + sessionId + ' received message ', message);

           switch (message.id) {
           case 'start':
               sessionId = request.session.id;
               start(sessionId, ws, message.sdpOffer, function(error, sdpAnswer) {
                   if (error) {
                       return ws.send(JSON.stringify({
                           id : 'error',
                           message : error
                       }));
                   }
                   ws.send(JSON.stringify({
                       id : 'startResponse',
                       sdpAnswer : sdpAnswer
                   }));
               });
               break;

           case 'stop':
               stop(sessionId);
               break;

           case 'onIceCandidate':
               onIceCandidate(sessionId, message.candidate);
               break;

           default:
               ws.send(JSON.stringify({
                   id : 'error',
                   message : 'Invalid message ' + message
               }));
               break;
           }

       });
   });

In order to control the media capabilities provided by the Kurento Media Server,
we need an instance of the *KurentoClient* in the Node application server. In
order to create this instance, we need to specify to the client library the
location of the Kurento Media Server. In this example, we assume it's located
at *localhost* listening in port 8888.

.. sourcecode:: js

   var kurento = require('kurento-client');

   var kurentoClient = null;

   var argv = minimist(process.argv.slice(2), {
       default: {
           as_uri: 'https://localhost:8443/',
           ws_uri: 'ws://localhost:8888/kurento'
       }
   });

   [...]

   function getKurentoClient(callback) {
       if (kurentoClient !== null) {
           return callback(null, kurentoClient);
       }

       kurento(argv.ws_uri, function(error, _kurentoClient) {
           if (error) {
               console.log("Could not find media server at address " + argv.ws_uri);
               return callback("Could not find media server at address" + argv.ws_uri
                       + ". Exiting with error " + error);
           }

           kurentoClient = _kurentoClient;
           callback(null, kurentoClient);
       });
   }

Once the *Kurento Client* has been instantiated, you are ready for communicating
with Kurento Media Server. Our first operation is to create a *Media Pipeline*,
then we need to create the *Media Elements* and connect them. In this example,
we need a *WebRtcEndpoint* connected to a *FaceOverlayFilter*, which is
connected to the sink of the same *WebRtcEndpoint*. These functions are called
in the ``start`` function, which is fired when the ``start`` message is
received:

.. sourcecode:: js

   function start(sessionId, ws, sdpOffer, callback) {
       if (!sessionId) {
           return callback('Cannot use undefined sessionId');
       }

       getKurentoClient(function(error, kurentoClient) {
           if (error) {
               return callback(error);
           }

           kurentoClient.create('MediaPipeline', function(error, pipeline) {
               if (error) {
                   return callback(error);
               }

               createMediaElements(pipeline, ws, function(error, webRtcEndpoint) {
                   if (error) {
                       pipeline.release();
                       return callback(error);
                   }

                   if (candidatesQueue[sessionId]) {
                       while(candidatesQueue[sessionId].length) {
                           var candidate = candidatesQueue[sessionId].shift();
                           webRtcEndpoint.addIceCandidate(candidate);
                       }
                   }

                   connectMediaElements(webRtcEndpoint, faceOverlayFilter, function(error) {
                       if (error) {
                           pipeline.release();
                           return callback(error);
                       }

                       webRtcEndpoint.on('OnIceCandidate', function(event) {
                           var candidate = kurento.getComplexType('IceCandidate')(event.candidate);
                           ws.send(JSON.stringify({
                               id : 'iceCandidate',
                               candidate : candidate
                           }));
                       });

                       webRtcEndpoint.processOffer(sdpOffer, function(error, sdpAnswer) {
                           if (error) {
                               pipeline.release();
                               return callback(error);
                           }

                           sessions[sessionId] = {
                               'pipeline' : pipeline,
                               'webRtcEndpoint' : webRtcEndpoint
                           }
                           return callback(null, sdpAnswer);
                       });

                       webRtcEndpoint.gatherCandidates(function(error) {
                           if (error) {
                               return callback(error);
                           }
                       });
                   });
               });
           });
       });
   }

   function createMediaElements(pipeline, ws, callback) {
       pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint) {
           if (error) {
               return callback(error);
           }

           return callback(null, webRtcEndpoint);
       });
   }

   function connectMediaElements(webRtcEndpoint, faceOverlayFilter, callback) {
       webRtcEndpoint.connect(faceOverlayFilter, function(error) {
           if (error) {
               return callback(error);
           }

           faceOverlayFilter.connect(webRtcEndpoint, function(error) {
               if (error) {
                   return callback(error);
               }

               return callback(null);
           });
       });
   }

As of Kurento Media Server 6.0, the WebRTC negotiation is done by exchanging
:term:`ICE` candidates between the WebRTC peers. To implement this protocol,
the ``webRtcEndpoint`` receives candidates from the client in
``OnIceCandidate`` function. These candidates are stored in a queue when the
``webRtcEndpoint`` is not available yet. Then these candidates are added to the
media element by calling to the ``addIceCandidate`` method.

.. sourcecode:: js

   var candidatesQueue = {};

   [...]

   function onIceCandidate(sessionId, _candidate) {
       var candidate = kurento.getComplexType('IceCandidate')(_candidate);

       if (sessions[sessionId]) {
           console.info('Sending candidate');
           var webRtcEndpoint = sessions[sessionId].webRtcEndpoint;
           webRtcEndpoint.addIceCandidate(candidate);
       }
       else {
           console.info('Queueing candidate');
           if (!candidatesQueue[sessionId]) {
               candidatesQueue[sessionId] = [];
           }
           candidatesQueue[sessionId].push(candidate);
       }
   }


Client-Side Logic
=================

Let's move now to the client-side of the application. To call the previously
created WebSocket service in the server-side, we use the JavaScript class
``WebSocket``. We use a specific Kurento JavaScript library called
**kurento-utils.js** to simplify the WebRTC interaction with the server. This
library depends on **adapter.js**, which is a JavaScript WebRTC utility
maintained by Google that abstracts away browser differences. Finally
**jquery.js** is also needed in this application. These libraries are linked in
the
`index.html <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-magic-mirror/static/index.html>`_
web page, and are used in the
`index.js <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-magic-mirror/static/js/index.js>`_.
In the following snippet we can see the creation of the WebSocket (variable
``ws``) in the path ``/magicmirror``. Then, the ``onmessage`` listener of the
WebSocket is used to implement the JSON signaling protocol in the client-side.
Notice that there are three incoming messages to client: ``startResponse``,
``error``, and ``iceCandidate``. Convenient actions are taken to implement each
step in the communication.

.. sourcecode:: javascript

   var ws = new WebSocket('ws://' + location.host + '/magicmirror');
   var webRtcPeer;

   const I_CAN_START = 0;
   const I_CAN_STOP = 1;
   const I_AM_STARTING = 2;

   [...]

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
         onError('Error message from server: ' + parsedMessage.message);
         break;
      case 'iceCandidate':
         webRtcPeer.addIceCandidate(parsedMessage.candidate)
         break;
      default:
         if (state == I_AM_STARTING) {
            setState(I_CAN_START);
         }
         onError('Unrecognized message', parsedMessage);
      }
   }

In the function ``start`` the method ``WebRtcPeer.WebRtcPeerSendrecv`` of
*kurento-utils.js* is used to create the ``webRtcPeer`` object, which is used
to handle the WebRTC communication.

.. sourcecode:: javascript

   videoInput = document.getElementById('videoInput');
   videoOutput = document.getElementById('videoOutput');

   [...]

   function start() {
      console.log('Starting video call ...')

      // Disable start button
      setState(I_AM_STARTING);
      showSpinner(videoInput, videoOutput);

      console.log('Creating WebRtcPeer and generating local sdp offer ...');

       var options = {
         localVideo: videoInput,
         remoteVideo: videoOutput,
         onicecandidate : onIceCandidate
       }

       webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options, function(error) {
           if(error) return onError(error);
           this.generateOffer(onOffer);
       });
   }

   function onIceCandidate(candidate) {
         console.log('Local candidate' + JSON.stringify(candidate));

         var message = {
            id : 'onIceCandidate',
            candidate : candidate
         };
         sendMessage(message);
   }

   function onOffer(error, offerSdp) {
      if(error) return onError(error);

      console.info('Invoking SDP offer callback function ' + location.host);
      var message = {
         id : 'start',
         sdpOffer : offerSdp
      }
      sendMessage(message);
   }


Dependencies
============

Server-side dependencies of this demo are managed using :term:`npm`. Our main
dependency is the Kurento Client JavaScript (*kurento-client*). The relevant
part of the
`package.json <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-magic-mirror/package.json>`_
file for managing this dependency is:

.. sourcecode:: js

   "dependencies": {
      [...]
      "kurento-client" : "|VERSION_CLIENT_JS|"
   }

At the client side, dependencies are managed using :term:`Bower`. Take a look to
the
`bower.json <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-magic-mirror/static/bower.json>`_
file and pay attention to the following section:

.. sourcecode:: js

   "dependencies": {
      [...]
      "kurento-utils" : "|VERSION_UTILS_JS|"
   }

.. note::

   We are in active development. You can find the latest version of
   Kurento JavaScript Client at `npm <https://npmsearch.com/?q=kurento-client>`_
   and `Bower <https://bower.io/search/?q=kurento-client>`_.
