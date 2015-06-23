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

An *RPC call* is represented by sending a *Request message* to a server. The
*Request message* has the following members:

-  **jsonrpc**: a string specifying the version of the JSON-RPC protocol. It
   must be exactly "2.0".
-  **id**: an unique identifier established by the client that contains a
   string or number. The server must reply with the same value in the
   *Response message*. This member is used to correlate the context between
   both messages.
-  **method**: a string containing the name of the method to be invoked.
-  **params**: a structured value that holds the parameter values to be used
   during the invocation of the method.

The following JSON shows a sample requests::

    {
      "jsonrpc": "2.0",
      "id": 1,
      "method": "create",
      "params": {
        "type": "PlayerEndPoint",
        "constructorParams": {
          "pipeline": "6829986",
          "uri": "http://host/app/video.mp4"
        },
        "sessionId": "c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      }
    }

Successful Response messages
----------------------------

When an *RPC call* is made the server replies with a *Response message*. In the
case of a successful response, the *Response message* will contain the
following members:

-  **jsonrpc**: a string specifying the version of the JSON-RPC protocol. It
   must be exactly "2.0".
-  **id**: this member is mandatory and it must match the value of the *id*
   member in the *Request message*.
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

When an *RPC call* is made the server replies with a *Response message*. In the
case of an error response, the *Response message* will contain the following
members:

-  **jsonrpc**: a string specifying the version of the JSON-RPC protocol. It
   must be exactly "2.0".
-  **id**: this member is mandatory and it must match the value of the *id*
   member in the *Request message*. If there was an error in detecting the *id*
   in the *Request message* (e.g. Parse Error/Invalid Request), it equals to
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

Once the WebSocket has been established, the Kurento Protocol offers five
different types of request/response messages:

 - **create**: Instantiates a new media object, that is, a pipeline or media
   element.
 - **invoke**: Calls a method of an existing media object.
 - **subscribe**: Creates a subscription to an event in a object.
 - **unsubscribe**: Removes an existing subscription to an event.
 - **release**: Deletes the object and release resources used by it.

The Kurento Protocol allows to Kurento Media Server send requests to clients:

 - **onEvent**: This request is sent from kurento Media server to clients
   when an event occurs.

Create messages
---------------

Create message requests the creation of an object of the Kurento API. The
parameter ``type`` specifies the type of the object to be created. The
parameter ``constructorParams`` contains all the information needed to create the
object. Each message needs different ``constructorParams`` to create the object.
These parameters are defined in :doc:`Kurento API section <kurento_API>`.

Finally, a ``sessionId`` parameter is included with the identifier of the
current session. The value of this parameter is sent by Kurento Media Server to
the client in each response. Only the first request from client to server is
allowed to not include the ''sessionId'' (because at this point is unknown for
the client).

The following example shows a Request message requesting the creation of an
object of the type ``PlayerEndpoint`` within the pipeline ``6829986`` and the
parameter ``uri: http://host/app/video.mp4`` in the session
``c93e5bf0-4fd0-4888-9411-765ff5d89b93``::

    {
      "jsonrpc": "2.0",
      "id": 1,
      "method": "create",
      "params": {
        "type": "PlayerEndPoint",
        "constructorParams": {
          "pipeline": "6829986",
          "uri": "http://host/app/video.mp4"
        },
        "sessionId": "c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      }
    }

The ``Response`` message contains the ``id`` of the new object in the field
``value``. This message ``id`` has to be used in other requests of the protocol
(as we will describe later). As stated before, the ``sessionId`` is also
returned in each response.

The following example shows a typical response to a create message::

    {
      "jsonrpc": "2.0",
      "id": 1,
      "result": {
        "value": "442352747",
        "sessionId": "c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      }
    }

Invoke messages
---------------

Invoke message requests the invocation of an operation in the specified object.
The parameter ``object`` indicates the ``id`` of the object in which the
operation will be invoked. The parameter ``operation`` carries the name of the
operation to be executed. Finally, the parameter ``operationParams`` has the
parameters needed to execute the operation. The object specified has to
understand the

The following example shows a ``Request`` message requesting the invocation of
the operation ``connect`` on the object ``442352747`` with parameter sink
``6829986``. The ``sessionId`` is also included as is mandatory for all
requests in the session (except the first one)::

    {
      "jsonrpc": "2.0",
      "id": 2,
      "method": "invoke",
      "params": {
        "object": "442352747", "operation": "connect",
        "operationParams": {
          "sink": "6829986"
        },
        "sessionId": "c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      }
    }

The ``Response message`` contains the value returned while executing the
operation invoked in the object or nothing if the operation doesn’t return any
value.

The following example shows a typical response while invoking the operation
``connect`` (that doesn’t return anything)::

    {
      "jsonrpc": "2.0",
      "result": {
        "sessionId": "c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      },
      "id": 2
    }

Release messages
----------------

Release message requests the release of the specified object. The parameter
``object`` indicates the ``id`` of the object to be released::

    {
      "jsonrpc": "2.0",
      "id": 3,
      "method": "release",
      "params": {
        "object": "442352747",
        "sessionId": "c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      }
    }

The ``Response`` message only contains the ``sessionID``. The following example
shows the typical response of a release request::

    {
      "jsonrpc":"2.0",
      "id":3,
      "result": {
        "sessionId":"c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      }
    }

Subscribe messages
------------------

Subscribe message requests the subscription to a certain kind of events in the
specified object. The parameter ``object`` indicates the ``id`` of the object
to subscribe for events. The parameter ``type`` specifies the type of the
events. If a client is subscribed for a certain type of events in an object,
each time an event is fired in this object, a request with method ``onEvent``
is sent from Kurento Media Server to the client. This kind of request is
described few sections later.

The following example shows a ``Request`` message requesting the subscription of
the event type ``EndOfStream`` on the object ``311861480``. The ``sessionId``
is also included::

    {
      "jsonrpc":"2.0",
      "id":4,
      "method":"subscribe",
      "params":{
        "object":"311861480",
        "type":"EndOfStream",
        "sessionId":"c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      }
    }

The ``Response`` message contains the subscription identifier. This value can be
used later to remove this subscription.

The following example shows the response of subscription request. The ``value``
attribute contains the subscription id::

    {
      "jsonrpc":"2.0",
      "id":4,
      "result": {
        "value":"353be312-b7f1-4768-9117-5c2f5a087429",
        "sessionId":"c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      }
    }

Unsubscribe messages
--------------------

Unsubscribe message requests the cancellation of a previous event subscription.
The parameter subscription contains the subscription ``id`` received from the
server when the subscription was created.

The following example shows a ``Request message`` requesting the cancellation of
the subscription ``353be312-b7f1-4768-9117-5c2f5a087429``::

    {
      "jsonrpc":"2.0",
      "id":5,
      "method":"unsubscribe",
      "params": {
        "subscription":"353be312-b7f1-4768-9117-5c2f5a087429",
        "sessionId":"c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      }
    }

The ``Response`` message only contains the ``sessionID``. The following example
shows the typical response of an unsubscription request::

    {
      "jsonrpc":"2.0",
      "id":5,
      "result": {
        "sessionId":"c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      }
    }

OnEvent Message
---------------

When a client is subscribed to a type of events in an object, the server sends
an ``onEvent`` request each time an event of that type is fired in the object.
This is possible because the Kurento Protocol is implemented with websockets
and there is a full duplex channel between client and server. The request that
server send to client has all the information about the event:

  - **data**: Information about this specific of this type of event.
  - **source**: the object source of the event.
  - **type**: The type of the event.
  - **subscription**: subscription ``id`` for which the event is fired.

The following example shows a notification sent for server to client to notify
an event of type ``EndOfStream`` in the object ``311861480`` with subscription
``353be312-b7f1-4768-9117-5c2f5a087429``::

    {
      "jsonrpc": "2.0",
      "id": 6,
      "method": "onEvent",
      "params": {
        "value": {
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

The ``Response`` message does not contain any information. Is only a form of
acknowledge message. The following example shows the typical response of an
onEvent request::

    {
      "jsonrpc":"2.0",
      "id":6,
      "result": ""
    }


Network issues
==============

Resources handled by KMS are high-consumming...

For this reason, KMS implements a garbage collector.

A Media Element is collected when the client is disconnected longer than 4
minutes. After that time, these media elements are dispossed automatically.

Therefore the websocket connection between client and KMS be active any time. In
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

- `KMS core <https://github.com/Kurento/kms-core/blob/develop/src/server/interface/core.kmd.json>`_

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
         "constructorParams":{}
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

.. sourcecode:: sh

   sudo apt-get install kurento-module-creator

The aim of this tools is to generate the client code and also the glue code
needed in the server-side. For code generation it uses
`Freemarker <http://freemarker.org/>`_ as template engine. The typicall way to
use Kurento Module Creater is by running a command like this:

.. sourcecode:: sh

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
