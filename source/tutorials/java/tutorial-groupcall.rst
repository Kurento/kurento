%%%%%%%%%%%%%%%%%
Java - Group Call
%%%%%%%%%%%%%%%%%

This tutorial shows how to work wih the concept of rooms, allowing to connect
several clients between them using `WebRTC`:term: technology, creating a
multiconference.

.. note::

   Web browsers require using *HTTPS* to enable WebRTC, so the web server must use SSL and a certificate file. For instructions, check :ref:`features-security-java-https`.

   For convenience, this tutorial already provides dummy self-signed certificates (which will cause a security warning in the browser).

For the impatient: running this example
=======================================

You need to have installed the Kurento Media Server before running this example.
Read the :doc:`installation guide </user/installation>` for further
information.

To launch the application, you need to clone the GitHub project where this demo
is hosted, and then run the main class:

.. sourcecode:: bash

    git clone https://github.com/Kurento/kurento-tutorial-java.git
    cd kurento-tutorial-java/kurento-group-call
    git checkout |VERSION_TUTORIAL_JAVA|
    mvn -U clean spring-boot:run

Access the application connecting to the URL https://localhost:8443/ in a WebRTC
capable browser (Chrome, Firefox).

.. note::

   These instructions work only if Kurento Media Server is up and running in the same machine
   as the tutorial. However, it is possible to connect to a remote KMS in other machine, simply adding
   the flag ``kms.url`` to the JVM executing the demo. As we'll be using maven, you should execute
   the following command

   .. sourcecode:: bash

      mvn -U clean spring-boot:run \
          -Dspring-boot.run.jvmArguments="-Dkms.url=ws://{KMS_HOST}:8888/kurento"


Understanding this example
==========================

This tutorial shows how to work with the concept of rooms. Each room will create
its own pipeline, being isolated from the other rooms. Clients connecting to a
certain room, will only be able to exchange media with clients in the same room.

Each client will send its own media, and in turn will receive the media from all
the other participants. This means that there will be a total of
n*n webrtc endpoints in each room, where n is the number of clients.

When a new client enters the room, a new webrtc will be created and negotiated
receive the media on the server. On the other hand, all participant will be
informed that a new user has connected. Then, all participants will request the
server to receive the new participant's media.

The newcomer, in turn, gets a list of all connected participants, and requests
the server to receive the media from all the present clients in the room.

When a client leaves the room, all clients are informed by the server. Then, the
client-side code requests the server to cancel all media elements related to
the client that left.

This is a web application, and therefore it follows a client-server
architecture. At the client-side, the logic is implemented in **JavaScript**.
At the server-side, we use a Spring-Boot based application server consuming the
**Kurento Java Client** API, to control **Kurento Media Server** capabilities.
All in all, the high level architecture of this demo is three-tier. To
communicate these entities, two WebSockets are used. First, a WebSocket is
created between client and application server to implement a custom signaling
protocol. Second, another WebSocket is used to perform the communication
between the Kurento Java Client and the Kurento Media Server. This
communication takes place using the **Kurento Protocol**. For further
information on it, please see this
:doc:`page </features/kurento_protocol>` of the documentation.

The following sections analyze in depth the server (Java) and client-side
(JavaScript) code of this application. The complete source code can be found in
`GitHub <https://github.com/Kurento/kurento-tutorial-java/tree/master/kurento-group-call>`_.

Application Server Logic
========================

This demo has been developed using **Java** in the server-side with
`Spring Boot`:term: framework. This technology can be used to embed the Tomcat
web server in the application and thus simplify the development process.

.. note::

   You can use whatever Java server side technology you prefer to build web
   applications with Kurento. For example, a pure Java EE application, SIP
   Servlets, Play, Vert.x, etc. Here we chose Spring Boot for convenience.


