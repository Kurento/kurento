========
Glossary
========

This is a glossary of terms that often appear in discussion about multimedia transmissions. Some of the terms are specific to :term:`GStreamer` or :term:`Kurento`, and most of them are described and linked to their RFC, W3C or Wikipedia documents.

.. glossary::

   Agnostic media
       One of the big problems of media is that the number of variants of video and audio codecs, formats and variants quickly creates high complexity in heterogeneous applications. So Kurento developed the concept of an automatic converter of media formats that enables development of *agnostic* elements. Whenever a media element's source is connected to another media element's sink, the Kurento framework verifies if media adaption and transcoding is necessary and, if needed, it transparently incorporates the appropriate transformations making possible the  chaining of the two elements into the resulting :term:`Pipeline <Media Pipeline>`.

   AVI
       Audio Video Interleaved, known by its initials AVI, is a multimedia container format introduced by Microsoft in November 1992 as part of its Video for Windows technology. AVI files can contain both audio and video data in a file container that allows synchronous audio-with-video playback. AVI is a derivative of the Resource Interchange File Format (RIFF).

       .. seealso::

          :wikipedia:`Audio Video Interleave`

          :wikipedia:`Resource Interchange File Format`

   Bower
       `Bower <https://bower.io/>`__ is a package manager for the web. It offers a generic solution to the problem of front-end package management, while exposing the package dependency model via an API that can be consumed by a build stack.

   Builder Pattern
       The builder pattern is an object creation software design pattern whose intention is to find a solution to the telescoping constructor anti-pattern. The telescoping constructor anti-pattern occurs when the increase of object constructor parameter combination leads to an exponential list of constructors. Instead of using numerous constructors, the builder pattern uses another object, a builder, that receives each initialization parameter step by step and then returns the resulting constructed object at once.

       .. seealso::

          :wikipedia:`Builder pattern`

   CORS
       Cross-origin resource sharing is a mechanism that allows JavaScript code on a web page to make XMLHttpRequests to different domains than the one the JavaScript originated from. It works by adding new HTTP headers that allow servers to serve resources to permitted origin domains. Browsers support these headers and enforce the restrictions they establish.

       .. seealso::

          :wikipedia:`Cross-origin resource sharing`

          `enable-cors.org <https://enable-cors.org/>`__
              Information on the relevance of CORS and how and when to enable it.

   DOM
       Document Object Model is a cross-platform and language-independent convention for representing and interacting with objects in HTML, XHTML and XML documents.

   EOS
       End Of Stream is an event that occurs when playback of some media source has finished. In Kurento, some elements will raise an ``EndOfStream`` event.

   GStreamer
       `GStreamer <https://gstreamer.freedesktop.org/>`__ is a pipeline-based multimedia framework written in the C programming language.

   H.264
       A Video Compression Format. The H.264 standard can be viewed as a "family of standards" composed of a number of profiles. Each specific decoder deals with at least one such profiles, but not necessarily all.

       .. seealso::

          :wikipedia:`H.264/MPEG-4 AVC`

          :rfc:`6184`
              RTP Payload Format for H.264 Video (This RFC obsoletes :rfc:`3984`).

   HTTP
       The Hypertext Transfer Protocol (HTTP) is an application protocol for distributed, collaborative, hypermedia information systems. HTTP is the foundation of data communication for the World Wide Web.

       .. seealso::

          :wikipedia:`Hypertext Transfer Protocol`

          :rfc:`2616`
              Hypertext Transfer Protocol -- HTTP/1.1

   ICE
       Interactive Connectivity Establishment (ICE) is a technique used to achieve :term:`NAT Traversal`. ICE makes use of the :term:`STUN` protocol and its extension, :term:`TURN`. ICE can be used by any aplication that makes use of the SDP Offer/Answer model..

       .. seealso::

          :wikipedia:`Interactive Connectivity Establishment`

          :rfc:`5245`
              Interactive Connectivity Establishment (ICE): A Protocol for Network Address Translator (NAT) Traversal for Offer/Answer Protocols.

   IMS
       IP Multimedia Subsystem (IMS) is the 3GPP's Mobile Architectural Framework for delivering IP Multimedia Services in 3G (and beyond) Mobile Networks.

       .. seealso::

          :wikipedia:`IP Multimedia Subsystem`

          :wikipedia:`3GPP`

          :rfc:`3574`
              Transition Scenarios for 3GPP Networks.

   jQuery
       `jQuery <https://jquery.com/>`__ is a cross-platform JavaScript library designed to simplify the client-side scripting of HTML.

   JSON
       `JSON <https://json.org/>`__ (JavaScript Object Notation) is a lightweight data-interchange format. It is designed to be easy to understand and write for humans and easy to parse for machines.

   JSON-RPC
       `JSON-RPC <https://www.jsonrpc.org/>`__ is a simple remote procedure call protocol encoded in JSON. JSON-RPC allows for notifications and for multiple calls to be sent to the server which may be answered out of order.

   Kurento
       `Kurento <https://www.kurento.org/>`__ is a platform for the development of multimedia-enabled applications. Kurento is the Esperanto term for the English word 'stream'. We chose this name because we believe the Esperanto principles are inspiring for what the multimedia community needs: simplicity, openness and universality. Some components of Kurento are the :term:`Kurento Media Server`, the :term:`Kurento API`, the :term:`Kurento Protocol`, and the :term:`Kurento Client`.

   Kurento API
        An object oriented API to create media pipelines to control media. It can be seen as and interface to Kurento Media Server. It can be used from the Kurento Protocol or from Kurento Clients.

   Kurento Client
        A programming library (Java or JavaScript) used to control an instance of **Kurento Media Server** from an application. For example, with this library, any developer can create a web application that uses Kurento Media Server to receive audio and video from the user web browser, process it and send it back again over Internet. The Kurento Client libraries expose the :term:`Kurento API` to application developers.

   Kurento Protocol
        Communication between KMS and clients by means of :term:`JSON-RPC` messages. It is based on :term:`WebSocket` that uses :term:`JSON-RPC` v2.0 messages for making requests and sending responses.

   KMS
   Kurento Media Server
        **Kurento Media Server** is the core element of Kurento since it responsible for media transmission, processing, loading and recording.

   Maven
       `Maven <https://maven.apache.org/>`__ is a build automation tool used primarily for Java projects.

   Media Element
       A **Media Element** is a module that encapsulates a specific media capability.  For example **RecorderEndpoint**, **PlayerEndpoint**, etc.

   Media Pipeline
       A :index:`Media Pipeline <single: Media; Pipeline>` is a chain of media elements, where the output stream generated by one element (source) is fed into one or more other elements input streams (sinks). Hence, the pipeline represents a "machine" capable of performing a sequence of operations over a stream.

   Media Plane
       In a traditional IP Multimedia Subsystem, the handling of media is conceptually splitted in two layers. The layer that handles the media itself -with functionalities such as media transport, encoding/decoding, and processing- is called :index:`Media Plane <single: Plane; Media>`.

       .. seealso::

          :wikipedia:`IP Multimedia Subsystem`

          :term:`Signaling Plane`

   MP4
       MPEG-4 Part 14 or MP4 is a digital multimedia format most commonly used to store video and audio, but can also be used to store other data such as subtitles and still images.

       .. seealso::

          :wikipedia:`MPEG-4 Part 14`

   Multimedia
       Multimedia is concerned with the computer controlled integration of text, graphics, video, animation, audio, and any other media where information can be represented, stored, transmitted and processed digitally.
       There is a temporal relationship between many forms of media, for instance audio, video and animations. There 2 are forms of problems involved in

           * Sequencing within the media, i.e. playing frames in correct order or time frame.
           * Synchronization, i.e. inter-media scheduling. For example, keeping video and audio synchronized or displaying captions or subtitles in the required intervals.

       .. seealso::

          :wikipedia:`Multimedia`

   Multimedia container format
       Container or wrapper formats are meta-file formats whose specification describes how different data elements and metadata coexist in a computer file.
       Simpler multimedia container formats can contain different types of audio formats, while more advanced container formats can support multiple audio and video streams, subtitles, chapter-information, and meta-data, along with the synchronization information needed to play back the various streams together.
       In most cases, the file header, most of the  metadata and the synchro chunks are specified by the container format.

       .. seealso::

          :wikipedia:`Multimedia container format <en,Digital_container_format#Multimedia_container_formats>`

   NAT
   Network Address Translation
       Network address translation (NAT) is the technique of modifying network address information in Internet Protocol (IP) datagram packet headers while they are in transit across a traffic routing device for the purpose of remapping one IP address space into another.

       .. seealso::

          :wikipedia:`Network address translation`

          :doc:`/knowledge/nat`
              Entry in our Knowledge Base.

          `How Network Address Translation Works <https://computer.howstuffworks.com/nat.htm>`__ (`archive <https://web.archive.org/web/20200213082726/https://computer.howstuffworks.com/nat.htm>`__)
              A comprehensive description of NAT and its mechanics.

   NAT-T
   NAT Traversal
       NAT traversal (sometimes abbreviated as NAT-T) is a general term for techniques that establish and maintain Internet protocol connections traversing network address translation (NAT) gateways, which break end-to-end connectivity. Intercepting and modifying traffic can only be performed transparently in the absence of secure encryption and authentication.

       .. seealso::

          :doc:`/knowledge/nat`
              Entry in our Knowledge Base.

   Node.js
       `Node.js <https://nodejs.org/>`__ is a cross-platform runtime environment for server-side and networking applications. Node.js applications are written in JavaScript, and can be run within the Node.js runtime on OS X, Microsoft Windows and Linux with no changes.

   npm
       `npm <https://www.npmjs.org/>`__ is the official package manager for :term:`Node.js`.

   OpenCV
       OpenCV (Open Source Computer Vision Library) is a BSD-licensed open source computer vision and machine learning software library. OpenCV aims to provide a common infrastructure for computer vision applications and to accelerate the use of machine perception.

   Pad, Media
       A :index:`Media Pad <single: Media; Pad>` is is an element's interface with the outside world. Data streams from the MediaSource pad to another element's MediaSink pad.

       .. seealso::

          `GStreamer Pad <https://gstreamer.freedesktop.org/documentation/application-development/basics/pads.html>`__
              Definition of the Pad structure in GStreamer.

   QR
       QR code (Quick Response Code) is a type of two-dimensional barcode. that became popular in the mobile phone industry due to its fast readability and greater storage capacity compared to standard UPC barcodes.

       .. seealso::

          :wikipedia:`QR code`

   REMB
       **Receiver Estimated Maximum Bitrate** (REMB) is a type of RTCP feedback message that a RTP receiver can use to inform the sender about what is the estimated reception bandwidth currently available for the stream itself. Upon reception of this message, the RTP sender will be able to adjust its own video bitrate to the conditions of the network. This message is a crucial part of the *Google Congestion Control* (GCC) algorithm, which provides any RTP session with the ability to adapt in cases of network congestion.

       The *GCC* algorithm is one of several proposed algorithms that have been proposed by an IETF Working Group named *RTP Media Congestion Avoidance Techniques* (RMCAT).

       .. seealso::

          `What is RMCAT congestion control, and how will it affect WebRTC? <https://blog.mozilla.org/webrtc/what-is-rmcat-congestion-control/>`__ (`archive <https://web.archive.org/web/20200219134737/https://blog.mozilla.org/webrtc/what-is-rmcat-congestion-control/>`__)

          `draft-alvestrand-rmcat-remb <https://tools.ietf.org/html/draft-alvestrand-rmcat-remb-03>`__
              RTCP message for Receiver Estimated Maximum Bitrate.

          `draft-ietf-rmcat-gcc <https://tools.ietf.org/html/draft-ietf-rmcat-gcc-02>`__
              A Google Congestion Control Algorithm for Real-Time Communication.

   REST
       Representational state transfer (REST) is an architectural style consisting of a coordinated set of constraints applied to components, connectors, and data elements, within a distributed hypermedia system. The term representational state transfer was introduced and defined in 2000 by Roy Fielding in his `doctoral dissertation <https://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm>`__.

       .. seealso::

          :wikipedia:`Representational state transfer`

   RTCP
       The RTP Control Protocol (RTCP) is a sister protocol of the :term:`RTP`, that provides out-of-band statistics and control information for an RTP flow.

       .. seealso::

          :wikipedia:`RTP Control Protocol`

          :rfc:`3605`
              Real Time Control Protocol (RTCP) attribute in Session Description Protocol (SDP).

   RTP
       Real-time Transport Protocol (RTP) is a standard packet format designed for transmitting audio and video streams on IP networks. It is used in conjunction with the :term:`RTP Control Protocol <RTCP>`. Transmissions using the RTP audio/video profile (RTP/AVP) typically use :term:`SDP` to describe the technical parameters of the media streams.

       .. seealso::

          :wikipedia:`Real-time Transport Protocol`

          :wikipedia:`RTP audio video profile`

          :rfc:`3550`
              RTP: A Transport Protocol for Real-Time Applications.

   Same-origin policy
       The "same-origin policy" is a web application security model. The policy permits scripts running on pages originating from the same domain to access each other's :term:`DOM` with no specific restrictions, but prevents access to :term:`DOM` on different domains.

       .. seealso::

          :wikipedia:`Same-origin policy`

   SDP
   Session Description Protocol
   SDP Offer/Answer
       The **Session Description Protocol** (SDP) is a text document that describes the parameters of a streaming media session. It is commonly used to describe the characteristics of RTP streams (and related protocols such as RTSP).

       The **SDP Offer/Answer** model is a negotiation between two peers of a unicast stream, by which the sender and the receiver share the set of media streams and codecs they wish to use, along with the IP addresses and ports they would like to use to receive the media.

       This is an example SDP Offer/Answer negotiation. First, there must be a peer that wishes to initiate the negotiation; we'll call it the *offerer*. It composes the following SDP Offer and sends it to the other peer -which we'll call the *answerer*-:

       .. code-block:: text

          v=0
          o=- 0 0 IN IP4 127.0.0.1
          s=Example sender
          c=IN IP4 127.0.0.1
          t=0 0
          m=audio 5006 RTP/AVP 96
          a=rtpmap:96 opus/48000/2
          a=sendonly
          m=video 5004 RTP/AVP 103
          a=rtpmap:103 H264/90000
          a=sendonly

       Upon receiving that Offer, the *answerer* studies the parameters requested by the *offerer*, decides if they can be satisfied, and composes an appropriate SDP Answer that is sent back to the *offerer*:

       .. code-block:: text

          v=0
          o=- 3696336115 3696336115 IN IP4 192.168.56.1
          s=Example receiver
          c=IN IP4 192.168.56.1
          t=0 0
          m=audio 0 RTP/AVP 96
          a=rtpmap:96 opus/48000/2
          a=recvonly
          m=video 31278 RTP/AVP 103
          a=rtpmap:103 H264/90000
          a=recvonly

       The SDP Answer is the final step of the SDP Offer/Answer Model. With it, the *answerer* agrees to some of the parameter requested by the *offerer*, but not all.

       In this example, ``audio 0`` means that the *answerer* rejects the audio stream that the *offerer* intended to send; also, it accepts the video stream, and the ``a=recvonly`` acknowledges that the *answerer* will exclusively act as a receiver, and won't send any stream back to the other peer.

       .. seealso::

          :wikipedia:`Session Description Protocol`

          `Anatomy of a WebRTC SDP <https://webrtchacks.com/anatomy-webrtc-sdp/>`__

          :rfc:`4566`
              SDP: Session Description Protocol.

          :rfc:`4568`
              Session Description Protocol (SDP) Security Descriptions for Media Streams.

   Semantic Versioning
      `Semantic Versioning <https://semver.org/>`__ is a formal convention for specifying compatibility using a three-part version number: major version; minor version; and patch.

   Signaling Plane
       It is the layer of a media system in charge of the information exchanges concerning the establishment and control of the different media circuits and the management of the network, in contrast to the transfer of media, done by the :index:`Media Plane <single: Plane; Media>`.
       Functions such as media negotiation, QoS parametrization, call establishment, user registration, user presence, etc. as managed in this plane.

       .. seealso::

          :term:`Media Plane`

          `WebRTC in the real world: STUN, TURN and signaling <https://www.html5rocks.com/en/tutorials/webrtc/infrastructure/>`__ (`archive <https://web.archive.org/web/20191210072708/https://www.html5rocks.com/en/tutorials/webrtc/infrastructure/>`__)

   Sink, Media
       A :index:`Media Sink <single: Media; Sink>` is a MediaPad that outputs a Media Stream.
       Data streams from a MediaSource pad to another element's MediaSink pad.

   SIP
       Session Initiation Protocol (SIP) is a :term:`signaling plane` protocol widely used for controlling multimedia communication sessions such as voice and video calls over Internet Protocol (IP) networks. SIP works in conjunction with several other application layer protocols:

       * :term:`SDP` for media identification and negotiation.
       * :term:`RTP`, :term:`SRTP` or :term:`WebRTC` for the transmission of media streams.
       * A :term:`TLS` layer may be used for secure transmission of SIP messages.

       .. seealso::

          :wikipedia:`Session Initiation Protocol`

   Source, Media
       A :index:`Media Source <single: Media; Source>` is a Media Pad that generates a Media Stream.

   SPA
   Single-Page Application
      A single-page application is a web application that fits on a single web page with the goal of providing a more fluid user experience akin to a desktop application.

   Sphinx
       `Sphinx <http://www.sphinx-doc.org/en/stable/>`__ is a documentation generation system. Text is first written using `reStructuredText <http://docutils.sourceforge.net/rst.html>`__ markup language, which then is transformed by Sphinx into different formats such as PDF or HTML.
       This is the documentation tool of choice for the Kurento project.

       .. seealso::

          `Easy and beautiful documentation with Sphinx <https://www.ibm.com/developerworks/linux/library/os-sphinx-documentation/index.html>`__ (`archive <https://web.archive.org/web/20160825195643/https://www.ibm.com/developerworks/linux/library/os-sphinx-documentation/index.html>`__)

   Spring Boot
       `Spring Boot <http://spring.io/projects/spring-boot>`__ is Spring's convention-over-configuration solution for creating stand-alone, production-grade Spring based applications that can you can "just run".
       It embeds Tomcat or Jetty directly and so there is no need to deploy WAR files in order to run web applications.

   SRTCP
       SRTCP provides the same security-related features to RTCP, as the ones provided by SRTP to RTP. Encryption, message authentication and integrity, and replay protection are the features added by SRTCP to :term:`RTCP`.

       .. seealso::

          :term:`SRTP`

   SRTP
       Secure RTP is a profile of RTP (:term:`Real-time Transport Protocol <RTP>`), intended to provide encryption, message authentication and integrity, and replay protection to the RTP data in both unicast and multicast applications. Similarly to how RTP has a sister RTCP protocol, SRTP also has a sister protocol, called Secure RTCP (or :term:`SRTCP`).

       .. seealso::

          :wikipedia:`Secure Real-time Transport Protocol`

          :rfc:`3711`
              The Secure Real-time Transport Protocol (SRTP).

   SSL
       Secure Socket Layer. See :term:`TLS`.

   STUN
       STUN stands for **Session Traversal Utilities for NAT**. It is a standard protocol (`IETF RFC 5389 <https://tools.ietf.org/html/rfc5389>`__) used by :term:`NAT` traversal algorithms to assist hosts in the discovery of their public network information.
       If the routers between peers use full cone, address-restricted, or port-restricted NAT, then a direct link can be discovered with STUN alone. If either one of the routers use symmetric NAT, then a link can be discovered with STUN packets only if the other router does not use symmetric or port-restricted NAT. In this later case, the only alternative left is to discover a relayed path through the use of :term:`TURN`.

   Trickle ICE
       Extension to the :term:`ICE` protocol that allows ICE agents to send and receive candidates incrementally rather than exchanging complete lists. With such incremental provisioning, ICE agents can begin connectivity checks while they are still gathering candidates and considerably shorten the time necessary for ICE processing to complete.

       .. seealso::

          `draft-ietf-ice-trickle <https://tools.ietf.org/html/draft-ietf-ice-trickle-15>`__
              Trickle ICE: Incremental Provisioning of Candidates for the Interactive Connectivity Establishment (ICE) Protocol.

   TLS
       Transport Layer Security (TLS) and its predecessor Secure Socket Layer (SSL).

       .. seealso::

          :wikipedia:`Transport Layer Security`

          :rfc:`5246`
              The Transport Layer Security (TLS) Protocol Version 1.2.

   TURN
       TURN stands for **Traversal Using Relays around NAT**. Like :term:`STUN`, it is a network protocol (`IETF RFC 5766 <https://tools.ietf.org/html/rfc5766>`__) used to assist in the discovery of paths between peers on the Internet.
       It differs from STUN in that it uses a public intermediary relay to act as a proxy for packets between peers. It is used when no other option is available since it consumes server resources and has an increased latency.
       The only time when TURN is necessary is when one of the peers is behind a symmetric NAT and the other peer is behind either a symmetric NAT or a port-restricted NAT.

   VP8
       VP8 is a video compression format created by On2 Technologies as a successor to VP7. Its  patents rights are owned by Google, who made an irrevocable patent promise on its patents for implementing it and released a specification under the `Creative Commons Attribution 3.0 license <https://creativecommons.org/licenses/by/3.0/>`__.

       .. seealso::

          :wikipedia:`VP8`

          :rfc:`6386`
              VP8 Data Format and Decoding Guide.

   WebM
       `WebM <https://www.webmproject.org/>`__ is an open media file format designed for the web. WebM files consist of video streams compressed with the VP8 video codec and audio streams compressed with the Vorbis audio codec. The WebM file structure is based on the Matroska media container.

   WebRTC
       `WebRTC <https://webrtc.org/>`__ is a set of protocols, mechanisms and APIs that provide browsers and mobile applications with Real-Time Communications (RTC) capabilities over peer-to-peer connections.

       .. seealso::

          `WebRTC Working Draft <https://www.w3.org/TR/webrtc/>`__

   WebSocket
       `WebSocket <https://www.websocket.org/>`__ specification (developed as part of the HTML5 initiative) defines a full-duplex single socket connection over which messages can be sent between client and server.
