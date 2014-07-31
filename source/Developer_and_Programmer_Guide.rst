.. _devguide:


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
 Developer and Programmer Guide
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

.. highlight:: java

Introduction
============

*Kurento* is a :term:`multimedia` platform helping
developers to add multimedia capabilities to their applications. The
core element is the *Kurento Media Server* (or :term:`KMS` for short) a
`Gstreamer <http://gstreamer.freedesktop.org/>`__ based multimedia
engine that provides the following features:

-  Networked streaming protocols, including :term:`HTTP` working as client and
   server, :term:`RTP` and :term:`WebRTC`.
-  Media transcodification between any of the codecs currently supported
   by Gstreamer.
-  Generic support for computational vision and augmented reality
   filters.
-  Media storage supporting writing operations for :term:`WebM` and
   :term:`MP4` and reading operations for any of *Gstreamer's* muxers.

`Java <http://www.java.com/>`__ and `Javascript
<http://www.w3.org/standards/webdesign/script>`__ SDKs are
available for developers, to incorporate the above features in their
applications.

About this guide
----------------

This guide, as *Kurento* itself, is under very
active development. Many features are constantly evolving and are not
completely documented yet. You can contribute to complete this guide and
also to Kurento effort by joining its community.

Programmer Guide
================

Things you need to know before start programing
-----------------------------------------------

-  *GE Kurento* software is released under `LGPL
   version 2.1 <http://www.gnu.org/licenses/lgpl-2.1.html>`__ license.
   This is quite a convenient license for programmers, but it is still
   recommended you check if it actually fits your application needs.

-  `Maven <http://maven.apache.org/>`__ is used as dependency
   management tool for *Java* SDKs. Most likely
   `Gradle <http://www.gradle.org/>`__ can also be used, but we still
   haven't tested it. If you don't use any dependency management you can
   still download the `KMF API
   Bundle <https://forge.fi-ware.org/frs/download.php/819/kmf-api.jar>`__
   and incorporate manually all dependencies to your application, but
   this is not recommended.

-  `Spring framework <http://spring.io/>`__ is extensively used for
   lifecycle management of *Java* components. Developers are not
   required to develop :term:`Spring` applications when using the *Stream
   Oriented GE Kurento* in :term:`Java EE` environments, but they'll have
   to when developing applications with KMF `Media API <kmf-media-api>`_.

Quick start
-----------

This section is intended to provide the very basic steps required to
integrate the *Kurento* framework into applications.

Basic Setup
~~~~~~~~~~~

* **Install and configure Kurento Media Server** (:term:`KMS`):
    This piece of software is the actual engine providing media
    processing and delivery.

* **Install and configure JBoss 7 Application Server** (:term:`KAS`):
    This is a :term:`Java EE` container that hosts the server side of
    applications. Other *Java* enterprise servers can be used, although no
    support from *Kurento* will be provided. This server will also be called
    *Kurento Application Server* (:term:`KAS`) through the document.

The  :doc:`Kurento Installation and Administration Guide <Installation_Guide>`
provides detailed information on installation and setup of above
components.

Create your first application
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Server side of your first application
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The *Kurento* server SDK is a *Java* library known as *Kurento Media Framework*
(:term:`KMF`). The following steps are required to create a *Kurento*
based application:

#. Create a *Maven* web project with your favourite IDE. You can use
   following ``pom.xml`` template. Please notice that '''Java 1.7'''
   is required to compile KMF-based Java projects.

   .. sourcecode:: xml


       <?xml version="1.0" encoding="UTF-8"?>
       <project xmlns="http://maven.apache.org/POM/4.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                    http://maven.apache.org/xsd/maven-4.0.0.xsd">

           <modelVersion>4.0.0</modelVersion>
           <groupId>my.organization</groupId>
           <artifactId>my-kurento-demo</artifactId>
           <version>0.0.1-SNAPSHOT</version>
           <packaging>war</packaging>

            <properties>
                <project.build.sourceEncoding>UTF-8 </project.build.sourceEncoding>
                <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
                <maven.compiler.source>1.7</maven.compiler.source>
                <maven.compiler.target>1.7</maven.compiler.target>
            </properties>

       </project>

#. You can add *KMF* dependencies to the ``pom.xml`` file

   .. sourcecode:: xml

       <dependencies>
           ...
           <dependency>
               <groupId>com.kurento.kmf</groupId>
               <artifactId>kmf-content-api</artifactId>
               <version>|version|</version>
           </dependency>
           ...
       </dependencies>

   .. note::
        We are in active development. Be sure that you have the latest
        Kurento version in your POM. You can find it in at `Maven Central
        <http://search.maven.org/#search%7Cga%7C1%7Ckurento>`_
        searching for kurento.

   **KMF** requires that the Application Server container supports the
   Servlet specification version 3.0. Therefore, ensure that this version
   is established in ``WEB-INF/web.xml``:

   .. sourcecode:: xml

      <web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	      xmlns="http://java.sun.com/xml/ns/javaee"
          xmlns:web="http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd"
	      xsi:schemaLocation="http://java.sun.com/xml/ns/javaee
               http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd"
	      version="3.0">
         <!-- ... -->
      </web-app>


#. Create a properties file named ``kurento.properties`` including
   following configuration keys:

   .. sourcecode:: properties

       # Put here the IP address where the KMS process is executing
       # If you launched KMS in the same hosts where you are executing KAS, let it as 127.0.0.1
       thriftInterfaceConfiguration.serverAddress=127.0.0.1

       # Put here the port where KMS management daemon is bound
       # If you did not modify KMS default configuration, let it as 9090
       thriftInterfaceConfiguration.serverPort=9090

       # Put here the IP address where KAS management handler must listen
       # If you launched KMS int the same host where you are executing KAS, let it as 127.0.0.1
       mediaApiConfiguration.handlerAddress=127.0.0.1

       # Port where KAS management daemon will bind
       # Your can choose the port you want. By default we assume 9100.
       mediaApiConfiguration.handlerPort=9100

   *Kurento* framework will search this file in the following locations (in
   the specified order):

   #. *JBoss* configuration folder defined by property:
      ``${jboss.server.config.dir}``
   #. Directory specified by java option *kurento.properties.dir*:
      ``-Dkurento.properties.dir=/home/user/kurento``
   #. *WEB-INF* directory of *WAR* archive

#. Create a *Java* Class that extends ``HttpPlayerHandler``, and add the
   annotation ``@PlayerService``. You'll have to implement the method
   ``onContentRequest()`` to set the media resource to be played::

       import com.kurento.kmf.content.HttpPlayerHandler;
       import com.kurento.kmf.content.HttpPlayerService;
       import com.kurento.kmf.content.HttpPlayerSession;

       @HttpPlayerService(path = "/playerService", useControlProtocol=false)
       public class MyService extends HttpPlayerHandler {

            @Override
            public void onContentRequest(HttpPlayerSession session) throws Exception {

                session.start("file:///path/to/myvideo.webm");
            }
        }


#. Place a *WebM* video so that the KMS process can reach it at whatever
   path you specified in ``/path/to/myvideo.webm``. This video will be
   the one read by the player element. You can replace the ``file:///``
   type URL by another one where a WebM file can be found, such as
   ``http://media.w3.org/2010/05/sintel/trailer.webm``
#. Package the project into a .war file.
#. Deploy your project into *JBoss 7* server installed during the basic
   setup and launch it.

   .. sourcecode:: bash

       sudo cp mykurento.war $JBOSS_HOME/standalone/deployments
       sudo /etc/init.d/jboss7 start

Client side of your first application
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

*Kurento* is designed to work with plain *HTML5* code. For testing
your application, you just have to include a ``<video>`` tag linked
to the *service URL* defined above. To do it, for example, create an
HTML file in your local machine containing the code
shown below and open it with your browser.