The main class of this demo is
`GroupCalldApp <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-group-call/src/main/java/org/kurento/tutorial/groupcall/GroupCallApp.java>`_.
As you can see, the *KurentoClient* is instantiated in this class as a Spring
Bean. This bean is used to create **Kurento Media Pipelines**, which are used
to add media capabilities to the application. In this instantiation we see that
we need to specify to the client library the location of the Kurento Media
Server. In this example, we assume it is located at *localhost* listening in
port TCP 8888. If you reproduce this example you'll need to insert the specific
location of your Kurento Media Server instance there.

Once the *Kurento Client* has been instantiated, you are ready for communicating
with Kurento Media Server and controlling its multimedia capabilities.

.. sourcecode:: java

   @EnableWebSocket
   @SpringBootApplication
   public class GroupCallApp implements WebSocketConfigurer {

     @Bean
     public UserRegistry registry() {
       return new UserRegistry();
     }

     @Bean
     public RoomManager roomManager() {
       return new RoomManager();
     }

     @Bean
     public CallHandler groupCallHandler() {
       return new CallHandler();
     }

     @Bean
     public KurentoClient kurentoClient() {
       return KurentoClient.create();
     }

     public static void main(String[] args) throws Exception {
       SpringApplication.run(GroupCallApp.class, args);
     }

     @Override
     public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
       registry.addHandler(groupCallHandler(), "/groupcall");
     }
   }

This web application follows a *Single Page Application* architecture
(`SPA`:term:), and uses a `WebSocket`:term: to communicate client with
application server by means of requests and responses. Specifically, the main
app class implements the interface ``WebSocketConfigurer`` to register a
``WebSocketHandler`` to process WebSocket requests in the path ``/groupcall``.

`CallHandler <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-group-call/src/main/java/org/kurento/tutorial/groupcall/CallHandler.java>`_
class implements ``TextWebSocketHandler`` to handle text WebSocket requests.
The central piece of this class is the method ``handleTextMessage``. This
method implements the actions for requests, returning responses through the
WebSocket. In other words, it implements the server part of the signaling
protocol depicted in the previous sequence diagram.

In the designed protocol there are five different kind of incoming messages to
the application server: ``joinRoom``, ``receiveVideoFrom``, ``leaveRoom`` and
``onIceCandidate``. These messages are treated in the *switch* clause, taking
the proper steps in each case.

.. sourcecode:: java

   public class CallHandler extends TextWebSocketHandler {

     private static final Logger log = LoggerFactory.getLogger(CallHandler.class);

     private static final Gson gson = new GsonBuilder().create();

     @Autowired
     private RoomManager roomManager;

     @Autowired
     private UserRegistry registry;

     @Override
     public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
       final JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);

       final UserSession user = registry.getBySession(session);

       if (user != null) {
         log.debug("Incoming message from user '{}': {}", user.getName(), jsonMessage);
       } else {
         log.debug("Incoming message from new user: {}", jsonMessage);
       }

       switch (jsonMessage.get("id").getAsString()) {
         case "joinRoom":
           joinRoom(jsonMessage, session);
           break;
         case "receiveVideoFrom":
           final String senderName = jsonMessage.get("sender").getAsString();
           final UserSession sender = registry.getByName(senderName);
           final String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
           user.receiveVideoFrom(sender, sdpOffer);
           break;
         case "leaveRoom":
           leaveRoom(user);
           break;
         case "onIceCandidate":
           JsonObject candidate = jsonMessage.get("candidate").getAsJsonObject();

           if (user != null) {
             IceCandidate cand = new IceCandidate(candidate.get("candidate").getAsString(),
                 candidate.get("sdpMid").getAsString(), candidate.get("sdpMLineIndex").getAsInt());
             user.addCandidate(cand, jsonMessage.get("name").getAsString());
           }
           break;
         default:
           break;
       }
     }

     @Override
     public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
         ...
     }

     private void joinRoom(JsonObject params, WebSocketSession session) throws IOException {
         ...
     }

     private void leaveRoom(UserSession user) throws IOException {
         ...
     }
   }


