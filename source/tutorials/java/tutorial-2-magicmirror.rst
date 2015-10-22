%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Java Tutorial 2 - WebRTC magic mirror
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application extends :doc:`Tutorial 1 <./tutorial-1-helloworld>` adding
media processing to the basic `WebRTC`:term: loopback.

For the impatient: running this example
=======================================

First of all, you should install Kurento Media Server to run this demo. Please
visit the :doc:`installation guide <../../installation_guide>` for further
information.

To launch the application you need to clone the GitHub project where this demo
is hosted and then run the main class, as follows:

.. sourcecode:: none

    git clone https://github.com/Kurento/kurento-tutorial-java.git
    cd kurento-tutorial-java/kurento-magic-mirror
    git checkout |TUTORIAL_JAVA_VERSION|
    mvn compile exec:java

The web application starts on port 8080 in the localhost by default. Therefore,
open the URL http://localhost:8080/ in a WebRTC compliant browser (Chrome,
Firefox).

.. note::

   These instructions work only if Kurento Media Server is up and running in the same machine
   than the tutorial. However, it is possible to locate the KMS in other machine simple adding
   the argument ``kms.ws.uri`` to the Maven execution command, as follows:

   .. sourcecode:: none

      mvn compile exec:java -Dkms.ws.uri=ws://kms_host:kms_port/kurento


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
  `Super Mario hat <http://files.kurento.org/imgs/mario-wings.png>`_).

.. figure:: ../../images/kurento-java-tutorial-2-magicmirror-pipeline.png
   :align:   center
   :alt:     WebRTC with filter in loopback Media Pipeline

   *WebRTC with filter in loopback Media Pipeline*

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

To communicate the client with the Java EE application server we have designed a
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
`GitHub <https://github.com/Kurento/kurento-tutorial-java/tree/master/kurento-magic-mirror>`_.

Application Server Side
=======================

This demo has been developed using a **Java EE** application server based on the
`Spring Boot`:term: framework. This technology can be used to embed the Tomcat
web server in the application and thus simplify the development process.

.. note::

   You can use whatever Java server side technology you prefer to build web
   applications with Kurento. For example, a pure Java EE application, SIP 
   Servlets, Play, Vert.x, etc. Here we chose Spring Boot for convenience.

In the following figure you can see a class diagram of the server side code:

.. figure:: ../../images/digraphs/MagicMirror.png
   :align: center
   :alt:   Server-side class diagram of the MagicMirror app

   *Server-side class diagram of the MagicMirror app*

..
 digraph:: MagicMirror
   :caption: Server-side class diagram of the MagicMirror app

   size="12,8"; fontname = "Bitstream Vera Sans" fontsize = 8

   node [
        fontname = "Bitstream Vera Sans" fontsize = 8 shape = "record"
         style=filled
        fillcolor = "#E7F2FA"
   ]

   edge [
        fontname = "Bitstream Vera Sans" fontsize = 8 arrowhead = "vee"
   ]

   MagicMirrorApp -> MagicMirrorHandler; MagicMirrorApp -> KurentoClient;
   MagicMirrorHandler -> UserSession; MagicMirrorHandler -> KurentoClient
   [constraint = false]

The main class of this demo is named
`MagicMirrorApp <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-magic-mirror/src/main/java/org/kurento/tutorial/magicmirror/MagicMirrorApp.java>`_.
As you can see, the *KurentoClient* is instantiated in this class as a Spring
Bean. This bean is used to create **Kurento Media Pipelines**, which are used
to add media capabilities to your applications. In this instantiation we see
that we need to specify to the client library the location of the Kurento Media
Server. In this example, we assume it's located at *localhost* listening in
port 8888. If you reproduce this tutorial you'll need to insert the specific
location of your Kurento Media Server instance there.

.. sourcecode:: java

   @Configuration
   @EnableWebSocket
   @EnableAutoConfiguration
   public class MagicMirrorApp implements WebSocketConfigurer {

      final static String DEFAULT_KMS_WS_URI = "ws://localhost:8888/kurento";
      final static String DEFAULT_APP_SERVER_URL = "http://localhost:8080";

      @Bean
      public MagicMirrorHandler handler() {
         return new MagicMirrorHandler();
      }
   
      @Bean
      public KurentoClient kurentoClient() {
         return KurentoClient.create(System.getProperty("kms.ws.uri",
               DEFAULT_KMS_WS_URI));
      }
   
      @Override
      public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
         registry.addHandler(handler(), "/magicmirror");
      }
   
      public static void main(String[] args) throws Exception {
         new SpringApplication(MagicMirrorApp.class).run(args);
      }
   }

This web application follows *Single Page Application* architecture
(`SPA`:term:) and uses a `WebSocket`:term: to communicate client with
application server by means of requests and responses. Specifically, the main
app class implements the interface ``WebSocketConfigurer`` to register a
``WebSocketHanlder`` to process WebSocket requests in the path ``/magicmirror``.


