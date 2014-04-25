.. _openapi:

%%%%%%%%%%%%%%%%%%%%%%%
 Open API Specification
%%%%%%%%%%%%%%%%%%%%%%%

.. highlight:: json

Introduction
============

Kurento Open API is a :term:`REST`\ful resource-oriented API accessed via
HTTP/HTTPS that uses :term:`JSON-RPC` V2.0 based representations for information
exchange. This document describes the API exposed by the Application
Server as defined in the :ref:`Kurento Architecture Description <architecture>`.

Intended Audience
-----------------

This specification is intended for both software developers and
implementors of this GE. For developers, this document details the
:term:`REST`\ful API to build interactive multimedia applications compliant with
the :doc:`Kurento Architecture Description <Architecture>`.
Implementors can build their GEi APIs based on the information contained
in this specification.

Before reading this document it is recommended to read first the
:doc:`Kurento Architecture Description <Architecture>` and
the :doc:`Programmers Guide <Developer_and_Programmer_Guide>`.
Moreover, the reader should be also familiar with:

-  :term:`REST` and RESTful web services
-  HTTP/1.1 (:rfc:`2616`)
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

Resource representation is transmitted between client and server by
using HTTP 1.1 protocol, as defined by IETF :rfc:`2616`. Each time an HTTP
request contains payload, a `Content-Type`:mailheader: header shall be used to specify
the MIME type of wrapped representation. In addition, both client and
server may use as many HTTP headers as they consider necessary.

Representation Format
---------------------

Kurento RESTful APIs support JSON as representation format for
request and response parameters following the recommendations in the
proposal `JSON-RPC over
HTTP <http://www.simple-is-better.org/json-rpc/jsonrpc20-over-http.html>`__.

The format of the requests is specified by using the `Content-Type`:mailheader:
header with a value of :mimetype:`application/json-rpc` and is required for
requests containing a body. The format required for the response is
specified in the request by setting the `Accept`:mailheader: header to the value
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
this GE, namely, the Content Manager API. It is recommended to review
the :doc:`Programmers Guide <Developer_and_Programmer_Guide>`
before proceeding with this section.

Content API
-----------

The Content API is exposed in the form of four services: *HttpPlayer*,
*HttpRecorder*, *RtpContent* and *WebRtcContent* described in the
following subsections.

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

    ============================= ====================================================
    **Verb**                      POST
    ============================= ====================================================
    **URI**                       :samp:`/{CONTEXT-ROOT}/{APP_LOGIC_PATH}/{ContentID}`
    ----------------------------- ----------------------------------------------------
    **Description**               Performs an RPC call regarding :samp:`{ContentID}`. The
                                  *Request object* is processed by the *RTPContent*
                                  application handler tied to :samp:`{APP_LOGIC_PATH}`
                                  in the :samp:`{CONTEXT-ROOT}` of the application.
    ============================= ====================================================



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
