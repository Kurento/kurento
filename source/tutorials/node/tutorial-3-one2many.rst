%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Tutorial 3 - One to many video call
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application consists on an one to one video call using `WebRTC`:term:
technology. In other words, it is an implementation of a video broadcasting web
application.

For the impatient: running this example
=======================================

First of all, you should install Kurento Media Server to run this demo. Please
visit the :doc:`installation guide <../../installation_guide>` for further
information.

Be sure to have installed `Node.js`:term: in your system. In an Ubuntu machine,
you can install both as follows:

.. sourcecode:: sh

   curl -sL https://deb.nodesource.com/setup | sudo bash -
   sudo apt-get install -y nodejs

To launch the application you need to clone the GitHub project where this demo
is hosted and then install and run it, as follows:

.. sourcecode:: sh

    git clone https://github.com/Kurento/kurento-tutorial-node.git
    cd kurento-tutorial-node/kurento-one2many-call
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

There will be two types of users in this application: 1 peer sending media
(let's call it *Presenter*) and N peers receiving the media from the
*Presenter* (let's call them *Viewers*). Thus, the Media Pipeline is composed
by 1+N interconnected *WebRtcEndpoints*. The following picture shows an
screenshot of the Presenter's web GUI:

.. figure:: ../../images/kurento-java-tutorial-3-one2many-screenshot.png
   :align:   center
   :alt:     One to many video call screenshot

   *One to many video call screenshot*

To implement this behavior we have to create a `Media Pipeline`:term: composed
by 1+N **WebRtcEndpoints**. The *Presenter* peer sends its stream to the rest
of the *Viewers*. *Viewers* are configured in receive-only mode. The
implemented media pipeline is illustrated in the following picture:

.. figure:: ../../images/kurento-java-tutorial-3-one2many-pipeline.png
   :align:   center
   :alt:     One to many video call Media Pipeline

   *One to many video call Media Pipeline*

This is a web application, and therefore it follows a client-server
architecture. At the client-side, the logic is implemented in **JavaScript**.
At the server-side we use the **Kurento JavaScript Client** in order to reach
the **Kurento Media Server**. All in all, the high level architecture of this
demo is three-tier. To communicate these entities two WebSockets are used. The
first is created between the client browser and a Node.js application server to
transport signaling messages. The second is used to communicate the Kurento
JavaScript Client executing at Node.js and the Kurento Media Server. This
communication is implemented by the **Kurento Protocol**. For further
information, please see this :doc:`page <../../mastering/kurento_protocol>`.

Client and application server communicate using a signaling protocol based on
`JSON`:term: messages over `WebSocket`:term: 's. The normal sequence between
client and server is as follows:

1. A *Presenter* enters in the system. There must be one and only one
*Presenter* at any time. For that, if a *Presenter* has already present, an
error message is sent if another user tries to become *Presenter*.

2. N *Viewers* connect to the presenter. If no *Presenter* is present, then an
error is sent to the corresponding *Viewer*.

3. *Viewers* can leave the communication at any time.

4. When the *Presenter* finishes the session each connected *Viewer* receives an
*stopCommunication* message and also terminates its session.


We can draw the following sequence diagram with detailed messages between
clients and server:

.. figure:: ../../images/kurento-java-tutorial-3-one2many-signaling.png
   :align:   center
   :alt:     One to many video call signaling protocol

   *One to many video call signaling protocol*

As you can see in the diagram, `SDP`:term: and :term:`ICE` candidates need to be
exchanged between client and server to establish the `WebRTC`:term: connection
between the Kurento client and server. Specifically, the SDP negotiation
connects the WebRtcPeer in the browser with the WebRtcEndpoint in the server.
The complete source code of this demo can be found in
`GitHub <https://github.com/Kurento/kurento-tutorial-node/tree/master/kurento-one2many-call>`_.

Application Server Logic
========================

This demo has been developed using the **express** framework for Node.js, but
express is not a requirement for Kurento. The main script of this demo is
`server.js <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-one2many-call/server.js>`_.

In order to communicate the JavaScript client and the Node application server a
WebSocket is used. The incoming messages to this WebSocket (variable ``ws`` in
the code) are conveniently handled to implemented the signaling protocol
depicted in the figure before (i.e. messages ``presenter``, ``viewer``,
``stop``, and ``onIceCandidate``).

.. sourcecode:: js

   var ws = require('ws');

   [...]

   var wss = new ws.Server({
       server : server,
       path : '/one2many'
   });

   /*
    * Management of WebSocket messages
    */
   wss.on('connection', function(ws) {

      var sessionId = nextUniqueId();
      console.log('Connection received with sessionId ' + sessionId);

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
           case 'presenter':
            startPresenter(sessionId, ws, message.sdpOffer, function(error, sdpAnswer) {
               if (error) {
                  return ws.send(JSON.stringify({
                     id : 'presenterResponse',
                     response : 'rejected',
                     message : error
                  }));
               }
               ws.send(JSON.stringify({
                  id : 'presenterResponse',
                  response : 'accepted',
                  sdpAnswer : sdpAnswer
               }));
            });
            break;

           case 'viewer':
            startViewer(sessionId, ws, message.sdpOffer, function(error, sdpAnswer) {
               if (error) {
                  return ws.send(JSON.stringify({
                     id : 'viewerResponse',
                     response : 'rejected',
                     message : error
                  }));
               }

               ws.send(JSON.stringify({
                  id : 'viewerResponse',
                  response : 'accepted',
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
we need a *WebRtcEndpoint* (in send-only mode) for the presenter connected to N
*WebRtcEndpoint* (in receive-only mode) for the viewers. These functions are
called in the ``startPresenter`` and ``startViewer`` function, which is fired
when the ``presenter`` and ``viewer`` message are received respectively:

.. sourcecode:: js

   function startPresenter(sessionId, ws, sdpOffer, callback) {
      clearCandidatesQueue(sessionId);

      if (presenter !== null) {
         stop(sessionId);
         return callback("Another user is currently acting as presenter. Try again later ...");
      }

      presenter = {
         id : sessionId,
         pipeline : null,
         webRtcEndpoint : null
      }

      getKurentoClient(function(error, kurentoClient) {
         if (error) {
            stop(sessionId);
            return callback(error);
         }

         if (presenter === null) {
            stop(sessionId);
            return callback(noPresenterMessage);
         }

         kurentoClient.create('MediaPipeline', function(error, pipeline) {
            if (error) {
               stop(sessionId);
               return callback(error);
            }

            if (presenter === null) {
               stop(sessionId);
               return callback(noPresenterMessage);
            }

            presenter.pipeline = pipeline;
            pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint) {
               if (error) {
                  stop(sessionId);
                  return callback(error);
               }

               if (presenter === null) {
                  stop(sessionId);
                  return callback(noPresenterMessage);
               }

               presenter.webRtcEndpoint = webRtcEndpoint;

                   if (candidatesQueue[sessionId]) {
                       while(candidatesQueue[sessionId].length) {
                           var candidate = candidatesQueue[sessionId].shift();
                           webRtcEndpoint.addIceCandidate(candidate);
                       }
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
                     stop(sessionId);
                     return callback(error);
                  }

                  if (presenter === null) {
                     stop(sessionId);
                     return callback(noPresenterMessage);
                  }

                  callback(null, sdpAnswer);
               });

                   webRtcEndpoint.gatherCandidates(function(error) {
                       if (error) {
                           stop(sessionId);
                           return callback(error);
                       }
                   });
               });
           });
      });
   }

   function startViewer(sessionId, ws, sdpOffer, callback) {
      clearCandidatesQueue(sessionId);

      if (presenter === null) {
         stop(sessionId);
         return callback(noPresenterMessage);
      }

      presenter.pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint) {
         if (error) {
            stop(sessionId);
            return callback(error);
         }
         viewers[sessionId] = {
            "webRtcEndpoint" : webRtcEndpoint,
            "ws" : ws
         }

         if (presenter === null) {
            stop(sessionId);
            return callback(noPresenterMessage);
         }

         if (candidatesQueue[sessionId]) {
            while(candidatesQueue[sessionId].length) {
               var candidate = candidatesQueue[sessionId].shift();
               webRtcEndpoint.addIceCandidate(candidate);
            }
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
               stop(sessionId);
               return callback(error);
            }
            if (presenter === null) {
               stop(sessionId);
               return callback(noPresenterMessage);
            }

            presenter.webRtcEndpoint.connect(webRtcEndpoint, function(error) {
               if (error) {
                  stop(sessionId);
                  return callback(error);
               }
               if (presenter === null) {
                  stop(sessionId);
                  return callback(noPresenterMessage);
               }

               callback(null, sdpAnswer);
                 webRtcEndpoint.gatherCandidates(function(error) {
                     if (error) {
                        stop(sessionId);
                        return callback(error);
                     }
                 });
             });
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
       var candidate = kurento.register.complexTypes.IceCandidate(_candidate);

       if (presenter && presenter.id === sessionId && presenter.webRtcEndpoint) {
           console.info('Sending presenter candidate');
           presenter.webRtcEndpoint.addIceCandidate(candidate);
       }
       else if (viewers[sessionId] && viewers[sessionId].webRtcEndpoint) {
           console.info('Sending viewer candidate');
           viewers[sessionId].webRtcEndpoint.addIceCandidate(candidate);
       }
       else {
           console.info('Queueing candidate');
           if (!candidatesQueue[sessionId]) {
               candidatesQueue[sessionId] = [];
           }
           candidatesQueue[sessionId].push(candidate);
       }
   }

   function clearCandidatesQueue(sessionId) {
      if (candidatesQueue[sessionId]) {
         delete candidatesQueue[sessionId];
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
`index.html <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-one2many-call/static/index.html>`_
web page, and are used in the
`index.js <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-one2many-call/static/js/index.js>`_.
In the following snippet we can see the creation of the WebSocket (variable
``ws``) in the path ``/one2many``. Then, the ``onmessage`` listener of the
WebSocket is used to implement the JSON signaling protocol in the client-side.
Notice that there are three incoming messages to client: ``presenterResponse``,
``viewerResponse``,``stopCommunication``, and ``iceCandidate``. Convenient
actions are taken to implement each step in the communication.

On the one hand, the function ``presenter`` uses the method
``WebRtcPeer.WebRtcPeerSendonly`` of *kurento-utils.js* to start a WebRTC
communication in send-only mode. On the other hand, the function ``viewer``
uses the method ``WebRtcPeer.WebRtcPeerRecvonly`` of *kurento-utils.js* to
start a WebRTC communication in receive-only mode.

.. sourcecode:: javascript

   var ws = new WebSocket('ws://' + location.host + '/one2many');
   var webRtcPeer;

   const I_CAN_START = 0;
   const I_CAN_STOP = 1;
   const I_AM_STARTING = 2;

   [...]

   ws.onmessage = function(message) {
      var parsedMessage = JSON.parse(message.data);
      console.info('Received message: ' + message.data);

      switch (parsedMessage.id) {
      case 'presenterResponse':
         presenterResponse(parsedMessage);
         break;
      case 'viewerResponse':
         viewerResponse(parsedMessage);
         break;
      case 'stopCommunication':
         dispose();
         break;
      case 'iceCandidate':
         webRtcPeer.addIceCandidate(parsedMessage.candidate)
         break;
      default:
         console.error('Unrecognized message', parsedMessage);
      }
   }

   function presenterResponse(message) {
      if (message.response != 'accepted') {
         var errorMsg = message.message ? message.message : 'Unknow error';
         console.warn('Call not accepted for the following reason: ' + errorMsg);
         dispose();
      } else {
         webRtcPeer.processAnswer(message.sdpAnswer);
      }
   }

   function viewerResponse(message) {
      if (message.response != 'accepted') {
         var errorMsg = message.message ? message.message : 'Unknow error';
         console.warn('Call not accepted for the following reason: ' + errorMsg);
         dispose();
      } else {
         webRtcPeer.processAnswer(message.sdpAnswer);
      }
   }

On the one hand, the function ``presenter`` uses the method
``WebRtcPeer.WebRtcPeerSendonly`` of *kurento-utils.js* to start a WebRTC
communication in send-only mode. On the other hand, the function ``viewer``
uses the method ``WebRtcPeer.WebRtcPeerRecvonly`` of *kurento-utils.js* to
start a WebRTC communication in receive-only mode.

.. sourcecode:: javascript

   function presenter() {
      if (!webRtcPeer) {
         showSpinner(video);

         var options = {
            localVideo: video,
            onicecandidate : onIceCandidate
          }

         webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerSendonly(options, function(error) {
            if(error) return onError(error);

            this.generateOffer(onOfferPresenter);
         });
      }
   }

   function onOfferPresenter(error, offerSdp) {
      if (error) return onError(error);

      var message = {
         id : 'presenter',
         sdpOffer : offerSdp
      };
      sendMessage(message);
   }

   function viewer() {
      if (!webRtcPeer) {
         showSpinner(video);

         var options = {
            remoteVideo: video,
            onicecandidate : onIceCandidate
         }

         webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options, function(error) {
            if(error) return onError(error);

            this.generateOffer(onOfferViewer);
         });
      }
   }

   function onOfferViewer(error, offerSdp) {
      if (error) return onError(error)

      var message = {
         id : 'viewer',
         sdpOffer : offerSdp
      }
      sendMessage(message);
   }

Dependencies
============

Server-side dependencies of this demo are managed using :term:`npm`. Our main
dependency is the Kurento Client JavaScript (*kurento-client*). The relevant
part of the
`package.json <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-one2many-call/package.json>`_
file for managing this dependency is:

.. sourcecode:: js

   "dependencies": {
      [...]
      "kurento-client" : "|CLIENT_JS_VERSION|"
   }

At the client side, dependencies are managed using :term:`Bower`. Take a look to
the
`bower.json <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-one2many-call/static/bower.json>`_
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
