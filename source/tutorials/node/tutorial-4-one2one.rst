%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Tutorial 4 - One to one video call
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application consists on a one-to-one video call using `WebRTC`:term:
technology. In other words, this application provides a simple video softphone.

For the impatient: running this example
=======================================

First of all, you should install Kurento Media Server to run this demo. Please
visit the :doc:`installation guide <../../installation_guide>` for further
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

.. sourcecode:: sh

    git clone https://github.com/Kurento/kurento-tutorial-node.git
    cd kurento-tutorial-node/kurento-one2one-call
    npm install
    cd static
    bower install
    cd ..
    node app.js

Access the application connecting to the URL http://localhost:8080/ through a
WebRTC capable browser (Chrome, Firefox).


Understanding this example
==========================

The following picture shows an screenshot of this demo running in a web browser:

.. figure:: ../../images/kurento-java-tutorial-4-one2one-screenshot.png
   :align:   center
   :alt:     One to one video call screenshot
   :width: 600px

   *One to one video call screenshot*

The interface of the application (an HTML web page) is composed by two HTML5
video tags: one for the local stream and other for the remote peer stream). If
two users, A and B, are using the application, the media flows in the following
way: The video camera stream of user A is sent to the Kurento Media Server,
which sends it to user B. In the same way, B send to Kurento Media Server,
which forwards it to A. This means that KMS is providing a B2B (back-to-back)
call service.

To implement this behavior create a `Media Pipeline`:term: composed by two
WebRtC endpoints connected in B2B. The implemented media pipeline is
illustrated in the following picture:

.. figure:: ../../images/kurento-java-tutorial-4-one2one-pipeline.png
   :align:   center
   :alt:     One to one video call media pipeline
   :width: 400px

   *One to one video call Media Pipeline*

The client and the server communicate through a signaling protocol based on
`JSON`:term: messages over `WebSocket`:term: 's. The normal sequence between
client and application server logic is as follows:

1. User A is registered in the application server with his name

2. User B is registered in the application server with her name

3. User A issues a call to User B

4. User B accepts the incoming call

5. The communication is established and media flows between User A and
   User B

6. One of the users finishes the video communication

The detailed message flow in a call are shown in the picture below:

.. figure:: ../../images/kurento-java-tutorial-4-one2one-signaling.png
   :align:   center
   :alt:     One to one video call signaling protocol
   :width: 600px

   *One to many one call signaling protocol*

As you can see in the diagram, `SDP`:term: needs to be interchanged between
client and server to establish the `WebRTC`:term: connection between the
browser and Kurento. Specifically, the SDP negotiation connects the WebRtcPeer
in the browser with the WebRtcEndpoint in the server.

The following sections describe in detail the server-side, the client-side, and
how to run the demo. The complete source code of this demo can be found in
`GitHub <https://github.com/Kurento/kurento-tutorial-node/tree/master/kurento-one2one-call>`_.

Application Server Logic
========================

This demo has been developed using the **express** framework for Node.js, but
express is not a requirement for Kurento.

The main script of this demo is
`app.js <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-one2one-call/app.js>`_.

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
(`SPA`:term:) and uses a `WebSocket` in the path ``/call`` to communicate
client with applications server by beans of requests and responses.

In the designed protocol there are three different kind of incoming messages to
the applications server : ``register``, ``call``, ``incomingCallResponse`` and
``stop``. These messages are treated in the *switch* clause, taking the proper
steps in each case.

The following code snippet implements the server part of the signaling protocol
depicted in the previous sequence diagram.


.. sourcecode:: js

   wss.on('connection', function(ws) {
   
      //...
   
      ws.on('message', function(_message) {
         var message = JSON.parse(_message);
   
         switch (message.id) { 
         case 'register':
            register(sessionId,
            message.name, ws);
            break;

         case 'call':
            call(sessionId, message.to,
            message.from, message.sdpOffer); break;
   
         case 'incomingCallResponse':
            incomingCallResponse(sessionId,
            message.from, message.callResponse, message.sdpOffer);
            break;
   
         case 'stop':
            stop(sessionId); break;
   
         }
      });
   });


In the following snippet, we can see the ``register`` method. Basically, it
obtains the ``name`` attribute from ``register`` message and check if there are
a registered user with that name. If not, the new user is registered and an
acceptance message is sent to it.

.. sourcecode :: js

   function register(id, name, ws, callback){      
        
      if(userRegistry.getByName(name)){
         return onError("already registered");
      }
        
      userRegistry.register(new UserSession(id, name, ws));
      ws.send(JSON.stringify({id: 'registerResponse', response: 'accepted'}));
   }


In the ``call`` method, the server checks if there are a registered user with
the name specified in ``to`` message attribute and send an ``incomingCall``
message to it. Or, if there isn't any user with that name, a ``callResponse``
message is sent to caller rejecting the call.

