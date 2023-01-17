============================
Writing Kurento Applications
============================

.. contents:: Table of Contents



Global Architecture
===================

Kurento can be used following the architectural principles of the web. That is, creating a multimedia application based on Kurento can be a similar experience to creating a web application using any of the popular web development frameworks.

At the highest abstraction level, web applications have an architecture comprised of three different layers:

- **Presentation layer (client side)**: Here we can find all the application code which is in charge of interacting with end users so that information is represented in a comprehensive way. This usually consists on HTML pages with JavaScript code.

- **Application logic (server side)**: This layer is in charge of implementing the specific functions executed by the application.

- **Service layer (server or Internet side)**: This layer provides capabilities used by the application logic such as databases, communications, security, etc. These services can be hosted in the same server as the application logic, or can be provided by external parties.

Following this parallelism, multimedia applications created using Kurento can also be implemented with the same architecture:

- **Presentation layer (client side)**: Is in charge of multimedia representation and multimedia capture. It is usually based on specific built-in capabilities of the client. For example, when creating a browser-based application, the presentation layer will use capabilities such as the ``<video>`` HTML tag or the :term:`WebRTC` JavaScript APIs.

- **Application logic**: This layer provides the specific multimedia logic. In other words, this layer is in charge of building the appropriate pipeline (by chaining the desired Media Elements) that the multimedia flows involved in the application will need to traverse.

- **Service layer**: This layer provides the multimedia services that support the application logic such as media recording, media ciphering, etc. The Kurento Media Server (i.e. the specific :term:`Media Pipeline` of :term:`Media Elements <Media Element>`) is in charge of this layer.

The interesting aspect of this discussion is that, as happens with web development, Kurento applications can place the Presentation layer at the client side and the Service layer at the server side. However the Application logic, in both cases, can be located at either of the sides or even distributed between them. This idea is represented in the following picture:

.. figure:: ../images/Applications_Layered_Architecture.png
   :align:  center
   :alt:    Layered architecture of web and multimedia applications

   *Layered architecture of web and multimedia applications. Applications created using Kurento (right) can be similar to standard Web applications (left). Both types of applications may choose to place the application logic at the client or at the server code.*

This means that Kurento developers can choose to include the code creating the specific media pipeline required by their applications at the client side (using a suitable :doc:`Kurento Client </features/kurento_client>` or directly with :doc:`Kurento Protocol </features/kurento_protocol>`) or can place it at the server side.

Both options are valid but each of them implies different development styles. Having said this, it is important to note that in the web developers usually tend to maintain client side code as simple as possible, bringing most of their application logic to the server. Reproducing this kind of development experience is the most usual way of using Kurento.

.. note::

   In the following sections it is considered that all Kurento handling is done at the server side. Although this is the most common way of using Kurento, is important to note that all multimedia logic can be implemented at the client with the **Kurento JavaScript Client**.



Application Architecture
========================

Kurento, as most multimedia communication technologies out there, is built using two layers (called *planes*) to abstract key functions in all interactive communication systems:

- **Signaling Plane**. The parts of the system in charge of the management of communications, that is, the modules that provides functions for media negotiation, QoS parametrization, call establishment, user registration, user presence, etc. are conceived as forming part of the :term:`Signaling Plane`.

- **Media Plane**. Functionalities such as media transport, media encoding/decoding and media processing make the  :term:`Media Plane`, which takes care of handling the media. The distinction comes from the telephony differentiation between the handling of voice and the handling of meta-information such as tone, billing, etc.

The following figure shows a conceptual representation of the high level architecture of Kurento:

.. figure:: ../images/Architecture.png
   :alt: Kurento Architecture
   :align: center

   *Kurento Architecture. Kurento architecture follows the traditional separation between signaling and media planes.*

