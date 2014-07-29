.. _openapi:

%%%%%%%%%%%%%%%%%%%%%%%
 Open API Specification
%%%%%%%%%%%%%%%%%%%%%%%

.. highlight:: json

Introduction
============

Kurento Open API is a :term:`REST`-like resource-oriented API accessed via
HTTP/HTTPS that uses :term:`JSON-RPC` V2.0 based representations for information
exchange. This document describes the API exposed by the Application
Server as defined in the :ref:`Kurento Architecture Description <architecture>`.

Intended Audience
-----------------

This specification is intended for both software developers and
implementors of this GE. For developers, this document details the
:term:`REST`-like API to build interactive multimedia applications compliant with
the :doc:`Kurento Architecture Description <Architecture>`.
Implementors can build their GEi APIs based on the information contained
in this specification.

Before reading this document it is recommended to read first the
:doc:`Kurento Architecture Description <Architecture>` and
the :doc:`Programmers Guide <Developer_and_Programmer_Guide>`.
Moreover, the reader should be also familiar with:

-  :term:`REST` web services
-  HTTP/1.1 (:rfc:`2616`)
-  WebSockets (:rfc:`6455`)
-  JSON data serialization format (:rfc:`4627`).

Conventions used in this document
---------------------------------

Some special notations are applied to differentiate some special words
or concepts. The following list summarizes these special notations:

-  A ``code style`` with mono-spaced font is used to represent code or logical
   entities, e.g., HTTP method (``GET``, ``PUT``, ``POST``, ``DELETE``).
-  An *italic* font is used to represent document titles or some other
   kind of special text, e.g., *URI*.
-  Variables are represented in italic font and code style. For instance
   :samp:`{id}`. When the reader finds one, it can assume that the
   variable can be changed for any value.

API General Features
====================

Authentication
--------------

Currently, the Steam Oriented GE does not include any kind of
authenthication mechanism, being the application responsible of
implementing it in case it is necessary.

Representation Transport
------------------------

Resource representation may be transmitted between client and server by
using directly HTTP 1.1 protocol, as defined by IETF :rfc:`2616` or through
a WebSocket transport, as defined by IETF :rfc:`6455`. Each time an HTTP
request contains payload, a `Content-Type`:mailheader: header shall be used to specify
the MIME type of wrapped representation. In addition, both client and
server may use as many HTTP headers as they consider necessary.

Representation Format
---------------------

Kurento REST-like APIs support JSON as representation format for
request and response parameters following the recommendations in the
proposal `JSON-RPC over
HTTP <http://www.simple-is-better.org/json-rpc/jsonrpc20-over-http.html>`__.

When using HTTP 1.1 transports, the format of the requests is specified
by using the `Content-Type`:mailheader: header with a value of
:mimetype:`application/json-rpc` and is required for requests containing
a body. The format required for the response is specified in the request by
setting the `Accept`:mailheader: header to the value
:mimetype:`application/json-rpc`, that is, request and response bodies are
serialized using the same format.

Request object
~~~~~~~~~~~~~~

An *RPC call* is represented by sending a *Request object* to a server.
The *Request object* has the following members:

-  *jsonrpc*: a string specifying the version of the JSON-RPC protocol.
   It must be exactly "2.0".
-  *method*: a string containing the name of the method to be invoked.
-  *params*: a structured value that holds the parameter values to be
   used during the invocation of the method.
-  *id*: an identifier established by the client that contains a string
   or number. The server must reply with the same value in the *Response
   object*. This member is used to correlate the context between both
   objects.

Successful Response object
~~~~~~~~~~~~~~~~~~~~~~~~~~

When an *RPC call* is made the server replies with a *Response object*.
In the case of a successful response, the *Response object* will contain
the following members:

-  *jsonrpc*: a string specifying the version of the JSON-RPC protocol.
   It must be exactly "2.0".
-  *result*: its value is determined by the method invoked on the
   server. In case the connection is rejected, it's returned an object
   with a *rejected* attribute containing an object with a *code* and
   *message* attributes with the reason why the session was not
   accepted, and no sessionId is defined.
