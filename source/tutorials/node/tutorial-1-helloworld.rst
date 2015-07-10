%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Node.js Tutorial 1 - Hello world
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application has been designed to introduce the principles of
programming with Kurento for Node.js developers. It consists on a
`WebRTC`:term: video communication in mirror (*loopback*). This tutorial
assumes you have basic knowledge on JavaScript, Node.js, HTML and WebRTC. We
also recommend reading the
:doc:`Introducing Kurento <../../introducing_kurento>` section before starting
this tutorial.

For the impatient: running this example
=======================================

You need to have installed the Kurento Media Server before running this example.
Read the :doc:`installation guide <../../installation_guide>` for further
information.

Be sure to have installed `Node.js`:term: and `Bower`:term: in your system. In
an Ubuntu machine, you can install both as follows:

.. sourcecode:: sh

   curl -sL https://deb.nodesource.com/setup | sudo bash -
   sudo apt-get install -y nodejs
   sudo npm install -g bower

To launch the application you need to clone the GitHub project where this demo
is hosted and then install and run it, as follows:

.. sourcecode:: sh

    git clone https://github.com/Kurento/kurento-tutorial-node.git
    cd kurento-tutorial-node/kurento-hello-world
    npm install
    npm start

If you have problems installing any of the dependencies, please remove them and
clean the npm cache, and try to install them again:

.. sourcecode:: sh

    rm -r node_modules
    npm cache clean

Access the application connecting to the URL http://localhost:8080/ through a
WebRTC capable browser (Chrome, Firefox).

.. note::

   These instructions work only if Kurento Media Server is up and running in the same machine
   than the tutorial. However, it is possible to locate the KMS in other machine simple adding
   the argument ``ws_uri`` to the npm execution command, as follows:

   .. sourcecode:: sh

      npm start -- --ws_uri=ws://kms_host:kms_host:kms_port/kurento

   In this case you need to use npm version 2. To update it you can use this command:

   .. sourcecode:: sh

      sudo npm install npm -g

Understanding this example
==========================

Kurento provides developers a **Kurento JavaScript Client** to control
**Kurento Media Server**. This client library can be used from compatible
JavaScript engines including browsers and Node.js.

This *hello world* demo is one of the simplest web application you can create
with Kurento. The following picture shows an screenshot of this demo running:

.. figure:: ../../images/kurento-java-tutorial-1-helloworld-screenshot.png
   :align:   center
   :alt:     Kurento Hello World Screenshot: WebRTC in loopback

   *Kurento Hello World Screenshot: WebRTC in loopback*

The interface of the application (an HTML web page) is composed by two HTML5
video tags: one showing the local stream (as captured by the device webcam) and
the other showing the remote stream sent by the media server back to the client.

The logic of the application is quite simple: the local stream is sent to the
Kurento Media Server, which returns it back to the client without
modifications. To implement this behavior we need to create a
`Media Pipeline`:term: composed by a single `Media Element`:term:, i.e. a
**WebRtcEndpoint**, which holds the capability of exchanging full-duplex
(bidirectional) WebRTC media flows. This media element is connected to itself
so that the media it receives (from browser) is send back (to browser). This
media pipeline is illustrated in the following picture:

.. figure:: ../../images/kurento-java-tutorial-1-helloworld-pipeline.png
   :align:   center
   :alt:     Kurento Hello World Media Pipeline in context

   *Kurento Hello World Media Pipeline in context*


This is a web application, and therefore it follows a client-server
architecture. At the client-side, the logic is implemented in **JavaScript**.
At the server-side we use a Java EE application server consuming the
**Kurento Java Client** API to control **Kurento Media Server** capabilities.
All in all, the high level architecture of this demo is three-tier. To
communicate these entities, two WebSockets are used. First, a WebSocket is
created between client and application server to implement a custom signaling
protocol. Second, another WebSocket is used to perform the communication
between the Kurento Java Client and the Kurento Media Server. This
communication takes place using the **Kurento Protocol**. For further
information on it, please see this
:doc:`page <../../mastering/kurento_protocol>` of the documentation.

The diagram below shows an complete sequence diagram from the interactions with
the application interface to: i) JavaScript logic; ii) Application server logic
(which uses the Kurento JavaScript Client); iii) Kurento Media Server.

.. figure:: ../../images/kurento-java-tutorial-1-helloworld-signaling.png
   :align:   center
   :alt:     Complete sequence diagram of Kurento Hello World (WebRTC in loopbak) demo

   *Complete sequence diagram of Kurento Hello World (WebRTC in loopbak) demo*

The following sections analyze in deep the server and client-side code of this
application. The complete source code can be found in
`GitHub <https://github.com/Kurento/kurento-tutorial-node/tree/master/kurento-hello-world>`_.

Application Server Logic
========================

This demo has been developed using the **express** framework for Node.js, but
express is not a requirement for Kurento. The main script of this demo is
`server.js <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-hello-world/server.js>`_.

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
       path : '/helloworld'
   });

   /*
    * Management of WebSocket messages
    */
   wss.on('connection', function(ws) {
       var sessionId = null;
       var request = ws.upgradeReq;
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
           as_uri: 'http://localhost:8080/',
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
we just need a single *WebRtcEndpoint* connected to itself (i.e. in loopback).
These functions are called in the ``start`` function, which is fired when the
``start`` message is received:

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

                   connectMediaElements(webRtcEndpoint, function(error) {
                       if (error) {
                           pipeline.release();
                           return callback(error);
                       }

                       webRtcEndpoint.on('OnIceCandidate', function(event) {
                           var candidate = kurento.register.complexTypes.IceCandidate(event.candidate);
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

    function connectMediaElements(webRtcEndpoint, callback) {
       webRtcEndpoint.connect(webRtcEndpoint, function(error) {
           if (error) {
               return callback(error);
           }
           return callback(null);
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
       var candidate = kurento.register.complexTypes.IceCandidate(_candidate);

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
``WebSocket``. We use an specific Kurento JavaScript library called
**kurento-utils.js** to simplify the WebRTC interaction with the server. This
library depends on **adapter.js**, which is a JavaScript WebRTC utility
maintained by Google that abstracts away browser differences. Finally
**jquery.js** is also needed in this application. These libraries are linked in
the
`index.html <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-hello-world/static/index.html>`_
web page, and are used in the
`index.js <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-hello-world/static/js/index.js>`_.
In the following snippet we can see the creation of the WebSocket (variable
``ws``) in the path ``/helloworld``. Then, the ``onmessage`` listener of the
WebSocket is used to implement the JSON signaling protocol in the client-side.
Notice that there are three incoming messages to client: ``startResponse``,
``error``, and ``iceCandidate``. Convenient actions are taken to implement each
step in the communication.

.. sourcecode:: javascript

   var ws = new WebSocket('ws://' + location.host + '/helloworld');
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
`package.json <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-hello-world/package.json>`_
file for managing this dependency is:

.. sourcecode:: js

   "dependencies": {
      [...]
      "kurento-client" : "|CLIENT_JS_VERSION|"
   }

At the client side, dependencies are managed using :term:`Bower`. Take a look to
the
`bower.json <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-hello-world/static/bower.json>`_
file and pay attention to the following section:

.. sourcecode:: js

   "dependencies": {
      [...]
      "kurento-utils" : "|UTILS_JS_VERSION|"
   }

.. note::

   We are in active development. You can find the latest version of
   Kurento JavaScript Client at `npm <http://npmsearch.com/?q=kurento-client>`_
   and `Bower <http://bower.io/search/?q=kurento-client>`_.
