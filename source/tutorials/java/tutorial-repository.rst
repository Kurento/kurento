%%%%%%%%%%%%%%%%%
Java - Repository
%%%%%%%%%%%%%%%%%

.. warning::

   This tutorial is not actively maintained. It was written to showcase the use of `Kurento Repository Server <https://github.com/Kurento/kurento-java/tree/master/kurento-repository/kurento-repository-server>`__, which itself if not maintained either.

   All content here is available for legacy reasons, but no support is provided at all, and you'll be on your own if you decide to use it.

This web application extends :doc:`Hello World <./tutorial-helloworld>` adding
recording capabilities by means of the
`Kurento Repository <https://doc-kurento-repository.readthedocs.io/>`_.

.. note::

   Web browsers require using *HTTPS* to enable WebRTC, so the web server must use SSL and a certificate file. For instructions, check :ref:`features-security-java-https`.

   For convenience, this tutorial already provides dummy self-signed certificates (which will cause a security warning in the browser).

For the impatient: running this example
=======================================

You need to have installed the Kurento Media Server before running this example.
Read the :doc:`installation guide </user/installation>` for further
information.

In addition, you also need the **kurento-repository-server**. This component is
in charge of the storage and retrieval of the media. Please visit the
`Kurento Repository Server installation guide <https://doc-kurento-repository.readthedocs.org/en/stable/repository_server.html>`_
for further details.

To launch the application, you need to clone the GitHub project where this demo
is hosted, and then run the main class:

.. sourcecode:: bash

    git clone https://github.com/Kurento/kurento-tutorial-java.git
    cd kurento-tutorial-java/kurento-hello-world-repository/
    git checkout |VERSION_TUTORIAL_JAVA|
    mvn -U clean spring-boot:run

Access the application connecting to the URL https://localhost:8443/ in a WebRTC
capable browser (Chrome, Firefox).

.. note::

   These instructions work only if Kurento Media Server is up and running in the same machine
   as the tutorial. However, it is possible to connect to a remote KMS in other machine, simply adding
   the flag ``kms.url`` to the JVM executing the demo. In addition, by default this demo is also
   suppossing that the Kurento Repository is up and running in the localhost. It can be changed by
   means of the property ``repository.uri``. All in all, and due to the fact that we can use Maven
   to run the tutorial, you should execute the following command:

   .. sourcecode:: bash

      mvn -U clean spring-boot:run \
          -Dspring-boot.run.jvmArguments="\
              -Dkms.url=ws://{KMS_HOST}:8888/kurento \
              -Drepository.uri=http://repository_host:repository_url \
          "



Understanding this example
==========================

On top of the recording capabilities from the base tutorial, this application
creates a repository element to store media in that repository. Additionally,
metadata about the recorded file can also be stored in the repository.

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

The following sections analyze in deep the server (Java) and client-side
(JavaScript) code of this application. The complete source code can be found in
`GitHub <https://github.com/Kurento/kurento-tutorial-java/tree/master/kurento-hello-world-repository>`_.

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

The main class of this demo is
`HelloWorldRecApp <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-hello-world-repository/src/main/java/org/kurento/tutorial/helloworld/HelloWorldRecApp.java>`_.
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

   @SpringBootApplication
   @EnableWebSocket
   public class HelloWorldRecApp implements WebSocketConfigurer {

     protected static final String DEFAULT_REPOSITORY_SERVER_URI = "http://localhost:7676";

     protected static final String REPOSITORY_SERVER_URI =
       System.getProperty("repository.uri", DEFAULT_REPOSITORY_SERVER_URI);

     @Bean
     public HelloWorldRecHandler handler() {
       return new HelloWorldRecHandler();
     }

     @Bean
     public KurentoClient kurentoClient() {
       return KurentoClient.create();
     }

     @Override
     public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
       registry.addHandler(handler(), "/repository");
     }

     @Bean
     public RepositoryClient repositoryServiceProvider() {
       return REPOSITORY_SERVER_URI.startsWith("file://") ? null
         : RepositoryClientProvider.create(REPOSITORY_SERVER_URI);
     }

     @Bean
     public UserRegistry registry() {
       return new UserRegistry();
     }

     public static void main(String[] args) throws Exception {
       new SpringApplication(HelloWorldRecApp.class).run(args);
     }
   }

