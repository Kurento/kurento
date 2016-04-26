.. _kurentoprotocol:

%%%%%%%%%%%%%%%%
Kurento Protocol
%%%%%%%%%%%%%%%%

.. highlight:: json

**Kurento Media Server** can be controlled by means of two out of the box
**Kurento Clients**, i.e. **Java** or **JavaScript**. These clients use the
**Kurento Protocol** to *speak* with the KMS. **Kurento Protocol** is based on
:term:`WebSocket` and uses :term:`JSON-RPC` V2.0 messages for making requests
and sending responses.

JSON-RPC Messages format
========================

Kurento Protocol uses :term:`JSON-RPC` V2.0 to code its messages. The following
subsections shows how to use this format in :term:`JSON` messages.

Request messages
----------------

An *RPC call* is represented by sending a *request* message to a server. The
*request* message has the following members:

-  **jsonrpc**: a string specifying the version of the JSON-RPC protocol. It
   must be exactly "2.0".
-  **id**: an unique identifier established by the client that contains a
   string or number. The server must reply with the same value in the
   *response* message. This member is used to correlate the context between
   both messages.
-  **method**: a string containing the name of the method to be invoked.
-  **params**: a structured value that holds the parameter values to be used
   during the invocation of the method.

The following JSON shows a sample request for the creation of a `PlayerEndpoint`
Media Element::

    {
      "jsonrpc": "2.0",
      "id": 1,
      "method": "create",
      "params": {
        "type": "PlayerEndpoint",
        "constructorParams": {
          "pipeline": "6829986",
          "uri": "http://host/app/video.mp4"
        },
        "sessionId": "c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      }
    }

Successful Response messages
----------------------------

When an *RPC call* is made the server replies with a *response* message. In the
case of a successful response, the *response* message will contain the
following members:

-  **jsonrpc**: a string specifying the version of the JSON-RPC protocol. It
   must be exactly "2.0".
-  **id**: this member is mandatory and it must match the value of the *id*
   member in the *request* message.
-  **result**: its value is determined by the method invoked on the server.
   In case the connection is rejected, it's returned an message with a
   *rejected* attribute containing an message with a *code* and *message*
   attributes with the reason why the session was not accepted, and no
   sessionId is defined.

The following example shows a typical successful response::

    {
      "jsonrpc": "2.0",
      "id": 1,
      "result": {
        "value": "442352747",
        "sessionId": "c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      }
    }

Error Response messages
-----------------------

When an *RPC call* is made the server replies with a *response* message. In the
case of an error response, the *response* message will contain the following
members:

-  **jsonrpc**: a string specifying the version of the JSON-RPC protocol. It
   must be exactly "2.0".
-  **id**: this member is mandatory and it must match the value of the *id*
   member in the *request* message. If there was an error in detecting the *id*
   in the *request* message (e.g. *Parse Error/Invalid Request*), it equals to
   null.
-  **error**: an message describing the error through the following members:

   -  **code**: an integer number that indicates the error type that
      occurred.
   -  **message**: a string providing a short description of the error.
   -  **data**: a primitive or structured value that contains additional
      information about the error. It may be omitted. The value of this member
      is defined by the server.

The following example shows a typical error response::

    {
      "jsonrpc": "2.0",
      "id": 1,
      "error": {
        "code": "33",
        "message": "Invalid paramter format"
      }
    }

Kurento API over JSON-RPC
=========================

As explained in :doc:`Kurento API section <kurento_API>`, Kurento Media Server
exposes a full fledged API to let applications to process media in several ways.

To allow this rich API, Kurento Clients require requires full-duplex
communications between client and server infrastructure. For this reason, the
Kurento Protocol is based on WebSocket transports.

Previous to issuing commands, the Kurento Client requires establishing a
WebSocket connection with Kurento Media Server to the URL:
``ws://hostname:port/kurento``

Once the WebSocket has been established, the Kurento Protocol offers different
types of request/response messages:

 - **ping**: Keep-alive method between client and Kurento Media Server.
 - **create**: Instantiates a new media object, that is, a pipeline or media
   element.
 - **invoke**: Calls a method of an existing media object.
 - **subscribe**: Creates a subscription to an event in a object.
 - **unsubscribe**: Removes an existing subscription to an event.
 - **release**: Deletes the object and release resources used by it.

The Kurento Protocol allows to Kurento Media Server send requests to clients:

 - **onEvent**: This request is sent from Kurento Media server to clients
   when an event occurs.

Ping
----

In order to warranty the WebSocket connectivity between the client and the
Kurento Media Server, a keep-alive method is implemented. This method is based
on a ``ping`` method sent by the client, which must be replied with a ``pong``
message from the server. If no response is obtained in a time interval, the
client is aware that the connectivity with the media server has been lost.The
parameter ``interval`` is the time out to receive the ``Pong`` message from the
server, in milliseconds. By default this value is ``240000`` (i.e. 40 seconds).
This is an example of ``ping`` request::

   {
       "id": 1,
       "method": "ping",
       "params": {
           "interval": 240000
       },
       "jsonrpc": "2.0"
   }

The response to a ``ping`` request must contain a ``result`` object with a
``value`` parameter with a fixed name: ``pong``. The following snippet shows
the ``pong`` response to the previous ``ping`` request::

   {
       "id": 1,
       "result": {
           "value": "pong"
       },
       "jsonrpc": "2.0"
   }

Create
------

Create message requests the creation of an object of the Kurento API (Media
Pipelines and Media Elements). The parameter ``type`` specifies the type of the
object to be created. The parameter ``constructorParams`` contains all the
information needed to create the object. Each message needs different
``constructorParams`` to create the object. These parameters are defined in
:doc:`Kurento API section <kurento_API>`.

Media Elements have to be contained in a previously created Media Pipeline.
Therefore, before creating Media Elements, a Media Pipeline must exist. The
response of the creation of a Media Pipeline contains a parameter called
``sessionId``, which must be included in the next create requests for Media
Elements.

The following example shows a request message requesting the creation of an
object of the type ``MediaPipeline``::

   {
       "id": 2,
       "method": "create",
       "params": {
           "type": "MediaPipeline",
           "constructorParams": {},
           "properties": {}
       },
       "jsonrpc": "2.0"
   }

The response to this request message is as follows. Notice that the parameter
``value`` identifies the created Media Pipelines, and ``sessionId`` is the
identifier of the current session::

   {
       "id": 2,
       "result": {
           "value": "6ba9067f-cdcf-4ea6-a6ee-d74519585acd_kurento.MediaPipeline",
           "sessionId": "bd4d6227-0463-4d52-b1c3-c71f0be68466"
       },
       "jsonrpc": "2.0"
   }

The response message contains the identifier of the new object in the field
value. As usual, the message ``id`` must match with the request message. The
``sessionId`` is also returned in each response. The following example shows a
request message requesting the creation of an object of the type
``WebRtcEndpoint`` within an existing Media Pipeline (identified by the
parameter ``mediaPipeline``). Notice that in this request, the ``sessionId`` is
already present, while in the previous example it was not (since at that point
was unknown for the client)::

   {
       "id": 3,
       "method": "create",
       "params": {
           "type": "WebRtcEndpoint",
           "constructorParams": {
               "mediaPipeline": "6ba9067f-cdcf-4ea6-a6ee-d74519585acd_kurento.MediaPipeline"
           },
           "properties": {},
           "sessionId": "bd4d6227-0463-4d52-b1c3-c71f0be68466"
       },
       "jsonrpc": "2.0"
   }

The following example shows a request message requesting the creation of an
object of the type ``WebRtcEndpoint`` within an existing Media Pipeline
(identified by the parameter ``mediaPipeline``). Notice that in this request,
the ``sessionId`` is already present, while in the previous example it was not
(since at that point was unknown for the client)::

   {
       "id": 3,
       "result": {
           "value": "6ba9067f-cdcf-4ea6-a6ee-d74519585acd_kurento.MediaPipeline/087b7777-aab5-4787-816f-f0de19e5b1d9_kurento.WebRtcEndpoint",
           "sessionId": "bd4d6227-0463-4d52-b1c3-c71f0be68466"
       },
       "jsonrpc": "2.0"
   }

Invoke
------

Invoke message requests the invocation of an operation in the specified object.
The parameter ``object`` indicates the ``id`` of the object in which the
operation will be invoked. The parameter ``operation`` carries the name of the
operation to be executed. Finally, the parameter ``operationParams`` has the
parameters needed to execute the operation.

The following example shows a request message requesting the invocation of the
operation ``connect`` on a ``PlayerEndpoint`` connected to a
``WebRtcEndpoint``::

   {
       "id": 5,
       "method": "invoke",
       "params": {
           "object": "6ba9067f-cdcf-4ea6-a6ee-d74519585acd_kurento.MediaPipeline/76dcb8d7-5655-445b-8cb7-cf5dc91643bc_kurento.PlayerEndpoint",
           "operation": "connect",
           "operationParams": {
               "sink": "6ba9067f-cdcf-4ea6-a6ee-d74519585acd_kurento.MediaPipeline/087b7777-aab5-4787-816f-f0de19e5b1d9_kurento.WebRtcEndpoint"
           },
           "sessionId": "bd4d6227-0463-4d52-b1c3-c71f0be68466"
       },
       "jsonrpc": "2.0"
   }

The response message contains the value returned while executing the operation
invoked in the object or nothing if the operation doesn’t return any value.

The following example shows a typical response while invoking the operation
``connect`` (that doesn’t return anything)::

   {
       "id": 5,
       "result": {
           "sessionId": "bd4d6227-0463-4d52-b1c3-c71f0be68466"
       },
       "jsonrpc": "2.0"
   }

Release
-------

Release message requests the release of the specified object. The parameter
``object`` indicates the ``id`` of the object to be released::

   {
       "id": 36,
       "method": "release",
       "params": {
           "object": "6ba9067f-cdcf-4ea6-a6ee-d74519585acd_kurento.MediaPipeline",
           "sessionId": "bd4d6227-0463-4d52-b1c3-c71f0be68466"
       },
       "jsonrpc": "2.0"
   }

The response message only contains the ``sessionId``. The following example
shows the typical response of a release request::

   {
       "id": 36,
       "result": {
           "sessionId": "bd4d6227-0463-4d52-b1c3-c71f0be68466"
       },
       "jsonrpc": "2.0"
   }

Subscribe
---------

Subscribe message requests the subscription to a certain kind of events in the
specified object. The parameter ``object`` indicates the ``id`` of the object
to subscribe for events. The parameter ``type`` specifies the type of the
events. If a client is subscribed for a certain type of events in an object,
each time an event is fired in this object, a request with method ``onEvent``
is sent from Kurento Media Server to the client. This kind of request is
described few sections later.

The following example shows a request message requesting the subscription of the
event type ``EndOfStream`` on a ``PlayerEndpoint`` object::

   {
       "id": 11,
       "method": "subscribe",
       "params": {
           "type": "EndOfStream",
           "object": "6ba9067f-cdcf-4ea6-a6ee-d74519585acd_kurento.MediaPipeline/76dcb8d7-5655-445b-8cb7-cf5dc91643bc_kurento.PlayerEndpoint",
           "sessionId": "bd4d6227-0463-4d52-b1c3-c71f0be68466"
       },
       "jsonrpc": "2.0"
   }

The response message contains the subscription identifier. This value can be
used later to remove this subscription.

The following example shows the response of subscription request. The ``value``
attribute contains the subscription id::

   {
       "id": 11,
       "result": {
           "value": "052061c1-0d87-4fbd-9cc9-66b57c3e1280",
           "sessionId": "bd4d6227-0463-4d52-b1c3-c71f0be68466"
       },
       "jsonrpc": "2.0"
   }


Unsubscribe
-----------

Unsubscribe message requests the cancellation of a previous event subscription.
The parameter subscription contains the subscription ``id`` received from the
server when the subscription was created.

The following example shows a request message requesting the cancellation of the
subscription ``353be312-b7f1-4768-9117-5c2f5a087429`` for a given ``object``::

   {
       "id": 38,
       "method": "unsubscribe",
       "params": {
           "subscription": "052061c1-0d87-4fbd-9cc9-66b57c3e1280",
           "object": "6ba9067f-cdcf-4ea6-a6ee-d74519585acd_kurento.MediaPipeline/76dcb8d7-5655-445b-8cb7-cf5dc91643bc_kurento.PlayerEndpoint",
           "sessionId": "bd4d6227-0463-4d52-b1c3-c71f0be68466"
       },
       "jsonrpc": "2.0"
   }

The response message only contains the ``sessionId``. The following example
shows the typical response of an unsubscription request::

   {
       "id": 38,
       "result": {
           "sessionId": "bd4d6227-0463-4d52-b1c3-c71f0be68466"
       },
       "jsonrpc": "2.0"
   }

OnEvent
-------

When a client is subscribed to a type of events in an object, the server sends
an ``onEvent`` request each time an event of that type is fired in the object.
This is possible because the Kurento Protocol is implemented with WebSockets
and there is a full duplex channel between client and server. The request that
server send to client has all the information about the event:

  - **source**: the object source of the event.
  - **type**: The type of the event.
  - **timestamp**: Date and time of the media server.
  - **tags**: Media elements can be labeled using the methods
    ``setSendTagsInEvents`` and ``addTag`` present in each element. These tags
    are key-value metadata that can be used by developers for custom purposes.
    Tags are returned with each event by the media server in this field.

The following example shows a notification sent for server to client to notify
an event of type ``EndOfStream`` for a ``PlayerEndpoint`` object::

    {
      "jsonrpc":"2.0",
      "method":"onEvent",
      "params":{
         "value":{
            "data":{
               "source":"681f1bc8-2d13-4189-a82a-2e2b92248a21_kurento.MediaPipeline/e983997e-ac19-4f4b-9575-3709af8c01be_kurento.PlayerEndpoint",
               "tags":[],
               "timestamp":"1441277150",
               "type":"EndOfStream"
            },
            "object":"681f1bc8-2d13-4189-a82a-2e2b92248a21_kurento.MediaPipeline/e983997e-ac19-4f4b-9575-3709af8c01be_kurento.PlayerEndpoint",
            "type":"EndOfStream"
         }
       }
    }

Notice that this message has no ``id`` field due to the fact that no response is
required.

Network issues
==============

Resources handled by KMS are high-consuming. For this reason, KMS implements a
garbage collector.

A Media Element is collected when the client is disconnected longer than 4
minutes. After that time, these media elements are disposed automatically.

Therefore the WebSocket connection between client and KMS be active any time. In
case of temporary network disconnection, KMS implements a mechanism to allow
the client reconnection.

There is an special kind of message with the format above. This message allows a
client to reconnect to the same KMS previously connected::

    {
      "jsonrpc": "2.0",
      "id": 7,
      "method": "connect",
      "params": {
        "sessionId":"4f5255d5-5695-4e1c-aa2b-722e82db5260"
      }
    }

If KMS replies as follows::

    {
      "jsonrpc": "2.0",
      "id": 7,
      "result": {
        "sessionId":"4f5255d5-5695-4e1c-aa2b-722e82db5260"
      }
    }

... this means that client is reconnected to the same KMS. In case of
reconnection to another KMS, the message is the following::

    {
       "jsonrpc":"2.0",
       "id": 7,
       "error":{
         "code":40007,
         "message":"Invalid session",
         "data":{
            "type":"INVALID_SESSION"
         }
       }
    }

In this case client is supposed to invoke the ``connect`` primitive once again
in order to get a new ``sessionId``::

    {
       "jsonrpc":"2.0",
       "id": 7,
       "method":"connect"
    }


Kurento API
===========

In order to implement a Kurento client you need the reference documentation. The
best way to know all details is take a look to IDL file that defines the
interface of the Kurento elements. We have defined a custom IDL format based on
JSON. From it, we generate the client code for Java and JavaScript. Kurento API
is defined in the following IDL files:

- `KMS core <https://github.com/Kurento/kms-core/blob/master/src/server/interface/core.kmd.json>`_

- `KMS elements <https://github.com/Kurento/kms-elements/tree/master/src/server/interface>`_

- `KMS filters <https://github.com/Kurento/kms-filters/tree/master/src/server/interface>`_


Example: WebRTC in loopback
===========================

This section describes an example of the messages interchanged between a Kurento
client and the Kurento Media Server in order to create a WebRTC in loopback.
This example is fully depicted in the :doc:`tutorials <../tutorials>` section.
The steps are the following:

1. Client sends a request message in order to a media pipeline::

    {
      "id":1,
      "method":"create",
      "params":{
         "type":"MediaPipeline",
         "constructorParams":{},
         "properties":{}
      },
      "jsonrpc":"2.0"
    }

2. KMS sends a response message with the identifier for the media pipeline and
the media session::

    {
      "id":1,
      "result":{
         "value":"c4a84b47-1acd-4930-9f6d-008c10782dfe_MediaPipeline",
         "sessionId":"ba4be2a1-2b09-444e-a368-f81825a6168c"
      },
      "jsonrpc":"2.0"
    }

3. Client sends a request to create a ``WebRtcEndpoint``::

    {
      "id":2,
      "method":"create",
      "params":{
         "type":"WebRtcEndpoint",
         "constructorParams":{
            "mediaPipeline":"c4a84b47-1acd-4930-9f6d-008c10782dfe_MediaPipeline"
         },
         "properties": {},
         "sessionId":"ba4be2a1-2b09-444e-a368-f81825a6168c"
      },
      "jsonrpc":"2.0"
    }

4. KMS creates the ``WebRtcEndpoint`` sending back the media element identifier
to the client::

    {
      "id":2,
      "result":{
         "value":"c4a84b47-1acd-4930-9f6d-008c10782dfe_MediaPipeline/e72a1ff5-e416-48ff-99ef-02f7fadabaf7_WebRtcEndpoint",
         "sessionId":"ba4be2a1-2b09-444e-a368-f81825a6168c"
      },
      "jsonrpc":"2.0"
    }

5. Client invokes the ``connect`` primitive in the ``WebRtcEndpoint`` in order
to create a loopback::

    {
      "id":3,
      "method":"invoke",
      "params":{
         "object":"c4a84b47-1acd-4930-9f6d-008c10782dfe_MediaPipeline/e72a1ff5-e416-48ff-99ef-02f7fadabaf7_WebRtcEndpoint",
         "operation":"connect",
         "operationParams":{
            "sink":"c4a84b47-1acd-4930-9f6d-008c10782dfe_MediaPipeline/e72a1ff5-e416-48ff-99ef-02f7fadabaf7_WebRtcEndpoint"
         },
         "sessionId":"ba4be2a1-2b09-444e-a368-f81825a6168c"
      },
      "jsonrpc":"2.0"
    }

6. KMS carry out the connection and acknowledges the operation::

    {
      "id":3,
      "result":{
         "sessionId":"ba4be2a1-2b09-444e-a368-f81825a6168c"
      },
      "jsonrpc":"2.0"
    }

7. Client invokes the ``processOffer`` primitive in the ``WebRtcEndpoint`` in
order to negotiate SDP in WebRTC::

    {
      "id":4,
      "method":"invoke",
      "params":{
         "object":"c4a84b47-1acd-4930-9f6d-008c10782dfe_MediaPipeline/e72a1ff5-e416-48ff-99ef-02f7fadabaf7_WebRtcEndpoint",
         "operation":"processOffer",
         "operationParams":{
            "offer":"SDP"
         },
         "sessionId":"ba4be2a1-2b09-444e-a368-f81825a6168c"
      },
      "jsonrpc":"2.0"
    }

8. KMS carry out the SDP negotiation and returns the SDP answer::

    {
      "id":4,
      "result":{
         "value":"SDP"
      },
      "jsonrpc":"2.0"
    }


Kurento Module Creator
======================

The default Kurento clients (Java and JavaScript) are created using a tool
called **Kurento Module Creator**. Therefore, this tool can be also be used to
create custom clients in other languages.

Kurento Module Creator can be installed in an Ubuntu machine using the following
command:

.. sourcecode:: bash

   sudo apt-get install kurento-module-creator

The aim of this tools is to generate the client code and also the glue code
needed in the server-side. For code generation it uses
`Freemarker <http://freemarker.org/>`_ as template engine. The typical way to
use Kurento Module Creator is by running a command like this:

.. sourcecode:: bash

    kurento-module-creator -c <CODEGEN_DIR> -r <ROM_FILE> -r <TEMPLATES_DIR>

Where:

- ``CODEGEN_DIR``: Destination directory for generated files.

- ``ROM_FILE``: A space separated list of Kurento Media Element Description
  (kmd) files or folders containing this files. As an example, you can take a
  look to the kmd files within the
  `Kurento Media Server <https://github.com/Kurento/kurento-media-server/tree/master/scaffold>`_
  source code.

- ``TEMPLATES_DIR``: Directory that contains template files. As an example,
  you can take a look to the internal
  `Java <https://github.com/Kurento/kurento-java/tree/master/kurento-client/src/main/resources/templates>`_
  and
  `JavaScript <https://github.com/Kurento/kurento-client-js/tree/master/templates>`_
  templates.
