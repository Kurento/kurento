%%%%%%%%
Glossary
%%%%%%%%

This is a glossary of terms that often appear in discussion about multimedia
transmissions. Most of the terms are described and linked to its wikipedia, RFC
or W3C relevant documents. Some of the terms are specific to :term:`gstreamer`
or :term:`kurento`.


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
        into the resulting :term:`Pipeline <Media Pipeline>`.

    AVI
        Audio Video Interleaved, known by its initials AVI, is a multimedia
        container format introduced by Microsoft in November 1992 as part
        of its Video for Windows technology. AVI files can contain both
        audio and video data in a file container that allows synchronous
        audio-with-video playback. AVI is a derivative of the Resource
        Interchange File Format (RIFF).

        .. seealso::
            :wikipedia:`en,Audio Video Interleave`
                Wikipedia reference of the AVI format
            :wikipedia:`en,Resource Interchange File Format`
                Wikipedia reference of the RIFF format

    Bower
        `Bower <http://http://bower.io/>`_ is a package manager for the web.
        It offers a generic solution to the problem of front-end package management,
        while exposing the package dependency model via an API that can be consumed by
        a build stack.

    Builder Pattern
        The builder pattern is an object creation software design pattern whose 
        intention is to find a solution to the telescoping constructor 
        anti-pattern. The telescoping 
        constructor anti-pattern occurs when the increase of object constructor 
        parameter combination leads to an exponential list of constructors. 
        Instead of using numerous constructors, the builder pattern uses another 
        object, a builder, that receives each initialization parameter step by 
        step and then returns the resulting constructed object at once.
        
        .. seealso::
            :wikipedia:`Builder_pattern`
                Wikipedia reference of the Builder Pattern

    CORS
        :wikipedia:`CORS <en,Cross-origin_resource_sharing>`
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

    DOM
    Document Object Model
        Document Object Model is a cross-platform and language-independent convention
        for representing and interacting with objects in HTML, XHTML and XML documents.

    EOS
        Acronym of End Of Stream. In Kurento some elements will raise an
        :rom:evnt:`EndOfStream` event when the media they are processing is
        finished.

    GStreamer
        `GStreamer <http://gstreamer.freedesktop.org/>`__ is a pipeline-based
        multimedia framework written in the C programming language.

    H.264
        A Video Compression Format.
        The H.264 standard can be viewed as a "family of standards" composed
        of a number of profiles.  Each specific decoder deals with at least
        one such profiles, but not necessarily all. See
        :wikipedia:`H.264 entry at wikipedia <en,H.264/MPEG-4_AVC>`

        .. seealso::
            :rfc:`6184`
                RTP Payload Format for H.264 Video. This RFC obsoletes
                :rfc:`3984`.

    HTTP
        The :wikipedia:`Hypertext Transfer Protocol <en,Hypertext_Transfer_Protocol>`
        is an application protocol for distributed, collaborative, hypermedia
        information systems. HTTP is the foundation of data communication for
        the World Wide Web.

        .. seealso:: :rfc:`2616`

    IMS
        :wikipedia:`IP Multimedia Subsystem <en,IP_Multimedia_Subsystem>` is
        :wikipedia:`3GPP <en,3rd_Generation_Partnership_Project>`
        Mobile Architectural Framework for delivering IP Multimedia Services
        in 3G (and beyond) Mobile Networks.

        .. seealso::
            :rfc:`3574`

    Java EE
        Java EE, or Java Platform, Enterprise Edition, is a standardised
        set of APIs for Enterprise software development.

        .. seealso::
            Oracle Site
                `Java EE Overview
                <http://www.oracle.com/technetwork/java/javaee/overview/index.html>`__
            Wikipedia
                :wikipedia:`Java Platform Enterprise Edition
                <en,Java_Platform,_Enterprise_Edition>`

    jQuery
        `jQuery <http://jquery.com/>`_ is a cross-platform JavaScript library designed
        to simplify the client-side scripting of HTML.


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
        `Kurento <http://kurento.org>`__ is a platform for the development of multimedia
        enabled applications. Kurento is the Esperanto term for the English word
        'stream'. We chose this name because we believe the Esperanto principles are
        inspiring for what the multimedia community needs: simplicity, openness and
        universality. Kurento is open source, released under LGPL 2.1, and has several
        components, providing solutions to most multimedia common services
        requirements. Those components include: term:`Kurento Media Server <Kurento Media Server>`,
        term:`Kurento API <Kurento API>`, term:`Kurento Protocol <Kurento Protocol>`, and
        term:`Kurento Client <Kurento Client>`.

    Kurento API
         **Kurento API** is an object oriented API to create media pipelines to control
         media. It can be seen as and interface to Kurento Media Server. It can be used from the
         Kurento Protocol or from Kurento Clients.

    Kurento Client
         A **Kurento Client** is a programming library (Java or JavaScript) used to control
         **Kurento Media Server** from an application. For example, with this library, any developer
         can create a web application that uses Kurento Media Server to receive audio and video from
         the user web browser, process it and send it back again over Internet. Kurento Client
         exposes the :term:`Kurento API <Kurento API>` to app developers.

    Kurento Protocol
         Communication between KMS and clients by means of :term:`JSON-RPC` messages.
         It is based on :term:`WebSocket` that uses :term:`JSON-RPC` V2.0 messages for making
         requests and sending responses.

    Kurento Media Server
         **Kurento Media Server** is the core element of Kurento since it responsible for media
         transmission, processing, loading and recording.

    Maven
        `Maven <http://maven.apache.org/>`_ is a build automation tool used primarily for Java projects.

    Media Element
        A :java:type:`MediaElement` is a module that encapsulates a specific
        media capability.  For example, a :java:type:`RecorderEndpoint`,
        a Video :java:type:`PlayerEndpoint`

    Media Pipeline
        A :index:`Media Pipeline <single: Media; Pipeline>` is a chain of media elements, where the output
        stream generated by one element (source) is fed into one or
        more other elements input streams (sinks). Hence, the pipeline
        represents a “machine” capable of performing a sequence of
        operations over a stream.

    Media Plane
        In the traditional :wikipedia:`3GPP Mobile Carrier Media Framework
        <en,IP_Multimedia_Subsystem>`, the handling of media is conceptually
        splitted in two layers.
        The one that handles the media itself, with functionalities such as
        media transport, encoding/decoding, and processing, is called
        :index:`Media Plane <single: Plane; Media>`.

        .. seealso:: :term:`Signalling Plane`

    MP4
        MPEG-4 Part 14 or MP4 is a digital multimedia format most commonly
        used to store video and audio, but can also be used to store other
        data such as subtitles and still images.

        .. seealso:: Wikipedia definition of :wikipedia:`MP4
                     <en,MPEG-4_Part_14>`.

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

        .. seealso:: Wikipedia definition of :wikipedia:`en,Multimedia`

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

        .. seealso::

           Wikipedia definition of :wikipedia:`multimedia container formats
           <en,Container_format_(digital)#Multimedia_container_formats>`

    NAT
    Network Address Translation
        Network address translation (NAT) is the technique of modifying
        network address information in Internet Protocol (IP) datagram
        packet headers while they are in transit across a traffic routing
        device for the purpose of remapping one IP address space into
        another.

        .. seealso::

            :wikipedia:`Network Address Translation
            <en,Network_address_translation>`
            definition at Wikipedia

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

    Node.js
        `Node.js <http://www.nodejs.org/>`_ is a cross-platform runtime environment for server-side
        and networking applications. Node.js applications are written in
        JavaScript, and can be run within the Node.js runtime on OS X,
        Microsoft Windows and Linux with no changes.

    npm
        `npm <https://www.npmjs.org/>`_ is the official package manager for `Node.js`:term:.

    OpenCL
        `OpenCL <http://www.khronos.org/opencl/>`__\ ™ is standard
        framework for  cross-platform, parallel programming of
        heterogeneous platforms consisting of central processing units
        (CPUs), graphics processing units (GPUs), digital signal
        processors (DSPs), field-programmable gate arrays (FPGAs) and
        other processors.

    OpenCV
        OpenCV (Open Source Computer Vision Library) is a BSD-licensed
        open source computer vision and machine learning software library.
        OpenCV aims to provide a common infrastructure for computer vision
        applications and to accelerate the use of machine perception.

    Pad, Media
        A :index:`Media Pad <single: Media; Pad>` is is an element´s
        interface with the outside world. Data streams from the MediaSource
        pad to another element’s MediaSink pad.

        .. seealso::

            GStreamer `Pad <http://hackage.haskell.org/package/gstreamer-0.12.1.1/docs/Media-Streaming-GStreamer-Core-Pad.html>`__
                Definition of the Pad structure in GStreamer

            Kurento :java:type:`MediaPad`
                Kurento Media API Java interface for the MediaPad

    PubNub
        `PubNub <http://www.pubnub.com/>`__ is a publish/subscribe cloud service for
        sending and routing data. It streams data to global audiences on any device
        using persistent socket connections. PubNub has been designed to deliver data
        with low latencies to end-user devices. These devices can be behind firewalls,
        NAT environments, and other hard-to-reach network environments. PubNub provides
        message caching for retransmission of lost signals over unreliable network
        environments. This is accomplished by maintaining an always open socket
        connection to every device.

    QR
        QR code (Quick Response Code) is a type of two-dimensional barcode.
        that became popular in the mobile phone industry due to its fast
        readability and greater storage capacity compared to standard UPC
        barcodes.

        .. seealso::

            :wikipedia:`QR Code<en,QR_Code>`
                Entry in wikipedia

    REST
        :wikipedia:`Representational State Transfer <en,Representational_state_transfer>`
        is an architectural style consisting of a coordinated set of constraints applied to
        components, connectors, and data elements, within a distributed hypermedia system.
        The term representational state transfer was introduced and defined in 2000 by
        Roy Fielding in his `doctoral dissertation
        <http://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm>`__.

    RTCP
        The :wikipedia:`RTP Control Protocol <en,RTP_Control_Protocol>` is a
        sister protocol of the :term:`RTP`, that provides out-of-band
        statistics and control information for an RTP flow.

        .. seealso:: :rfc:`3605`

    RTP
        The :wikipedia:`Real-Time Transport Protocol <en,Real-time_Transport_Protocol>`
        is a standard packet format designed for transmitting audio and video
        streams on IP networks. It is used in conjunction with the
        :term:`RTP Control Protocol <RTCP>`. Transmissions using
        :wikipedia:`the RTP audio/video profile <en,RTP_audio_video_profile>`
        typically use :term:`SDP` to describe the technical parameters of
        the media streams.

        .. seealso:: :rfc:`3550`

    Same-origin policy
        The :wikipedia:`Same-origin policy <en,Same-origin_policy>` is web application
        security model. The policy permits scripts running on pages originating from the
        same site to access each other's `DOM`:term: with no specific restrictions, but prevents
        access to `DOM`:term: on different sites.

    SDP
    Session Description Protocol
        The :wikipedia:`Session Description Protocol
        <en,Session_Description_Protocol>` describes initialization
        parameters for a streaming media session.
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
        :wikipedia:`Session Initiation Protocol <en,Session_Initiation_Protocol>`
        is a `signalling plane`:term: protocol widely used for controlling
        multimedia communication sessions such as voice and video calls
        over Internet Protocol (IP) networks. SIP works in conjunction with
        several other application layer protocols:

        * `SDP`:term: for media identification and negotiation
        * `RTP`:term:, `SRTP`:term: or `WebRTC`:term: for the transmission of media streams
        * A `TLS`:term: layer may be used for secure transmission of SIP messages

    Source, Media
        A :index:`Media Source <single: Media; Source>` is a Media Pad
        that generates a Media Stream.

    SPA
    Single-Page Application
       A single-page application is a web application that fits on a single web page with the goal
       of providing a more fluid user experience akin to a desktop application.

    Sphinx
        Documentation generation system used for kurento documentation

        .. seealso:: `Easy and beautiful documentation with Sphinx <http://www.ibm.com/developerworks/linux/library/os-sphinx-documentation/index.html?ca=dat>`_

    Spring Boot
        `Spring Boot <http://projects.spring.io/spring-boot/>`_ is Spring's convention-over-configuration
        solution for creating stand-alone, production-grade Spring based applications that can you can "just run".
        It embeds Tomcat or Jetty directly and so there is no need to deploy WAR files in order to run
        web applications.


    SRTCP
        SRTCP provides the same security-related features to RTCP,
        as the ones provided by SRTP to RTP. Encryption, message
        authentication and integrity, and replay protection are the
        features added by SRTCP to `RTCP`:term:.

        .. seealso:: :term:`SRTP`

    SRTP
        :wikipedia:`Secure RTP <,enSecure_Real-time_Transport_Protocol>`
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
        :wikipedia:`Transport Layer Security <en,Transport_Layer_Security>`
        and its prececessor Secure Socket Layer (SSL)

        .. seealso::
            :rfc:`5246`
                Version 1.2 of the Transport Layer Security protocol

    VP8
        VP8 is a video compression format created by On2 Technologies as a
        successor to VP7. Its  patents rights are owned by Google, who made
        an irrevocable patent promise on its patents for implementing it
        and released a specification under the `Creative Commons Attribution
        3.0 license <https://creativecommons.org/licenses/by/3.0/>`__.

        .. seealso::
                :rfc:`6386`
                    VP8 Data Format and Decoding Guide
                :wikipedia:`en,VP8`
                    VP8 page at Wikipedia

    WebM
        `WebM <http://www.webmproject.org/>`__ is an open media file format
        designed for the web. WebM files consist of video streams compressed
        with the VP8 video codec and audio streams compressed with the
        Vorbis audio codec. The WebM file structure is based on the
        Matroska media container.

    WebRTC
        `WebRTC <http://www.webrtc.org/>`__ is an open source project that
        provides rich Real-Time Communcations capabilities to web browsers
        via Javascript and HTML5 APIs and components. These APIs are being
        drafted by the World Wide Web Consortium (W3C).

        .. seealso:: `WebRTC Working Draft <http://www.w3.org/TR/webrtc/>`__

    WebSocket
        `WebSocket <https://www.websocket.org/>`__ specification (developed as
        part of the HTML5 initiative) defines a full-duplex single socket
        connection over which messages can be sent between client and server.
