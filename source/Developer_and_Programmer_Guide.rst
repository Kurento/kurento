%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
 Developer and Programmer Guide
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

.. highlight:: java

Introduction
============

The *Stream Oriented GE Kurento* is a multimedia platform helping
developers to add multimedia capabilities to their applications. The
core element is *Kurento Media Server* (*KMS*) a
`Gstreamer <http://gstreamer.freedesktop.org/>`__ based multimedia
engine that provides following features:

-  Networked streaming protocols, including *HTTP* working as client and
   server, *RTP* and *WebRTC*.
-  Media transcodification between any of the codecs currently supported
   by Gstreamer.
-  Generic support for computational vision and augmented reality
   filters.
-  Media storage supporting writing operations for *WebM* and *MP4* and
   reading operations for any of *Gstreamer's* muxers.

`Java <http://www.java.com/>`__ and
`Javascript <http://www.w3.org/standards/webdesign/script>`__ SDKs are
available for developers to incorporate above features to applications.

About this guide
----------------

This guide, as the *Stream Oriented GE Kurento* itself, is under very
active development. Many features are constantly evolving and are not
completely documented yet. You can contribute to complete this guide and
also to Kurento effort by joining its community.

User Guide
==========

The *Stream Oriented GE Kurento* offers APIs devoted to programmers, not
to final users, so this section does not apply.

Programmer Guide
================

Things you need to know before start programing
-----------------------------------------------

-  The *Stream Oriented GE Kurento* software is released under `LGPL
   version 2.1 <http://www.gnu.org/licenses/lgpl-2.1.html>`__ license.
   This is quite a convenient license for programmers, but it is still
   recommended you check if actually fits your application needs.

-  `Maven <http://maven.apache.org/>`__ is used as dependency
   management tool for *Java* SDKs. Most likely
   `Gradle <http://www.gradle.org/>`__ can also be used, but we still
   haven't tested. If you don't use any dependency management you can
   still download the `KMF API
   Bundle <https://forge.fi-ware.eu/frs/download.php/819/kmf-api.jar>`__
   and incorporate manually all dependencies to your application, but
   this is not recommended.

-  `Spring framework <http://spring.io/>`__ is extensively used for
   lifecycle management of *Java* components. Developers are not
   required to develop ''Spring '' applications when using the *Stream
   Oriented GE Kurento* in *JEE* environments, but they'll have to in
   stand-alone *Java* applications.

Quick start
-----------

This section is intended to provide the very basic steps required to
integrate the *Stream Oriented GE Kurento's* framework into
applications.

Basic Setup
~~~~~~~~~~~

-  ***Install and configure KMS***: This piece of software is the actual
   engine providing media processing and delivery.

-  ***Install and configure JBoss 7 Application Server***: This is a
   *JEE* container that hosts the server side of applications. Other
   *Java* enterprise servers can be used, although no support from
   *Kurento* will be provided. This server will also be called *Kurento
   Application Server* (*KAS*) through the document.

The  :doc:`Kurento Installation and Administration
Guide <Installation_Guide>`
provides detailed information on installation and setup of above
components.

Create your first application
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Server side of your first application
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The *Stream Oriented GE Kurento* server SDK is a *Java* library known as
*Kurento Media Framework* (*KMF*). Following steps are required to
create a *Kurento* based application:

#. Create a *Maven* web project with your favourite IDE. You can use
   following ``pom.xml`` template

   .. sourcecode:: xml


       <?xml version="1.0" encoding="UTF-8"?>
       <project xmlns="http://maven.apache.org/POM/4.0.0"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                    http://maven.apache.org/xsd/maven-4.0.0.xsd">

           <modelVersion>4.0.0</modelVersion>
           <groupId>my.organization</groupId>
           <artifactId>my-kurento-demo</artifactId>
           <version>1.0.0 </version>
           <packaging>war</packaging>

       </project>

#. Make sure you add *KMF* dependencies to the ``pom.xml`` file

   .. sourcecode:: xml

       <dependencies>
           ...
           <dependency>
               <groupId>com.kurento.kmf</groupId>
               <artifactId>kmf-content-api</artifactId>
               <version>1.0.0</version>
           </dependency>
           ...
       </dependencies>

