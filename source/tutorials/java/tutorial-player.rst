%%%%%%%%%%%%%
Java - Player
%%%%%%%%%%%%%

This tutorial opens a URL and plays its content to WebRTC where it is possible
to choose if it plays video and audio, only video or only audio.

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
    cd kurento-tutorial-java/kurento-player
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

To implement this behavior we have to create a `Media Pipeline`:term: composed
by one **PlayerEndpoint** and one **WebRtcEndpoint**. The **PlayerEnpdoint**
plays a video and **WebRtcEndpoint** shows it.

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
`GitHub <https://github.com/Kurento/kurento-tutorial-java/tree/master/kurento-player>`_.

Application Server Logic
========================

This demo has been developed using **Java** in the server-side, based on the
`Spring Boot`:term: framework, which embeds a Tomcat web server within the
generated maven artifact, and thus simplifies the development and deployment
process.

.. note::

   You can use whatever Java server side technology you prefer to build web
   applications with Kurento. For example, a pure Java EE application, SIP
   Servlets, Play, Vert.x, etc. Here we chose Spring Boot for convenience.

..
 digraph:: Player
   :caption: Server-side class diagram of the Player app

   size="12,8"; fontname = "Bitstream Vera Sans" fontsize = 8

   node [
        fontname = "Bitstream Vera Sans" fontsize = 8 shape = "record"
         style=filled
        fillcolor = "#E7F2FA"
   ]

   edge [
        fontname = "Bitstream Vera Sans" fontsize = 8 arrowhead = "vee"
   ]

   PlayerApp -> PlayerHandler; PlayerApp -> KurentoClient; PlayerHandler ->
   KurentoClient [constraint = false] PlayerHandler -> UserSession;

The main class of this demo is
`PlayerApp <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-player/src/main/java/org/kurento/tutorial/player/PlayerApp.java>`_.
As you can see, the *KurentoClient* is instantiated in this class as a Spring
Bean. This bean is used to create **Kurento Media Pipelines**, which are used
to add media capabilities to the application. In this instantiation we see that
we need to specify to the client library the location of the Kurento Media
Server. In this example, we assume it's located at *localhost* listening in
port 8888. If you reproduce this example you'll need to insert the specific
location of your Kurento Media Server instance there.

Once the *Kurento Client* has been instantiated, you are ready for communicating
with Kurento Media Server and controlling its multimedia capabilities.

.. sourcecode:: java

   @EnableWebSocket
   @SpringBootApplication
   public class PlayerApp implements WebSocketConfigurer {

     private static final String KMS_WS_URI_PROP = "kms.url";
     private static final String KMS_WS_URI_DEFAULT = "ws://localhost:8888/kurento";

     @Bean
     public PlayerHandler handler() {
       return new PlayerHandler();
     }

     @Bean
     public KurentoClient kurentoClient() {
       return KurentoClient.create(System.getProperty(KMS_WS_URI_PROP, KMS_WS_URI_DEFAULT));
     }

     @Override
     public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
       registry.addHandler(handler(), "/player");
     }

     public static void main(String[] args) throws Exception {
       new SpringApplication(PlayerApp.class).run(args);
     }
   }

This web application follows a *Single Page Application* architecture
(`SPA`:term:), and uses a `WebSocket`:term: to communicate client with
application server by means of requests and responses. Specifically, the main
app class implements the interface ``WebSocketConfigurer`` to register a
``WebSocketHandler`` to process WebSocket requests in the path ``/player``.

`PlayerHandler <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-player/src/main/java/org/kurento/tutorial/player/PlayerHandler.java>`_
class implements ``TextWebSocketHandler`` to handle text WebSocket requests.
The central piece of this class is the method ``handleTextMessage``. This
method implements the actions for requests, returning responses through the
WebSocket. In other words, it implements the server part of the signaling
protocol depicted in the previous sequence diagram.

In the designed protocol, there are seven different kinds of incoming messages
to the *Server* : ``start``, ``stop``, ``pause``, ``resume``, ``doSeek``,
``getPosition`` and ``onIceCandidates``. These messages are treated in the
*switch* clause, taking the proper steps in each case.

