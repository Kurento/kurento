%%%%%%%%%%%%
  Glossary
%%%%%%%%%%%%

This is a glossary of terms that often appear in discussion about
multimedia transmissions. Most of the terms are described
and linked to its wikipedia, RFC or W3C relevant documents.
Some of the terms are specific to :term:`gstreamer` or :term:`kurento`.

.. glossary::

    Agnostic, Media
        One of the big problems of media is that the number of variants
        of video and audio codecs, formats and variants quickly creates
        high complexity in heterogeneous applications. So kurento developed
        the concept of an automatic converter of media formats that enables
        development of *agnostic* elements. Whenever a media element’s
        source is connected to another media element’s sink, the kurento
        framework verifies if media adaption and transcoding is necessary
        and, if needed, it transparently incorporates the appropriate
        transformations making possible the chaining of the two elements
        into the resulting :term:`Pipeline <pipeline, media>`.

    CORS
        `CORS <http://en.wikipedia.org/wiki/Cross-origin_resource_sharing>`__
        is a mechanism that allows JavaScript code on a web page to make
        XMLHttpRequests to different domains than the one the
        JavaScript originated from. It works by adding new HTTP headers
        that allow servers to serve resources to permitted origin domains.
        Browsers support these headers and enforce the restrictions
        they establish.

        .. seealso::
            `enable-cors.org <http://enable-cors.org/>`__ 
                for information on the relevance of CORS and how and when 
                to enable it.

    Element, Media
        A :java:type:`MediaElement` is a module that encapsulates a specific
        media capability.  For example, a :java:type:`RecorderEndpoint`,
        a Video :java:type:`PlayerEndpoint`

    GStreamer
        `GStreamer <http://gstreamer.freedesktop.org/>`__ is a pipeline-based
        multimedia framework written in the C programming language.

    H.264
        A Video Compression Format. 
        The H.264 standard can be viewed as a "family of standards" composed
        of a number of profiles.  Each specific decoder deals with at least
        one such profiles, but not necessarily all. See
        `H.264 entry at wikipedia <http://en.wikipedia.org/wiki/H.264/MPEG-4_AVC>`__

        .. seealso::
            :rfc:`6184`
                RTP Payload Format for H.264 Video. This RFC obsoletes :rfc:`3984`.

    HTTP
        The `Hypertext Transfer Protocol <http://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol>`__
        is an application protocol for distributed, collaborative, hypermedia
        information systems. HTTP is the foundation of data communication for
        the World Wide Web. 

        .. seealso:: :rfc:`2616`

    IMS
    `IP Multimedia Subsystem <http://en.wikipedia.org/wiki/IP_Multimedia_Subsystem>`__
        `3GPP <http://en.wikipedia.org/wiki/3rd_Generation_Partnership_Project>`__
        Mobile Architectural Framework for delivering IP Multimedia Services
        in 3G (and beyond) Mobile Networks.

        .. seealso::
            :rfc:`3574`

    Java EE
        Java EE, or Java Platform, Enterprise Edition, is a standardised
        set of APIs for Enterprise software development. The Kurento Media
        Framework (:term:`KMF`) deploys application in Java EE containter.

        .. seealso::
            Oracle Site
                `Java EE Overview <http://www.oracle.com/technetwork/java/javaee/overview/index.html>`__
            Wikipedia
                `Java Platform Enterprise Edition <http://en.wikipedia.org/wiki/Java_Platform,_Enterprise_Edition>`__

    JSON
        `JSON <http://json.org>`__ (JavaScript Object Notation) is a lightweight
        data-interchange format. It is designed to be easy to understand and
        write for humans and easy to parse for machines.

    JSON-RPC
        `JSON-RPC <http://json-rpc.org/>`__ is a simple remote procedure
        call protocol encoded in JSON. JSON-RPC allows for notifications
        and for multiple calls to be sent to the server which may be
        answered out of order.

    Kurento
        `Kurento <http://kurento.org>`__ is a platform for the
        development of multimedia enabled aplications.
        Kurento is open source, released under LGPL 2.1, and has
        several components, providing solutions to most multimedia
        common services requirements. Those components include:

        * Kurento Media Server (:term:`KMS`).
        * Kurento Media Framework (:term:`KMF`).
        * Kurento Web SDK (:term:`KWS`).
        * Kurento Android SDK (:term:`KANDS`).

    Kurento Android SDK
    KANDS
        An SDK that integrates audio and video streaming into 
        Android applications.

    Kurento Application Server
    KAS
        A :term:`Java EE` Application container that hosts the server side
        :term:`signalling plane` of Kurento applications. Currently
        Kurento support the use of `JBoss 7 <http://en.wikipedia.org/wiki/WildFly>`__.

    KMF
    Kurento Media Framework
        Framework for the development of rich media based applications
        using Java EE technologies. It exposes APIs for accessing and
        controlling KMS capabilities from Java applications.

    KMS
    Kurento Media Server
        A media server that provides low-level multimedia capabilities.
        Kurento Media Server processes and runs the :term:`Media
        Pipeline <pipeline, media>` of Kurento applications.

    KWS
    Kurento Web SDK
        A JavaScript client side API taking advantage of
        HTML5 multimedia features for writing clients that
        interact easy and naturally with KAS.

    Media Plane
        In the traditional `3GPP Mobile Carrier Media Framework
        <http://en.wikipedia.org/wiki/IP_Multimedia_Subsystem>`__,
        the handling of media is conceptually splitted in two layers.
        The one that handles the media itself, with functionalities such as
        media transport, encoding/decoding, and processing, is called
        :index:`Media Plane <single: Plane; Media>`.

        .. seealso:: :term:`Signalling Plane`

    MP4
        MPEG-4 Part 14 or MP4 is a digital multimedia format most commonly
        used to store video and audio, but can also be used to store other
        data such as subtitles and still images.

        .. seealso:: Wikipedia definition of `MP4
           <http://en.wikipedia.org/wiki/MPEG-4_Part_14>`__.

    Multimedia
        Multimedia is concerned with the computer controlled integration
        of text, graphics, video, animation, audio, and any other media where
        information can be represented, stored, transmitted and processed
        digitally.

        There is a temporal relationship between many forms of media, 
        for instance audio, video and animations. There 2 are forms of problems
        involved in 

            * Sequencing within the media, i.e. playing frames in correct
              order or time frame.
            * Synchronisation, i.e. inter-media scheduling. For example, 
              keeping video and audio synchronized or displaying captions
              or subtitles in the required intervals.

        .. seealso:: Wikipedia definition of `multimedia
            <http://en.wikipedia.org/wiki/Multimedia>`__

    Multimedia container format
        Container or wrapper formats are metafile formats whose
        specification describes how different data elements and metadata
        coexist in a computer file.

        Simpler multimedia container formats can contain different types
        of audio formats, while more advanced container formats can
        support multiple audio and video streams, subtitles,
        chapter-information, and meta-data, along with the synchronization
        information needed to play back the various streams together.
        In most cases, the file header, most of the  metadata and the
        synchro chunks are specified by the container format.

        .. seealso:: Wikipedia definition of `multimedia containter formats
           <http://en.wikipedia.org/wiki/Container_format_(digital)#Multimedia_container_formats>`__

    NAT
    Network Address Translation
        Network address translation (NAT) is the technique of modifying
        network address information in Internet Protocol (IP) datagram
        packet headers while they are in transit across a traffic routing
        device for the purpose of remapping one IP address space into
        another.

        .. seealso::

            Network Address Translation definition at Wikipedia
                <http://en.wikipedia.org/wiki/Network_address_translation>__

    NAT-T
    NAT Traversal
        NAT traversal (sometimes abbreviated as NAT-T) is a general term
        for techniques that establish and maintain Internet protocol
        connections traversing network address translation (NAT) gateways,
        which break end-to-end connectivity. Intercepting and modifying
        traffic can only be performed transparently in the absence of
        secure encryption and authentication.

        .. seealso::

            `NAT Traversal White Paper <http://www.nattraversal.com/>`_
                White paper on NAT-T and solutions for end-to-end
                connectivity in its presence

    Pad, Media
        A :index:`Media Pad <single: Media; Pad>` is is an element´s
        interface with the outside world. Data streams from the MediaSource
        pad to another element’s MediaSink pad.

        .. seealso::

            GStreamer `Pad <http://hackage.haskell.org/package/gstreamer-0.12.1.1/docs/Media-Streaming-GStreamer-Core-Pad.html>`__
                Definition of the Pad structure in GStreamer

            Kurento :java:type:`MediaPad`
                Kurennto Media API Java interface for the MediaPad

    Pipeline, Media
        A :index:`Media Pipeline <single: Media; Pipeline>` is a chain of media elements, where the output
        stream generated by one element (source) is fed into one or
        more other elements input streams (sinks). Hence, the pipeline
        represents a “machine” capable of performing a sequence of
        operations over a stream.

    REST
        `Representational State Transfer <http://en.wikipedia.org/wiki/Representational_state_transfer>`__
        is an architectural style consisting of a coordinated set of constraints applied to
        components, connectors, and data elements, within a distributed hypermedia system.
        The term representational state transfer was introduced and defined in 2000 by
        Roy Fielding in his `doctoral dissertation
        <http://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm>`__.

    RTCP
        The `RTP Control Protocol <http://en.wikipedia.org/wiki/RTP_Control_Protocol>`__
        is a sister protocol of the :term:`RTP`, that provides out-of-band statistics and
        control information for an RTP flow.

        .. seealso:: :rfc:`3605`

    RTP
        The `Real-Time Transport Protocol <http://en.wikipedia.org/wiki/Real-time_Transport_Protocol>`__
        is a standard packet format designed for transmitting audio and video streams on IP networks.
        It is used in conjunction with the :term:`RTP Control Protocol <RTCP>`.
        Transmissions using `the RTP audio/video profile <http://en.wikipedia.org/wiki/RTP_audio_video_profile>`__
        typically use :term:`SDP` to describe the technical parameters of the media streams.

        .. seealso:: :rfc:`3550`

    SDP
    Session Description Protocol
        The `Session Description Protocol <http://en.wikipedia.org/wiki/Session_Description_Protocol>`__
        describes initialization parameters for a streaming media session.
        Both parties of a streaming media session exchange SDP files
        to negotiate and agree in the parameters to be used for the
        streaming.

        .. seealso::

            :rfc:`4566`
                Definition of Session Description Protocol
            :rfc:`4568`
                Security Descriptions for Media Streams in SDP

    Signalling Plane
        It is the layer of a media system in charge of the information exchanges
        concerning the establishment and control of the different media circuits
        and the management of the network, in contrast to the transfer of media,
        done by the :index:`Signalling Plane <single: Plane; Signalling>`.

        Functions such as media negotiation, QoS parametrization, call establishment,
        user registration, user presence, etc. as managed in this plane.

        .. seealso:: :term:`Media Plane`

    Sink, Media
        A :index:`Media Sink <single: Media; Sink>` is a MediaPad that outputs a Media Stream.  
        Data streams from a MediaSource pad to another element’s MediaSink pad.

    SIP
        `Session Initiation Protocol <http://en.wikipedia.org/wiki/Session_Initiation_Protocol>`__ 
        is a `signalling plane`:term: widely used for controlling multimedia
        communication sessions such as voice and video calls over Internet
        Protocol (IP) networks. SIP works in conjunction with several other application
        layer protocols:

        * `SDP`:term: for media identification and negotiation
        * `RTP`:term:, `SRTP`:term: or `WebRTC`:term: for the transmission of media streams
        * A `TLS`:term: layer may be used for secure transmission of SIP messages

    Source, Media
        A :index:`Media Source <single: Media; Source>` is a Media Pad
        that generates a Media Stream.

    SRTCP
        SRTCP provides the same security-related features to RTCP,
        as the ones provided by SRTP to RTP. Encryption, message
        authentication and integrity, and replay protection are the
        features added by SRTCP to `RTCP`:term:.

        .. seealso:: :term:`SRTP`

    SRTP
        `Secure RTP <http://en.wikipedia.org/wiki/Secure_Real-time_Transport_Protocol>`__
         is a profile of RTP (`Real-time Transport Protocol <RTP>`:term:),
         intended to provide encryption, message authentication and integrity,
         and replay protection to the RTP data in both unicast and multicast
         applications. Similar to how RTP has a sister RTCP protocol, SRTP
         also has a sister protocol, called Secure RTCP (or `SRTCP`:term:);

        .. seealso::
            :rfc:`3711`

    SSL
        Secure Socket Layer. See `TLS`:term:.

    TLS
        `Transport Layer Security <http://en.wikipedia.org/wiki/Transport_Layer_Security>`__
        and its prececessor Secure Socket Layer (SSL) 

        .. seealso::
            :rfc:`5246`
                Version 1.2 of the Transport Layer Security protocol

    WebM
        `WebM <http://www.webmproject.org/>`__ is an open media file format 
        designed for the web. WebM files consist of video streams compressed
        with the VP8 video codec and audio streams compressed with the
        Vorbis audio codec. The WebM file structure is based on the
        Matroska media container.

    WebRTC
        `WebRTC <http://www.webrtc.org/>`__ is a project that tries to enable
        web browsers with rich Real-Time Communcations capabilities via
        simple Javascript and HTML5 APIs and Components.

        .. seealso:: `WebRTC Working Draft <http://www.w3.org/TR/webrtc/>`__