The **right side** of the picture shows the application, which is in charge of the signaling plane and contains the business logic and connectors of the particular multimedia application being deployed. It can be build with any programming technology like Java, Node.js, PHP, Ruby, .NET, etc. The application can use mature technologies such as :term:`HTTP` and :term:`SIP` Servlets, Web Services, database connectors, messaging services, etc. Thanks to this, this plane provides access to the multimedia signaling protocols commonly used by end-clients such as :term:`SIP`, RESTful and raw HTTP based formats, SOAP, RMI, CORBA or JMS. These signaling protocols are used by client side of applications to command the creation of media sessions and to negotiate their desired characteristics on their behalf. Hence, this is the part of the architecture, which is in contact with application developers and, for this reason, it needs to be designed pursuing simplicity and flexibility.

On the **left side**, we have the Kurento Media Server, which implements the media plane capabilities providing access to the low-level media features: media transport, media encoding/decoding, media transcoding, media mixing, media processing, etc. The Kurento Media Server must be capable of managing the multimedia streams with minimal latency and maximum throughput. Hence the Kurento Media Server must be optimized for efficiency.



Communicating client, server and Kurento
----------------------------------------

As can be observed in the figure below, a Kurento application involves interactions
among three main modules:

- **Client Application**: Involves the native multimedia capabilities of the client platform plus the specific client-side application logic. It can use Kurento Clients designed for client platforms (for example, Kurento JavaScript Client).

- **Application Server**: Involves an application server and the server-side application logic. It can use Kurento Clients designed to server platforms (for example, Kurento Java Client for *Java EE* and Kurento JavaScript Client for *Node.js*).

- **Kurento Media Server**: Receives commands to create specific multimedia capabilities (i.e. specific pipelines adapted to the needs of the application).

The interactions maintained among these modules depend on the specifics of each application. However, in general, for most applications can be reduced to the following conceptual scheme:

.. figure:: ../images/Generic_interactions.png
   :align:  center
   :alt:    Main interactions between architectural modules

   *Main interactions between architectural modules. These occur in two phases: negotiation and media exchange. Remark that the color of the different arrows and boxes is aligned with the architectural figures presented above.
   For example, orange arrows show exchanges belonging to the signaling plane, blue arrows show exchanges belonging to the Kurento Protocol, red boxes are associated to the Kurento Media Server, and green boxes with the application.*



1. Media negotiation phase (signaling)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

At a first stage, a client (a browser in a computer, a mobile application, etc.) issues a message to the application requesting some kind of multimedia capability. This message can be implemented with any protocol (HTTP, WebSocket, SIP, etc.). For instance, that request could ask for the visualization of a given video clip.

When the application receives the request, if appropriate, it will carry out the specific server side application logic, which can include Authentication, Authorization and Accounting (AAA), CDR generation, consuming some type of web service, etc.

After that, the application processes the request and, according to the specific instructions programmed by the developer, commands Kurento Media Server to instantiate the suitable Media Elements and to chain them in an appropriate Media Pipeline. Once the pipeline has been created successfully, Kurento Media Server responds accordingly and the application forwards the successful response to the client, showing it how and where the media service can be reached.

During the above mentioned steps no media data is really exchanged. All the interactions have the objective of negotiating the *whats*, *hows*, *wheres* and *whens* of the media exchange. For this reason, we call it the negotiation phase. Clearly, during this phase only signaling protocols are involved.



2. Media exchange phase
~~~~~~~~~~~~~~~~~~~~~~~

After the signaling part, a new phase starts with the aim to produce the actual media exchange. The client addresses a request for the media to the Kurento Media Server using the information gathered during the negotiation phase.

Following with the video-clip visualization example mentioned above, the browser will send a GET request to the IP address and port of the Kurento Media Server where the clip can be obtained and, as a result, an HTTP reponse containing the media will be received.