In the following snippet, we can see the ``afterConnectionClosed`` method.
Basically, it removes the ``userSession`` from ``registry`` and throws out the
user from the room.

.. sourcecode :: java

   @Override
   public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
      UserSession user = registry.removeBySession(session);
      roomManager.getRoom(user.getRoomName()).leave(user);
   }

In the ``joinRoom`` method, the server checks if there are a registered room
with the name specified, add the user into this room and registries the user.

.. sourcecode :: java

   private void joinRoom(JsonObject params, WebSocketSession session) throws IOException {
      final String roomName = params.get("room").getAsString();
      final String name = params.get("name").getAsString();
      log.info("PARTICIPANT {}: trying to join room {}", name, roomName);

      Room room = roomManager.getRoom(roomName);
      final UserSession user = room.join(name, session);
      registry.register(user);
   }


The ``leaveRoom`` method finish the video call from one user.

.. sourcecode :: java

   private void leaveRoom(UserSession user) throws IOException {
       final Room room = roomManager.getRoom(user.getRoomName());
       room.leave(user);
       if (room.getParticipants().isEmpty()) {
         roomManager.removeRoom(room);
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
**jquery.js** is also needed in this application.

These libraries are linked in the
`index.html <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-group-call/src/main/resources/static/index.html>`_
web page, and are used in the
`conferenceroom.js <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-group-call/src/main/resources/static/js/conferenceroom.js>`_.
In the following snippet we can see the creation of the WebSocket (variable
``ws``) in the path ``/groupcall``. Then, the ``onmessage`` listener of the
WebSocket is used to implement the JSON signaling protocol in the client-side.
Notice that there are three incoming messages to client:
``existingParticipants``, ``newParticipantArrived``, ``participantLeft``,
``receiveVideoAnswer`` and ``iceCandidate``. Convenient actions are taken to
implement each step in the communication. For example, in functions ``start``
the function ``WebRtcPeer.WebRtcPeerSendrecv`` of *kurento-utils.js* is used to
start a WebRTC communication.

.. sourcecode:: javascript

   var ws = new WebSocket('wss://' + location.host + '/groupcall');
   var participants = {};
   var name;

   window.onbeforeunload = function() {
      ws.close();
   };

   ws.onmessage = function(message) {
      var parsedMessage = JSON.parse(message.data);
      console.info('Received message: ' + message.data);

      switch (parsedMessage.id) {
      case 'existingParticipants':
         onExistingParticipants(parsedMessage);
         break;
      case 'newParticipantArrived':
         onNewParticipant(parsedMessage);
         break;
      case 'participantLeft':
         onParticipantLeft(parsedMessage);
         break;
      case 'receiveVideoAnswer':
         receiveVideoResponse(parsedMessage);
         break;
      case 'iceCandidate':
         participants[parsedMessage.name].rtcPeer.addIceCandidate(parsedMessage.candidate, function (error) {
              if (error) {
               console.error("Error adding candidate: " + error);
               return;
              }
          });
          break;
      default:
         console.error('Unrecognized message', parsedMessage);
      }
   }

   function register() {
      name = document.getElementById('name').value;
      var room = document.getElementById('roomName').value;

      document.getElementById('room-header').innerText = 'ROOM ' + room;
      document.getElementById('join').style.display = 'none';
      document.getElementById('room').style.display = 'block';

      var message = {
         id : 'joinRoom',
         name : name,
         room : room,
      }
      sendMessage(message);
   }

   function onNewParticipant(request) {
      receiveVideo(request.name);
   }

   function receiveVideoResponse(result) {
      participants[result.name].rtcPeer.processAnswer (result.sdpAnswer, function (error) {
         if (error) return console.error (error);
      });
   }

   function callResponse(message) {
      if (message.response != 'accepted') {
         console.info('Call not accepted by peer. Closing call');
         stop();
      } else {
         webRtcPeer.processAnswer(message.sdpAnswer, function (error) {
            if (error) return console.error (error);
         });
      }
   }

   function onExistingParticipants(msg) {
      var constraints = {
         audio : true,
         video : {
            mandatory : {
               maxWidth : 320,
               maxFrameRate : 15,
               minFrameRate : 15
            }
         }
      };
      console.log(name + " registered in room " + room);
      var participant = new Participant(name);
      participants[name] = participant;
      var video = participant.getVideoElement();

      var options = {
            localVideo: video,
            mediaConstraints: constraints,
            onicecandidate: participant.onIceCandidate.bind(participant)
          }
      participant.rtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendonly(options,
         function (error) {
           if(error) {
              return console.error(error);
           }
           this.generateOffer (participant.offerToReceiveVideo.bind(participant));
      });

      msg.data.forEach(receiveVideo);
   }

   function leaveRoom() {
      sendMessage({
         id : 'leaveRoom'
      });

      for ( var key in participants) {
         participants[key].dispose();
      }

      document.getElementById('join').style.display = 'block';
      document.getElementById('room').style.display = 'none';

      ws.close();
   }

   function receiveVideo(sender) {
      var participant = new Participant(sender);
      participants[sender] = participant;
      var video = participant.getVideoElement();

      var options = {
         remoteVideo: video,
         onicecandidate: participant.onIceCandidate.bind(participant)
       }

      participant.rtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
            function (error) {
              if(error) {
                 return console.error(error);
              }
              this.generateOffer (participant.offerToReceiveVideo.bind(participant));
      });;
   }

   function onParticipantLeft(request) {
      console.log('Participant ' + request.name + ' left');
      var participant = participants[request.name];
      participant.dispose();
      delete participants[request.name];
   }

   function sendMessage(message) {
      var jsonMessage = JSON.stringify(message);
      console.log('Sending message: ' + jsonMessage);
      ws.send(jsonMessage);
   }


Dependencies
============

This Java Spring application is implemented using `Maven`:term:. The relevant
part of the
`pom.xml <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-show-data-channel/pom.xml>`_
is where Kurento dependencies are declared. As the following snippet shows, we
need two dependencies: the Kurento Client Java dependency (*kurento-client*)
and the JavaScript Kurento utility library (*kurento-utils*) for the
client-side. Other client libraries are managed with
`webjars <https://www.webjars.org/>`_:

.. sourcecode:: xml

   <dependencies>
      <dependency>
         <groupId>org.kurento</groupId>
         <artifactId>kurento-client</artifactId>
      </dependency>
      <dependency>
         <groupId>org.kurento</groupId>
         <artifactId>kurento-utils-js</artifactId>
      </dependency>
      <dependency>
         <groupId>org.webjars</groupId>
         <artifactId>webjars-locator</artifactId>
      </dependency>
      <dependency>
         <groupId>org.webjars.bower</groupId>
         <artifactId>bootstrap</artifactId>
      </dependency>
      <dependency>
         <groupId>org.webjars.bower</groupId>
         <artifactId>demo-console</artifactId>
      </dependency>
      <dependency>
         <groupId>org.webjars.bower</groupId>
         <artifactId>adapter.js</artifactId>
      </dependency>
      <dependency>
         <groupId>org.webjars.bower</groupId>
         <artifactId>jquery</artifactId>
      </dependency>
      <dependency>
         <groupId>org.webjars.bower</groupId>
         <artifactId>ekko-lightbox</artifactId>
      </dependency>
   </dependencies>

.. note::

   You can find the latest version of
   Kurento Java Client at `Maven Central <https://search.maven.org/#search%7Cga%7C1%7Ckurento-client>`_.

Kurento Java Client has a minimum requirement of **Java 7**. Hence, you need to
include the following properties in your pom:

.. sourcecode:: xml

   <maven.compiler.target>1.7</maven.compiler.target>
   <maven.compiler.source>1.7</maven.compiler.source>
