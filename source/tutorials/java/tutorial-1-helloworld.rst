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
At the server-side we use a Java application server consuming the
**Kurento Java Client** API to control **Kurento Media Server** capabilities.
All in all, the high level architecture of this demo is three-tier. To
communicate these entities the following technologies are used:

* `REST`:term:: Communication between JavaScript client-side and Java
  application server-side.

* `WebSocket`:term:: Communication between the Kurento Java Client and the
  Kurento Media Server. This communication is implemented by the
  **Kurento Protocol**. For further information, please see this
  :doc:`page <../../mastering/kurento_protocol>` of the documentation.

The diagram below shows an complete sequence diagram from the interactions with
the application interface to: i) JavaScript logic; ii) Application server logic
(which uses the Kurento Java Client); iii) Kurento Media Server.

.. figure:: ../../images/kurento-java-tutorial-1-helloworld-signaling.png
   :align:   center
   :alt:     Complete sequence diagram of Kurento Hello World (WebRTC in loopbak) demo

   *Complete sequence diagram of Kurento Hello World (WebRTC in loopbak) demo*

.. note::

   The communication between client and server-side does not need to be
   REST. For simplicity, in this tutorial REST has been used. In later examples
   a more complex signaling between client and server has been implement,
   using WebSockets. Please see later tutorials for further information.

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

   HelloWorldApp -> HelloWorldController;
   HelloWorldApp -> KurentoClient;
   HelloWorldController -> KurentoClient [constraint = false]

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

   @ComponentScan
   @EnableAutoConfiguration
   public class HelloWorldApp {
   
      final static String DEFAULT_KMS_WS_URI = "ws://localhost:8888/kurento";

      @Bean
      public KurentoClient kurentoClient() {
         return KurentoClient.create(System.getProperty("kms.ws.uri",
               DEFAULT_KMS_WS_URI));
      }
   
      public static void main(String[] args) throws Exception {
         new SpringApplication(HelloWorldApp.class).run(args);
      }
   }

As introduced before, we use `REST`:term: to communicate the client with the
Java application server. Specifically, we use the Spring annotation
*@RestController* to implement REST services in the server-side. Take a look to
the
`HelloWorldController <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-hello-world/src/main/java/org/kurento/tutorial/helloworld/HelloWorldController.java>`_
class:

.. sourcecode:: java

   @RestController
   public class HelloWorldController {
   
      @Autowired
      private KurentoClient kurento;
   
      @RequestMapping(value = "/helloworld", method = RequestMethod.POST)
      private String processRequest(@RequestBody String sdpOffer)
            throws InterruptedException {
         // 1. Media Logic
         MediaPipeline pipeline = kurento.createMediaPipeline();
         WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline)
               .build();
         webRtcEndpoint.connect(webRtcEndpoint);
         webRtcEndpoint.processOffer(sdpOffer);
   
         // 2. Gather candidates
         final CountDownLatch latchCandidates = new CountDownLatch(1);
         webRtcEndpoint
               .addOnIceGatheringDoneListener(new EventListener<OnIceGatheringDoneEvent>() {
                  @Override
                  public void onEvent(OnIceGatheringDoneEvent event) {
                     latchCandidates.countDown();
                  }
               });
         webRtcEndpoint.gatherCandidates();
         latchCandidates.await();

         // 3. SDP negotiation
         String responseSdp = webRtcEndpoint.getLocalSessionDescriptor();
         return responseSdp;
      }

   }