.. sourcecode:: html

    <video>
        <source src="http://myServer/myApp/playerService" type ="video/webm"/>
    </video>

You can read also section :ref:`programming-with-kws` to
find out more sophisticated ways to access media resources.

Next steps
~~~~~~~~~~

-  Read section :ref:`basic-streaming-concepts` in
   order to understand how *Kurento* features can
   help you to build multimedia applications.
-  Review :ref:`programming-with-kmf-content-api` for a detailed
   reference on content services.
-  Go to :ref:`programming-with-kmf-media-api` for a detailed explanation
   about how to achieve full control of :term:`Kurento Media Server <KMS>`.
-  Review :ref:`programming-with-kws` for a
   detailed reference of capabilities available in browsers.

.. _basic-streaming-concepts:

Basic streaming concepts
------------------------

There are several streaming concepts that might be of interest in order
to know the precise behaviour that can be expected when adding multimedia
resources to applications.

Any streaming protocol requires two main components: a *control
function* to manage connection setup and a *media function*, that
actually provides media process & transfer capabilities. For true
streaming protocols, like *RTP*, *RTSP*, *RTMP* or *WebRTC* there is a
clear differentiation between both functions. Actually *RTP* is the
media function of the *RTSP* protocol. *RTP* can also be used in
conjunction with other control protocols like *SIP* or *XMPP*. *WebRTC*
is a media function like *RTP* and it also requires a control protocol
that negotiates connection setup.

Streaming over *HTTP* (a.k.a. *HTML5 streaming*) is somehow special
because *HTTP* is not a protocol designed for media transfer. *HTML5
streaming* sessions starts with the browser sending a GET request to the
server. In this step both: browser and server play the *control
function* role. The server then maps the URL to the actual resource,
encapsulates its content in the response and sends it back to the
``<video>`` component, just like any download operation. Now browser and
server switch to the *media function*. There isn't a clear
differentiation between control and media functions that are played
sequentially by the same element in both sides. Apart form this function
mixup, many people will argue the *HTTP* is not really a streaming protocol,
since there is no relation between media transfer pace and playing pace,
i.e. the network transfer rate is not limited by the media consumption
rate and you might find situations where the whole content of a 1 hour
video is already downloaded when still playing the first minute.

There is quite an important and somehow confusing concept related to the
capability to jump to a time position within a stream. This operation is
normally called *SEEK* and streams that supports it are called
*seek-able*. Those not supporting *SEEK* operation are called *live* or
*non-seek-able*. There are two conditions a stream must meet in order to
be *seek-able*. First, the control protocol must provide a *SEEK*
command and second, the media resource must be completely available
before the stream starts transmission. The reason for the second condition
is because seeks must specify somehow the file position where the stream
must jump and that requires to know in advance the size or length of the
media resource and hence the whole resource must be available in
advance. Streaming protocols like *RTSP* and *HTTP* use header ``Range``
as a mean to build seek command. When the ``<video>`` component in an
*HTML5* application request a seek operation, the browser sends a new
GET request with the appropriate ``Range`` header. But this is only
available if the server provided the resource size in advance in the
first request (the one that initiated the stream). If resource size is
not available at start time, the video component does not show any kind
of progress bar, switching into *live* mode. *Kurento* is currently
supporting only *live* mode, independently of the prior availabily of
the media resource.

When designing streaming services it is also very important to determine
the type of service that is being offered. There are two main
classifications for streaming services: *Video on demand* (*VoD*) and
*Broadcast*. Main difference between these two services is the streaming
time scale. In *Broadcast* mode any new client connecting to the
streaming service assumes the time scale defined by the source, and this
time scale is shared among all connected clients. In *VoD* service a new
time scale is built for each client. The client not only selects
resource, but also the starting time. When many *VoD* clients access the
same resource, each one has its own time scale, and each time scale is
reset if the client breaks the connection. *Kurento* is currently supporting
Broadcast services, but in future versions it will also support true
*VoD* mode.

Kurento API architecture
------------------------

*Kurento* is a multimedia platform that provides
streaming capabilities in a very flexible way. As described in the
:ref:`Architecture Description <architecture>`,
Kurento is a modular system where a set of basic functional blocks,
called :term:`MediaElements  <element, media>`, that live in containers, called :term:`MediaPipelines <pipeline, media>`,
are connected together to build multimedia services. There are three main
:rom:cls:`MediaElement` families:

-  **Endpoints**: Endpoints provide transfer capabilities, allowing
   bidirectional communication channels with external systems. Supported
   protocols include muxers, like *WebM* or *MP4* for file operations
   and following streaming protocols: *HTTP*, *RTP* and *WebRTC*.

-  **Filters**: Filters are responsible of media processing, including
   transcodification, computer vision, augmented reality, etc.

-  **Mixers**: Mixers combines the stream from endpoints. They are also
   known as :rom:cls:`Hub`. The main mixers types are :rom:cls:`Dispatcher`
   and :rom:cls:`Composite`.

*Kurento* consists of two main software components: Kurento Media
Server (:term:`KMS`) and Kurento Media Framework
(:term:`KMF`):

-  **KMS**: *Kurento Media Server* is a stand-alone server responsible
   of the media process and delivery. It is the component that hosts
   *Endpoints* and *Filters*.

-  **KMF**: *Kurento Media Framework* is the SDK that enables
   applications to control *KMS* features and publish multimedia
   services. *KMF* can be incorporated to web applications hosted by
   *Kurento Application Server* (:term:`KAS`) and provides the following APIs:

   -  :ref:`Content API <kmf-content-api>`: High-level middleware layer
      of services intended to simplify communications with clients.
      It also offers Open API to clients.
   -  :ref:`Media API <kmf-media-api>`: Low-level API that provides
      full control of :term:`KMS` elements. It is normally used in
      conjunction with *Content API*.
   -  *HTML5 SDK*: Javascript SDK intended to provide better control of
      media playing in web applications. It uses Open API (based on
      JSON-RPC over http and websockets) to communicate with Content
      API in the server.

.. _programming-with-kmf-content-api:

Programming with the Kurento Java EE Content API
-----------------------------------------------------------

The *Content API* SDK is intended to simplify setup and management of
multimedia connections between *KMS* and web applications. Built on top
of the *Java Servlet* API, implements a *REST*-like interface based on
JSON-RPC that controls the following multimedia services:

-  **HTTP services**: Enables download and upload of multimedia
   contents.
-  **RTP services**: Allows the setup of bidirectional RTP
   connections.
-  **WebRTC services**: Controls *WebRTC* connections with browsers
   and mobile devices implementing the *WebRTC* stack.

It is important to notice that the *Content API* is just a *KMS* control
interface and does not handles media directly.

Content services
~~~~~~~~~~~~~~~~

Applications offering multimedia services have to setup and manage *KMS*
*Endpoints*. The problem with *Endpoints* is that they are heterogeneous
and their operation depends on the underlying streaming protocol. This
is the reason why the *Content API* defines the concept of *content
service* as a mechanism to provide a simple and homogeneous interface
for the creation and management of multimedia connections.

A *content service* consists of a standard *Java bean* implementing the
*service handler* interface. *Service handlers* are identified because
they are annotated as follows:

#. ``@HttPlayerService``: Declares a player service intended to deliver
   content to *HTML5* ``<video>`` elements. The *service handler* must
   extend class ``HttpPlayerHandler``.
   ::

       @HttpPlayerService(path = "/myPlayerService")
       public class MyService extends HttpPlayerHandler{
           //…
       }

#. ``@HttpRecorderService``: Allows the application to publish a
   recorder service, enabling media injection into *KMS* through the
   *HTTP file upload* protocol. The recorder *service handler* must
   extend class ``HttpRecorderHandler``.
   ::

       @HttpRecorderService(path = "/myRecorderService")
       public class MyService extends HttpRecorderHandler{
           //…
       }