-  *id*: this member is mandatory and it must match the value of the
   *id* member in the *Request object*.

Error Response object
~~~~~~~~~~~~~~~~~~~~~

When an *RPC call* is made the server replies with a *Response object*.
In the case of an error response, the *Response object* will contain the
following members:

-  *jsonrpc*: a string specifying the version of the JSON-RPC protocol.
   It must be exactly "2.0".
-  *error*: an object describing the error through the following
   members:

   -  *code*: an integer number that indicates the error type that
      occurred.
   -  *message*: a string providing a short description of the error.
   -  *data*: a primitive or structured value that contains additional
      information about the error. It may be omitted. The value of this
      member is defined by the server.

-  *id*: this member is mandatory and it must match the value of the
   *id* member in the *Request object*. If there was an error in
   detecting the *id* in the *Request object* (e.g. Parse Error/Invalid
   Request), it equals to null.

Limits
------

Media processing is very CPU intensive and therefore the developer
should be aware that the creation of multiple simultaneous sessions can
exhaust server resources.

Extensions
----------

Querying extensions is not supported in current version of the Stream
Oriented GE.

API Specification
=================

This section details the actual APIs of each of the managers defined in
this GE, namely, the Content Manager API and the Media Manager API. It is
recommended to review the :doc:`Programmers Guide <Developer_and_Programmer_Guide>`
before proceeding with this section. Kurento API is split into two different
sub-APIs, which satisfy different types of requirements: the Content API
and the Media API. The following sections introduce both.

Content API
-----------

The Content API is based on HTTP 1.1. transports and is exposed in the
form of four services: *HttpPlayer*, *HttpRecorder*, *RtpContent* and
*WebRtcContent* described in the following subsections.

HttpPlayer Service
~~~~~~~~~~~~~~~~~~

This service allows requesting a content to be retrieved from a Media
Server using HTTP pseudostreaming.

.. table:: HttpPlayer service

    =============== ==================================================
    **Verb**        POST
    =============== ==================================================
    **URI**         :samp:`/{CONTEXT-ROOT}/{APP_LOGIC_PATH}/{ContentID}`
    --------------- --------------------------------------------------
    **Description** Performs an RPC call regarding :samp:`{ContentID}`.
                    The *Request object* is processed by the
                    *HttpPlayer* application handler tied to
                    :samp:`{APP_LOGIC_PATH}` in the :samp:`{CONTEXT-ROOT}`
                    of the application.  The *Request object* (body
                    of the HTTP request) can contain one of these
                    four methods: ``start``, ``poll``,
                    ``execute``, and ``terminate``.
    =============== ==================================================

