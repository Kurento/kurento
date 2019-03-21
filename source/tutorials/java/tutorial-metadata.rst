%%%%%%%%%%%%%%%
Java - Metadata
%%%%%%%%%%%%%%%

This tutorial detects and draws faces into the webcam video. The demo connects
two filters, the KmsDetectFaces and the KmsShowFaces.


.. note::

   This tutorial has been configured to use https. Follow the `instructions </features/security.html#configure-java-applications-to-use-https>`_
   to secure your application.

For the impatient: running this example
=======================================

You need to have installed the Kurento Media Server before running this example.
Read the :doc:`installation guide </user/installation>` for further
information.

To launch the application, you need to clone the GitHub project where this demo
is hosted, and then run the main class:

.. sourcecode:: bash

    git clone https://github.com/Kurento/kurento-tutorial-java.git
    cd kurento-tutorial-java/kurento-metadata-example
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

      mvn -U clean spring-boot:run -Dkms.url=ws://kms_host:kms_port/kurento

.. note::

   This demo needs the kms-datachannelexample module installed in the media server. That module is
   available in the Kurento repositories, so it is possible to install it with:


   .. sourcecode:: bash

      sudo apt-get install kms-datachannelexample


Understanding this example
==========================

To implement this behavior we have to create a `Media Pipeline`:term: composed
by one **WebRtcEndpoint** and two filters **KmsDetectFaces** and
**KmsShowFaces**. The first one detects faces into the image and it puts the
info about the face (position and dimensions) into the buffer metadata. The
second one reads the buffer metadata to find info about detected faces. If
there is info about faces, the filter draws the faces into the image.

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
`GitHub <https://github.com/Kurento/kurento-tutorial-java/tree/master/kurento-metadata-example>`_.

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
 digraph:: Metadata
   :caption: Server-side class diagram of the Metadata app

   size="12,8"; fontname = "Bitstream Vera Sans" fontsize = 8

   node [
        fontname = "Bitstream Vera Sans" fontsize = 8 shape = "record"
         style=filled
        fillcolor = "#E7F2FA"
   ]

   edge [
        fontname = "Bitstream Vera Sans" fontsize = 8 arrowhead = "vee"
   ]

   MetadataApp -> MetadataHandler; MetadataApp -> KurentoClient;
   MetadataHandler -> KurentoClient [constraint = false] MetadataHandler ->
   UserSession;

The main class of this demo is
`MetadataApp <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-metadata-example/src/main/java/org/kurento/tutorial/metadata/MetadataApp.java>`_.
As you can see, the *KurentoClient* is instantiated in this class as a Spring
Bean. This bean is used to create **Kurento Media Pipelines**, which are used
to add media capabilities to the application. In this instantiation we see that
we need to specify to the client library the location of the Kurento Media
Server. In this example, we assume it is located at *localhost*, listening in
port 8888. If you reproduce this example, you'll need to insert the specific
location of your Kurento Media Server instance there.

Once the *Kurento Client* has been instantiated, you are ready for communicating
with Kurento Media Server and controlling its multimedia capabilities.

.. sourcecode:: java

   @EnableWebSocket
   @SpringBootApplication
   public class MetadataApp implements WebSocketConfigurer {

     static final String DEFAULT_APP_SERVER_URL = "https://localhost:8443";

     @Bean
     public MetadataHandler handler() {
       return new MetadataHandler();
     }

     @Bean
     public KurentoClient kurentoClient() {
       return KurentoClient.create();
     }

     @Override
     public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
       registry.addHandler(handler(), "/metadata");
     }

     public static void main(String[] args) throws Exception {
       new SpringApplication(MetadataApp.class).run(args);
     }
   }

This web application follows a *Single Page Application* architecture
(`SPA`:term:), and uses a `WebSocket`:term: to communicate client with
application server by means of requests and responses. Specifically, the main
app class implements the interface ``WebSocketConfigurer`` to register a
``WebSocketHanlder`` to process WebSocket requests in the path ``/metadata``.