Following the discussion with that simple example, one may wonder why such a complex scheme for just playing a video, when in most usual scenarios clients just send the request to the appropriate URL of the video without requiring any negotiation. The answer is straightforward. Kurento is designed for media applications involving complex media processing. For this reason, we need to establish a two-phase mechanism enabling a negotiation before the media exchange. The price to pay is that simple applications, such as one just downloading a video, also need to get through these phases. However, the advantage is that when creating more advanced services the same simple philosophy will hold. For example, if we want to add Augmented Reality or Computer Vision features to that video-clip, we just need to create the appropriate pipeline holding the desired Media Elements during the negotiation phase. After that, from the client perspective, the processed clip will be received as any other video.



Real time WebRTC applications with Kurento
------------------------------------------

The client communicates its desired media capabilities through an :term:`SDP Offer/Answer` negotiation. Hence, Kurento is able to instantiate the appropriate WebRTC endpoint, and to require it to generate an SDP Answer based on its own capabilities and on the SDP Offer. When the SDP Answer is obtained, it is given back to the client and the media exchange can be started. The interactions among the different modules are summarized in the following picture:

.. figure:: ../images/RTC_session.png
   :align: center
   :alt:   Interactions in a WebRTC session

   *Interactions in a WebRTC session. During the negotiation phase, an SDP Offer is sent to KMS, requesting the capabilities of the client. As a result, Kurento Media Server generates an SDP Answer that can be used by the client for establishing the media exchange.*

The application developer is able to create the desired pipeline during the negotiation phase, so that the real-time multimedia stream is processed accordingly to the application needs.

As an example, imagine that you want to create a WebRTC application recording the media received from the client and augmenting it so that if a human face is found, a hat will be rendered on top of it. This pipeline is schematically shown in the figure below, where we assume that the Filter element is capable of detecting the face and adding the hat to it.

.. figure:: ../images/RTC_session_pipeline.png
   :align: center
   :alt:   Example pipeline for a WebRTC session

   *Example pipeline for a WebRTC session. A WebRtcEndpoint is connected to a RecorderEndpoint storing the received media stream and to an Augmented Reality filter, which feeds its output media stream back to the client. As a result, the end user will receive its own image filtered (e.g. with a hat added onto her head) and the stream will be recorded and made available for further recovery into a repository (e.g. a file).*



.. _writing-app-pipelines:

Media Plane
===========

From the application developer perspective, Media Elements are like *Lego* pieces: you just need to take the elements needed for an application and connect them, following the desired topology. In Kurento jargon, a graph of connected media elements is called a **Media Pipeline**. Hence, when creating a pipeline, developers need to determine the capabilities they want to use (the Media Elements) and the topology determining which Media Element provides media to which other Media Elements (the connectivity).

.. figure:: /images/example-pipeline-browser-recorder.png
   :align: center
   :alt: Simple Example of a Media Pipeline

   *Simple Example of a Media Pipeline*

The connectivity is controlled through the *connect* primitive, exposed on all Kurento Client APIs.

This primitive is always invoked in the element acting as source and takes as argument the sink element following this scheme:

.. code-block:: java

   sourceMediaElement.connect(sinkMediaElement)

For example, if you want to create an application recording WebRTC streams into the file system, you'll need two media elements: *WebRtcEndpoint* and *RecorderEndpoint*. When a client connects to the application, you will need to instantiate these media elements making the stream received by the
*WebRtcEndpoint* (which is capable of receiving WebRTC streams) to be fed to the *RecorderEndpoint* (which is capable of recording media streams into the file system). Finally you will need to connect them so that the stream received by the former is transferred into the latter:

.. code-block:: java

   WebRtcEndpoint.connect(RecorderEndpoint)

To simplify the handling of WebRTC streams in the client-side, Kurento provides an utility called *WebRtcPeer*. Nevertheless, the standard WebRTC API (*getUserMedia*, *RTCPeerConnection*, and so on) can also be used to connect to *WebRtcEndpoints*. For further information please visit the :doc:`Tutorials section </user/tutorials>`.