#. Create a properties file named ``kurento.properties`` including
   following configuration keys:

   .. sourcecode:: properties

       # Put here the IP address where the KMS process is executing
       # If you launched KMS in the same hosts where you are executing KAS, let it as 127.0.0.1
       mediaApiConfiguration.serverAddress=127.0.0.1

       # Put here the port where KMS management daemon is bound
       # If you did not movify KMS default configuation, let it as 9090
       mediaApiConfiguration.serverPort=9090

       # Put here the IP address where KAS management handler must listen
       # If you launched KMS int the same host where you are executing KAS, let it as 127.0.0.1
       mediaApiConfiguration.handlerAddress=127.0.0.1

       # Port where KAS management daemon will bind
       # Your can choose the port you want. By default we assume 9100.
       mediaApiConfiguration.handlerPort=9100

   *Kurento* framework will search this file in the following paths (in
   the specified order):

   #. *JBoss* configuration folder defined by property:
      ``${jboss.server.config.dir}``
   #. Directory specified by java option *kurento.properties.dir*:
      ``-Dkurento.properties.dir=/home/user/kurento``
   #. *WEB-INF* directory of *WAR* archive

#. Create a *Java* Class that extends ``HttpPlayerHandler`` and add
   annotation ``@PlayerService``. You'll have to implement method
   ``onContentRequest()`` to set the media resource to be played::

       import com.kurento.kmf.content.HttpPlayerHandler;
       import com.kurento.kmf.content.HttpPlayerService;
       import com.kurento.kmf.content.HttpPlayerSession;

       @HttpPlayerService(name = "myService", path = "/playerService", useControlProtocol=false)
       public class MyService extends HttpPlayerHandler{

            @Override
            public void onContentRequest(HttpPlayerSession session) throws Exception {

                session.start("file:///path/to/myvideo.webm ");
            }
        }


#. Place a *WebM* video so that the KMS process can reach it at whatever
   path you specified in ``/path/to/myvideo.webm``. This video will be
   the one read by the player element. You can replace the ``file:///``
   type URL by another one where a WebM file can be found, such as
   ``http://media.w3.org/2010/05/sintel/trailer.webm``
#. Deploy your project into *JBoss 7* server installed during the basic
   setup and launch it.

   .. sourcecode:: bash

       sudo cp mykurento.war $JBOSS_HOME/standalone/deployments
       $JBOSS_HOME/bin/standalone.sh

Client side of your first application
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The *Stream Oriented GE Kurento* is designed to work with an old plain
*HTML5* code. For testing your application, you just have to include a
``<video>`` tag linked to the *service URL* defined above. To do it, for
example, create an HTML file in your local machine containing the code
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
   order to understand how *Stream Oriented GE Kurento* features can
   help you to build multimedia applications.
-  Review :ref:`programming-with-kmf-content-api` for a detailed
   reference on content services.
-  Go to :ref:`programming-with-kmf-media-api` for a detailed explanation
   about how to achieve full control of :term:`Kurento Media Server`.
-  Review :ref:`programming-with-kws` for a
   detailed reference of capabilities available in browsers.

.. _basic-streaming-concepts:

Basic streaming concepts
------------------------

There are several streaming concepts that might be of interest in order
to know the precise behaviour that can expected when adding multimedia
resources to applications. This section is not strictly necessary and
can be skipped in a first reading.

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
because *HTTP* is a protocol not designed for media transfer. *HTML5
streaming* sessions starts with the browser sending a GET request to the
server. In this step both: browser and server play the *control
function* role. The server then maps the URL to the actual resource,
encapsulates its content in the response and sends it back to the
``<video>`` component, just like any download operation. Now browser and
server switch to the *media function*. There isn't a clear
differentiation between control and media functions that are played
sequentially by the same element in both sides. Apart form this function
mixup, many people will argue *HTTP* is not really streaming protocol as
there is no relation at all between media transfer pace an playing pace,
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
before stream starts transmission. The reason for the second condition
is because seeks must specify somehow the file position where the stream
must jump and that requires to know in advance the size or length of the
media resource and hence the whole resource must be available in
advance. Streaming protocols like *RTSP* and *HTTP* use header ``Range``
as a mean to build seek command. When the ``<video>`` component in a
*HTML5* application request a seek operation, the browser sends a new
GET request with the appropriate ``Range`` header. But this is only
available if the server provided the resource size in advance in the
first request (the one that initiated the stream). If resource size is
not available at start time, the video component does not show any kind
of progress bar, switching into *live* mode. *Stream Oriented GE
Kurento* is currently supporting only *live* mode, independently whether
the media resource is or not available in advance.

