%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Java Tutorial 1 - Hello world
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application has been designed to introduce the principles of
programming with Kurento for Java developers. It consists on a `WebRTC`:term:
video communication in mirror (*loopback*). This tutorial assumes you have
basic knowledge on JavaScript, HTML and WebRTC. We also recommend reading the
:doc:`Introducing Kurento <../../introducing_kurento>` section before starting
this tutorial.

For the impatient: running this example
=======================================

You need to have installed the Kurento Media Server before running this example.
Read the :doc:`installation guide <../../installation_guide>` for further
information.

To launch the application you need to clone the GitHub project where this demo
is hosted and then run the main class, as follows:

.. sourcecode:: sh

    git clone https://github.com/Kurento/kurento-tutorial-java.git
    cd kurento-tutorial-java/kurento-hello-world
    mvn compile exec:java

Access the application connecting to the URL http://localhost:8080/ through a
WebRTC capable browser (Chrome, Firefox).

.. note::

   These instructions work only if Kurento Media Server is up and running in the same machine
   than the tutorial. However, it is possible to locate the KMS in other machine simple adding
   the argument ``kms.ws.uri`` to the Maven execution command, as follows:

   .. sourcecode:: sh

      mvn compile exec:java -Dkms.ws.uri=ws://kms_host:kms_port/kurento


Understanding this example
==========================

Kurento provides developers a **Kurento Java Client** to control
**Kurento Media Server**. This client library can be used in any kind of Java
application: Server Side Web, Desktop, Android, etc. It is compatible with any
framework like Java EE, Spring, Play, Vert.x, Swing and JavaFX.

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
(which uses the Kurento Java Client); iii) Kurento Media Server.

.. figure:: ../../images/kurento-java-tutorial-1-helloworld-signaling.png
   :align:   center
   :alt:     Complete sequence diagram of Kurento Hello World (WebRTC in loopbak) demo

   *Complete sequence diagram of Kurento Hello World (WebRTC in loopbak) demo*

The following sections analyze in deep the server (Java) and client-side
(JavaScript) code of this application. The complete source code can be found in
`GitHub <https://github.com/Kurento/kurento-tutorial-java/tree/master/kurento-hello-world>`_.


Application Server Logic
========================

This demo has been developed using **Java** in the server-side with
`Spring Boot`:term: framework. This technology can be used to embed the Tomcat
web server in the application and thus simplify the development process.

.. note::

   You can use whatever Java server side technology you prefer to build web
   applications with Kurento. For example, a pure Java EE application, SIP 
   Servlets, Play, Vert.x, etc. Here we chose Spring Boot for convenience.

In the following figure you can see a class diagram of the server side code:

.. digraph:: HelloWorld
   :caption: Server-side class diagram of the HelloWorld app

   size="12,8";
   fontname = "Bitstream Vera Sans"
   fontsize = 8

   node [
        fontname = "Bitstream Vera Sans"
        fontsize = 8
        shape = "record"
         style=filled
        fillcolor = "#E7F2FA"
   ]

   edge [
        fontname = "Bitstream Vera Sans"
        fontsize = 8
        arrowhead = "vee"
   ]

   HelloWorldApp -> HelloWorldHandler;
   HelloWorldApp -> KurentoClient;
   HelloWorldHandler -> KurentoClient [constraint = false]
   HelloWorldHandler -> UserSession;

The main class of this demo is
`HelloWorldApp <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-hello-world/src/main/java/org/kurento/tutorial/helloworld/HelloWorldApp.java>`_.
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

   @Configuration
   @EnableWebSocket
   @EnableAutoConfiguration
   public class HelloWorldApp implements WebSocketConfigurer {
   
      final static String DEFAULT_KMS_WS_URI = "ws://localhost:8888/kurento";

      @Bean
      public HelloWorldHandler handler() {
         return new HelloWorldHandler();
      }

      @Bean
      public KurentoClient kurentoClient() {
         return KurentoClient.create(System.getProperty("kms.ws.uri", DEFAULT_KMS_WS_URI));
      }

      @Override
      public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
         registry.addHandler(handler(), "/helloworld");
      }

      public static void main(String[] args) throws Exception {
         new SpringApplication(HelloWorldApp.class).run(args);
      }
   }

This web application follows *Single Page Application* architecture
(`SPA`:term:) and uses a `WebSocket`:term: to communicate client with
application server by means of requests and responses. Specifically, the main
app class implements the interface ``WebSocketConfigurer`` to register a
``WebSocketHanlder`` to process WebSocket requests in the path ``/helloworld``.

`HelloWorldHandler <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-hello-world/src/main/java/org/kurento/tutorial/helloworld/HelloWorldHandler.java>`_
class implements ``TextWebSocketHandler`` to handle text WebSocket requests.
The central piece of this class is the method ``handleTextMessage``. This
method implements the actions for requests, returning responses through the
WebSocket. In other words, it implements the server part of the signaling
protocol depicted in the previous sequence diagram.

