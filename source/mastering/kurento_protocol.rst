.. _kurentoprotocol:

%%%%%%%%%%%%%%%%
Kurento Protocol
%%%%%%%%%%%%%%%%

.. highlight:: json

**Kurento Protocol** is the Kurento Server protocol based on :term:`WebSocket`
that uses :term:`JSON-RPC` V2.0 messages for making requests and sending
responses.

Currently there are two :term:`Kurento Client <Kurento Client>` out of the box:
Java and JavaScript. If you have another favorite language, you can still use
Kurento by means of :doc:`Kurento Protocol <Kurento_Protocol>`. This protocol
allows to control Kurento Server and it is based on Internet standards like
:term:`WebSocket` and :term:`Json-RPC`.


JSON-RPC Messages format
========================

Kurento Protocolo uses :term:`JSON-RPC` V2.0 to code its messages. In the
following subsections we will show how this format code the messages in
:term:`JSON`.

Request object
~~~~~~~~~~~~~~

An *RPC call* is represented by sending a *Request object* to a server. The
*Request object* has the following members:

-  **jsonrpc**: a string specifying the version of the JSON-RPC protocol. It
   must be exactly "2.0".
-  **id**: an identifier established by the client that contains a string or
   number. The server must reply with the same value in the *Response object*.
   This member is used to correlate the context between both objects.
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
        "creationParams": {
          "pipeline": "6829986",
          "uri": "http://host/app/video.mp4"
        },
        "sessionId": "c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      }
    }

Successful Response object
~~~~~~~~~~~~~~~~~~~~~~~~~~

When an *RPC call* is made the server replies with a *Response object*. In the
case of a successful response, the *Response object* will contain the following
members:

-  **jsonrpc**: a string specifying the version of the JSON-RPC protocol. It
   must be exactly "2.0".
-  **id**: this member is mandatory and it must match the value of the *id*
   member in the *Request object*.
-  **result**: its value is determined by the method invoked on the server.
   In case the connection is rejected, it's returned an object with a
   *rejected* attribute containing an object with a *code* and *message*
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

Error Response object
~~~~~~~~~~~~~~~~~~~~~

When an *RPC call* is made the server replies with a *Response object*. In the
case of an error response, the *Response object* will contain the following
members:

-  **jsonrpc**: a string specifying the version of the JSON-RPC protocol. It
   must be exactly "2.0".
-  **id**: this member is mandatory and it must match the value of the *id*
   member in the *Request object*. If there was an error in detecting the *id*
   in the *Request object* (e.g. Parse Error/Invalid Request), it equals to
   null.
-  **error**: an object describing the error through the following members:

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

As explained in :doc:``Kurento API section <Kurento_API>``, Kurento Server
exposes a full fledged API to let applications to process media in several ways.

To allow this rich API, Kurento Clients require requires full-duplex
communications between client and server infrastructure. For this reason, the
Kurento Protocol is based on WebSocket transports.

Previous to issuing commands, the Kurento Client requires establishing a
WebSocket connection with Kurento Server to the URL:
``ws://hostname:port/kurento``

Once the WebSocket has been established, the Kurento Protocol offers five
different types of request/response messages:
 - **create**: Instantiates a new media object, that is, a pipeline or media
   element.
 - **invoke**: Calls a method of an existing media object.
 - **subscribe**: Creates a subscription to an event in a object.
 - **unsubscribe**: Removes an existing subscription to an event.
 - **release**: Deletes the object and release resources used by it.

The Kurento Protocol allows to Kurento Server send requests to clients:

 - **onEvent**: This request is sent from kurento server to clients when an
   event occurs.

Create messages
~~~~~~~~~~~~~~~

Create message requests the creation of an object of the Kurento API. The
parameter ``type`` specifies the type of the object to be created. The
parameter ``creationParams`` contains all the information needed to create the
object. Each object type needs different ``creationParams`` to create the
object. These parameters are defined in
:doc:`Kurento API section <Kurento_API>`.

