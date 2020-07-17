%%%%%%%%%%%%%%%%%%%%%%%%%%%
Java Module - Chroma Filter
%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application consists of a `WebRTC`:term: video communication in mirror
(*loopback*) with a chroma filter element.

.. note::

   Web browsers require using *HTTPS* to enable WebRTC, so the web server must use SSL and a certificate file. For instructions, check :ref:`features-security-java-https`.

   For convenience, this tutorial already provides dummy self-signed certificates (which will cause a security warning in the browser).

For the impatient: running this example
=======================================

First of all, you should install Kurento Media Server to run this demo. Please
visit the :doc:`installation guide </user/installation>` for further
information. In addition, the built-in module ``kms-chroma`` should be also
installed:

.. sourcecode:: bash

    sudo apt-get install kms-chroma

To launch the application, you need to clone the GitHub project where this demo
is hosted, and then run the main class:

.. sourcecode:: bash

    git clone https://github.com/Kurento/kurento-tutorial-java.git
    cd kurento-tutorial-java/kurento-chroma
    git checkout |VERSION_TUTORIAL_JAVA|
    mvn -U clean spring-boot:run

The web application starts on port 8443 in the localhost by default. Therefore,
open the URL https://localhost:8443/ in a WebRTC compliant browser (Chrome,
Firefox).

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

This application uses computer vision and augmented reality techniques to detect
a chroma in a WebRTC stream based on color tracking.

The interface of the application (an HTML web page) is composed by two HTML5
video tags: one for the video camera stream (the local client-side stream) and
other for the mirror (the remote stream). The video camera stream is sent to
Kurento Media Server, which processes and sends it back to the client as a
remote stream. To implement this, we need to create a `Media Pipeline`:term:
composed by the following `Media Element`:term: s:

.. figure:: ../../images/kurento-module-tutorial-chroma-pipeline.png
   :align:   center
   :alt:     WebRTC with Chroma filter Media Pipeline

   *WebRTC with Chroma filter Media Pipeline*

The complete source code of this demo can be found in
`GitHub <https://github.com/Kurento/kurento-tutorial-java/tree/master/kurento-chroma>`_.

This example is a modified version of the
:doc:`Magic Mirror <./tutorial-magicmirror>` tutorial. In this case, this demo
uses a **Chroma** instead of **FaceOverlay** filter.

In order to perform chroma detection, there must be a color calibration stage.
To accomplish this step, at the beginning of the demo, a little square appears
in upper left of the video, as follows:

.. figure:: ../../images/kurento-module-tutorial-chroma-screenshot-01.png
   :align:   center
   :alt:     Chroma calibration stage

   *Chroma calibration stage*

In the first second of the demo, a calibration process is done, by detecting the
color inside that square. When the calibration is finished, the square
disappears and the chroma is substituted with the configured image. Take into
account that this process requires good lighting condition. Otherwise the
chroma substitution will not be perfect. This behavior can be seen in the upper
right corner of the following screenshot:

.. figure:: ../../images/kurento-module-tutorial-chroma-screenshot-02.png
   :align:   center
   :alt:     Chroma filter in action

   *Chroma filter in action*

The media pipeline of this demo is is implemented in the server-side logic as
follows:

.. sourcecode:: java

   private void start(final WebSocketSession session, JsonObject jsonMessage) {
      try {
         // Media Logic (Media Pipeline and Elements)
         UserSession user = new UserSession();
         MediaPipeline pipeline = kurento.createMediaPipeline();
         user.setMediaPipeline(pipeline);
         WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline)
               .build();
         user.setWebRtcEndpoint(webRtcEndpoint);
         users.put(session.getId(), user);

         webRtcEndpoint
               .addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

                  @Override
                  public void onEvent(IceCandidateFoundEvent event) {
                     JsonObject response = new JsonObject();
                     response.addProperty("id", "iceCandidate");
                     response.add("candidate", JsonUtils
                           .toJsonObject(event.getCandidate()));
                     try {
                        synchronized (session) {
                           session.sendMessage(new TextMessage(
                                 response.toString()));
                        }
                     } catch (IOException e) {
                        log.debug(e.getMessage());
                     }
                  }
               });

         ChromaFilter chromaFilter = new ChromaFilter.Builder(pipeline,
               new WindowParam(5, 5, 40, 40)).build();
         String appServerUrl = System.getProperty("app.server.url",
               ChromaApp.DEFAULT_APP_SERVER_URL);
         chromaFilter.setBackground(appServerUrl + "/img/mario.jpg");

         webRtcEndpoint.connect(chromaFilter);
         chromaFilter.connect(webRtcEndpoint);

         // SDP negotiation (offer and answer)
         String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
         String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

         // Sending response back to client
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