.. sourcecode:: java

   public class HelloWorldHandler extends TextWebSocketHandler {

      private final Logger log = LoggerFactory.getLogger(HelloWorldHandler.class);
      private static final Gson gson = new GsonBuilder().create();

      @Autowired
      private KurentoClient kurento;

      private final ConcurrentHashMap<String, UserSession> users = new ConcurrentHashMap<String, UserSession>();

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

      private void start(final WebSocketSession session, JsonObject jsonMessage) {
         try {
            // 1. Media logic (webRtcEndpoint in loopback)
            MediaPipeline pipeline = kurento.createMediaPipeline();
            WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
            webRtcEndpoint.connect(webRtcEndpoint);

            // 2. Store user session
            UserSession user = new UserSession();
            user.setMediaPipeline(pipeline);
            user.setWebRtcEndpoint(webRtcEndpoint);
            users.put(session.getId(), user);

            // 3. SDP negotiation
            String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
            String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

            JsonObject response = new JsonObject();
            response.addProperty("id", "startResponse");
            response.addProperty("sdpAnswer", sdpAnswer);

            synchronized (session) {
               session.sendMessage(new TextMessage(response.toString()));
            }

            // 4. Gather ICE candidates
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
                     log.error(e.getMessage());
                  }
               }
            });
            webRtcEndpoint.gatherCandidates();

         } catch (Throwable t) {
            sendError(session, t.getMessage());
         }
      }

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
   }

The ``start`` method performs the following actions:

#. **Configure media processing logic**: This is the part in which the
   application configures how Kurento has to process the media. In other words,
   the media pipeline is created here. To that aim, the object *KurentoClient*
   is used to create a *MediaPipeline* object. Using it, the media elements we
   need are created and connected. In this case, we only instantiate one
   *WebRtcEndpoint* for receiving the WebRTC stream and sending it back to the
   client.

#. **Store user session**: In order to release orderly the resources in the
   Kurento Media Server, we store the user session (i.e. *Media Pipeline* and
   *WebRtcEndpoint*) to be able to perform a release process when the stop
   method is called.

#. **WebRTC SDP negotiation**: In WebRTC, :term:`SDP` (Session Description
   protocol) is used for negotiating media exchanges between peers. Such
   negotiation is based on the SDP offer and answer exchange mechanism. This
   negotiation is finished in the third part of the method *processRequest*,
   using the SDP offer obtained from the browser client and returning a SDP
   answer generated by *WebRtcEndpoint*.

#. **Gather ICE candidates**: As of version 6, Kurento fully supports the
   :term:`Trickle ICE` protocol. For that reason, *WebRtcEndpoint* can receive
   :term:`ICE` candidates asynchronously. To handle this, each *WebRtcEndpoint*
   offers a listener (*addOnIceGatheringDoneListener*) that receives an event
   when the ICE gathering process is done.


Client-Side Logic
=================

Let's move now to the client-side of the application. To call the previously
created WebSocket service in the server-side, we use the JavaScript class
``WebSocket``. We use an specific Kurento JavaScript library called
**kurento-utils.js** to simplify the WebRTC interaction with the server. This
library depends on **adapter.js**, which is a JavaScript WebRTC utility
maintained by Google that abstracts away browser differences. Finally
**jquery.js** is also needed in this application.

These libraries are linked in the
`index.html <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-hello-world/src/main/resources/static/index.html>`_
web page, and are used in the
`index.js <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-hello-world/src/main/resources/static/js/index.js>`_.
In the following snippet we can see the creation of the WebSocket (variable
``ws``) in the path ``/helloworld``. Then, the ``onmessage`` listener of the
WebSocket is used to implement the JSON signaling protocol in the client-side.
Notice that there are three incoming messages to client: ``startResponse``,
``error``, and ``iceCandidate``. Convenient actions are taken to implement each
step in the communication. For example, in functions ``start`` the function
``WebRtcPeer.WebRtcPeerSendrecv`` of *kurento-utils.js* is used to start a
WebRTC communication.

.. sourcecode:: javascript

   var ws = new WebSocket('ws://' + location.host + '/helloworld');

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
         webRtcPeer.addIceCandidate(parsedMessage.candidate, function(error) {
            if (error)
               return console.error('Error adding candidate: ' + error);
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
      console.log('Starting video call ...');

      // Disable start button
      setState(I_AM_STARTING);
      showSpinner(videoInput, videoOutput);

      console.log('Creating WebRtcPeer and generating local sdp offer ...');

      var options = {
         localVideo : videoInput,
         remoteVideo : videoOutput,
         onicecandidate : onIceCandidate
      }
      webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options,
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
         sdpOffer : offerSdp
      }
      sendMessage(message);
   }

   function onIceCandidate(candidate) {
      console.log('Local candidate' + JSON.stringify(candidate));

      var message = {
         id : 'onIceCandidate',
         candidate : candidate
      };
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

   function stop() {
      console.log('Stopping video call ...');
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
`pom.xml <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-hello-world/pom.xml>`_
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

Kurento Java Client has a minimum requirement of **Java 7**. Hence, you need to
include the following in the properties section:

.. sourcecode:: xml 

   <maven.compiler.target>1.7</maven.compiler.target>
   <maven.compiler.source>1.7</maven.compiler.source>

Browser dependencies (i.e. *bootstrap*, *ekko-lightbox*, and *adapter.js*) are
handled with :term:`Bower`. This dependencies are defined in the file
`bower.json <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-hello-world/bower.json>`_.
The command ``bower install`` is automatically called from Maven. Thus, Bower
should be present in your system. It can be installed in an Ubuntu machine as
follows:

.. sourcecode:: sh

   curl -sL https://deb.nodesource.com/setup | sudo bash -
   sudo apt-get install -y nodejs
   sudo npm install -g bower

.. note::

   *kurento-utils-js* can be resolved as a Java dependency but also is available on Bower. To use this
   library from Bower, add this dependency to the file
   `bower.json <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-hello-world/bower.json>`_:

   .. sourcecode:: js

      "dependencies": {
         "kurento-utils": "|UTILS_JS_VERSION|"
      }