.. sourcecode :: js

   function call(callerId, to, from, sdpOffer){
        var caller = userRegistry.getById(callerId);
        var rejectCause = 'user ' + to + ' is not registered';
        if(userRegistry.getByName(to)){
                var callee = userRegistry.getByName(to);
                caller.sdpOffer = sdpOffer
                callee.peer = from;
                caller.peer = to;
                var message = {
                        id: 'incomingCall',
                        from: from
                };
                return callee.sendMessage(message);
        } 
        var message  = {
                id: 'callResponse',
                response: 'rejected: ',
                message: rejectCause
        };
        caller.sendMessage(message);     
   }


The ``stop`` method finish the video call. This procedure can be called both by
caller and callee in the communication. The result is that both peers release
the Media Pipeline and ends the video communication:

.. sourcecode :: js

   function stop(sessionId){
        
        var pipeline = pipelines[sessionId];
        delete pipelines[sessionId];
        pipeline.release();
        var stopperUser = userRegistry.getById(sessionId);
        var stoppedUser = userRegistry.getByName(stopperUser.peer);
        stopperUser.peer = null;
        if(stoppedUser){
                stoppedUser.peer = null;
                delete pipelines[stoppedUser.id];
                var message = {
                        id: 'stopCommunication',
                        message: 'remote user hanged out'
                }
                stoppedUser.sendMessage(message)
        }
   }


In the ``incomingCallResponse`` method, if the callee user accepts the call, it
is established and the media elements are created to connect the caller with
the callee in a B2B manner. Basically, the server creates a
``CallMediaPipeline`` object, to encapsulate the media pipeline creation and
management. Then, this object is used to negotiate media interchange with
user's browsers.


The negotiation between WebRTC peer in the browser and WebRtcEndpoint in Kurento
Media Server is made by means of `SDP`:term: s. An SDP answers is produced by
WebRtcEndpoints when invoking ``generateSdpAnswerForCallee`` and
``generateSdpAnswerForCaller`` functions:

.. sourcecode :: js

   function incomingCallResponse(calleeId, from, callResponse, calleeSdp){

      var callee = userRegistry.getById(calleeId);
         if(!from || !userRegistry.getByName(from)){
            return onError(null, 'unknown from = ' + from);
         }               
         var caller = userRegistry.getByName(from);

         if(callResponse === 'accept'){  
            var pipeline = new CallMediaPipeline(); 

            pipeline.createPipeline(function(error){                     
               pipeline.generateSdpAnswerForCaller(caller.sdpOffer, function(error, callerSdpAnswer){
               if(error){
                  return onError(error, error);
               }

               pipeline.generateSdpAnswerForCallee(calleeSdp, function(error, calleeSdpAnswer){
                                        
                  pipelines[caller.id] = pipeline;
                  pipelines[callee.id] = pipeline;
                                        
                  var message = {
                     id: 'startCommunication',
                     sdpAnswer: calleeSdpAnswer
                  };

                  callee.sendMessage(message);

                  message = {
                     id: 'callResponse',
                     response : 'accepted',
                     sdpAnswer: callerSdpAnswer
                  };

                  caller.sendMessage(message);                                    
               });                             
            });                     
         });
      } else {
         var decline = {
            id: 'callResponse',
            response: 'rejected',
            message: 'user declined'
         };

         caller.sendMessage(decline);
        }
   }

           
The media logic is implemented in the class `CallMediaPipeline`. As you can see,
the required media pipeline is quite simple: two ``WebRtcEndpoint`` elements
directly interconnected. Note that the WebRtcEndpoints need to be connected
twice, one for each media direction. Also observe how the methods
``generateSdpAnswerForCaller`` and ``generateSdpAnswerForCallee`` described
above are implemented.

.. sourcecode:: js

   CallMediaPipeline.prototype.createPipeline = function(callback){
      var self = this;
	
      //...
                
      kurentoClient.create('MediaPipeline', function(error, pipeline){
         pipeline.create('WebRtcEndpoint', function(error, callerWebRtcEndpoint){                                
            pipeline.create('WebRtcEndpoint', function(error, calleeWebRtcEndpoint){                                        
               callerWebRtcEndpoint.connect(calleeWebRtcEndpoint, function(error){                                                
                  calleeWebRtcEndpoint.connect(callerWebRtcEndpoint, function(error){
                                                
                     self._pipeline = pipeline;
                     self._callerWebRtcEndpoint = callerWebRtcEndpoint;
                     self._calleeWebRtcEndpoint = calleeWebRtcEndpoint;
                                                
                     callback(null);
                  });                                     
               });
            });                     
         });
      });             
   }

   CallMediaPipeline.prototype.generateSdpAnswerForCaller = function(sdpOffer, callback){
      this._callerWebRtcEndpoint.processOffer(sdpOffer, callback);
   }

   CallMediaPipeline.prototype.generateSdpAnswerForCallee = function(sdpOffer, callback){
      this._calleeWebRtcEndpoint.processOffer(sdpOffer, callback);
   }

   CallMediaPipeline.prototype.release = function(){
      if(this._pipeline) this._pipeline.release();
      this._pipeline = null;
   }





