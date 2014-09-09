.. _Introducing_Kurento:

%%%%%%%%%%%%%%%%%%%
Introducing Kurento
%%%%%%%%%%%%%%%%%%%

WebRTC media servers
====================
WebRTC has been conceived as a peer-to-peer technology where browsers can directly
communicate without the mediation of any kind of infrastructure. This model is
enough for creating basic applications but features such as group communications,
media stream recording, media broadcasting or media transcoding are difficult
to implement on top of it. For this reason, many applications require using
a media server.

Conceptually, a WebRTC media server is just a kind of “multimedia middleware”
(it is in the middle of the communicating peers) where media traffic pass
through when moving from source to destinations. Media servers are
capable of processing media streams and offering different types including
groups communications (distributing the media stream one peer
generates among several receivers), mixing (transforming several incoming stream into one single composite stream), transcoding (adapting codecs and formats
between incompatible clients), recording (storing in a persistent way the
media exchanged among peers), etc.

*Picture showing media server value goes here*

Kurento Media Server
====================

At the heart of the Kurento architecture there is a media server called the
**Kurento Media Server (KMS)**. Kurento Media Server is based on pluggable media
processing capabilities meaning that any of its provided features is a pluggable
module that can be activated or deactivated. Moreover, developers can 
seamlessly create additional modules extending Kurento Media Server with new functionalities which can be plugged dynamically.

Kurento Media Server provides, out of the box, group communications, mixing,
transcoding, recording and playing. In addition, it also provides advanced
modules for media processing including computer vision, augmented reality,
alpha blending and much more.

*Picture showing KMS value goes here*

Kurento Clients and APIs
========================

Those capabilities are exposed by libraries called **Kurento Clients** to 
application developers. Kurento Client's API is based on the concept of 
**Media Element**. A Media Element holds a specific media capability. For 
example, the media element called *WebRtcEndpoint* holds the capability of 
sending and receiving WebRTC media streams, the media element called 
*RecorderEndpoint* has the capability of recording into the file system any
media streams it receives, the *FaceOverlayFilter* detects faces on the 
exchanged video streams and adds a specific overlaid image on top of them, etc. 
Kurento exposes a rich toolbox of media elements as part of its APIs.

Creating applications with Kurento
==================================

From the application developer perspective, Media Elements are like *Lego*
pieces: you just need to take the elements needed for an application and
connect them following the desired topology. In Kurento jargon, a graph of
connected media elements is called a **Media Pipeline**. Hence, when creating
a pipeline, developers need to determine the capabilities they want to use
(the media elements) and the topology determining which media elements
provide media to which other media elements (the connectivity). The
connectivity is controlled through the *connect* primitive, exposed on all
Kurento Client APIs. This primitive is always invoked in the element acting as
source and takes as argument the sink element following this scheme:
*sourceMediaElement.connect(sinkMediaElement)* 

For example, if you want to create an application recording WebRTC streams into
the file system, you'll need two media elements: *WebRtcEndpoint* and
*RecorderEndpoint*. When a client connects to the application, you will need to
instantiate these media elements making the stream received by the
*WebRtcEndpoint* (which is capable of receiving WebRTC streams) to be feed to the
*RecorderEndpoint* (which is capable of recording media streams into the file system).
Finally you will need to connect them so that the stream received by the former
is fed into the later: *WebRtcEndpoint.connect(RecorderEndpoint)*

To better understand theses concepts it is recommended to take a look to
:doc:`Kurento API section <../../mastering/kurento_API>` section.