.. sourcecode:: java

   public class PlayerHandler extends TextWebSocketHandler {

     @Autowired
     private KurentoClient kurento;

     private final Logger log = LoggerFactory.getLogger(PlayerHandler.class);
     private final Gson gson = new GsonBuilder().create();
     private final ConcurrentHashMap<String, PlayerMediaPipeline> pipelines =
         new ConcurrentHashMap<>();

     @Override
     public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
       JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);
       String sessionId = session.getId();
       log.debug("Incoming message {} from sessionId", jsonMessage, sessionId);

       try {
         switch (jsonMessage.get("id").getAsString()) {
           case "start":
             start(session, jsonMessage);
             break;
           case "stop":
             stop(sessionId);
             break;
           case "pause":
             pause(sessionId);
             break;
           case "resume":
             resume(session);
             break;
           case "doSeek":
             doSeek(session, jsonMessage);
             break;
           case "getPosition":
             getPosition(session);
             break;
           case "onIceCandidate":
             onIceCandidate(sessionId, jsonMessage);
             break;
           default:
             sendError(session, "Invalid message with id " + jsonMessage.get("id").getAsString());
             break;
         }
       } catch (Throwable t) {
         log.error("Exception handling message {} in sessionId {}", jsonMessage, sessionId, t);
         sendError(session, t.getMessage());
       }
     }


     private void start(final WebSocketSession session, JsonObject jsonMessage) {
       ...
     }

     private void pause(String sessionId) {
      ...
     }

     private void resume(final WebSocketSession session) {
     ...
     }

     private void doSeek(final WebSocketSession session, JsonObject jsonMessage) {
     ...
     }

     private void getPosition(final WebSocketSession session) {
     ...
     }

     private void stop(String sessionId) {
     ...
     }

     private void sendError(WebSocketSession session, String message) {
       ...
     }
   }

In the following snippet, we can see the ``start`` method. It handles the ICE
candidates gathering, creates a Media Pipeline, creates the Media Elements
(``WebRtcEndpoint`` and ``PlayerEndpoint``) and makes the connections between
them and plays the video. A ``startResponse`` message is sent back to the
client with the SDP answer. When the ``MediaConnected`` event is received, info
about the video is retrieved and sent back to the client in a ``videoInfo``
message.

.. sourcecode:: java

  private void start(final WebSocketSession session, JsonObject jsonMessage) {
    final UserSession user = new UserSession(); MediaPipeline pipeline =
    kurento.createMediaPipeline(); user.setMediaPipeline(pipeline);
    WebRtcEndpoint webRtcEndpoint = new
    WebRtcEndpoint.Builder(pipeline).build();
    user.setWebRtcEndpoint(webRtcEndpoint); String videourl =
    jsonMessage.get("videourl").getAsString(); final PlayerEndpoint
    playerEndpoint = new PlayerEndpoint.Builder(pipeline, videourl).build();
    user.setPlayerEndpoint(playerEndpoint); users.put(session.getId(), user);

    playerEndpoint.connect(webRtcEndpoint);

    // 2. WebRtcEndpoint // ICE candidates
    webRtcEndpoint.addIceCandidateFoundListener(new
    EventListener<IceCandidateFoundEvent>() {
      @Override public void onEvent(IceCandidateFoundEvent event) {
        JsonObject response = new JsonObject();
        response.addProperty("id", "iceCandidate"); response.add("candidate",
        JsonUtils.toJsonObject(event.getCandidate())); try {
          synchronized (session) {
            session.sendMessage(new
            TextMessage(response.toString()));
          }
        } catch (IOException e) {
          log.debug(e.getMessage());
        }
      }
    });

    String sdpOffer = jsonMessage.get("sdpOffer").getAsString(); String
    sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

    JsonObject response = new JsonObject(); response.addProperty("id",
    "startResponse"); response.addProperty("sdpAnswer", sdpAnswer);
    sendMessage(session, response.toString());

    webRtcEndpoint.addMediaStateChangedListener(new
    EventListener<MediaStateChangedEvent>() {
      @Override public void onEvent(MediaStateChangedEvent event) {

        if (event.getNewState() == MediaState.CONNECTED) {
          VideoInfo videoInfo = playerEndpoint.getVideoInfo();

          JsonObject response = new JsonObject();
          response.addProperty("id", "videoInfo");
          response.addProperty("isSeekable", videoInfo.getIsSeekable());
          response.addProperty("initSeekable", videoInfo.getSeekableInit());
          response.addProperty("endSeekable", videoInfo.getSeekableEnd());
          response.addProperty("videoDuration", videoInfo.getDuration());
          sendMessage(session, response.toString());
        }
      }
    });

    webRtcEndpoint.gatherCandidates();

    // 3. PlayEndpoint playerEndpoint.addErrorListener(new
    EventListener<ErrorEvent>() {
      @Override public void onEvent(ErrorEvent event) {
        log.info("ErrorEvent: {}", event.getDescription());
        sendPlayEnd(session);
      }
    });

    playerEndpoint.addEndOfStreamListener(new
    EventListener<EndOfStreamEvent>() {
      @Override public void onEvent(EndOfStreamEvent event) {
        log.info("EndOfStreamEvent: {}", event.getTimestamp());
        sendPlayEnd(session);
      }
    });

    playerEndpoint.play();
  }