`MetadataHandler <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-metadata-example/src/main/java/org/kurento/tutorial/metadata/MetadataHandler.java>`_
class implements ``TextWebSocketHandler`` to handle text WebSocket requests.
The central piece of this class is the method ``handleTextMessage``. This
method implements the actions for requests, returning responses through the
WebSocket. In other words, it implements the server part of the signaling
protocol depicted in the previous sequence diagram.

In the designed protocol there are three different kinds of incoming messages to
the *Server* : ``start``, ``stop`` and ``onIceCandidates``. These messages are
treated in the *switch* clause, taking the proper steps in each case.

.. sourcecode:: java

   public class MetadataHandler extends TextWebSocketHandler {

     private final Logger log = LoggerFactory.getLogger(MetadataHandler.class);
     private static final Gson gson = new GsonBuilder().create();

     private final ConcurrentHashMap<String, UserSession> users = new ConcurrentHashMap<>();

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
                 jsonCandidate.get("sdpMid").getAsString(),
                 jsonCandidate.get("sdpMLineIndex").getAsInt());
             user.addCandidate(candidate);
           }
           break;
         }
         default:
           sendError(session, "Invalid message with id " + jsonMessage.get("id").getAsString());
           break;
       }
     }

     private void start(final WebSocketSession session, JsonObject jsonMessage) {
       ...
     }

     private void sendError(WebSocketSession session, String message) {
      ...
     }
   }

In the following snippet, we can see the ``start`` method. It handles the ICE
candidates gathering, creates a Media Pipeline, creates the Media Elements
(``WebRtcEndpoint``, ``KmsShowFaces`` and ``KmsDetectFaces``) and make the
connections among them. A ``startResponse`` message is sent back to the client
with the SDP answer.

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
         webRtcEndpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {
           @Override
           public void onEvent(IceCandidateFoundEvent event) {
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
         KmsShowFaces showFaces = new KmsShowFaces.Builder(pipeline).build();
         KmsDetectFaces detectFaces = new KmsDetectFaces.Builder(pipeline).build();

         webRtcEndpoint.connect(detectFaces);
         detectFaces.connect(showFaces);
         showFaces.connect(webRtcEndpoint);

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
`index.html <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-metadata-example/src/main/resources/static/index.html>`_
web page, and are used in the
`index.js <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-metadata-example/src/main/resources/static/js/index.js>`_.
In the following snippet we can see the creation of the WebSocket (variable
``ws``) in the path ``/metadata``. Then, the ``onmessage`` listener of the
WebSocket is used to implement the JSON signaling protocol in the client-side.
Notice that there are three incoming messages to client: ``startResponse``,
``error``, and ``iceCandidate``. Convenient actions are taken to implement each
step in the communication. For example, in functions ``start`` the function
``WebRtcPeer.WebRtcPeerSendrecv`` of *kurento-utils.js* is used to start a
WebRTC communication.

.. sourcecode:: javascript

    var ws = new WebSocket('wss://' + location.host + '/metadata');

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
         webRtcPeer.addIceCandidate(parsedMessage.candidate, function(error) {
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
         localVideo : videoInput,
         remoteVideo : videoOutput,
         onicecandidate : onIceCandidate
      }
      webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options,
            function(error) {
               if (error) {
                  return console.error(error);
               }
               webRtcPeer.generateOffer(onOffer);
            });
   }

   function onOffer(error, offerSdp) {
      if (error)
         return console.error("Error generating the offer");
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

   function onIceCandidate(candidate) {
      console.log("Local candidate" + JSON.stringify(candidate));

      var message = {
         id : 'onIceCandidate',
         candidate : candidate
      };
      sendMessage(message);
   }

   function startResponse(message) {
      setState(I_CAN_STOP);
      console.log("SDP answer received from server. Processing ...");

      webRtcPeer.processAnswer(message.sdpAnswer, function(error) {
         if (error)
            return console.error(error);
      });
   }

   function stop() {
      console.log("Stopping video call ...");
      setState(I_CAN_START);
      if (webRtcPeer) {
         webRtcPeer.dispose();
         webRtcPeer = null;

         var message = {
            id : 'stop'
         }
         sendMessage(message);
      }
      hideSpinner(videoInput, videoOutput);
   }

   function sendMessage(message) {
      var jsonMessage = JSON.stringify(message);
      console.log('Senging message: ' + jsonMessage);
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