When designing streaming services it is also very important to determine
the type of service that is being offered. There are two main
classifications for streaming services: *Video on demand* (*VoD*) and
*Broadcast*. Main difference between these two services is the streaming
time scale. In *Broadcast* mode any new client connecting to the
streaming service assumes the time scale defined by the source, and this
time scale is shared among all connected clients. In *VoD* service a new
time scale is build for each client. The client not only selects
resource, but also the time origin. When many *VoD* clients access the
same resource, each one has its own time scale, and this time scale is
reset if the client breaks the connection. *Stream Oriented GE Kurento*
is currently supporting Broadcast services, but in future versions it
will also support true *VoD* mode.

Stream Oriented GE Kurento API architecture
-------------------------------------------

The *Stream Oriented GE Kurento* is a multimedia platform that provides
streaming capabilities in a very flexible way. As described in the
:doc:`Architecture Description <Architecture>`,
*Kurento* is a modular system where a set of basic functional blocks,
called *MediaElements*, that live in containers, called *MediaPipeline*,
are connected together to build multimedia services. There are two main
*MediaElements* families:

-  **Endpoints**: Endpoints provide transfer capabilities, allowing
   bidirectional communication channels with external systems. Supported
   protocols include muxers, like *WebM* or *MP4* for file operations
   and following streaming protocols: *HTTP*, *RTP* and *WebRTC*.

-  **Filters**: Filters are responsible of media processing, including
   transcodification, computer vision, augmented reality, etc.

The *Stream Oriented GE Kurento* consists of two main software
components: :term:`Kurento Media Server` (*KMS*) and :term:`Kurento Media Framework`
(*KMF*)

-  **KMS**: *Kurento Media Server* is a stand-alone server responsible
   of the media process and delivery. It is the component that hosts
   *Endpoints* and *Filters*.

-  **KMF**: *Kurento Media Framework* is the SDK that enables
   applications to control *KMS* features and publish multimedia
   services. *KMF* can be incorporated to web applications hosted by
   *Kurento Application Server* (*KAS*) and provides the following APIs:

   -  *Content API*: High-level middleware layer of services intended to
      simplify input/output operations.
   -  *Media API*: Low-level API that provides full control of *KMS*
      elements. It is normally used in conjunction with *Content API*.
   -  *HTML5 SDK*: Javascript SDK intended to provide better control of
      media reproduction in web applications.

.. _programming-with-kmf-content-api:

Programming with the Stream Oriented GE Java EE Content API
-----------------------------------------------------------

The *Content API* SDK is intended to simplify setup and management of
multimedia connections between *KMS* and web applications. Built on top
of the *JEE Servlet* API, implements a *REST* like interface that
controls following multimedia services:

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

A *content service* consist of a standard *Java bean* implementing the
*service handler* interface. *Service handlers* are identified because
they are annotated as follows:

#. ``@HttPlayerService``: Declares a player service intended to deliver
   content to *HTML5* ``<video>`` elements. The *service handler* must
   extend class ``HttpPlayerHandler``.
   ::

       @HttpPlayerService(path = "/myPlayerService")
       public class MyService extends HttpPlayerHandler{
           …
       }

#. ``@HttpRecorderService``: Allows the application to publish a
   recorder service, enabling media injection into *KMS* through *HTTP
   file upload* protocol. The recorder *service handler* must extend
   class ``HttpRecorderHandler``.
   ::

       @HttpRecorderService(path = "/myRecorderService")
       public class MyService extends HttpRecorderHandler{
           …
       }

#. ``@RtpContentService``: Defines a bidirectional *RTP* connection. The
   *service handler* must extend class ``RtpContentHandler``.
   ::

       @RtpContentService(path = "/myRtpService")
       public class MyService extends RtpContentHandler{
           …
       }

#. ``@WebRtcContentService``: Intended for bidirectional WebRTC
   connections. Its *service handler* must extend class
   ``WebRtcContentHandler``
   ::

       @WebRtcContentService(path = "/myWebRtcService")
       public class MyService extends WebRtcContentHandler{
           …
       }

At runtime the *Content API* engine searches *content service*
annotations, instantiating a *service entry point* for each *service
handler* found. A *service entry point* is basically an *HTTP servlet*
mapped to a *service URL* where clients can send HTTP request with
control commands. Developers do not have to care about servlet
configuration or initialization, as the "Content API" takes care of this
operations. The *service URL* has format below::

    http://myserver/myApp/myServiceName