`MagicMirrorHandler <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-magic-mirror/src/main/java/org/kurento/tutorial/magicmirror/MagicMirrorHandler.java>`_
class implements ``TextWebSocketHandler`` to handle text WebSocket requests.
The central piece of this class is the method ``handleTextMessage``. This
method implements the actions for requests, returning responses through the
WebSocket. In other words, it implements the server part of the signaling
protocol depicted in the previous sequence diagram.

In the designed protocol there are three different kinds of incoming messages to
the *Server* : ``start``, ``stop`` and ``onIceCandidates``. These messages are
treated in the *switch* clause, taking the proper steps in each case.

.. sourcecode:: java

   public class MagicMirrorHandler extends TextWebSocketHandler {
   
      private final Logger log = LoggerFactory.getLogger(MagicMirrorHandler.class);
      private static final Gson gson = new GsonBuilder().create();
   
      private final ConcurrentHashMap<String, UserSession> users = new ConcurrentHashMap<String, UserSession>();
   
      @Autowired
      private KurentoClient kurento;
   
      @Override
      public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
         JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);
   
         log.debug("Incoming message: {}", jsonMessage);
   
         switch (jsonMessage.get("id").getAsString()) {
         case "start":
            start(session, jsonMessage);
            break;
         case "stop": {
            UserSession user = users.remove(session.getId());
            if (user != null) {
               user.release();
            }
            break;
         }
         case "onIceCandidate": {
            JsonObject jsonCandidate = jsonMessage.get("candidate").getAsJsonObject();
   
            UserSession user = users.get(session.getId());
            if (user != null) {
               IceCandidate candidate = new IceCandidate(jsonCandidate.get("candidate").getAsString(),
                     jsonCandidate.get("sdpMid").getAsString(), jsonCandidate.get("sdpMLineIndex").getAsInt());
               user.addCandidate(candidate);
            }
            break;
         }
         default:
            sendError(session, "Invalid message with id " + jsonMessage.get("id").getAsString());
            break;
         }
      }
   
      private void start(WebSocketSession session, JsonObject jsonMessage) {
         ...
      }
   
      private void sendError(WebSocketSession session, String message) {
         ...
      }
   }

In the following snippet, we can see the ``start`` method. It handles the ICE
candidates gathering, creates a Media Pipeline, creates the Media Elements
(``WebRtcEndpoint`` and ``FaceOverlayFilter``) and make the connections among
them. A ``startResponse`` message is sent back to the client with the SDP
answer.

.. sourcecode:: java

   private void start(final WebSocketSession session, JsonObject jsonMessage) {
      try {
         // User session
         UserSession user = new UserSession();
         MediaPipeline pipeline = kurento.createMediaPipeline();
         user.setMediaPipeline(pipeline);
         WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
         user.setWebRtcEndpoint(webRtcEndpoint);
         users.put(session.getId(), user);

         // ICE candidates
         webRtcEndpoint.addOnIceCandidateListener(new EventListener<OnIceCandidateEvent>() {
            @Override
            public void onEvent(OnIceCandidateEvent event) {
               JsonObject response = new JsonObject();
               response.addProperty("id", "iceCandidate");
               response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
               try {
                  synchronized (session) {
                     session.sendMessage(new TextMessage(response.toString()));
                  }
               } catch (IOException e) {
                  log.debug(e.getMessage());
               }
            }
         });

         // Media logic
         FaceOverlayFilter faceOverlayFilter = new FaceOverlayFilter.Builder(pipeline).build();

         String appServerUrl = System.getProperty("app.server.url", MagicMirrorApp.DEFAULT_APP_SERVER_URL);
         faceOverlayFilter.setOverlayedImage(appServerUrl + "/img/mario-wings.png", -0.35F, -1.2F, 1.6F, 1.6F);

         webRtcEndpoint.connect(faceOverlayFilter);
         faceOverlayFilter.connect(webRtcEndpoint);

         // SDP negotiation (offer and answer)
         String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
         String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

         JsonObject response = new JsonObject();
         response.addProperty("id", "startResponse");
         response.addProperty("sdpAnswer", sdpAnswer);

         synchronized (session) {
            session.sendMessage(new TextMessage(response.toString()));
         }

         webRtcEndpoint.gatherCandidates();

      } catch (Throwable t) {
         sendError(session, t.getMessage());
      }
   }

The ``sendError`` method is quite simple: it sends an ``error`` message to the
client when an exception is caught in the server-side.

.. sourcecode:: java

   private void sendError(WebSocketSession session, String message) {
      try {
         JsonObject response = new JsonObject();
         response.addProperty("id", "error");
         response.addProperty("message", message);
         session.sendMessage(new TextMessage(response.toString()));
      } catch (IOException e) {
         log.error("Exception sending message", e);
      }
   }


