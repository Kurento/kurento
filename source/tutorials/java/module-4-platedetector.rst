%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Java Module Tutorial 4 - Plate Detector Filter
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application consists on a `WebRTC`:term: video communication in mirror
(*loopback*) with a plate detector filter element.

For the impatient: running this example
=======================================

First of all, you should install Kurento Media Server to run this demo. Please
visit the :doc:`installation guide <../../installation_guide>` for further
information. In addition, the built-in module ``kms-platedetector`` should be
also installed:

.. sourcecode:: sh

    sudo apt-get install kms-platedetector

To launch the application you need to clone the GitHub project where this demo
is hosted and then run the main class, as follows:

.. sourcecode:: sh

    git clone https://github.com/Kurento/kurento-tutorial-java.git
    cd kurento-tutorial-java/kurento-platedetector
    mvn compile exec:java

The web application starts on port 8080 in the localhost by default. Therefore,
open the URL http://localhost:8080/ in a WebRTC compliant browser (Chrome,
Firefox).

Understanding this example
==========================

This application uses computer vision and augmented reality techniques to detect
a plate in a WebRTC stream on optical character recognition (OCR).

The interface of the application (an HTML web page) is composed by two HTML5
video tags: one for the video camera stream (the local client-side stream) and
other for the mirror (the remote stream). The video camera stream is sent to
Kurento Media Server, which processes and sends it back to the client as a
remote stream. To implement this, we need to create a `Media Pipeline`:term:
composed by the following `Media Element`:term: s:

.. figure:: ../../images/kurento-module-tutorial-platedetector-pipeline.png
   :align:   center
   :alt:     WebRTC with plateDetector filter Media Pipeline

   *WebRTC with plateDetector filter Media Pipeline*

The complete source code of this demo can be found in
`GitHub <https://github.com/Kurento/kurento-tutorial-java/tree/master/kurento-platedetector>`_.

This example is a modified version of the
:doc:`Magic Mirror <./tutorial-1-magicmirror>` tutorial. In this case, this
demo uses a **PlateDetector** instead of **FaceOverlay** filter. An screenshot
of the running example is shown in the following picture:

.. figure:: ../../images/kurento-module-tutorial-plate-screenshot-01.png
   :align:   center
   :alt:     Plate detector demo in action

   *Plate detector demo in action*

The following snippet shows how the media pipeline is implemented in the Java
server-side code of the demo. An important issue in this code is that a
listener is added to the ``PlateDetectorFilter`` object
(``addPlateDetectedListener``). This way, each time a plate is detected in the
stream, a message is sent to the client side. As shown in the screenshot below,
this event is printed in the console of the GUI.

.. sourcecode:: java

   private void start(final WebSocketSession session, JsonObject jsonMessage) {
      try {
         // Media Logic (Media Pipeline and Elements)
         MediaPipeline pipeline = kurento.createMediaPipeline();
         pipelines.put(session.getId(), pipeline);

         WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline)
               .build();
         PlateDetectorFilter plateDetectorFilter = new PlateDetectorFilter.Builder(
               pipeline).build();

         webRtcEndpoint.connect(plateDetectorFilter);
         plateDetectorFilter.connect(webRtcEndpoint);

         plateDetectorFilter
               .addPlateDetectedListener(new EventListener<PlateDetectedEvent>() {
                  @Override
                  public void onEvent(PlateDetectedEvent event) {
                     JsonObject response = new JsonObject();
                     response.addProperty("id", "plateDetected");
                     response.addProperty("plate", event.getPlate());
                     try {
                        session.sendMessage(new TextMessage(response
                              .toString()));
                     } catch (Throwable t) {
                        sendError(session, t.getMessage());
                     }
                  }
               });

         // SDP negotiation (offer and answer)
         String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
         String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

         // Sending response back to client
         JsonObject response = new JsonObject();
         response.addProperty("id", "startResponse");
         response.addProperty("sdpAnswer", sdpAnswer);
         session.sendMessage(new TextMessage(response.toString()));
      } catch (Throwable t) {
         sendError(session, t.getMessage());
      }
   }

Dependencies
============

This Java Spring application is implemented using `Maven`:term:. The relevant
part of the *pom.xml* is where Kurento dependencies are declared. As the
following snippet shows, we need three dependencies: the Kurento Client Java
dependency (*kurento-client*), the JavaScript Kurento utility library
(*kurento-utils*) for the client-side, and the plate detector module
(*platedetector*):

.. sourcecode:: xml 

   <dependencies> 
      <dependency>
         <groupId>org.kurento</groupId>
         <artifactId>kurento-client</artifactId>
         <version>[5.0.0,6.0.0)</version>
      </dependency> 
      <dependency> 
         <groupId>org.kurento</groupId>
         <artifactId>kurento-utils-js</artifactId> 
         <version>[5.0.0,6.0.0)</version>
      </dependency>
      <dependency>
         <groupId>org.kurento.module</groupId>
         <artifactId>platedetector</artifactId>
         <version>[1.0.0,2.0.0)</version>
      </dependency>
   </dependencies>

Kurento framework uses `Semantic Versioning`:term: for releases. Notice that
ranges (``[5.0.0,6.0.0)`` for *kurento-client* and *kurento-utils-js*,  and
``[1.0.0,2.0.0)`` for *platedetector*) downloads the latest version of Kurento
artifacts from Maven Central.