where

-  \ *myserver*\  : is the IP address or hostname of *Kurento
   Application Server*.
-  \ *myApp*\ : is the application context, that use to be the WAR
   archive name.
-  \ *myServiceName*\  : is the value given to mandatory attribute
   ``path`` of service annotation.

As a summary, in order to create a *content service* the application
must implement a *service handler*, which is a *Java bean* with a common
interface. The *Content API* instantiates an *HTTP servlet* for each
*service handler* found. This servlet is known as the *service entry
point*, and can be reached at the *service URL*. Service operation and
management is independent of the underlying *KMS* *Endpoint* type. It is
important to understand that developers do not need to care about
instantiation of ''service entry points' '' servlets and that these are
used just for control purposes and no for media delivery.

HTTP Player Service
^^^^^^^^^^^^^^^^^^^

The *HTTP Player service* instantiates a download service intended for
*HTML5 streaming*. Method ``onContentRequest()`` is called every time
the *service entry point* receives a GET request from browser.

::

    import com.kurento.kmf.content.HttpPlayerHandler;
    import com.kurento.kmf.content.HttpPlayerService;
    import com.kurento.kmf.content.HttpPlayerSession;

    @HttpPlayerService(path = "/myPlayerService")
    public class MyService extends HttpPlayerHandler{

        @Override
        public void onContentRequest(HttpPlayerSession session) throws Exception {
            
            session.start("/path/to/myvideo ");
        }
    }

*KMS* instantiates *HTTP Endpoints* on behalf of this service every time
a new request arrives. *HTTP Endpoints* transform content on the fly to
*WebM* before encapsulation and delivery, allowing source files to have
any format supported by *Gstreamer*.

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