Methods of the HttpPlayer service
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    :samp:`start({constraints)}`
        Requests the retrieval of the content. The parameter *constraints*
        indicates the kind of media (audio or/and video) to be received. In the
        case of *HttpPlayer*, the values for these constraints for audio and
        video should be *recvonly*. The following example shows a *Request
        object* requesting to receive audio and video::

            {
              "jsonrpc": "2.0",
              "method": "start",
              "params":
              {
                "constraints":
                {
                  "audio": "recvonly",
                  "video": "recvonly"
                }
              },
              "id": 1
            }

        The *Response object* contains a *sessionId* to identify the session and
        the actual URL to retrieve the content from::

            {
              "jsonrpc": "2.0",
              "result":
              {
                "sessionId": 1234,
                "url": "http://mediaserver/a13e9469-fec1-4eee-b40c-8cd90d5fc155"
              },
              "id": 1
            }
    :samp:`poll({sessionId})`
        This method allows emulating *push events* coming from the server by
        using a technique kown as *long polling*. With long polling, the client
        requests information from the server in a way similar to a normal
        polling; however, if the server does not have any information available
        for the client, instead of sending an empty response, it holds the
        request and waits for information to become available until a timeout is
        expired. If the timeout is expired before any information has become
        available the server sends an empty response and the client re-issues a
        new poll request. If, on the contrary, some information is available,
        the server pushes that information to the client and then the client
        re-issues a new poll request to restart the process.

        The *params* includes an object with only a *sessionId* attribute
        containing the ID for this session::

            {
              "jsonrpc": "2.0",
              "method": "poll",
              "params":
              {
                "sessionId": 1234
              },
              "id": 1
            }

        The *Response object* has a *contentEvents* attribute containing an
        array with the latest MediaEvents, and a *controlEvents* attribute
        containing an array with the latest control events for this session, or
        an empty object if none was generated. Each control event can has an
        optional data attribute containing an object with a *code* and a
        *message* attributes::

            {
              "jsonrpc": "2.0",
              "result":
              {
                "contentEvents":
                [
                  {"type": "typeOfEvent1",
                   "data": "dataOfEvent1"},
                  {"type": "typeOfEvent2",
                   "data": "dataOfEvent2"}
                ],
                "controlEvents":
                [
                  {
                    "type": "typeOfEvent1",
                    "data":
                    {
                      "code": 1,
                      "message": "license plate"
                    }
                  }
                ]
              },
              "id": 1
            }
    :samp:`execute({sessionId},{command})`
        Exec a command on the server. The *param* object has a *sessionId*
        attribute containing the ID for this session, and a *command* object
        with a *type* string attribute for the command type and a *data*
        attribute for the command specific parameters.

        ::

            {
              "jsonrpc": "2.0",
              "method": "execute",
              "params":
              {
                "sessionId": 1234,
                "command":
                {
                  "type": "commandType",
                  "data": ["the", "user", "defined", "command", "parameters"]
                }
              },
              "id": 1
            }

        The *Response object* is an object with only a *commandResult* attribute
        containing a string with the command results.

        ::

            {
              "jsonrpc": "2.0",
              "result":
              {
                "commandResult": "Everything has gone allright"
              },
              "id": 1
            }
    :samp:`terminate({sessionId},{reason})`
        Requests the termination of the session identified by *sessionId* so the
        server can release the resources assigned to it:

        ::

            {
              "jsonrpc": "2.0",
              "method": "terminate",
              "params":
              {
                "sessionId": 1234,
                "reason":
                {
                  "code": 1,
                  "message": "User ended session"
                }
              }
            }

        The *Response object* is an empty object:

        ::

            {
              "jsonrpc": "2.0",
              "result": {},
              "id": 2
            }

Simplified alternative approach
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The *HttpPlayer* service just described is consistent with the rest of
APIs defined in Kurento. However, it is recommended to
also expose an extra, simpler API, not requiring the use of
JSON.

.. table:: **Simplified HttpPlayer GET request**

    ============================= ====================================================
    **Verb**                      GET
    ============================= ====================================================
    **URI**                       :samp:`/{CONTEXT-ROOT}/{APP_LOGIC_PATH}/{ContentID}`
    ----------------------------- ----------------------------------------------------
    **Description**               Requests :samp:`{ContentID}` to be served according to
                                  the application handler tied to :samp:`{APP_LOGIC_PATH}`
                                  in the :samp:`{CONTEXT-ROOT}` of the application
    ----------------------------- ----------------------------------------------------
    **Successful Reponse codes**  ``200 OK``

                                  ``307 Temporary Redirect`` (to actual content)
    ----------------------------- ----------------------------------------------------
    **Error Reponse codes**       ``404 Not Found``

                                  ``500 Internal Server Error``
    ============================= ====================================================



HttpRecorder Service
~~~~~~~~~~~~~~~~~~~~

This service allows the upload of a content through HTTP to be stored in
a Media Server.

.. table:: **HttpRecorder service**

    ============================= ====================================================
    **Verb**                      POST
    ============================= ====================================================
    **URI**                       :samp:`/{CONTEXT-ROOT}/{APP_LOGIC_PATH}/{ContentID}`
    ----------------------------- ----------------------------------------------------
    **Description**               Performs an RPC call regarding :samp:`{ContentID}`.
                                  The *Request object* is processed by the *HttpRecorder*
                                  application handler tied to :samp:`{APP_LOGIC_PATH}` in the
                                  :samp:`{CONTEXT-ROOT}` of the application.
    ============================= ====================================================