#. ``@RtpContentService``: Defines a bidirectional *RTP* connection. The
   *service handler* must extend class ``RtpContentHandler``.
   ::

       @RtpContentService(path = "/myRtpService")
       public class MyService extends RtpContentHandler{
           //…
       }

#. ``@WebRtcContentService``: Intended for bidirectional WebRTC
   connections. Its *service handler* must extend class
   ``WebRtcContentHandler``
   ::

       @WebRtcContentService(path = "/myWebRtcService")
       public class MyService extends WebRtcContentHandler{
           //…
       }

At runtime the *Content API* engine searches *content service*
annotations, instantiating a *service entry point* for each *service
handler* found. Internally a *service entry point* is basically an
*HTTP servlet* mapped to a *service URL* where clients can send HTTP
request with control commands. Developers do not have to care about servlet
configuration or initialization, as the "Content API" takes care of this
operations. The *service URL* has format below::

    http://myserver/myApp/myServiceName

where

-  \ *myserver*\  : is the IP address or hostname of the *Kurento
   Application Server*.
-  \ *myApp*\ : is the application context, namely the WAR
   archive name if not otherwise specified.
-  \ *myServiceName*\  : is the value given to the
   ``path`` attribute of service annotation.

As a summary, in order to create a *content service* the application
must implement a *service handler*, which is a *Java bean* with a common
interface. The *Content API* instantiates an *HTTP servlet* for each
*service handler* found. This servlet is known as the *service entry
point*, and can be reached at the *service URL*. Service operation and
management is independent of the underlying *KMS* *Endpoint* type. It is
important to understand that developers do not need to care about
instantiation of ''service entry points' '' servlets and that these are
used just for control purposes and not for media delivery.

HTTP Player Service
^^^^^^^^^^^^^^^^^^^

The *HTTP Player service* instantiates a download service intended for
*HTML5 streaming*. Method ``onContentRequest()`` is called every time
the *service entry point* receives a GET request from a client using Open
API (directly or with HTML5 SDK).

::

    import com.kurento.kmf.content.HttpPlayerHandler;
    import com.kurento.kmf.content.HttpPlayerService;
    import com.kurento.kmf.content.HttpPlayerSession;

    @HttpPlayerService(path = "/myPlayerService")
    public class MyService extends HttpPlayerHandler{

        @Override
        public void onContentRequest(HttpPlayerSession session) throws Exception {
            session.start("file:///path/to/myvideo");
        }
    }

*KMS* instantiates *HTTP Endpoints* on behalf of this service every time
a new request arrives. *HTTP Endpoints* transform content on the fly to
:term:`WebM` (by default) or :term:`MP4`  before encapsulation and delivery,
allowing source files to have any format supported by *Gstreamer*.

*HTML5* browsers can access the content by adding the *service URL* as
source of the tag ``<video>``.

.. sourcecode:: html

    <video>
        <source src="http://myServer/myApp/myPlayerService" type ="video/webm"/>
    </video>

Current version of the *Content API* only supports *live* mode
independently of the nature of the media archive. Future versions will
support pseudo-streaming for media resources whose file size can be
known before transmission is started.

-  **Known issues**:

   -  In current version, only the WebM muxer is supported. Hence,
      the HTTP endpoints generated media flows can be only consumed by
      browsers supporting that format (i.e. Firefox an Chrome). Future
      versions will also support MP4 making HTTP endpoints compatible with
      Microsoft IE and Safari.
   -  It is known a bad behaviour with Chrome when the *service URL* is
      placed in the address bar of the browser. This is due to a
      reconnection Chrome performs when detects MIME of type video or
      audio. Root cause for this problem relates to the fact that
      *Kurento* provides *VoD* services based on top of a broadcast
      service, and time scale initialization is not performed on
      reconnection. Future versions will provide true *VoD*
      capabilities, solving this problem.

HTTP Recorder Service
^^^^^^^^^^^^^^^^^^^^^