This web application follows a *Single Page Application* architecture
(`SPA`:term:), and uses a `WebSocket`:term: to communicate client with
application server by means of requests and responses. Specifically, the main
app class implements the interface ``WebSocketConfigurer`` to register a
``WebSocketHandler`` to process WebSocket requests in the path ``/repository``.

`HelloWorldRecHandler <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-hello-world-repository/src/main/java/org/kurento/tutorial/helloworld/HelloWorldRecHandler.java>`_
class implements ``TextWebSocketHandler`` to handle text WebSocket requests.
The central piece of this class is the method ``handleTextMessage``. This
method implements the actions for requests, returning responses through the
WebSocket. In other words, it implements the server part of the signaling
protocol depicted in the previous sequence diagram.

In the designed protocol there are three different kinds of incoming messages to
the *Server* : ``start``, ``stop``, ``stopPlay``, ``play`` and
``onIceCandidates``. These messages are treated in the *switch* clause, taking
the proper steps in each case.

.. sourcecode:: java

   public class HelloWorldRecHandler extends TextWebSocketHandler {

     // slightly larger timeout
     private static final int REPOSITORY_DISCONNECT_TIMEOUT = 5500;

     private static final String RECORDING_EXT = ".webm";

     private final Logger log = LoggerFactory.getLogger(HelloWorldRecHandler.class);
     private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-S");
     private static final Gson gson = new GsonBuilder().create();

     @Autowired
     private UserRegistry registry;

     @Autowired
     private KurentoClient kurento;

     @Autowired
     private RepositoryClient repositoryClient;

     @Override
     public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
       JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);

       log.debug("Incoming message: {}", jsonMessage);

       UserSession user = registry.getBySession(session);
       if (user != null) {
         log.debug("Incoming message from user '{}': {}", user.getId(), jsonMessage);
       } else {
         log.debug("Incoming message from new user: {}", jsonMessage);
       }

       switch (jsonMessage.get("id").getAsString()) {
         case "start":
           start(session, jsonMessage);
           break;
         case "stop":
         case "stopPlay":
           if (user != null) {
             user.release();
           }
           break;
         case "play":
           play(user, session, jsonMessage);
           break;
         case "onIceCandidate": {
           JsonObject jsonCandidate = jsonMessage.get("candidate").getAsJsonObject();

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

     private void play(UserSession user, final WebSocketSession session, JsonObject jsonMessage) {
       ...
     }

     private void sendError(WebSocketSession session, String message) {
       ...
     }
   }

In the following snippet, we can see the ``start`` method. If a repository REST
client or interface has been created, it will obtain a RepositoryItem from the
remote service. This item contains an ID and a recording URI that will be used
by the Kurento Media Server. The ID will be used after the recording ends in
order to manage the stored media. If the client doesn't exist, the recording
will be performed to a local URI, on the same machine as the KMS. This method
also deals with the ICE candidates gathering, creates a Media Pipeline, creates
the Media Elements (``WebRtcEndpoint`` and ``RecorderEndpoint``) and makes the
connections between them. A ``startResponse`` message is sent back to the
client with the SDP answer.

.. sourcecode:: java

   private void start(final WebSocketSession session, JsonObject jsonMessage) {
      try {
         // 0. Repository logic
         RepositoryItemRecorder repoItem = null;
         if (repositoryClient != null) {
           try {
             Map<String, String> metadata = Collections.emptyMap();
             repoItem = repositoryClient.createRepositoryItem(metadata);
           } catch (Exception e) {
             log.warn("Unable to create kurento repository items", e);
           }
         } else {
           String now = df.format(new Date());
           String filePath = HelloWorldRecApp.REPOSITORY_SERVER_URI + now + RECORDING_EXT;
           repoItem = new RepositoryItemRecorder();
           repoItem.setId(now);
           repoItem.setUrl(filePath);
         }
         log.info("Media will be recorded {}by KMS: id={} , url={}",
             (repositoryClient == null ? "locally" : ""), repoItem.getId(), repoItem.getUrl());

         // 1. Media logic (webRtcEndpoint in loopback)
         MediaPipeline pipeline = kurento.createMediaPipeline();
         WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
         webRtcEndpoint.connect(webRtcEndpoint);
         RecorderEndpoint recorder = new RecorderEndpoint.Builder(pipeline, repoItem.getUrl())
             .withMediaProfile(MediaProfileSpecType.WEBM).build();
         webRtcEndpoint.connect(recorder);

         // 2. Store user session
         UserSession user = new UserSession(session);
         user.setMediaPipeline(pipeline);
         user.setWebRtcEndpoint(webRtcEndpoint);
         user.setRepoItem(repoItem);
         registry.register(user);

         // 3. SDP negotiation
         String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
         String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

         // 4. Gather ICE candidates
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
               log.error(e.getMessage());
             }
           }
     }