The *Request object* (body of the HTTP request) can contain one of these
four methods: *start*, *poll*, *execute*, and *terminate*.

start
^^^^^

Requests the storage of the content. The parameter *constraints*
indicates the kind of media (audio or/and video) to be sent. In the case
of *HttpRecorder*, the values for these constraints for audio and video
should be *sendonly*. The following example shows a *Request object*
requesting to send audio and video:

::

    {
      "jsonrpc": "2.0",
      "method": "start",
      "params":
      {
        "constraints":
        {
          "audio": "sendonly",
          "video": "sendonly"
        }
      },
    "id": 1
    }

The *Response object* contains a *sessionId* to identify the session and
the actual URL to upload the content to:

::

    {
      "jsonrpc": "2.0",
      "result":
      {
        "url": "http://mediaserver/a13e9469-fec1-4eee-b40c-8cd90d5fc155",
        "sessionId": 1234
      },
      "id": 1
    }

poll, execute, and terminate
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

These operations work in the same way than *HttpPlayer*. Therefore, for
an example of *Request object* and *Response object* see the sections of
*poll*, *execute*, and *terminate* respectively in *HttpPlayer*.

Simplified alternative approach
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The *HttpRecorder* service just described is consistent with the rest of
APIs defined in Kurento. However, it is recommended to
also expose a simpler API as described here not requiring the use of
JSON.

.. table:: **Simplified HttpRecorder POST request**

    ============================= =========================================================
    **Verb**                      POST
    ============================= =========================================================
    **URI**                       :samp:`/{CONTEXT-ROOT}/{APP_LOGIC_PATH}/{ContentID}`
    ----------------------------- ---------------------------------------------------------
    **Description**               Uploads :samp:`{ContentID}` to be stored according to the
                                  application handler tied to :samp:`{APP_LOGIC_PATH}` in
                                  the :samp:`{CONTEXT-ROOT}` of the application
    ----------------------------- ---------------------------------------------------------
    **Successful Reponse codes**  ``200 OK``

                                  ``307 Temporary Redirect`` (to actual content)
    ----------------------------- ---------------------------------------------------------
    **Error Reponse codes**       ``404 Not Found``

                                  ``500 Internal Server Error``
    ============================= =========================================================


The request body of this method is the content to be uploaded.

RtpContent
~~~~~~~~~~

This service allows establishing an *RTP content session* between the
client performing the request and a Media Server.

.. table:: **RtpContent service**

    ============================= =======================================================
    **Verb**                      POST
    ============================= =======================================================
    **URI**                       :samp:`/{CONTEXT-ROOT}/{APP_LOGIC_PATH}/{ContentID}`
    ----------------------------- ----------------------------------------------------
    **Description**               Performs an RPC call regarding :samp:`{ContentID}`. The
                                  *Request object* is processed by the *RTPContent*
                                  application handler tied to :samp:`{APP_LOGIC_PATH}`
                                  in the :samp:`{CONTEXT-ROOT}` of the application.
    ============================= =======================================================



The *Request object* (body of the HTTP request) can contain one of these
four methods: *start*, *poll*, *execute*, and *terminate*.

start
^^^^^

Requests the establishment of the RTP session. The parameter *sdp*
contains the client SDP (Session Description Protocol) offer, that is, a
description of the desired session from the caller's perspective. The
parameter *constraints* indicates the media (audio or/and video) to be
received, sent, or sent and received by setting their values to
*recvonly*, *sendonly*, *sendrecv* or *inactive*. The following example
shows a *Request object* requesting bidirectional audio and video (i.e.
*sendrecv* for both audio and video)::

    {
      "jsonrpc": "2.0",
      "method": "start",
      "params":
      {
        "sdp": "Contents_of_Caller_SDP",
        "constraints":
        {
          "audio": "sendrecv",
          "video": "sendrecv"
        }
      },
      "id": 1
    }