Finally, a ``sessionId`` parameter is included with the identifier of the
current session. The value of this parameter is sent by Kurento server to the
client in each response. Only the first request from client to server is
allowed to not include the ''sessionId'' (because at this point is unknown for
the client).

The following example shows a Request object requesting the creation of an
object of the type ``PlayerEndpoint`` within the pipeline ``6829986`` and the
parameter ``uri: http://host/app/video.mp4`` in the session
``c93e5bf0-4fd0-4888-9411-765ff5d89b93``::

    {
      "jsonrpc": "2.0",
      "id": 1,
      "method": "create",
      "params": {
        "type": "PlayerEndPoint",
        "creationParams": {
          "pipeline": "6829986",
          "uri": "http://host/app/video.mp4"
        },
        "sessionId": "c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      }
    }

The ``Response`` object contains the id of the new object in the field
``value``. This object id has to be used in other requests of the protocol (as
we will describe later). As stated before, the ``sessionId`` is also returned
in each response.

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
~~~~~~~~~~~~~~~

Invoke message requests the invocation of an operation in the specified object.
The parameter ``object`` indicates the id of the object in which the operation
will be invoked. The parameter ``operation`` carries the name of the operation
to be executed. Finally, the parameter ``operationParams`` has the parameters
needed to execute the operation. The object specified has to understand the
operation name and parameters. In the :doc:`Kurento API section <Kurento_API>`
is described the valid operations for all object types.

The following example shows a ``Request`` object requesting the invocation of
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

The ``Response object`` contains the value returned while executing the
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
~~~~~~~~~~~~~~~~

Release message requests the release of the specified object. The parameter
``object`` indicates the id of the object to be released::

    {
      "jsonrpc": "2.0",
      "id": 3,
      "method": "release",
      "params": {
        "object": "442352747",
        "sessionId": "c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      }
    }

The ``Response`` object only contains the ``sessionID``. The following example
shows the typical response of a release request::

    {
      "jsonrpc":"2.0",
      "id":3,
      "result": {
        "sessionId":"c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      }
    }

Subscribe messages
~~~~~~~~~~~~~~~~~~

Subscribe message requests the subscription to a certain kind of events in the
specified object. The parameter ``object`` indicates the id of the object to
subscribe for events. The parameter ``type`` specifies the type of the events.
If a client is subscribed for a certain type of events in an object, each time
an event is fired in this object, a request with method ``onEvent`` is sent
from kurento Server to the client. This kind of request is described few
sections later.

The following example shows a ``Request`` object requesting the subscription of
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

The ``Response`` object contains the subscription identifier. This value can be
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
~~~~~~~~~~~~~~~~~~~~

Unsubscribe message requests the cancellation of a previous event subscription.
The parameter subscription contains the ``subscription`` id received from the
server when the subscription was created.

The following example shows a ``Request object`` requesting the cancellation of
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

The ``Response`` object only contains the ``sessionID``. The following example
shows the typical response of an unsubscription request::

    {
      "jsonrpc":"2.0",
      "id":5,
      "result": {
        "sessionId":"c93e5bf0-4fd0-4888-9411-765ff5d89b93"
      }
    }


OnEvent Message
~~~~~~~~~~~~~~~

When a client is subscribed to a type of events in an object, the server send an
``onEvent`` request each time an event of that type is fired in the object.
This is possible because the Kurento Protocol is implemented with websockets
and there is a full duplex channel between client and server. The request that
server send to client has all the information about the event:

  - **data**: Information about this specific of this type of event.
  - **source**: the object source of the event.
  - **type**: The type of the event.
  - **subscription**: subscription id for which the event is fired.

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

The ``Response`` object does not contain any information. Is only a form of
acknowledge message. The following example shows the typical response of an
onEvent request::

    {
      "jsonrpc":"2.0",
      "id":6,
      "result": ""
    }