The ``play`` method, creates a Media Pipeline with the Media Elements
(``WebRtcEndpoint`` and ``PlayerEndpoint``) and make the connections between
them. It will then send the recorded media to the client. The media can be
served from the repository or directly from the disk. If the repository
interface exists, it will try to connect to the remote service in order to
obtain an URI from which the KMS will read the media streams. The inner
workings of the repository restrict reading an item before it has been closed
(after the upload finished). This will happen only when a certain number of
seconds elapse after the last byte of media is uploaded by the KMS (safe-guard
for gaps in the network communications).

.. sourcecode:: java

   private void play(UserSession user, final WebSocketSession session, JsonObject jsonMessage) {
      try {
         // 0. Repository logic
         RepositoryItemPlayer itemPlayer = null;
         if (repositoryClient != null) {
           try {
             Date stopTimestamp = user.getStopTimestamp();
             if (stopTimestamp != null) {
               Date now = new Date();
               long diff = now.getTime() - stopTimestamp.getTime();
               if (diff >= 0 && diff < REPOSITORY_DISCONNECT_TIMEOUT) {
                 log.info(
                     "Waiting for {}ms before requesting the repository read endpoint "
                         + "(requires {}ms before upload is considered terminated "
                         + "and only {}ms have passed)",
                     REPOSITORY_DISCONNECT_TIMEOUT - diff, REPOSITORY_DISCONNECT_TIMEOUT, diff);
                 Thread.sleep(REPOSITORY_DISCONNECT_TIMEOUT - diff);
               }
             } else {
               log.warn("No stop timeout was found, repository endpoint might not be ready");
             }
             itemPlayer = repositoryClient.getReadEndpoint(user.getRepoItem().getId());
           } catch (Exception e) {
             log.warn("Unable to obtain kurento repository endpoint", e);
           }
         } else {
           itemPlayer = new RepositoryItemPlayer();
           itemPlayer.setId(user.getRepoItem().getId());
           itemPlayer.setUrl(user.getRepoItem().getUrl());
         }
         log.debug("Playing from {}: id={}, url={}",
             (repositoryClient == null ? "disk" : "repository"), itemPlayer.getId(),
             itemPlayer.getUrl());

         // 1. Media logic
         final MediaPipeline pipeline = kurento.createMediaPipeline();
         WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
         PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline, itemPlayer.getUrl()).build();
         player.connect(webRtcEndpoint);

         // Player listeners
         player.addErrorListener(new EventListener<ErrorEvent>() {
           @Override
           public void onEvent(ErrorEvent event) {
             log.info("ErrorEvent for session '{}': {}", session.getId(), event.getDescription());
             sendPlayEnd(session, pipeline);
           }
         });
         player.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
           @Override
           public void onEvent(EndOfStreamEvent event) {
             log.info("EndOfStreamEvent for session '{}'", session.getId());
             sendPlayEnd(session, pipeline);
           }
         });

         // 2. Store user session
         user.setMediaPipeline(pipeline);
         user.setWebRtcEndpoint(webRtcEndpoint);

         // 3. SDP negotiation
         String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
         String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

         JsonObject response = new JsonObject();
         response.addProperty("id", "playResponse");
         response.addProperty("sdpAnswer", sdpAnswer);

         // 4. Gather ICE candidates
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
               log.error(e.getMessage());
             }
           }
     });


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
`index.html <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-hello-world-repository/src/main/resources/static/index.html>`_
web page, and are used in the
`index.js <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-hello-world-repository/src/main/resources/static/js/index.js>`_.
In the following snippet we can see the creation of the WebSocket (variable
``ws``) in the path ``/repository``. Then, the ``onmessage`` listener of the
WebSocket is used to implement the JSON signaling protocol in the client-side.
Notice that there are three incoming messages to client: ``startResponse``,
``playResponse``, ``playEnd``,``error``, and ``iceCandidate``. Convenient
actions are taken to implement each step in the communication. For example, in
functions ``start`` the function ``WebRtcPeer.WebRtcPeerSendrecv`` of
*kurento-utils.js* is used to start a WebRTC communication.