The *Response object* contains the Media Server SDP answer, that is, a
description of the desired session from the callee's perspective, and a
*sessionId* to identify the session::

    {
      "jsonrpc": "2.0",
      "result":
      {
        "sdp": "Contents_of_Callee_SDP",
        "sessionId": 1234
      },
      "id": 1
    }

poll, execute, and terminate
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

These operations work in the same way than *HttpPlayer* and
*HttpRecorder*. Therefore, for an example of *Request object* and
*Response object* see the sections of *poll*, *execute*, and *terminate*
respectively in *HttpPlayer*.

WebRtcContent
~~~~~~~~~~~~~

Conceptually, *RtpContent* and *WebRtcContent* are very similar, the
main difference is the underlying protocol to exchange media, so all the
descriptions in the section above apply to *WebRtcContent*.

Media API
---------

The Media API provides full control of Kurento Media Server through
:rom:cls:`Media Elements <MediaElement>`, which are the building blocks
providing a specific media functionality. They are used to send, receive,
process and transform media. The Media API provides a toolbox of Media
Elements ready to be used. It also provides the capability of creating
:rom:cls:`Media Pipelines <MediaPipeline>` by joining Media Elements of
the toolbox.

The Media API requires full-duplex communications between client and server
infrastructure. For this reason, the Media API is based on WebSocket
transports and not on plain HTTP 1.1 transports as the Content API does.

Previous to issuing commands, the Media API client requires establishing
a WebSocket connection with the server infrastructure.

    ============================= ==========================================================
    **Verb**                      NA
    ============================= ==========================================================
    **URI**                       :samp:`ws://{SERVER_IP}:{SERVER_PORT}/thrift/ws/websocket`
    ----------------------------- ----------------------------------------------------------
    **Description**               Establishment of WebSocket connection for
                                  the exchange of full-duplex JSON-RPC messages.
    ============================= ==========================================================

Once the WebSocket has been established, the Media API offers five different
types of request/response messages:

*	''create'': Instantiates a new pipeline or media element in the media server.
*	''invoke'': Calls a method of an existing media element.
*	''subscribe'': Creates a subscription to a media event in a media element.
*	''unsubscribe'': Removes an existing subscription to a media event.
*	''release'': Explicit termination of a media element.

The Media API allows to servers send requests to clients:

*	''onEvent'': This request is sent from server to clients when a media event occurs.

Create
~~~~~~

Create message requests the creation of an element of the MediaAPI toolbox.
The parameter ``type`` specifies the type of the object to be created.
The parameter ``creationParams`` contains all the information needed to
create the object. Each object type needs different ``creationParams``
to create the object. These parameters are defined later in this document.
Finally, a ``sessionId`` parameter is included with the identifier of the
current session. The value of this parameter is sent by the server to the
client in each response to the client. Only the first request from client
to server is allowed to not include the ''sessionId'' (because at this
point is unknown for the client).