-  ***Known issues***:

   -  In current version, only the WebM muxer is supported. Hence,
      HttpEndpoint generated media flows can be only consumed by
      browsers supporting that format (i.e. Firefox an Chrome). Future
      versions will also support MP4 making HttpEndpoint compatible with
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
*KMS* through the standard file upload protocol. Method
``onContentRequest()`` will be called for each ``multipart/form`` *POST*
request received in the *service entry point*. The receiver *HTTP
Endpoint* will search for the first *content part* with a supported
multimedia format and will feed the media resource specified by the
handler (``file://myfile``). *Recorder service* accepts from client any
multimedia format supported by *Gstreamer*, but transforms content to
*WebM* or *MP4* before writing to file.

::

    import com.kurento.kmf.content.HttpRecorderHandler;
    import com.kurento.kmf.content.HttpRecorderService;
    import com.kurento.kmf.content.HttpRecorderSession;

    @HttpRecorderService(name = "myRecorder", path = "/myRecorderService")
    public class MyRecorderService extends HttpRecorderHandler{

        @Override
        public void onContentRequest(HttpRecorderSession contentSession)
                throws Exception {
            
            contentSession.start("file://myfile.webm");
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
hide negotiation complexity offering applications the same interface
used for the well-known *HTTP* services. Method ``onContentRequest()``
is called each time a *POST* request with a connection offer is received
by the *service entry point*.

::

    import com.kurento.kmf.content.WebRtcContentHandler;
    import com.kurento.kmf.content.WebRtcContentService;
    import com.kurento.kmf.content.WebRtcContentSession;
    import com.kurento.kmf.media.MediaPipeline;
    import com.kurento.kmf.media.MediaPipelineFactory;
    import com.kurento.kmf.media.PlayerEndPoint;
    import com.kurento.kmf.media.RecorderEndPoint;

    @WebRtcContentService(path = "/myWebRtcService")
    public class MyWebRtpService extends WebRtcContentHandler{

        @Override
        public void onContentRequest(WebRtcContentSession contentSession)throws Exception {
            
        contentSession.start(sourceMediaElement, sinkMediaElement);
        }
    }

*RTP* and *WebRTC* are bidirectional protocols that can send and receive
at the same time. For that reason method start requires both: *source*
and *sink* elements. The input/ouput stream configuration for a given
connection can be known thanks to methods ``getVideoConstraints()`` and
``getAudioConstraints()``, that returns one of following values:

-  ***SENDONLY***: *KMS* delivers media to remote peer and does not
   receive.
-  ***RECVONLY***: *KMS* receives media from remote peer and does not
   deliver.
-  ***SENDRECV***: *KMS* sends and receives media at the same time.
   Received media is stored into connected recorder while delivered
   media is read from connected player.
-  ***INACTIVE***: There is no media transfer in any direction,
   independently of any player or recorded connected.

Played file can take any format supported by *Gstreamer* and will be
translated to format negotiated with remote peer. Stored file will be
converted to format *WebM* or *MP4* from format negotiated with remote
peer.
::

    @Override
    public void onContentRequest(WebRtcContentSession contentSession)throws Exception {

        Constraints videoConstraints = contentSession.getVideoConstraints();
        Constraints audioConstraints = contentSession.getAudioConstraints();
            
        if ( videoConstraints.equals(Constraints.SENDONLY) &&
             audioConstraints.equals(Constraints.SENDONLY) ) {
                contentSession.start(sourceMediaElement, null);
        } else {
                contentSession.start(sourceMediaElement, sinkMediaElement);
        }   
    }

Content Session & Media lifecycle
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The *content session* is the mechanism offered by the *Content API* to
manage multimedia transactions. Its state depends on: media events
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
*Endpoint*. Method ``start()`` is called for this purpose.
::

    @Override
    public void onContentRequest(WebRtcContentSession contentSession) throws Exception {
        //Create appropriate MediaElements using Media API
        contentSession.start(sourceMediaElement, sinkMediaElement);
    } 

The *Endpoint* informs applications when media transfer starts by
calling the optional method ``onContentStart()``.
::

    @Override
    public void onContentStarted(WebRtcContentSession contentSession) Exception {
        // Execute specific application logic when content (media) starts being served to the client
    }

Optional method ``onSessionTerminate()`` is called when *Endpoint*
completes media transfer. The *content session* termination code is
provided in this call.
::

    @Override
    public void onSessionTerminated(WebRtcContentSession contentSession, int code, String reason) throws Exception {
        // Execute specific application logic when content session terminates
    }

The *content session* is terminated automatically if the *Endpoint*
experiences an unrecoverable error not caused by a direct application
command. Events like client disconnection, file system access fail, etc.
are the main error cause . Method ``onSessionError()`` is called with
the error code.
::

    @Override
    public void onSessionError(WebRtcContentSession contentSession, int code, String description) throws Exception {
        // Execute specific application logic if there is an unrecoverable
            // error on the media infrastructure. Session is destroyed after 
            // executing this code
    }

The *content session* is able to store and manage application attributes
through its lifecycle, in a similar way as ``HttpSession`` does. Method
``setAttribute()`` stores an object that can later be retrieved with
method ``getAttribute()`` or deleted with method ``removeAttribute()``.
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
channel. In order to interchange messages with a browser an
:doc:`Open API <Open_API_Specification>` client, like the one implemented by the HTML5 SDK, has to be used.
Messages can be interchanged between the *service handler* and the
client while the *content session* is active. Method ``publisEvent()``
is used for this purpose. This capability is quite useful combined with
computer vision filter, as it allows sending events to clients coming
from video content analysis (e.g. plate recognized, QR code detected,
face detected, etc.)
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
    public ContentCommandResult onContentCommand( WebRtcContentSession contentSession, ContentCommand contentCommand) throws Exception {
        contentCommand.getData();
        contentCommand.getType();
            
        ContentCommandResult result = new ContentCommandResult();
        result.setResult("OK");
        return result;  
    }

See the
:doc:`Open API <Open_API_Specification>` specification for a detailed reference of available commands and
events that can be exchange between *service handlers* and HTML5 SDK
clients.

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
the like, is used, the application can directly access requested URL
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
            
    PlayerEndPoint player = mp.createPlayerEndPoint("file:///path/to/myplayed.avi");
    contentSession.releaseOnTerminate(player);

    contentSession.start(player);

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
        
        PlayerEndPoint player = mp.newPlayerEndPoint("file:///d").build();

        contentSession.start(player);
    }
        
    @Override
    public void onSessionTerminated(WebRtcContentSession contentSession, int code, String reason)
                    throws Exception {
        player.release();
    }

.. _programming-with-kmf-media-api:

Programming with the Stream Oriented GE Java Media API
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
    …
        <dependency>
            <groupId>com.kurento.kmf</groupId>
            <artifactId>kmf-media-api</artifactId>
            <version>1.0.0</version>
        </dependency>
    …
    </dependencies>

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

    <beans xmlns=http://www.springframework.org/schema/beans 
                xmlns:xsi=http://www.w3.org/2001/XMLSchema-instance 
                xmlns:context=http://www.springframework.org/schema/context
        xsi:schemaLocation="http://www.springframework.org/schema/beans
               http://www.springframework.org/schema/beans/spring-beans-3.0.xsd
               http://www.springframework.org/schema/context
               http://www.springframework.org/schema/context/spring-context-3.0.xsd">

        <context:annotation-config />
        <context:component-scan base-package="com.kurento.kmf.media" />

        <bean id="mediaApiConfiguration" class="com.kurento.kmf.media.MediaApiConfiguration">
            <property name="serverAddress" value="127.0.0.1" />
            <property name="serverPort" value="9090" />
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
::

    public void createMediaElements() {
        MediaPipeline mp = mpf.create();
        HttpEndPoint httpEndPoint = mp.newHttpEndPoint()
            .withDisconnectionTimeout(1000).withGarbagePeriod(100)
            .withMediaProfile(MediaProfileSpecType.WEBM).build();

        PlayerEndPoint player = mp.newPlayerEndPoint("file:///myfile.avi")
            .build();

        RecorderEndPoint recorder = mp.newRecorderEndPoint("file:///myfile.mp4")
            .withMediaProfile(MediaProfileSpecType.MP4)
            .build();
            
        RtpEndPoint rtp = mp.newRtpEndPoint()
            .build();
            
        WebRtcEndPoint webrtc = mp.newWebRtcEndPoint()
            .build();
            
        ZBarFilter zbar = mp.newZBarFilter().build();
            
        // Do something with media elements
    }

*KMS MediaElements* are created through specific builders, allowing a
flexible initialization. Mandatory parameters must be provided in the
builder constructor, like the URL in the ``PlayerEndpoint``. Optional
parameters are set to defaults unless the application overrides their
values. *MediaElements* can be connected with method ``connect()`` of
owner ``MediaPipeline``.
::

    public void connectElements() {
        MediaPipeline mp = mpf.create();

        HttpEndPoint httpEndPoint = mp.newHttpEndPoint()
            .build();
        PlayerEndPoint player = mp.newPlayerEndPoint("file:///myfile.avi")
            .build();
            
        mp.connect(player, httpEndPoint);
            
    }

Method ``connect()`` creates a directional connection between elements
*source* and *sink* provided as parameters. All output streams of the
*source* element are connected to the input streams of the *sink*
element.
::

    public void connectElements() {
        MediaPipeline mp = mpf.create();

        HttpEndPoint httpEndPoint = mp.newHttpEndPoint()
            .build();

        PlayerEndPoint player = mp.newPlayerEndPoint("file:///myfile.avi")
            .build();
            
        mp.connect(player, httpEndPoint);
    }

In order to create bidirectional connections the application must
perform a connect operation in both directions.
::

    public void back2back () {
        MediaPipeline mp = mpf.create();
        
        RtpEndPoint rtpA = mp.newRtpEndPoint().build();
        RtpEndPoint rtpB = mp.newRtpEndPoint().build();
            
        mp.connect(rtpA, rtpB);
        mp.connect(rtpB, rtpA);
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
                    
        mp.newHttpEndPoint().buildAsync( new Continuation<HttpEndPoint>() {

            @Override
            public void onSuccess(HttpEndPoint result) {
                connectAsync (null, result);
            }
            @Override
            public void onError(Throwable cause) {
                // log error
            }
                
        });
            
        mp.newPlayerEndPoint("file:///myfile.webm").buildAsync( new
            Continuation<PlayerEndPoint>() {

            @Override
            public void onSuccess(PlayerEndPoint result) {
                connectAsync (result, null);
            }
            @Override
            public void onError(Throwable cause) {
                // log error
            }
            
        });
    }
        
    private HttpEndPoint http;
    private PlayerEndPoint player;

    public void connectAsync(PlayerEndPoint player, HttpEndPoint http) {
        if (player != null) {
            this.player = player;
        }
        if ( http != null) {
            this.http = http;
        }
        if (player != null && http != null){
            mp.connect(player, http);
        }
    }

.. _programming-with-kws:

Programming with the Stream Oriented GE HTML5 SDK
-------------------------------------------------

The *Stream Oriented GE HTML5* SDK is a *Javascript* library
implementing a *Content APi* client. It has been designed to be
compatible with *node.js* infrastructure and all its dependencies have
been included into the *Node Package Modules* (*NPM*). For that reason
it is required the *NPM* dependency management infrastructure to be
installed.

.. sourcecode:: bash

    sudo apt-get install npm

Current release of HTML5 SDK does not provide a library archive, so it
must be built directly from the `source
code <https://github.com/Kurento/kws-content-api>`__. A `bundle
file <https://forge.fi-ware.eu/frs/download.php/818/kws-content-api.min.js>`__
is also available at FI-WARE download page.

.. sourcecode:: bash

    git clone https://github.com/Kurento/kws-content-api.git
    cd kws-content-api/src/main/resources
    npm install
    npm update
    node_modules/.bin/grunt

*Grunt* will place into directory ``dist`` four different *Javascript*
bundles adapted to browser usage. Take the one that better suits to your
application needs and add it to your application project.

.. sourcecode:: html

    <html>
         <head>
        <script src=”js/kws-content-api.js”/>
         </head>
         <body>
        …
         </body>
    </html>

In order to use the *Stream Oriented GE HTML5* SDK the *Content API*
must activate the control protocol at handler level. Boolean attribute
``useControlProtocol`` is used for this purpose.
::

    @HttpPlayerService(path = "/myPlayerService" , useControlProtocol=true)
    public class MyPlayerService extends HttpPlayerHandler {

        @Override
        public void onContentRequest(HttpPlayerSession contentSession) throws Exception {
            // Handler actions
        }

The *Stream Oriented GE HTML5* SDK provides the following set of
*Content API* clients:

-  ***KwsContentPlayer***: Allows connection with Kurento's *HTTP player
   handler* in order to implement download services.
-  ***KwsContentUploader***: Intended to interoperate with the *HTTP
   recorder handler*. It allows implementing file upload services.
-  ***KwsWebRtcContent***: Helps applications to setup WebRTC
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
-  ***video***: Sets the video stream mode with the same alternatives
   available to audio. Default value is ``sendrecv``.
-  ***localVideoTag***: ID of the ``<video>`` tag where local video will
   be displayed. No local video will be displayed if not defined.
-  ***remoteVideoTag***: ID of the ``<video>`` tag where remote video
   will be displayed. No remote video will be displayed if not defined.
-  ***iceServers***: *STUN/TURN* server array used by *WebRTC ICE*
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

Examples
--------

This section provides two examples of the *Stream Oriented GE Kurento*
platform. Both examples implement a *MediaPipeline* composed by a
*PlayerEndPoint* connected to a *Filter* and generating a media flow
through an *HttpEndpoint*. The main difference between these two example
is the filter. The first example uses the *JackVaderFilter*. This filter
is an example of augmented reality element, since it recognizes faces in
media streams adding Jack Sparrow or Darth Vader hat onto these
faces.The second example uses the *ZBarFilter*. This filter is an
example of computational vision element, since it recognize bar and QR
codes in a media stream generating events with the information of the
detected codes in the stream. Therefore, the *MediaPipelines* used in
these examples are the following:

-  *PlayerEndpoint* → *JackVaderFilter* → *HttpEndpoint*
-  *PlayerEndpoint* → *ZBarFilter* → *HttpEndpoint*

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

            //Create a PlayerEndPoint for injecting a video into the platform
            PlayerEndPoint playerEndPoint = mp.newPlayerEndPoint(
                    "https://ci.kurento.com/video/fiwarecut.webm").build();

            //Create a filter for augmenting the video stream in real time.
            JackVaderFilter filter = mp.newJackVaderFilter().build();

            //Connect both elements
            playerEndPoint.connect(filter);

            //Store a player reference for later use
            session.setAttribute("player", playerEndPoint);

            //Calling "start" creates the HttpEndPoint and connects it to the filter
            session.start(filter);
        }

        @Override
        public void onContentStarted(HttpPlayerSession session) {
            //Content starts when the client connects to the HttpEndpoin
            //At that instant, the player must start reproducing the file
            PlayerEndPoint playerendPoint = (PlayerEndPoint) session
                    .getAttribute("player");
            playerendPoint.play();
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
    <title>Stream Oriented GE Kurento</title>
    <script src="./js/kws-content-api.js"></script>
    <script>
        function start() {
            // Handlers are deployed in the localhost. The path for these handlers 
            // is determined by the value of the HTML Select field "handler" 
            var path = document.URL.substring(0, document.URL.lastIndexOf("/") + 1);
            var handlerUrl = path + document.getElementById("handler").value;

            // KwsContentPlayer instantiation
            var KwsContentPlayer = kwsContentApi.KwsContentPlayer;
            var options = {
                remoteVideoTag: "remoteVideo"
            };
            var conn = new KwsContentPlayer(handlerUrl, options);

            // Media events log
            conn.on("mediaevent", function(data) {
                document.getElementById("events").value += JSON.stringify(data) + "\n";
            });
        }
    </script>
    </head>

    <body>
        <h1>Stream Oriented GE Kurento Examples</h1>

        <label for="selectFilter">Handler</label>
        <select id="handler">
            <option value="playerJsonJackVader">JackVaderFilter</option>
            <option value="playerJsonZBar">ZBarFilter</option>
        </select>
        <br />

        <label for="status">Events</label>
        <textarea id="events"></textarea>
        <br />

        <button id="start" onclick="start()">Start</button>
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
``http://myserver/myApp/playerJsonZBar``. The *PlayerEndPoint* uses an
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
            PlayerEndPoint player = mp.newPlayerEndPoint(
                    "https://ci.kurento.com/video/barcodes.webm").build();
            session.setAttribute("player", player);
            ZBarFilter zBarFilter = mp.newZBarFilter().build();
            player.connect(zBarFilter);
            session.start(zBarFilter);
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
            PlayerEndPoint playerendPoint = (PlayerEndPoint) session
                    .getAttribute("player");
            playerendPoint.play();
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
of ``pom.xml`` for this Maven project in shown below. As can be seen,
there are two dependencies of KMF: ``kmf-content-api`` (Java API) and
``kws-content-api`` (JavaScript API). The version for both dependencies
is 1.0.0. On one hand, ``kmf-content-api`` is used as a regular Maven
dependency. On the other hand, the JavaScript libraries contained in
``kws-content-api`` are unpacked in the root of the resulting WAR. Thus,
the JavaScript API is available for web components (e.g. HTML pages) by
including these libraries located in the ``js`` folder on the web root
(e.g. ``<script src="./js/kws-content-api.js"></script>``).

.. sourcecode:: xml

    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                 http://maven.apache.org/xsd/maven-4.0.0.xsd">

       <modelVersion>4.0.0</modelVersion>
       <groupId>com.kurento.kmf</groupId>
       <artifactId>kmf-content-helloworld</artifactId>
       <version>1.0.0</version>
       <packaging>war</packaging>

       <properties>
          <project.build.sourceEncoding>UTF-8 </project.build.sourceEncoding>
          <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
          <maven.compiler.source>1.6</maven.compiler.source>
          <maven.compiler.target>1.6</maven.compiler.target>

          <!-- Kurento Dependencies Versions -->
          <kmf-content-api.version>1.0.0</kmf-content-api.version>
          <kws-content-api.version>1.0.0</kws-content-api.version>

          <!-- Plugins Versions -->
          <maven-war-plugin.version>2.3</maven-war-plugin.version>
          <maven-dependency-plugin.version>2.8</maven-dependency-plugin.version>
       </properties>

       <dependencies>
          <dependency>
             <groupId>com.kurento.kmf</groupId>
             <artifactId>kmf-content-api</artifactId>
             <version>${kmf-content-api.version}</version>
          </dependency>
       </dependencies>

       <build>
          <plugins>
             <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-war-plugin</artifactId>
                <version>${maven-war-plugin.version}</version>
             </plugin>
             <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <version>${maven-dependency-plugin.version}</version>
                <executions>
                   <execution>
                      <id>copy-js-deps</id>
                      <phase>generate-sources</phase>
                      <goals>
                         <goal>unpack</goal>
                      </goals>
                      <configuration>
                         <artifactItems>
                            <artifactItem>
                               <groupId>com.kurento.kws</groupId>
                               <artifactId>kws-content-api</artifactId>
                               <version>${kws-content-api.version}</version>
                               <type>jar</type>
                               <overWrite>true</overWrite>
                               <outputDirectory>
                                 ${basedir}/target/${project.artifactId}-${project.version}
                               </outputDirectory>
                               <includes>**/*.*</includes>
                            </artifactItem>
                         </artifactItems>
                      </configuration>
                   </execution>
                </executions>
             </plugin>
          </plugins>
       </build>

    </project>