Client-Side
===========

Let's move now to the client-side of the application. To call the previously
created WebSocket service in the server-side, we use the JavaScript class
``WebSocket``. We use an specific Kurento JavaScript library called
**kurento-utils.js** to simplify the WebRTC interaction with the server. This
library depends on **adapter.js**, which is a JavaScript WebRTC utility
maintained by Google that abstracts away browser differences. Finally
**jquery.js** is also needed in this application.

These libraries are linked in the
`index.html <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-magic-mirror/src/main/resources/static/index.html>`_
web page, and are used in the
`index.js <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-magic-mirror/src/main/resources/static/js/index.js>`_.
In the following snippet we can see the creation of the WebSocket (variable
``ws``) in the path ``/magicmirror``. Then, the ``onmessage`` listener of the
WebSocket is used to implement the JSON signaling protocol in the client-side.
Notice that there are three incoming messages to client: ``startResponse``,
``error``, and ``iceCandidate``. Convenient actions are taken to implement each
step in the communication. For example, in functions ``start`` the function
``WebRtcPeer.WebRtcPeerSendrecv`` of *kurento-utils.js* is used to start a
WebRTC communication.

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
         onError("Error message from server: " + parsedMessage.message);
         break;
      case 'iceCandidate':
          webRtcPeer.addIceCandidate(parsedMessage.candidate, function (error) {
            if (error) {
               console.error("Error adding candidate: " + error);
               return;
            }
          });
          break;
      default:
         if (state == I_AM_STARTING) {
            setState(I_CAN_START);
         }
         onError('Unrecognized message', parsedMessage);
      }
   }

   function start() {
      console.log("Starting video call ...")
      // Disable start button
      setState(I_AM_STARTING);
      showSpinner(videoInput, videoOutput);
   
      console.log("Creating WebRtcPeer and generating local sdp offer ...");

       var options = {
            localVideo: videoInput,
            remoteVideo: videoOutput,
            onicecandidate: onIceCandidate
          }
      webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options,
         function (error) {
           if (error) {
              return console.error(error);
           }
           webRtcPeer.generateOffer(onOffer);
         });
   }

   function onOffer(offerSdp) {
      console.info('Invoking SDP offer callback function ' + location.host);
      var message = {
         id : 'start',
         sdpOffer : offerSdp
      }
      sendMessage(message);
   }

   function onIceCandidate(candidate) {
        console.log("Local candidate" + JSON.stringify(candidate));

        var message = {
          id: 'onIceCandidate',
          candidate: candidate
        };
        sendMessage(message);
   }

Dependencies
============

This Java Spring application is implemented using `Maven`:term:. The relevant
part of the
`pom.xml <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-magic-mirror/pom.xml>`_
is where Kurento dependencies are declared. As the following snippet shows, we
need two dependencies: the Kurento Client Java dependency (*kurento-client*)
and the JavaScript Kurento utility library (*kurento-utils*) for the
client-side:

.. sourcecode:: xml 

   <dependencies> 
      <dependency>
         <groupId>org.kurento</groupId>
         <artifactId>kurento-client</artifactId>
         <version>|CLIENT_JAVA_VERSION|</version>
      </dependency> 
      <dependency> 
         <groupId>org.kurento</groupId>
         <artifactId>kurento-utils-js</artifactId>
         <version>|CLIENT_JAVA_VERSION|</version>
      </dependency> 
   </dependencies>

.. note::

   We are in active development. You can find the latest version of
   Kurento Java Client at `Maven Central <http://search.maven.org/#search%7Cga%7C1%7Ckurento-client>`_.

Kurento Java Client has a minimum requirement of **Java 7**. To configure the
application to use Java 7, we have to include the following properties in the
properties section:

.. sourcecode:: xml 

   <maven.compiler.target>1.7</maven.compiler.target>
   <maven.compiler.source>1.7</maven.compiler.source>

Browser dependencies (i.e. *bootstrap*, *ekko-lightbox*, and *adapter.js*) are
handled with :term:`Bower`. This dependencies are defined in the file
`bower.json <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-magic-mirror/bower.json>`_.
The command ``bower install`` is automatically called from Maven. Thus, Bower
should be present in your system. It can be installed in an Ubuntu machine as
follows:

.. sourcecode:: none

   curl -sL https://deb.nodesource.com/setup | sudo bash -
   sudo apt-get install -y nodejs
   sudo npm install -g bower

.. note::

   *kurento-utils-js* can be resolved as a Java dependency but also is available on Bower. To use this
   library from Bower, add this dependency to the file
   `bower.json <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-magic-mirror/bower.json>`_:

   .. sourcecode:: js

      "dependencies": {
         "kurento-utils": "|UTILS_JS_VERSION|"
      }