The following example shows a Request object requesting the creation of an
object of the type :rom:cls:`PlayerEndpoint` within the pipeline
``6829986`` and uri ``http://host/app/video.mp4`` in the session
``c93e5bf0-4fd0-4888-9411-765ff5d89b93``::

    {
      "jsonrpc": "2.0",
      "method": "create",
      "params": {
        "type": "PlayerEndPoint",
        "creationParams": {
          "pipeline": "6829986",
          "uri": "http://host/app/video.mp4"
        },
        "sessionId": "c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      },
      "id": 1
    }

The ``Response`` object contains the object id of the new object in the
field ``value``. This object id has to be used in other requests of the
protocol (as we will describe later). As stated before, the ``sessionId``
is also returned in each response.

The following example shows a typical response to a create message::

    {
      "jsonrpc": "2.0",
      "result": {
        "value": "442352747",
        "sessionId": "c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      },
      "id": 1
    }

Invoke
~~~~~~

Invoke message requests the invocation of an operation in the specified
object. The parameter ``object`` indicates the id of the object in which
the operation will be invoked. The parameter ``operation`` carries the name
of the operation to be executed. Finally, the parameter ``operationParams``
has the parameters needed to execute the operation. The object specified
has to understand the operation name and parameters. Later in this document
it is described the valid operations for all object types.

The following example shows a ``Request`` object requesting the invocation
of the operation ``connect`` on the object ``442352747`` with parameter
sink ``6829986``. The ``sessionId`` is also included as is mandatory for
all requests in the session (except the first one).
::

    {
      "jsonrpc": "2.0",
      "method": "invoke",
      "params": {
        "object": "442352747",
        "operation": "connect",
        "operationParams": {
          "sink": "6829986"
        },
        "sessionId": "c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      },
      "id": 2
    }

The ``Response object`` contains the value returned while executing the
operation invoked in the object or nothing if the operation doesn’t return
any value.

The following example shows a typical response while invoking the operation
``connect`` (that doesn’t return anything)::

    {
      "jsonrpc": "2.0",
      "result": {
        "sessionId": "c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      },
      "id": 2
    }

Release
~~~~~~~

Release message requests the release of the specified object. The parameter
``object`` indicates the id of the object to be released.
::

    {
      "jsonrpc": "2.0",
      "method": "release",
      "params": {
        "object": "442352747",
        "sessionId": "c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      },
      "id": 3
    }

The ``Response`` object only contains the ``sessionID``. The following
example shows the typical response of a release request::

    {
      "jsonrpc":"2.0",
      "result":
      {
        "sessionId":"c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      },
      "id":3
    }

Subscribe
~~~~~~~~~

Subscribe message requests the subscription to a certain kind of events
in the specified object. The parameter ``object`` indicates the id of the
object to subscribe for events. The parameter ``type`` specifies the type
of the events. If a client is subscribed for a certain type of events in
an object, each time an event is fired in this object, a request with method
``onEvent`` is sent to the client. This kind of request is described few
sections later.

The following example shows a ``Request`` object requesting the
subscription of the event type ``EndOfStream`` on the object ``311861480``.
The ``sessionId`` is also included.
::

    {
      "jsonrpc":"2.0",
      "method":"subscribe",
      "params":{
        "object":"311861480",
        "type":"EndOfStream",
        "sessionId":"c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      },
      "id":4
    }

The ``Response`` object contains the subscription identifier. This value
can be used later to remove this subscription.

The following example shows the response of subscription request. The
``value`` attribute contains the subscription id::

    {
      "jsonrpc":"2.0",
      "result":
      {
        "value":"353be312-b7f1-4768-9117-5c2f5a087429",
        "sessionId":"c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      },
      "id":4
    }

Unsubscribe
~~~~~~~~~~~

Unsubscribe message requests the cancelation of a previous event
subscription. The parameter subscription contains the ``subscription``
id received from the server when the subscription was created.

The following example shows a ``Request object`` requesting the
cancelation of the subscription
``353be312-b7f1-4768-9117-5c2f5a087429``::

    {
      "jsonrpc":"2.0",
      "method":"unsubscribe",
      "params":{
        "subscription":"353be312-b7f1-4768-9117-5c2f5a087429",
        "sessionId":"c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      },
      "id":5
    }


The ``Response`` object only contains the ``sessionID``. The
following example shows the typical response of an unsubscription
request::

    {
      "jsonrpc":"2.0",
      "result":
      {
        "sessionId":"c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      },
      "id":5
    }


onEvent
~~~~~~~

When a client is subscribed to a type of events in an object, the server
send an ``onEvent`` notification each time an event of that type is
fired in the incumbent object. This is possible because the Media API
is implemented with websockets and there is a full duplex channel between
client and server. The notification that server send to client has all
the information about the event:

*	``data``: Information about this specific of this type of event.
*	``source``: the object source of the event.
*	``type``: The type of the event.
*	``subscription``: subscription id for which the event is fired.

The following example shows a notification sent for server to client to notify an event of type ``EndOfStream`` in the object ``311861480`` with subscription ``353be312-b7f1-4768-9117-5c2f5a087429``.

    {
      "jsonrpc":"2.0",
      "method":"onEvent",
      "params":{
        "value":{
          "data":{
            "source":"311861480",
            "type":"EndOfStream"
          },
          "object":"311861480",
          "subscription":"353be312-b7f1-4768-9117-5c2f5a087429",
          "type":"EndOfStream",
        },
        "sessionId":"4f5255d5-5695-4e1c-aa2b-722e82db5260"
      }
    }


In jsonrpc format, the notifications are different from request in that notifications haven’t got an id field. Also, the notifications cannot be responded. For this reason, in the example before, there is no id in the message.

Error responses
~~~~~~~~~~~~~~~

If errors arise processing a request, there is a generic error response,
in which an error code and a description message in sent, as follows::

    {
      "jsonrpc": "2.0",
      "error":
      {
        "code": -32601,
        "message": "Error description"
      },
      "id": 2
    }

Media Element toolbox
~~~~~~~~~~~~~~~~~~~~~

The Media Element toolbox provided by Media API is divided into Endpoints,
Filters and Hubs.

* Endpoints offer capabilities to work with protocols and codecs
  (:rom:cls:`HttpEndpoint`, :rom:cls:`RtpEndpoint` and
  :rom:cls:`WebRtcEndpoint`) and also media repository handling
  (:rom:cls:`PlayerEndpoint` and :rom:cls:`RecorderEndpoint`).
* Filters are responsible of media processing, such as computer vision
  (Face detection, pointer tracking or bar and QR code reading) and
  augmented reality (Chroma filtering or face overlay filtering).
* Hubs offer capabilities to connect several inputs and outputs and create
  different types of connections between them (:rom:cls:`Composite`,
  :rom:cls:`Dispatcher`, :rom:cls:`DispatcherOneToMany`).

Therefore, the Open API protocol specification provides capabilities to
create and handle these Media Elements. The following table shows a
description at a glance of the Media Elements provided by Media API.


EndPoint
    Protocols and Codecs
        HttpGetEndpoint
            .. image:: images/http.jpg
               :scale: 80%
               :alt: HttpGet

            This type of Endpoint provides unidirectional communications.
            Its :rom:cls:`MediaSink` are associated with the HTTP GET
            method. It contains source :rom:cls:`MediaPad` for audio and
            video, delivering media using HTML5 pseudo-streaming mechanism.

        HttpPostEndpoint
            .. image:: images/http2.jpg
               :scale: 80%
               :alt: HttpPost

            This type of Endpoint provide unidirectional ommunications. Its
            :rom:cls:`MediaSource` are related to HTTP POST method. It
            contains sink :rom:cls:`MediaPad` for audio and video, which
            provide access to an HTTP file upload function.

        RtpEndpoint
            .. image:: images/rtp.jpg
               :scale: 80%
               :alt: Rtp

            Endpoint that provides bidirectional content delivery capabilities
            with remote networked peers through RTP protocol. It contains
            paired sink and source :rom:cls:`MediaPad` for audio and video.

        WebRtcEndpoint
            .. image:: images/webrtc.jpg
               :scale: 80%
               :alt: WebRtc

            This Endpoint offers media streaming using WebRTC.

    Media Repository
        PlayerEndpoint
            .. image:: images/player.jpg
               :scale: 80%
               :alt: Player

            It provides function to retrieve contents from seekable sources
            in reliable mode (does not discard media information) and inject
            them into :term:`KMS`. It contains one :rom:cls:`MediaSource`
            for each media type detected.

        RecorderEndpoint
            .. image:: images/recorder.jpg
               :scale: 80%
               :alt: Recorder

            Provides function to store contents in reliable mode (doesn't
            discard data). It contains :rom:cls:`MediaSink` pads for audio
            and video.


Filter
    Computer Vision
        FaceOverlayFilter
            .. image:: images/face.jpg
               :scale: 80%
               :alt: Face

            It detects faces in a video feed. The face is then overlaid with an image.


        PointerDetectorFilter
            .. image:: images/pointer.jpg
               :scale: 80%
               :alt: PointerDetector

            It detects pointers in a video feed. The detection of this
            :rom:cls:`Filter` is based on color tracking in a video fed.


        PointerDetectorAdvFilter
            .. image:: images/pointerAdv.jpg
               :scale: 80%
               :alt: PointerDetectorAdv

            It detects pointers in a video feed. The detection of this
            :rom:cls:`Filter` is based on color tracking for round shapes
            (e.g. a ball) in a video fed.


        ZBarFilter
            .. image:: images/bar.jpg
               :scale: 80%
               :alt: ZBar

            This :rom:cls:`Filter` detects :term:`QR` and other bar codes
            in a video feed. When a code is found, the filter raises a
            :rom:evnt:`CodeFound` event.


        PlateDetectorFilter
            .. image:: images/plate.jpg
               :scale: 80%
               :alt: PlateDetector

            This Filter detects vehicle plates in a video feed.


    Augmented Reality
        ChromaFilter
            .. image:: images/chroma.jpg
               :scale: 80%
               :alt: Chroma

            This type of :rom:cls:`Filter` makes transparent a color range
            in the top layer, revealing another image behind.


        JackVaderFilter
            .. image:: images/jackvader.jpg
               :scale: 80%
               :alt: JackVader

            :rom:cls:`Filter` that detects faces in a video feed. Those
            on the right half of the feed are overlaid with a pirate hat,
            and those on the left half are covered by a Darth Vader helmet.
            This is an example filter, intended to demonstrate how to
            integrate computer vision capabilities into the multimedia
            infrastructure.


Hub
    Media Mix and Distribution
        Composite
            .. image:: images/Composite.png
               :scale: 80%
               :alt: Composite

            A :rom:cls:`Hub` that mixes the audio stream of its connected
            sources and constructs a grid with the video streams of its
            connected sources into its sink.


        Dispatcher
            .. image:: images/Dispatcher.png
               :scale: 80%
               :alt: Dispatcher

            A :rom:cls:`Hub` that allows routing between arbitrary port pairs.

        DispatcherOneToMany
            .. image:: images/OneToMany.png
               :scale: 80%
               :alt: DispatcherOneToMany

            A :rom:cls:`Hub` that sends a given source to all the connected sinks.


Media Element descriptions
~~~~~~~~~~~~~~~~~~~~~~~~~~

Each Media Element accessible through the Media API has their specific
capabilities, which are accessible through the JSON-RPC methods shown
above. In this section, we show the specific operations and parameters
that each Media Element accepts. Introducing this information directly
onto the protocol specification will significantly decrease readability.
On the sake of simplicity, we are going to present Media Element
descriptions directly as a JavaScript API of Media Elements consuming the
JSON-RPC Media API.

For example, the following JavaScript code that creates a media element:


.. sourcecode:: javascript

    var pipeline = //…;
    PlayerEndpoint.create(pipeline, {uri: "https://ci.kurento.com/video/small.webm"},
                          function(error, player)
                          {
                              //…
                          });


Is translated to the following create request::

    {
      "jsonrpc": "2.0",
      "method": "create",
      "params": {
        "type": "PlayerEndPoint",
        "creationParams": {
          "pipeline": "6829986",
          "uri": "https://ci.kurento.com/video/small.webm"
        },
        "sessionId": "c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      },
      "id": 1
    }

In the same sense, the following method invocation:

.. sourcecode:: javascript

    var httpGet = //…;
    httpGet.getUrl(function(error, url)
                    {
                        //…
                    });


Is translated to the following invoke request::

    {
      "jsonrpc": "2.0",
      "method": "invoke",
      "params": {
        "object": "442352747",
        "operation": "getUrl",
        "sessionId": "c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      },
      "id": 2
    }

Mapping from the JavaScript API to the JSON-RPC API is immediate:

* From the JSON-RPC message perspective, ``create`` methods are
  converted to ``create`` messages specifying in the ``type`` field
  the type of object to be built.
* The parameters of the create methods are converted to ``creationParams``
  fields on the message.
* Method invocations are just ``invoke`` request specifying the method
  name in the ``operation`` field.
* The JavaScript API is object oriented. In this sense, if a ``parent``
  element has a method, that method is valid for all its children.

**Note**: Parameters starting with ``?`` are optionals. ``=<value>`` after a
parameter indicates the default value.