The ``pause`` method retrieves the *user* associated to the current session, and
invokes the *pause* method on the ``PlayerEndpoint``.

.. sourcecode:: java

  private void pause(String sessionId) {
    UserSession user = users.get(sessionId);

    if (user != null) {
      user.getPlayerEndpoint().pause();
    }
  }

The ``resume`` method starts the ``PlayerEndpoint`` of the current user, sending
back the information about the video, so the client side can refresh the stats.

.. sourcecode:: java

  private void resume(String sessionId) {
    UserSession user = users.get(session.getId());

    if (user != null) {
      user.getPlayerEndpoint().play(); VideoInfo videoInfo =
      user.getPlayerEndpoint().getVideoInfo();

      JsonObject response = new JsonObject(); response.addProperty("id",
      "videoInfo"); response.addProperty("isSeekable",
      videoInfo.getIsSeekable()); response.addProperty("initSeekable",
      videoInfo.getSeekableInit()); response.addProperty("endSeekable",
      videoInfo.getSeekableEnd()); response.addProperty("videoDuration",
      videoInfo.getDuration()); sendMessage(session, response.toString());
    }
  }

The ``doSeek`` method gets the *user* by *sessionId*, and calls the method
setPosition of the ``PlayerEndpoint`` with the new playing position. A ``seek``
message is sent back to the client if the seek fails.

.. sourcecode:: java

  private void doSeek(final WebSocketSession session, JsonObject jsonMessage) {
    UserSession user = users.get(session.getId());

    if (user != null) {
      try {
        user.getPlayerEndpoint().setPosition(jsonMessage.get("position").getAsLong());
      } catch (KurentoException e) {
        log.debug("The seek cannot be performed"); JsonObject response =
        new JsonObject(); response.addProperty("id", "seek");
        response.addProperty("message", "Seek failed"); sendMessage(session,
        response.toString());
      }
    }
  }

The ``getPosition`` calls the method getPosition of the ``PlayerEndpoint`` of
the current *user*. A ``position`` message is sent back to the client with the
actual position of the video.

.. sourcecode:: java

  private void getPosition(final WebSocketSession session) {
    UserSession user = users.get(session.getId());

    if (user != null) {
      long position = user.getPlayerEndpoint().getPosition();

      JsonObject response = new JsonObject(); response.addProperty("id",
      "position"); response.addProperty("position", position);
      sendMessage(session, response.toString());
    }
  }

The ``stop`` method is quite simple: it searches the *user* by *sessionId* and
stops the ``PlayerEndpoint``. Finally, it releases the media elements and
removes the user from the list of active users.

.. sourcecode:: java

  private void stop(String sessionId) {
    UserSession user = users.remove(sessionId);

    if (user != null) {
      user.release();
    }
  }

The ``sendError`` method is quite simple: it sends an ``error`` message to the
client when an exception is caught in the server-side.