The application logic is implemented in the method *processRequest*. POST
Requests to path */helloworld* will fire this method, whose execution has three
main  parts:

 - **1. Configure media processing logic**: This is the part in which the
   application configures how Kurento has to process the media. In other words,
   the media pipeline is created here. To that aim, the object *KurentoClient*
   is used to create a *MediaPipeline* object. Using it, the media elements we
   need are created and connected. In this case, we only instantiate one
   *WebRtcEndpoint* for receiving the WebRTC stream and sending it back to the
   client.

 - **2. Gather ICE candidates**: As of version 6, Kurento fully supports the
   :term:`Trickle ICE` protocol. For that reason, *WebRtcEndpoint* can receive
   :term:`ICE` candidates asynchronous. To handle this, each *WebRtcEndpoint*
   offers a listener (*addOnIceGatheringDoneListener*) that receives an event
   when the ICE gathering process is done. In this example, the wait to this
   process to be ended is done by means of a Java
   `CountDownLatch <http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/CountDownLatch.html>`_

 - **3. WebRTC SDP negotiation**: In WebRTC, :term:`SDP` (Session Description
   protocol) is used for negotiating media exchanges between peers. Such
   negotiation is based on the SDP offer and answer exchange mechanism. This
   negotiation is finished in the third part of the method *processRequest*,
   using the SDP offer obtained from the browser client and returning a SDP
   answer generated by *WebRtcEndpoint*.


Client-Side Logic
=================

Let's move now to the client-side of the application, which follows
*Single Page Application* architecture (`SPA`:term:). To call the previously
created REST service, we use the JavaScript library `jQuery`:term:. In
addition, we use a Kurento JavaScript utilities library called
**kurento-utils.js** to simplify the WebRTC management in the browser. This
library depends on **adapter.js**, which is a JavaScript WebRTC utility
maintained by Google that abstracts away browser differences. Finally
**jquery.js** is also needed in this application.

These libraries are linked in the
`index.html <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-hello-world/src/main/resources/static/index.html>`_
web page, and are used in the
`index.js <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-hello-world/src/main/resources/static/js/index.js>`_.
In the *start* function we can see how the function *WebRtcPeer.startSendRecv*
of **kurento-utils.js** is used to simplify the WebRTC internal details (i.e.
*PeerConnection* and *getUserStream*) making possible to gather ICE candidates
and start a full-duplex WebRTC communication. In the options we specify the
HTML video tag with id *videoInput* in which the video camera (local stream) is
shown, and the video tag *videoOutput* in which the remote stream provided by
the Kurento Media Server is shown. In the function *onCandidateGatheringDone*,
the jQuery method *$.ajax* is used to send a POST request with the SDP offer to
the path */helloworld*, where the application server REST service is listening.

.. sourcecode:: javascript

   function start() {
      console.log('Starting video call ...');
      showSpinner(videoInput, videoOutput);

      var options = {
         localVideo : videoInput,
         remoteVideo : videoOutput,
         oncandidategatheringdone : onCandidateGatheringDone
      }

      webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options,
            function(error) {
               if (error) return console.error(error);
               webRtcPeer.generateOffer(onOffer);
            });
   }

   function onCandidateGatheringDone() {
      $.ajax({
         url : location.protocol + '/helloworld',
         type : 'POST',
         dataType : 'text',
         contentType : 'application/sdp',
         data : webRtcPeer.getLocalSessionDescriptor().sdp,
         success : function(sdpAnswer) {
            console.log("Received sdpAnswer from server. Processing ...");
            webRtcPeer.processAnswer(sdpAnswer, function(error) {
               if (error) return console.error(error);
            });
         },
         error : function(jqXHR, textStatus, error) {
            console.error(error);
         }
      });
   }

   function onOffer(error, sdpOffer) {
      console.info('Waiting for ICE candidates ...');
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

   <parent>
      <groupId>org.kurento</groupId>
      <artifactId>kurento-parent-pom</artifactId>
      <version>|CLIENT_JAVA_VERSION|</version>
   </parent>

   <dependencies> 
      <dependency>
         <groupId>org.kurento</groupId>
         <artifactId>kurento-client</artifactId>
      </dependency> 
      <dependency> 
         <groupId>org.kurento</groupId>
         <artifactId>kurento-utils-js</artifactId> 
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
