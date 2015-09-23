%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Java Module Tutorial 3 - Crowd Detector Filter
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application consists on a `WebRTC`:term: video communication in mirror
(*loopback*) with a crowd detector filter. This filter detects people
agglomeration in video streams.


For the impatient: running this example
=======================================

First of all, you should install Kurento Media Server to run this demo. Please
visit the :doc:`installation guide <../../installation_guide>` for further
information. In addition, the built-in module ``kms-crowddetector-6.0`` should
be also installed:

.. sourcecode:: none

    sudo apt-get install kms-crowddetector-6.0

To launch the application you need to clone the GitHub project where this demo
is hosted and then run the main class, as follows:

.. sourcecode:: none

    git clone https://github.com/Kurento/kurento-tutorial-java.git
    cd kurento-tutorial-java/kurento-crowddetector
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
a crowd in a WebRTC stream.

The interface of the application (an HTML web page) is composed by two HTML5
video tags: one for the video camera stream (the local client-side stream) and
other for the mirror (the remote stream). The video camera stream is sent to
Kurento Media Server, which processes and sends it back to the client as a
remote stream. To implement this, we need to create a `Media Pipeline`:term:
composed by the following `Media Element`:term: s:

.. figure:: ../../images/kurento-module-tutorial-crowddetector-pipeline.png
   :align:   center
   :alt:     WebRTC with crowdDetector filter Media Pipeline

   *WebRTC with crowdDetector filter Media Pipeline*

The complete source code of this demo can be found in
`GitHub <https://github.com/Kurento/kurento-tutorial-java/tree/master/kurento-crowddetector>`_.

This example is a modified version of the
:doc:`Magic Mirror <./tutorial-2-magicmirror>` tutorial. In this case, this
demo uses a **CrowdDetector** instead of **FaceOverlay** filter.

To setup a ``CrowdDetectorFilter``, first we need to define one or more
*region of interests* (ROIs). A ROI delimits the zone within the video stream
in which crowd are going to be tracked. To define a ROI, we need to configure
at least three points. These points are defined in relative terms (0 to 1) to
the video width and height.

``CrowdDetectorFilter`` performs two actions in the defined ROIs. On the one
hand, the detected crowd are colored over the stream. On the other hand,
different events are raised to the client.

To understand crowd coloring, we can take a look to an screenshot of a running
example of ``CrowdDetectorFilter``. In the picture below, we can see that there
are two ROIs (bounded with white lines in the video). On these ROIs, we can see
two different colors over the original video stream: red zones are drawn over
detected static crowds (or moving slowly). Blue zones are drawn over the
detected crowds moving fast.

.. figure:: ../../images/kurento-module-tutorial-crowd-screenshot-01.png
   :align:   center
   :alt:     Crowd detection sample

   *Crowd detection sample*

Regarding crowd events, there are three types of events, namely:

* CrowdDetectorFluidityEvent. Event raised when a certain level of fluidity is
  detected in a ROI. Fluidity can be seen as the level of general movement in a
  crowd.

* CrowdDetectorOccupancyEvent. Event raised when a level of occupancy is
  detected in a ROI. Occupancy can be seen as the level of agglomeration in
  stream.

* CrowdDetectorDirectionEvent. Event raised when a movement direction is
  detected in a ROI by a crowd.

Both fluidity as occupancy are quantified in a relative metric from 0 to 100%.
Then, both attributes are qualified into three categories: i) Minimum (min);
ii) Medium (med); iii) Maximum (max).

Regarding direction, it is quantified as an angle (0-360º), where 0 is the
direction from the central point of the video to the top (i.e., north), 90
correspond to the direction to the right (east), 180 is the south, and finally
270 is the west.

With all these concepts, now we can check out the Java server-side code of this
demo. As depicted in the snippet below, we create a ROI by adding
``RelativePoint`` instances to a list. Each ROI is then stored into a list of
``RegionOfInterest`` instances.

Then, each ROI should be configured. To do that, we have the following methods:

 * ``setFluidityLevelMin``: Fluidity level (0-100%) for the category
   *minimum*.
 * ``setFluidityLevelMed``: Fluidity level (0-100%) for the category *medium*.
 * ``setFluidityLevelMax``: Fluidity level (0-100%) for the category
   *maximum*.
 * ``setFluidityNumFramesToEvent``: Number of consecutive frames detecting a
   fluidity level to rise a  event.
 * ``setOccupancyLevelMin``:  Occupancy level (0-100%) for the category
   *minimum*.
 * ``setOccupancyLevelMed``: Occupancy level (0-100%) for the category
   *medium*.
 * ``setOccupancyLevelMax``: Occupancy level (0-100%) for the category
   *maximum*.
 * ``setOccupancyNumFramesToEvent``: Number of consecutive frames detecting a
   occupancy level to rise a event.
 * ``setSendOpticalFlowEvent``: Boolean value that indicates whether or not
   directions events are going to be tracked by the filter. Be careful with
   this feature, since it is very demanding in terms of resource usage (CPU,
   memory) in the media server. Set to true this parameter only when you are
   going to need directions events in your client-side.
 * ``setOpticalFlowNumFramesToEvent``: Number of consecutive frames detecting
   a direction level to rise a event.
 * ``setOpticalFlowNumFramesToReset``: Number of consecutive frames detecting
   a occupancy level in which the counter is reset.
 * ``setOpticalFlowAngleOffset``: Counterclockwise offset of the angle. This
   parameters is useful to move the default axis for directions (0º=north,
   90º=east, 180º=south, 270º=west).