*HTTP recorder service* allows applications to inject contents into
:term:`KMS` through the standard file upload protocol. Every time that
a ``POST`` request with a :mimetype:`multipart/form-data` body is received
in the *service entry point*, the method
:java:meth:`~com.kurento.kmf.content.HttpRecorderHandler#onContentRequest()`.
The receiver *HTTP Endpoint* will search for the first *content part*
with a supported multimedia format and will send it to the media resource
specified by the handler (``file:///myfile``). *Recorder service* accepts
from client any multimedia format supported by *Gstreamer*, but transforms
content to :term:`WEBM` before storing it. [#]_

::

    import com.kurento.kmf.content.HttpRecorderHandler;
    import com.kurento.kmf.content.HttpRecorderService;
    import com.kurento.kmf.content.HttpRecorderSession;

    @HttpRecorderService(path = "/myRecorderService")
    public class MyRecorderService extends HttpRecorderHandler {

        @Override
        public void onContentRequest(HttpRecorderSession contentSession)
                throws Exception {

            contentSession.start("file:///myfile.webm");
        }
    }

Browsers can access this service through HTML forms, addressed to the
*service URL*, that include inputs of type file. If more than one file
is present the request will accept only first one found.

.. sourcecode:: html

    <form action=”http://myServer/myApp/myRecorderService”>
        File: <input type="file" name="data" >
    </form>

RTP & WebRTC Service
^^^^^^^^^^^^^^^^^^^^

*RTP* and *WebRTC* requires a negotiation process where each side sends
its connection details and supported formats encoded in a *SDP*
(*Session Description Protocol*) packet. *RTP* and *WebRTC* services
hide negotiation complexity, offering applications the same interface
used for the well-known *HTTP* services. Method ``onContentRequest()``
is called each time a *POST* request with a connection offer is received
by the *service entry point*.

::

    import com.kurento.kmf.content.WebRtcContentHandler;
    import com.kurento.kmf.content.WebRtcContentService;
    import com.kurento.kmf.content.WebRtcContentSession;
    import com.kurento.kmf.media.MediaPipeline;
    import com.kurento.kmf.media.MediaPipelineFactory;
    import com.kurento.kmf.media.PlayerEndpoint;
    import com.kurento.kmf.media.RecorderEndpoint;

    @WebRtcContentService(path = "/myWebRtcService")
    public class MyWebRtpService extends WebRtcContentHandler {

        @Override
        public void onContentRequest(WebRtcContentSession contentSession)throws Exception {

                   contentSession.start("file:///fileToSend.webm", "file:///fileToRecord.webm");
        }
    }

*RTP* and *WebRTC* are bidirectional protocols that can send and receive
at the same time. For that reason, ``start`` method requires
both: *source* and *sink* elements.

In the previous example, the received media from the client will be
recorded into ``fileToRecord.webm`` and the media to deliver to the
client is read from ``fileToSend.webm``. The start method it not limited
to read from files and write to files. More complex media pipelines can 
be created with Media API as we will see in the following sections
of this document.

The client starting the communication with the server specifies some
constraints for media direction in the negotiating phase. The handler
can access to this constraints individually for video and audio streams
with methods :java:meth:`SdpContentSession.getVideoConstraints()` and
:java:meth:`SdpContentSession.getAudioConstraints()`.
These methods return one of the following values:

-  **SENDONLY**: *KMS* delivers media to remote peer and does not
   receive.
-  **RECVONLY**: *KMS* receives media from remote peer and does not
   deliver.
-  **SENDRECV**: *KMS* sends and receives media at the same time.
   Received media is stored into connected recorder while delivered
   media is read from connected player.
-  **INACTIVE**: There is no media transfer in any direction,
   independently of any player or recorded connected.

Played file can take any format supported by *Gstreamer* and will be
translated to format negotiated with remote peer. Stored file will be
converted to format *WebM* or *MP4* from format negotiated with remote
peer.


Content Session & Media lifecycle
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The *content session* is the mechanism offered by the *Content API* to
manage multimedia transactions. Its state depends on media events
detected in the *Endpoint*, control events detected in the *service
entry point* and application commands.

The *content session* is created when a request is received in the
*service entry point*. Method ``onContentRequest()`` is called in the
*service handler*, so the application can accept or reject requests.
Rejected requests must provide the message and the *HTTP* error code
that will be returned to browser.
::

    @Override
    public void onContentRequest(WebRtcContentSession contentSession) throws Exception {
        contentSession.terminate(404, "Content not found");
    }

When the *service handler* wants to accept a request it must provide the
source and sink media resources that will be connected to the
*Endpoint*. Method :java:meth:`start()` is called for this purpose.
::

    @Override
    public void onContentRequest(WebRtcContentSession contentSession) throws Exception {

        contentSession.start("file:///fileToSend.webm","file:///fileToRecord.webm");
    }

The *Endpoint* informs applications when a media transfer starts by
calling the optional method :java:meth:`onContentStarted()`.
::

    @Override
    public void onContentStarted(WebRtcContentSession contentSession) Exception {
        // Execute specific application logic when content (media) starts being served to the client
    }

Optional method :java:meth:`onSessionTerminated()` is called when *Endpoint*
completes media transfer. The *content session* termination code is
provided in this call.
::

    @Override
    public void onSessionTerminated(WebRtcContentSession contentSession, int code, String reason)
                            throws Exception {
        // Execute specific application logic when content session terminates
    }

The *content session* is terminated automatically if the *Endpoint*
experiences an unrecoverable error not caused by a direct application
command. Events like client disconnection, file system access fail, etc.
are the main error cause . Any of these exceptions can be handled on
:java:meth:`onUncaughtException()`.
::

    @Override
    public void onUncaughtException(HttpPlayerSession contentSession, Throwable exception) throws Exception {
        // Execute specific application logic if there is an unrecoverable
        // error on the media infrastructure. Session is destroyed after
        // executing this code
    }

If exceptions are not handled, they will be propagated and the method
:java:meth:`onSessionError()` will be called with the error code and description.
::

    @Override
    public void onSessionError(WebRtcContentSession contentSession, int code, String description) throws Exception {
        // Execute specific application logic if there is an unrecoverable
        // error on the media infrastructure. Session is destroyed after
        // executing this code
    }

The *content session* is able to store and manage application attributes
through its lifecycle, in a similar way as ``HttpSession`` does. Method
:java:meth:`setAttribute()` stores an object that can later be retrieved with
method :java:meth:`getAttribute()` or deleted with method :java:meth:`removeAttribute()`.
::

    @Override
    public void onContentRequest(WebRtcContentSession contentSession) throws Exception {

        contentSession.setAttribute("source", "source.avi");
        contentSession.setAttribute("sink", "sink.webm");
        //...
    }

    @Override
    public void onContentStarted(WebRtcContentSession contentSession) throws Exception {
        String source = (String) contentSession.getAttribute("source");
        String sink = (String) contentSession.getAttribute("sink");
        log.info("Start playing: " + source);
        log.info("Start recording:" + sink);
    }

One important feature of the *content session* is its capability to
share real time information with clients through a bidirectional
channel. In order to interchange messages with a client the
:doc:`Open API <Open_API_Specification>` has to be used. For web browsers 
ti is recommended to connect to the server with the HTML5 SDK,
because it fully implements OpenAPI.

The OpenAPI is implemented following a signalling protocol based on
:term:`JSON-RPC` 2.0. Messages can be interchanged between the *service
handler* and the client while the *content session* is active. Method
:java:meth:``publishEvent()`` is used for this purpose. This capability
is quite useful combined with computer vision filter, as it allows sending
events to clients coming from video content analysis (e.g. plate recognised,
QR code detected, face detected, etc.)
::

    @Override
    public void onContentStarted(WebRtcContentSession contentSession) throws Exception {
        ContentEvent event = new ContentEvent();
        event.setType("tittle");
        event.setData("My Video");
        contentSession.publishEvent(event);
    }

Clients can also send messages to the *content session* through this
channel. Client messages are called commands and are received on handler
method ``onContentCommand()``
::

    @Override
    public ContentCommandResult onContentCommand( WebRtcContentSession contentSession,
                    ContentCommand contentCommand)
      throws Exception {
        String data = contentCommand.getData();
        String type = contentCommand.getType();
            
        //Process command...

        return new ContentCommandResult("OK");
    }

See the
:doc:`Open API <Open_API_Specification>` specification for a detailed
reference of available commands and events that can be exchanged between
*service handlers* and HTML5 SDK clients.

Content identification
~~~~~~~~~~~~~~~~~~~~~~

Content identification can be understood as the process of mapping media
resources to URLs. The rules and algorithms used are quite variable and
application dependant, although there are several possible strategies. A
very common one is the direct mapping between the URL path and a file
system path, which actually is the strategy used by the most HTTP
servers to map static resources. Other alternative is to assign a
content ID to each media resource. This content ID can be placed in the
URL's path info or in the query string, as parameter. The server
searches for the content ID in the appropriate place and looks up a
mapping table.

The *content session* provides method ``getContentId()`` that returns
the path info of requested URL’s, assuming the content ID is placed
there, as shown below:

Content URL: `http://myserver/myApp/myServicePath/{contentId}`
    *myserver*: IP address or name of *Kurento Application Server*
    *myApp*: Application name. Normally is the WAR archive name
    *myServicePath*: Value assigned to ``path`` attribute of service
    annotation
    *{contentId}*: URL's path info. Everything left between service name
    and the URL's query string.

::

    @Override
    public void onContentRequest(HttpPlayerSession contentSession) throws Exception {
        String contentId = contentSession.getContentId();
        contentSession.start("file:///path/to/myrepo/" + contentId);
    }

If a different content ID strategy, based in a query string parameter or
the like, is used, the application can directly access the requested URL
through method ``getHttpServletRequest()``
::

    @Override
    public void onContentRequest(HttpPlayerSession contentSession) throws Exception {
        String contentId;
        HttpServletRequest request = contentSession.getHttpServletRequest();
        request.getContextPath();
        request.getQueryString();

        // build content ID from URL

        contentSession.start("file:///path/to/myrepo/" +contentId);
    }

Notice you'll have to add the Servlet API dependency to the ``pom.xml``
before being able to import ``HttpServletRequest`` in your code.

.. sourcecode:: xml

    <dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>javax.servlet-api</artifactId>
        <version>3.0.1</version>
        <scope>provided</scope>
    </dependency>

Media resource management
~~~~~~~~~~~~~~~~~~~~~~~~~

The *Content API* does not require an explicit resource management
unless the application directly builds *KMS MediaElements*. Lifecycle of
created *MediaElements* is not managed anymore by the *content session*,
so the application must care about how and when resources are released.
In order to facilitate resource management, the *content session*
provides a mechanism to attach *MediaElements* to the session lifecycle.
Method ``releaseOnTerminate()`` can be used for this purpose.
::

    MediaPipelineFactory mpf = contentSession.getMediaPipelineFactory();
    MediaPipeline mp = mpf.create();

    PlayerEndpoint player = mp.createPlayerEndpoint("file:///path/to/myplayed.avi");
    contentSession.releaseOnTerminate(player);

    HttpGetEndpoint httpEndpoint = mp.newHttpGetEndpoint().terminateOnEOS().build();
    player.connect(httpEndpoint);
    contentSession.start(httpEndpoint)


Single elements can be attached to a session lifecycle, but also the
whole *MediaPipeline*, depending on application needs.
::

    MediaPipelineFactory mpf = contentSession.getMediaPipelineFactory();
    MediaPipeline mp = mpf.create();
    contentSession.releaseOnTerminate(mp);

*MediaElements* not attached to the *content session* will remain active
until an explicit release is performed.
::

    @Override
    public void onContentRequest(WebRtcContentSession contentSession) throws Exception {

        MediaPipelineFactory mpf = contentSession.getMediaPipelineFactory();
        MediaPipeline mp = mpf.create();

        PlayerEndpoint player = mp.newPlayerEndpoint("file:///d").build();

        contentSession.start(player);
    }

    @Override
    public void onSessionTerminated(WebRtcContentSession contentSession, int code, String reason)
                    throws Exception {
        player.release();
    }


Content Repository
~~~~~~~~~~~~~~~~~~

The Stream Oriented GE Java Content API provides a built-in *content
repository* to store media streams (video and audio files). The elements
stored in the repository (called *repository items*) can be accessed using
the method :java:meth:`HttpContentSession.start()` of the
:java:ref:`HttpContentSession` provided by the Java Content API.

The list of features implemented by the *content repository* are:

* Create repository items
* Set metadata in the repository items (key-value attributes)
* Find repository items (by its identifier, attribute value or regular expressions)
* Remove repository items

Let see a couple of examples to illustrate the way of working of the
*content repository*. First, the following  example shows how to use the
*content repository* to store the stream from an :java:type:`HttpRecorderEndpoint`::

    @HttpRecorderService(path = "/recorderRepository")
    public class RecorderRepository extends HttpRecorderHandler {

	    @Override
	    public void onContentRequest(HttpRecorderSession contentSession)
			    throws Exception {
		    final String itemId = "itemTunnel";
		    Repository repository = contentSession.getRepository();
		    RepositoryItem repositoryItem;
		    try {
			    repositoryItem = repository.findRepositoryItemById(itemId);
			    getLogger().info("Deleting existing repository '{}'", itemId);
			    repository.remove(repositoryItem);
		    } catch (NoSuchElementException e) {
			    getLogger().info("Repository item '{}' does not previously exist",
					    itemId);
		    }
		    repositoryItem = contentSession.getRepository().createRepositoryItem(
				    itemId);
		    contentSession.start("itemTunnel");
	    }

    }

This other example shows how to implement an :java:ref:`HttpPlayerHandler`
to play a *repository item* identified by the ``contentId`` parameter::

    @HttpPlayerService(path = "/playerRepository/*")
    public class PlayerRepository extends HttpPlayerHandler {

	    @Override
	    public void onContentRequest(HttpPlayerSession contentSession)
			    throws Exception {
		    String contentId = contentSession.getContentId();
		    RepositoryItem repositoryItem = contentSession.getRepository()
				    .findRepositoryItemById(contentId);
		    if (repositoryItem == null) {
			    String message = "Repository item " + contentId + " does no exist";
			    getLogger().warn(message);
			    contentSession.terminate(404, message);
		    } else {
			    contentSession.start(repositoryItem);
		    }
	    }

    }


.. _programming-with-kmf-media-api:

Programming with the Kurento Java Media API
------------------------------------------------------

*Kurento Media API* is a low level *Java* SDK providing full control of
*Kurento Media Server*. It is intended to be used at server side, in
conjunction with *Kurento Content API*, although it can also be used on
its own and even within standard *Java projects*, outside *Kurento
Application Server*.

Following dependency has to be added to ``pom.xml`` in order to use
*Kurento Media API*

.. sourcecode:: xml

    <dependencies>
    <!-- … -->
        <dependency>
            <groupId>com.kurento.kmf</groupId>
            <artifactId>kmf-media-api</artifactId>
            <version>|version|</version>
        </dependency>
    <!-- … -->
    </dependencies>

MediaPipeline
~~~~~~~~~~~~~

The ``MediaPipelineFactory`` is the API entry point. It can be obtained
from the *content session* when used in conjunction with the ''Content
API ''.
::

       @Override
        public void onContentRequest(HttpPlayerSession contentSession) throws Exception {
            MediaPipelineFactory mpf = contentSession.getMediaPipelineFactory();
        }

In order to use the *Media API* in stand-alone mode the application must
setup a `Spring framework <http://spring.io/>`__ context.
::

    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext("classpath:kmf-media-config.xml");
        MediaPipelineFactory mpf = context.getBean(MediaPipelineFactory.class);
    }

The Spring configuration file (``kmf-media-config.xml`` in example
above) must contain directive
``<context:component-scan base-package="com.kurento.kmf.media" />``, so
*Media API* components can be found. Optionally a bean of class
``com.kurento.kmf.media.MediaApiConfiguration`` can be added with custom
configurations.

.. sourcecode:: xml

    <beans xmlns="http://www.springframework.org/schema/beans"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:context="http://www.springframework.org/schema/context"
        xsi:schemaLocation="http://www.springframework.org/schema/beans
               http://www.springframework.org/schema/beans/spring-beans-3.0.xsd
               http://www.springframework.org/schema/context
               http://www.springframework.org/schema/context/spring-context-3.0.xsd">

        <context:annotation-config />
        <context:component-scan base-package="com.kurento.kmf" />

        <bean id="thriftInterfaceConfiguration" class="com.kurento.kmf.thrift.ThriftInterfaceConfiguration">
            <property name="serverAddress" value="127.0.0.1" />
            <property name="serverPort" value="9090" />
        </bean>
        <bean id="mediaApiConfiguration" class="com.kurento.kmf.media.MediaApiConfiguration">
            <property name="handlerAddress" value="127.0.0.1" />
            <property name="handlerPort" value="9191" />
        </bean>
    </beans>

The ``MediaPipelineFactory`` can now be injected with any of the
mechanism provided by Spring.
::

    public class MyApplication {

        @Autowired
        MediaPipelineFactory mpf;

        // Application code
    }

A ``MediaPipeline`` object is required to build media services. Method
``create()`` can be used in the ``MediaPipelineFactory`` for this
purpose.
::

    public void init() {
        MediaPipeline mp = mpf.create ();

        // Other initializations
    }

*MediaPipelines* are the containers where *KMS MediaElements* live.
*MediaElements* within a pipeline can be connected to build services,
but they are isolated from the rest of the system. This has to be taken
into account when programming applications.

As introduced before, currently there are two kinds of `MediaElements`,
namely `Endpoints` and `Filters`

Endpoints
~~~~~~~~~

.. _mp4-recorder:

:term:`KMS` :java:type:`MediaElements <MediaElement>` are created through
specific builders, allowing a
flexible initialization. Mandatory parameters must be provided in the
builder constructor, like the URL in the :java:type:`PlayerEndpoint`.
Optional parameters are set to defaults unless the application overrides
their values.
::

    public void createMediaElements() {
        MediaPipeline mp = mpf.create();
        HttpGetEndpoint httpEndpoint = mp.newHttpGetEndpoint()
            .withDisconnectionTimeout(1000)
            .withMediaProfile(MediaProfileSpecType.WEBM).build();

        PlayerEndpoint player = mp.newPlayerEndpoint("file:///myfile.avi")
            .build();

        RecorderEndpoint recorder = mp.newRecorderEndpoint("file:///myfile.mp4")
            .withMediaProfile(MediaProfileSpecType.MP4)
            .build();

        RtpEndpoint rtp = mp.newRtpEndpoint()
            .build();

        WebRtcEndpoint webrtc = mp.newWebRtcEndpoint()
            .build();

        ZBarFilter zbar = mp.newZBarFilter().build();

        // Do something with media elements
    }

*MediaElements* can be connected with method ``connect()``.
This method creates a directional connection between receiver *source*
and *sink* provided as parameter. All output streams of the *source*
element are connected to the input streams of the *sink* element.
 
::

    public void connectElements() {
        MediaPipeline mp = mpf.create();

        HttpGetEndpoint httpEndpoint = mp.newHttpGetEndpoint()
            .terminateOnEos().build();
        PlayerEndpoint player = mp.newPlayerEndpoint("file:///myfile.avi")
            .build();
        player.connect(httpEndpoint);

    }

Method ``connect()`` creates a directional connection between elements
*source* and *sink* provided as parameters. All output streams of the
*source* element are connected to the input streams of the *sink*
element.
::

    public void connectElements() {
        MediaPipeline mp = mpf.create();

        HttpGetEndpoint httpEndpoint = mp.newHttpGetEndpoint()
            .build();

        PlayerEndpoint player = mp.newPlayerEndpoint("file:///myfile.avi")
            .build();

        mp.connect(player, httpEndpoint);
>>>>>>> Stashed changes
    }

In order to create bidirectional connections the application must
perform a connect operation in both directions.
::

    public void back2back () {
        MediaPipeline mp = mpf.create();

        RtpEndpoint rtpA = mp.newRtpEndpoint().build();
        RtpEndpoint rtpB = mp.newRtpEndpoint().build();
            
        rtpA.connect(rtpB);
        rtpB.connect(rtpA);
    }

Notice that method ``connect()`` won't do anything when elements without
input streams, like ``PlayerEndpoint`` are passed as *sink* or elements
with no output streams, like ``RecorderEndpoint``, are passed as
*source*.

The *Media API* provides an asynchronous interface for those
applications that cannot afford to block their calls until *KMS*
responds. The asynchronous interface improves performance at a cost of
increase in complexity.
::

    private MediaPipeline mp;

    public void buildAsync () {
        mp = mpf.create();
        mp.newHttpGetEndpoint().buildAsync( new Continuation<HttpGetEndpoint>() {
            @Override
            public void onSuccess(HttpGetEndpoint result) {
                connectAsync (null, result);
            }
            @Override
            public void onError(Throwable cause) {
                // log error
            }
        });

        mp.newPlayerEndpoint("file:///myfile.webm").buildAsync( new
            Continuation<PlayerEndpoint>() {
            @Override
            public void onSuccess(PlayerEndpoint result) {
                connectAsync (result, null);
            }
            @Override
            public void onError(Throwable cause) {
                // log error
            }

        });
    }

    private HttpGetEndpoint http;
    private PlayerEndpoint player;

    public void connectAsync(PlayerEndpoint player, HttpGetEndpoint http) {
        if (player != null) {
            this.player = player;
        }
        if ( http != null) {
            this.http = http;
        }
        if (player != null && http != null){
            player.connect(http);
        }
    }

Let us discuss briefly the different Endpoints offered by kurento:

HttpGetEndpoint
    An ''HttpGetEndpoint'' contains source ''Media Pads'' for audio
    and video, delivering media using HTML5 pseudo-streaming mechanism.
    This type of  endpoint provide unidirectional communications. Its
    ''Media Sink'' is associated with the HTTP GET method.

    A ''Media Pad'' is an element´s interface with the outside world.
    The data streams flow from the ''Media Source'' pad to another
    element's ''Media Sink'' pad.
HttpPostEndpoint
    An ''HttpPostEndpoint'' contains sink pads for audio and video,
    which provide access to an HTTP file upload function This type
    of endpoint provide unidirectional communications. Its
    ''Media Sources'' are accessed through the HTTP POST method.
PlayerEndpoint
    A ''PlayerEndpoint'' retrieves content from seekable sources in
    reliable mode (does not discard media information) and inject
    them into KMS. It contains one ''Media Source'' for each media
    type detected.
RecorderEndpoint
    A ''RecorderEndpoint''  provides function to store contents in
    reliable mode (doesn't discard data). It contains ''Media Sink''
    pads for audio and video.
RtpEndpoint
    A ''RtpEndpoint'' provides bidirectional content delivery capabilities with remote networked peers through RTP protocol. It contains paired sink and source ''Media Padsource '' for audio and video.
WebRtcEndpoint
    A ''WebRtcEndpoint'' provide media streaming for Real Time Communications (RTC) through the web.


Filters
~~~~~~~

Filters are MediaElements that perform media processing, computer vision,
augmented reality, and so on. Let's see some of the filters available:

JackVaderFilter
    JackVaderFilter detects faces in a video feed. Those on the right half
    of the feed are overlaid with a pirate hat, and those on the left half
    are covered by a Darth Vader helmet. This is an example filter, intended
    to demonstrate how to integrate computer vision capabilities into the KMS
    multimedia infrastructure.

    .. sourcecode:: java

            JackVaderFilter filter = mediaPipeline.newJackVaderFilter().build();

ZBarFilter
    This filter detects QR and bar codes in a video feed. When a code is found,
    the filter raises a :java:type:`CodeFoundEvent`. Clients can add a listener
    to this event using the method:

    .. sourcecode:: java

        ZBarFilter zBarFilter = mediaPipeline.newZBarFilter().build();
        zBarFilter.addCodeFoundDataListener(new MediaEventListener<CodeFoundEvent>() {
        @Override
        public void onEvent(CodeFoundEvent event) {
            log.info("Code Found " + event.getValue());
            // ...
            });
        }

FaceOverlayFilter
    This type of filter detects faces in a video feed. The face is then overlaid with an image.

    .. sourcecode:: java

        MediaPipeline mp = session.getMediaPipelineFactory().create();
        FaceOverlayFilter faceOverlayFilter = mp.newFaceOverlayFilter().build();
        // xoffset%, y offset%, width%, height%
        faceOverlayFilter.setOverlayedImage("/img/masks/mario-wings.png", -0.35F, -1.2F, 1.6F, 1.6F);

PointerDetectorFilter and PointerDetectorAdvFilter
    These type of filters detects pointers in a video feed. The difference is
    in the way of calibration of such pointers.

    .. sourcecode:: java

        PointerDetectorWindowMediaParam start = new PointerDetectorWindowMediaParamBuilder(
            "start", 100, 100, 280, 380).withImage("/img/buttons/start.png").build();
        PointerDetectorAdvFilter pointerDetectorAdvFilter = mediaPipeline
                    .newPointerDetectorAdvFilter(new WindowParam(5, 5, 50, 50))
                    .withWindow(start).build();

GStreamerFilter
    This is a generic filter interface, that creates GStreamer filters in the media server.

    .. sourcecode:: java

        GStreamerFilter mirrorFilter = mediaPipeline.newGStreamerFilter("videoflip method=4")
                    .build();

ChromaFilter
    This type of filter makes transparent a colour range in the top layer,
    revealing another image behind.

    .. sourcecode:: java

        ChromaFilter chromaFilter = mediaPipeline.newChromaFilter(
                    new WindowParam(100, 10, 500, 400)).build();


.. _programming-with-kws:

Programming with the Kurento HTML5 SDK
--------------------------------------

The *Kurento HTML5* SDK is a *Javascript* library implementing a *Content
API* and a *Media API* client. The following sections provides details
about these SDK libraries.

KWS Content API
~~~~~~~~~~~~~~~

It has been designed to be compatible with *node.js* infrastructure and
all its dependencies have been included into the *Node Package Modules*
(*NPM*). For that reason it is required the *NPM* dependency management
infrastructure to be installed.

.. sourcecode:: bash

    sudo apt-get install npm

Current release of HTML5 SDK does not provide a library archive, so it
must be built directly from the `source
code <https://github.com/Kurento/kws-content-api>`__. A `bundle
file <https://forge.fi-ware.org/frs/download.php/818/kws-content-api.min.js>`__
is also available at FI-WARE download page.

.. sourcecode:: bash

    git clone https://github.com/Kurento/kws-content-api.git
    cd kws-content-api
    npm install
    npm update
    node_modules/.bin/grunt

*Grunt* will place into directory ``dist`` four different *Javascript*
bundles adapted to browser usage.

If you are developing your application with maven, simply add the Kurento
Content Management API for Web SDK library (``kws-content-api.js``) as a
regular dependency:

.. sourcecode:: xml

    <dependencies>
        <!-- … -->
        <dependency>
            <groupId>com.kurento.kmf</groupId>
            <artifactId>kws-content-api</artifactId>
            <version>|version|</version>
        </dependency>
        <!-- … -->
    </dependencies>

This way, `kws-content-api.js` will be available in your web application root, as follows:


.. sourcecode:: html

    <html>
      <head>
        <script src=”./kws-content-api.js”/>
      </head>
      <body>
        <!-- … -->
      </body>
    </html>

In order to use the *Kurento HTML5* SDK the *Content API*
must activate the control protocol at handler level. Boolean attribute
``useControlProtocol`` is used for this purpose.
::

    @HttpPlayerService(path = "/myPlayerService" , useControlProtocol=true)
    public class MyPlayerService extends HttpPlayerHandler {

        @Override
        public void onContentRequest(HttpPlayerSession contentSession) throws Exception {
            // … Handler actions
        }

The *Kurento HTML5* SDK provides the following set of
*Content API* clients:

-  **KwsContentPlayer**: Allows connection with Kurento's *HTTP player
   handler* in order to implement download services.
-  **KwsContentUploader**: Intended to interoperate with the *HTTP
   recorder handler*. It allows implementing file upload services.
-  **KwsWebRtcContent**: Helps applications to setup WebRTC
   connections with the *WebRTC handler*.

Clients above are intended to connect one *Content API service*. The
constructor must provide the URL of the *service entry point*.

.. sourcecode:: html

    <script>
    function play(){
            var KwsContentPlayer = kwsContentApi.KwsContentPlayer;
            conn = new KwsContentPlayer("http://myServer/myApp/myPlayerService", options);
    }
    </script>

Optional parameters can be provided with configurations customized to
the service.

-  **'audio**': Sets the audio stream mode. Can be any of ``inactive``,
   ``sendonly``, ``recvonly`` and ``sendrecv``. Default value is
   ``sendrecv``.
-  **video**: Sets the video stream mode with the same alternatives
   available to audio. Default value is ``sendrecv``.
-  **localVideoTag**: ID of the ``<video>`` tag where local video will
   be displayed. No local video will be displayed if not defined.
-  **remoteVideoTag**: ID of the ``<video>`` tag where remote video
   will be displayed. No remote video will be displayed if not defined.
-  **iceServers**: *STUN/TURN* server array used by *WebRTC ICE*
   client. By default *Google* public *STUN* server is used.

Upon creation the client sends a start request to the server, causing
the method ``onContentRequest()`` to be called in the service handler.

The same *content session* events received in the *service handler* are
also available on the client side. Listeners are provided for this
purpose.

.. sourcecode:: html

    <html>
        <script>
        var uri = "http://www.example.com/jsonrpc";

        var options =
        {
             localVideoTag:  'localVideo',
             remoteVideoTag: 'remoteVideo'
        };

        var conn = new KwsWebRtcContent(uri, options);

        // Start and terminate events
        conn.on('start', function()
        {
            console.log("Connection started");
        });
        conn.on('terminate', function(reason)
        {
            console.log("Connection terminated due to "+reason.message);
        });

        // LocalStream and remoteStream events
        conn.on('localstream', function(data)
        {
            console.info("LocalStream set to "+data.url);
        });
        conn.on('remotestream', function(data)
        {
            console.info("RemoteStream set to "+data.url);
        });

        // Media event
        conn.on('mediaevent', function(data)
        {
            console.info("MediaEvent: "+JSON.stringify(data));
        });

        // Error
        conn.on('error', function(error)
        {
            console.error(error.message);
        });
        </script>
        <body>
        <video id=”localVideo”/>
        <video id=”remoteVideo”/>
        </body>
    </html>



KWS Media API
~~~~~~~~~~~~~

KWS Media API provides the capabilities to create Media Pipelines and
Media Elements in the KMS without a KAS. In other words, with KWS Media
API we can create Kurento-based applications directly in JavaScript.

To describe this API, we are going to show how to create a basic pipeline
that play a video file from its URL and stream it over HTTP. You can also
download and check this `example full source code
<https://github.com/Kurento/kws-media-api/tree/develop/example/PlayerEndpoint-HttpGetEndpoint>`_.

* Create an instance of the KwsMedia class that will manage the connection
  with the Kurento Media Server, so you'll need to provide the URI of its
  WebSocket endpoint. Alternatively, instead of using a constructor, you
  can also provide success and error callbacks:

  .. sourcecode:: js

   var kwsMedia = kwsMediaApi.KwsMedia(ws_uri);
   kwsMedia.onconnect = function(kwsMedia)
   {
     //…
   };
   kwsMedia.onerror = function(error)
   {
     //…
   };
   kwsMediaApi.KwsMedia(ws_uri, function(kwsMedia)
   {
     //…
   },
   function(error)
   {
     //…
   });

* Create a pipeline. This will host and connect the diferent elements. In
  case of error, it will be notified on the ```error``` parameter of the
  callback, otherwise this will be null as it's common on Node.js style APIs:

  .. sourcecode:: js

   kwsMedia.createMediaPipeline(function(error, pipeline)
    {
     //…
    });

* Create the elements. The player need an object with the URL of the video,
  and and we'll also subscribe to the 'EndOfStream' event of the HTTP stream:

  .. sourcecode:: js

   PlayerEndpoint.create(pipeline,
   {uri: "https://ci.kurento.com/video/small.webm"},
   function(error, player)
   {
     //…
   });

   HttpGetEndpoint.create(pipeline, function(error, httpGet)
   {
     httpGet.on('EndOfStream', function(event)
     {
       //…
     });

     //…
   });

* Connect the elements, so the media stream can flow between them:

  .. sourcecode:: js

   pipeline.connect(player, httpGet, function(error, pipeline)
   {
     //…
   });


* Get the URL where the media stream will be available:

  .. sourcecode:: js

   httpGet.getUrl(function(error, url)
   {
     //…
   });


* Start the reproduction of the media:

  .. sourcecode:: js

   player.play(function(error)
   {
     //…
   });


Examples
--------

This section provides several examples of the *Kurento*
platform. To that aim we are going to use the Java Content and Media API
in the server-side, and the JavaScript Content API in the client-side.
The provided examples implement a *MediaPipeline* composed by a
*PlayerEndpoint* connected to a *Filter* and generating a media flow
through an *HttpGetEndpoint*. The main difference between these two example
is the filter. The first example uses the *JackVaderFilter*. This filter
is an example of augmented reality element, since it recognizes faces in
media streams adding Jack Sparrow or Darth Vader hat onto these
faces.The second example uses the *ZBarFilter*. This filter is an example
of computational vision element, since it recognize bar and QR
codes in a media stream generating events with the information of the
detected codes in the stream. Therefore, the *MediaPipelines* used in
these examples are the following:

-  *PlayerEndpoint* → *JackVaderFilter* → *HttpGetEndpoint*
-  *PlayerEndpoint* → *ZBarFilter* → *HttpGetEndpoint*

For both examples, the handler (Java) and client (JavaScript) code is
provided.

JackVaderFilter
~~~~~~~~~~~~~~~

The handler code (Java) for this example is shown in the snippet below.
This handler is deployed in the KAS at the path
``http://myserver/myApp/playerJsonJackVader``. The *PlayerEndpoint* uses
an URL to locate a media stream
(https://ci.kurento.com/video/fiwarecut.webm) and then *JackVaderFilter*
puts a pirate hat in the faces of this video.
::

    //This annotation configures the platform to deploy a handler on the specified path
    @HttpPlayerService(path = "/playerJsonJackVader")
    public class PlayerJsonJackVaderFilter extends HttpPlayerHandler {

        @Override
        public void onContentRequest(HttpPlayerSession session) throws Exception {
            MediaPipelineFactory mpf = session.getMediaPipelineFactory();
            MediaPipeline mp = mpf.create();

            //This makes the pipeline (and all its elements) to be released when the session terminates
            session.releaseOnTerminate(mp);

            //Create a PlayerEndpoint for injecting a video into the platform
            PlayerEndpoint playerEndpoint = mp.newPlayerEndpoint(
                    "https://ci.kurento.com/video/fiwarecut.webm").build();

            //Create a filter for augmenting the video stream in real time.
            JackVaderFilter filter = mp.newJackVaderFilter().build();

            //Connect both elements
            playerEndpoint.connect(filter);

            //Store a player reference for later use
            session.setAttribute("player", playerEndpoint);

            //Calling "start" creates the HttpGetEndpoint and connects it to the filter
            session.start(filter);
            //Create an HttpGetEndpoint and connect it to the filter
            HttpGetEndpoint httpEndpoint = mp.newHttpGetEndpoint()
                            .terminateOnEOS().build();
            filter.connect(httpEndpoint);

            //Start session
            session.start(httpEndpoint);

        }

        @Override
        public void onContentStarted(HttpPlayerSession session) {
            //Content starts when the client connects to the HttpGetEndpoint
            //At that instant, the player must start reproducing the file
            PlayerEndpoint playerEndpoint = (PlayerEndpoint) session
                    .getAttribute("player");
            playerEndpoint.play();
        }

    }

In order to perform a request to this handler, we create a simple HTML
page in which the JavaScript Content API library (i.e.
*kws-content-api.js*) is used. Depending on your development methodoloy,
you may need to dowload that library to the appropriate directoy. This
HTML page must be included in the same WAR than the handler. Thus, in
order to locate the handler path the JavaScript object ``document.URL``
is used:

.. sourcecode:: html

    <!DOCTYPE html>
    <html>
    <head>
    <meta charset="utf-8">
    <title>Kurento</title>
    <script src="./kws-content-api.js"></script>

    <script>
        var conn;

        function start() {
            // Handler
            var handler = document.getElementById("handler").value;


            // Options
            var options = {
                remoteVideoTag: "remoteVideo"
            };

            // KwsContentPlayer instantiation
            var KwsContentPlayer = kwsContentApi.KwsContentPlayer;
            conn = new KwsContentPlayer(handler, options);

            // Media events log
            conn.on("mediaevent", function(data) {
                document.getElementById("events").value += JSON.stringify(data) + "\n";
            });
        }

        function stop() {
		    conn.terminate();
        }
    </script>
    </head>

    <body>
        <h1>Kurento Examples</h1>

        <label for="selectFilter">Handler</label>
        <select id="handler">
            <option value="./playerJsonJackVader">JackVaderFilter</option>
            <option value="./playerJsonZBar">ZBarFilter</option>

        </select>
        <br />

        <label for="status">Events</label>
        <textarea id="events"></textarea>
        <br />

        <button id="start" onclick="start()">Start</button>
        <button id="stop" onclick="stop()">Stop</button>

        <br />

        <video id="remoteVideo" autoplay></video>
    </body>
    </html>

All in all, to run this example we have to make a request using a
browser to hte URL of this HTML page (e.g.
``http://myserver/myApp/mypage.html``), select the *JackVaderFilter*
option and finally press the *Start* button. As a result, the stream
played is the video located in the URL determined in the handler
(https://ci.kurento.com/video/fiwarecut.webm) but showing the speaker of
the video with a pirate hut in his head. Notice that this example is
providing the media in WebM format, so it will only work on browsers
supporting it (e.g. Chrome and Firefox).

ZBarFilter
~~~~~~~~~~

The handler code (Java) for this example is shown below. This handler is
deployed in the KAS at the path
``http://myserver/myApp/playerJsonZBar``. The *PlayerEndpoint* uses an
URL to locate a media stream
(https://ci.kurento.com/video/barcodes.webm) and then *ZBarFilter*
generates media events with the detected codes within the video.
::

    @HttpPlayerService(path = "/playerJsonZBar")
    public class PlayerJsonZBarFilter extends HttpPlayerHandler {

        @Override
        public void onContentRequest(final HttpPlayerSession session)
                throws Exception {
            MediaPipelineFactory mpf = session.getMediaPipelineFactory();
            MediaPipeline mp = mpf.create();
            PlayerEndpoint player = mp.newPlayerEndpoint(
                    "https://ci.kurento.com/video/barcodes.webm").build();
            session.setAttribute("player", player);
            ZBarFilter zBarFilter = mp.newZBarFilter().build();
            player.connect(zBarFilter);
            HttpGetEndpoint httpEndpoint = mp.newHttpGetEndpoint()
                            .terminateOnEOS().build();
            zBarFilter.connect(httpEndpoint);
            session.start(httpEndpoint);

            zBarFilter
                    .addCodeFoundDataListener(new MediaEventListener<CodeFoundEvent>() {
                        @Override
                        public void onEvent(CodeFoundEvent event) {
                            session.publishEvent(new ContentEvent(event.getType(),
                                    event.getValue()));
                        }
                    });

        }

        @Override
        public void onContentStarted(HttpPlayerSession session) {
            PlayerEndpoint playerEndpoint = (PlayerEndpoint) session
                    .getAttribute("player");
            playerEndpoint.play();
        }

    }

To visualize the result of this handler, we use the same JavaScript code
included in the previous example. This time, we select the *ZBarFilter*
in the combo box and then press the *Start* button. As a result, the
video containing QR codes is played
(https://ci.kurento.com/video/barcodes.webm) and the detected codes by
the filter are written in the HTML textarea with id *events*.

Both *JackVaderFilter* and *ZBarFilter* examples can be developed as a
Maven project, and the resulting WAR is deployed in the KAS. An example
of ``pom.xml`` for this Maven project in shown below.

.. sourcecode:: xml

    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                 http://maven.apache.org/xsd/maven-4.0.0.xsd">

       <modelVersion>4.0.0</modelVersion>
       <groupId>com.kurento.kmf</groupId>
       <artifactId>kmf-content-helloworld</artifactId>
       <version>0.0.1-SNAPSHOT</version>
       <packaging>war</packaging>

       <properties>
          <project.build.sourceEncoding>UTF-8 </project.build.sourceEncoding>
          <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
          <maven.compiler.source>1.6</maven.compiler.source>
          <maven.compiler.target>1.6</maven.compiler.target>

          <!-- Kurento Dependencies Versions -->
          <kurento.version>|version|</kurento.version>

          <!-- Plugins Versions -->
          <maven-war-plugin.version>2.3</maven-war-plugin.version>
       </properties>

       <dependencies>
          <dependency>
             <groupId>com.kurento.kmf</groupId>
             <artifactId>kmf-content-api</artifactId>
             <version>${kurento.version}</version>
          </dependency>
        <dependency>
         <groupId>com.kurento.kmf</groupId>
         <artifactId>kws-content-api</artifactId>
         <version>${kws-content-api.version}</version>
      </dependency>

       </dependencies>

       <build>
          <plugins>
             <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-war-plugin</artifactId>
                <version>${maven-war-plugin.version}</version>
                <configuration>
                    <failOnMissingWebXml>false</failOnMissingWebXml>
                </configuration>
             </plugin>
          </plugins>
       </build>

    </project>

The previous examples and many others are available on `GitHub <https://github.com/Kurento/kmf-content-demo>`_:

.. sourcecode:: bash

    git clone https://github.com/Kurento/kmf-content-demo

.. rubric:: Footnotes

.. [#]

    WEBM is the format supported *out of the box* by the Content API.
    To use :term:`MP4` a specific Media Pipeline needs to be constructed,
    and  :java:field:`~com.kurento.kmf.media.MediaProfileSpecType.WebM`
    specified, like in `this sample <mp4-recorder>`:ref:.