.. sourcecode:: javascript

   var ws = new WebSocket('wss://' + location.host + '/repository');

   ws.onmessage = function(message) {
      var parsedMessage = JSON.parse(message.data);
      console.info('Received message: ' + message.data);

      switch (parsedMessage.id) {
      case 'startResponse':
         startResponse(parsedMessage);
         break;
      case 'playResponse':
         playResponse(parsedMessage);
         break;
      case 'playEnd':
         playEnd();
         break;
      case 'error':
         setState(NO_CALL);
         onError('Error message from server: ' + parsedMessage.message);
         break;
      case 'iceCandidate':
         webRtcPeer.addIceCandidate(parsedMessage.candidate, function(error) {
            if (error)
               return console.error('Error adding candidate: ' + error);
         });
         break;
      default:
         setState(NO_CALL);
      onError('Unrecognized message', parsedMessage);
      }
   }

   function start() {
   console.log('Starting video call ...');

   // Disable start button
   setState(DISABLED);
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
            sdpOffer : offerSdp,
            mode :  $('input[name="mode"]:checked').val()
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
      };
      sendMessage(message);
   }

   function startResponse(message) {
      setState(IN_CALL);
      console.log('SDP answer received from server. Processing ...');

      webRtcPeer.processAnswer(message.sdpAnswer, function(error) {
         if (error)
            return console.error(error);
      });
   }

   function stop() {
      var stopMessageId = (state == IN_CALL) ? 'stop' : 'stopPlay';
      console.log('Stopping video while in ' + state + '...');
      setState(POST_CALL);
      if (webRtcPeer) {
         webRtcPeer.dispose();
         webRtcPeer = null;

         var message = {
               id : stopMessageId
         }
         sendMessage(message);
      }
      hideSpinner(videoInput, videoOutput);
   }

   function play() {
      console.log("Starting to play recorded video...");

      // Disable start button
      setState(DISABLED);
      showSpinner(videoOutput);

      console.log('Creating WebRtcPeer and generating local sdp offer ...');

      var options = {
         remoteVideo : videoOutput,
         onicecandidate : onIceCandidate
      }
      webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
            function(error) {
               if (error)
                  return console.error(error);
               webRtcPeer.generateOffer(onPlayOffer);
            });
   }

   function onPlayOffer(error, offerSdp) {
      if (error)
         return console.error('Error generating the offer');
      console.info('Invoking SDP offer callback function ' + location.host);
      var message = {
            id : 'play',
            sdpOffer : offerSdp
      }
      sendMessage(message);
   }

   function playResponse(message) {
      setState(IN_PLAY);
      webRtcPeer.processAnswer(message.sdpAnswer, function(error) {
         if (error)
            return console.error(error);
      });
   }

   function playEnd() {
      setState(POST_CALL);
      hideSpinner(videoInput, videoOutput);
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