Client-Side
===========

Let's move now to the client-side of the application. To call the previously
created WebSocket service in the server-side, we use the JavaScript class
``WebSocket``. We use an specific Kurento JavaScript library called
**kurento-utils.js** to simplify the WebRTC interaction with the server. These
libraries are linked in the
`index.html <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-one2one-call/src/main/resources/static/index.html>`_
web page, and are used in the
`index.js <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-one2one-call/src/main/resources/static/js/index.js>`_.

In the following snippet we can see the creation of the WebSocket (variable
``ws``) in the path ``/call``. Then, the ``onmessage`` listener of the
WebSocket is used to implement the JSON signaling protocol in the client-side.
Notice that there are four incoming messages to client: ``resgisterResponse``,
``callResponse``, ``incomingCall``, and ``startCommunication``. Convenient
actions are taken to implement each step in the communication. For example, in
functions ``call`` and ``incomingCall`` (for caller and callee respectively),
the function ``WebRtcPeer.startSendRecv`` of *kurento-utils.js* is used to
start a WebRTC communication.

.. sourcecode:: javascript

    var ws = new WebSocket('ws://' + location.host + '/call');

   ws.onmessage = function(message) {
      var parsedMessage = JSON.parse(message.data);
      console.info('Received message: ' + message.data);
   
      switch (parsedMessage.id) {
      case 'resgisterResponse':
         resgisterResponse(parsedMessage);
         break;
      case 'callResponse':
         callResponse(parsedMessage);
         break;
      case 'incomingCall':
         incomingCall(parsedMessage);
         break;
      case 'startCommunication':
         startCommunication(parsedMessage);
         break;
      case 'stopCommunication':
         console.info("Communication ended by remote peer");
         stop(true);
         break;
      default:
         console.error('Unrecognized message', parsedMessage);
      }
   }

   function incomingCall(message) {
      //If bussy just reject without disturbing user
      if(callState != NO_CALL){
         var response = {
            id : 'incomingCallResponse',
            from : message.from,
            callResponse : 'reject',
            message : 'bussy'
         };
         return sendMessage(response);
      }
      
      setCallState(PROCESSING_CALL);
      if (confirm('User ' + message.from  + ' is calling you. Do you accept the call?')) {
         showSpinner(videoInput, videoOutput);
         webRtcPeer = kurentoUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput, function(sdp, wp) {
            var response = {
               id : 'incomingCallResponse',
               from : message.from,
               callResponse : 'accept',
               sdpOffer : sdp
            };
            sendMessage(response);
         }, function(error){
            setCallState(NO_CALL);
         });
      } else {
         var response = {
            id : 'incomingCallResponse',
            from : message.from,
            callResponse : 'reject',
            message : 'user declined'
         };
         sendMessage(response);
         stop();
      }
   }

   function call() {
      if(document.getElementById('peer').value == ''){
         window.alert("You must specify the peer name");
         return;
      }
      setCallState(PROCESSING_CALL);
      
      showSpinner(videoInput, videoOutput);
   
      kurentoUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput, function(offerSdp, wp) {
         webRtcPeer = wp;
         console.log('Invoking SDP offer callback function');
         var message = {
            id : 'call',
            from : document.getElementById('name').value,
            to : document.getElementById('peer').value,
            sdpOffer : offerSdp
         };
         sendMessage(message);
      }, function(error){
         console.log(error);
         setCallState(NO_CALL);
      });
   }


Dependencies
============

Dependencies of this demo are managed using npm. Our main dependency is the
Kurento Client JavaScript (*kurento-client*). The relevant part of the
`package.json <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-one2one-call/package.json>`_
file for managing this dependency is:

.. sourcecode:: js

   "dependencies": {
     ...
     "kurento-client" : "^5.0.0"
   }

At the client side, dependencies are managed using Bower. Take a look to the
`bower.json <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-one2one-call/static/bower.json>`_
file and pay attention to the following section:

.. sourcecode:: js

   "dependencies": {
     "kurento-utils" : "^5.0.0"
   }

Kurento framework uses `Semantic Versioning`:term: for releases. Notice that
range ``^5.0.0`` downloads the latest version of Kurento artefacts from Bower
in version 5 (i.e. 5.x.x). Major versions are released when incompatible
changes are made.

.. note::

   We are in active development. You can find the latest version of
   Kurento JavaScript Client at `Bower <http://bower.io/search/>`_.