All in all, the media pipeline of this demo is is implemented as follows:

.. sourcecode:: java

   // Media Logic (Media Pipeline and Elements)
   MediaPipeline pipeline = kurento.createMediaPipeline();
   pipelines.put(session.getId(), pipeline);

   WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline)
         .build();
   webRtcEndpoint
      .addOnIceCandidateListener(new EventListener<OnIceCandidateEvent>() {
         @Override
         public void onEvent(OnIceCandidateEvent event) {
         JsonObject response = new JsonObject();
         response.addProperty("id", "iceCandidate");
         response.add("candidate",
            JsonUtils.toJsonObject(event.getCandidate()));
         try {
            synchronized (session) {
            session.sendMessage(new TextMessage(response
               .toString()));
            }
         } catch (IOException e) {
            log.debug(e.getMessage());
         }
         }
      });

   List<RegionOfInterest> rois = new ArrayList<>();
   List<RelativePoint> points = new ArrayList<RelativePoint>();

   points.add(new RelativePoint(0, 0));
   points.add(new RelativePoint(0.5F, 0));
   points.add(new RelativePoint(0.5F, 0.5F));
   points.add(new RelativePoint(0, 0.5F));

   RegionOfInterestConfig config = new RegionOfInterestConfig();

   config.setFluidityLevelMin(10);
   config.setFluidityLevelMed(35);
   config.setFluidityLevelMax(65);
   config.setFluidityNumFramesToEvent(5);
   config.setOccupancyLevelMin(10);
   config.setOccupancyLevelMed(35);
   config.setOccupancyLevelMax(65);
   config.setOccupancyNumFramesToEvent(5);
   config.setSendOpticalFlowEvent(false);
   config.setOpticalFlowNumFramesToEvent(3);
   config.setOpticalFlowNumFramesToReset(3);
   config.setOpticalFlowAngleOffset(0);

   rois.add(new RegionOfInterest(points, config, "roi0"));

   CrowdDetectorFilter crowdDetectorFilter = new CrowdDetectorFilter.Builder(
         pipeline, rois).build();

   webRtcEndpoint.connect(crowdDetectorFilter);
   crowdDetectorFilter.connect(webRtcEndpoint);

   // addEventListener to crowddetector
   crowdDetectorFilter.addCrowdDetectorDirectionListener(
      new EventListener<CrowdDetectorDirectionEvent>() {
      @Override
      public void onEvent(CrowdDetectorDirectionEvent event) {
         JsonObject response = new JsonObject();
         response.addProperty("id", "directionEvent");
         response.addProperty("roiId", event.getRoiID());
         response.addProperty("angle",
         event.getDirectionAngle());
         try {
            session.sendMessage(new TextMessage(response
            .toString()));
         } catch (Throwable t) {
            sendError(session, t.getMessage());
         }
      }
         });

   crowdDetectorFilter.addCrowdDetectorFluidityListener(
      new EventListener<CrowdDetectorFluidityEvent>() {
      @Override
      public void onEvent(CrowdDetectorFluidityEvent event) {
         JsonObject response = new JsonObject();
         response.addProperty("id", "fluidityEvent");
         response.addProperty("roiId", event.getRoiID());
         response.addProperty("level",
         event.getFluidityLevel());
         response.addProperty("percentage",
         event.getFluidityPercentage());
         try {
            session.sendMessage(new TextMessage(response
            .toString()));
         } catch (Throwable t) {
            sendError(session, t.getMessage());
         }
      }
         });

   crowdDetectorFilter.addCrowdDetectorOccupancyListener(
      new EventListener<CrowdDetectorOccupancyEvent>() {
      @Override
      public void onEvent(CrowdDetectorOccupancyEvent event) {
         JsonObject response = new JsonObject();
         response.addProperty("id", "occupancyEvent");
         response.addProperty("roiId", event.getRoiID());
         response.addProperty("level",
         event.getOccupancyLevel());
         response.addProperty("percentage",
         event.getOccupancyPercentage());
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

   webRtcEndpoint.gatherCandidates();

Dependencies
============

This Java Spring application is implemented using `Maven`:term:. The relevant
part of the
`pom.xml <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-crowddetector/pom.xml>`_
is where Kurento dependencies are declared. As the following snippet shows, we
need three dependencies: the Kurento Client Java dependency (*kurento-client*),
the JavaScript Kurento utility library (*kurento-utils*) for the client-side,
and the crowd detector module (*crowddetector*):

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
         <artifactId>crowddetector</artifactId>
         <version>|CLIENT_JAVA_VERSION|</version>
      </dependency>
   </dependencies>

.. note::

   We are in active development. You can find the latest versions at `Maven Central <http://search.maven.org/>`_.