.. sourcecode:: java

  private void sendError(WebSocketSession session, String message) {
    try {
      JsonObject response = new JsonObject(); response.addProperty("id",
      "error"); response.addProperty("message", message);
      session.sendMessage(new TextMessage(response.toString()));
    } catch (IOException e) {
      log.error("Exception sending message", e);
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
`index.html <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-player/src/main/resources/static/index.html>`_
web page, and are used in the
`index.js <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-player/src/main/resources/static/js/index.js>`_.
In the following snippet we can see the creation of the WebSocket (variable
``ws``) in the path ``/player``. Then, the ``onmessage`` listener of the
WebSocket is used to implement the JSON signaling protocol in the client-side.
Notice that there are seven incoming messages to client: ``startResponse``,
``playEnd``, ``error``, ``videoInfo``, ``seek``, ``position`` and
``iceCandidate``. Convenient actions are taken to implement each step in the
communication. For example, in functions ``start`` the function
``WebRtcPeer.WebRtcPeerSendrecv`` of *kurento-utils.js* is used to start a
WebRTC communication.

.. sourcecode:: javascript


   var ws = new WebSocket('wss://' + location.host + '/player');

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
      case 'playEnd':
         playEnd();
         break;
        break;
      case 'videoInfo':
         showVideoData(parsedMessage);
         break;
      case 'iceCandidate':
         webRtcPeer.addIceCandidate(parsedMessage.candidate, function(error) {
            if (error)
               return console.error('Error adding candidate: ' + error);
         });
         break;
      case 'seek':
         console.log (parsedMessage.message);
         break;
      case 'position':
         document.getElementById("videoPosition").value = parsedMessage.position;
         break;
      default:
         if (state == I_AM_STARTING) {
            setState(I_CAN_START);
         }
         onError('Unrecognized message', parsedMessage);
      }
   }

   function start() {
      // Disable start button
      setState(I_AM_STARTING);
      showSpinner(video);

      var mode = $('input[name="mode"]:checked').val();
      console
            .log('Creating WebRtcPeer in " + mode + " mode and generating local sdp offer ...');

      // Video and audio by default
      var userMediaConstraints = {
         audio : true,
         video : true
      }

      if (mode == 'video-only') {
         userMediaConstraints.audio = false;
      } else if (mode == 'audio-only') {
         userMediaConstraints.video = false;
      }

      var options = {
         remoteVideo : video,
         mediaConstraints : userMediaConstraints,
         onicecandidate : onIceCandidate
      }

      console.info('User media constraints' + userMediaConstraints);

      webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
            function(error) {
               if (error)
                  return console.error(error);
               webRtcPeer.generateOffer(onOffer);
            });
   }

   function onOffer(error, offerSdp) {
      if (error)
         return console.error('Error generating the offer');
      console.info('Invoking SDP offer callback function ' + location.host);

      var message = {
         id : 'start',
         sdpOffer : offerSdp,
         videourl : document.getElementById('videourl').value
      }
      sendMessage(message);
   }

   function onError(error) {
      console.error(error);
   }

   function onIceCandidate(candidate) {
      console.log('Local candidate' + JSON.stringify(candidate));

      var message = {
         id : 'onIceCandidate',
         candidate : candidate
      }
      sendMessage(message);
   }

   function startResponse(message) {
      setState(I_CAN_STOP);
      console.log('SDP answer received from server. Processing ...');

      webRtcPeer.processAnswer(message.sdpAnswer, function(error) {
         if (error)
            return console.error(error);
      });
   }

   function pause() {
      togglePause()
      console.log('Pausing video ...');
      var message = {
         id : 'pause'
      }
      sendMessage(message);
   }

   function resume() {
      togglePause()
      console.log('Resuming video ...');
      var message = {
         id : 'resume'
      }
      sendMessage(message);
   }

   function stop() {
      console.log('Stopping video ...');
      setState(I_CAN_START);
      if (webRtcPeer) {
         webRtcPeer.dispose();
         webRtcPeer = null;

         var message = {
            id : 'stop'
         }
         sendMessage(message);
      }
      hideSpinner(video);
   }

   function playEnd() {
      setState(I_CAN_START);
      hideSpinner(video);
   }

   function doSeek() {
      var message = {
        id : 'doSeek',
        position: document.getElementById("seekPosition").value
      }
      sendMessage(message);
    }

    function getPosition() {
      var message = {
        id : 'getPosition'
      }
      sendMessage(message);
    }

    function showVideoData(parsedMessage) {
      //Show video info
      isSeekable = parsedMessage.isSeekable;
      if (isSeekable) {
          document.getElementById('isSeekable').value = "true";
          enableButton('#doSeek', 'doSeek()');
        } else {
          document.getElementById('isSeekable').value = "false";
        }

        document.getElementById('initSeek').value = parsedMessage.initSeekable;
        document.getElementById('endSeek').value = parsedMessage.endSeekable;
        document.getElementById('duration').value = parsedMessage.videoDuration;

        enableButton('#getPosition', 'getPosition()');
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

   We are in active development. You can find the latest version of
   Kurento Java Client at `Maven Central <https://search.maven.org/#search%7Cga%7C1%7Ckurento-client>`_.

Kurento Java Client has a minimum requirement of **Java 7**. Hence, you need to
include the following properties in your pom:

.. sourcecode:: xml

   <maven.compiler.target>1.7</maven.compiler.target>
   <maven.compiler.source>1.7</maven.compiler.source>
