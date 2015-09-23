%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Java Module Tutorial 2 - Chroma Filter
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application consists on a `WebRTC`:term: video communication in mirror
(*loopback*) with a chroma filter element.


For the impatient: running this example
=======================================

First of all, you should install Kurento Media Server to run this demo. Please
visit the :doc:`installation guide <../../installation_guide>` for further
information. In addition, the built-in module ``kms-chroma-6.0`` should be also
installed:

.. sourcecode:: none

    sudo apt-get install kms-chroma-6.0

To launch the application you need to clone the GitHub project where this demo
is hosted and then run the main class, as follows:

.. sourcecode:: none

    git clone https://github.com/Kurento/kurento-tutorial-java.git
    cd kurento-tutorial-java/kurento-chroma
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
:doc:`Magic Mirror <./tutorial-2-magicmirror>` tutorial. In this case, this
demo uses a **Chroma** instead of **FaceOverlay** filter.

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
account that this process requires lighting condition. Otherwise the chroma
substitution will not be perfect. This behavior can be seen in the upper right
corner of the following screenshot:

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
               .addOnIceCandidateListener(new EventListener<OnIceCandidateEvent>() {

                  @Override
                  public void onEvent(OnIceCandidateEvent event) {
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
`pom.xml <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-chroma/pom.xml>`_
is where Kurento dependencies are declared. As the following snippet shows, we
need three dependencies: the Kurento Client Java dependency (*kurento-client*),
the JavaScript Kurento utility library (*kurento-utils*) for the client-side,
and the chroma module (*chroma*):

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
      <dependency>
         <groupId>org.kurento.module</groupId>
         <artifactId>chroma</artifactId>
         <version>|CLIENT_JAVA_VERSION|</version>
      </dependency>
   </dependencies>

.. note::

   We are in active development. You can find the latest versions at `Maven Central <http://search.maven.org/>`_.
