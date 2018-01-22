# KMS Error Troubleshooting

<!-- TOC -->

- [KMS Error Troubleshooting](#kms-error-troubleshooting)
  - [Introduction](#introduction)
  - [KurentoWebSocketTransport: RPC Messages](#kurentowebsockettransport-rpc-messages)
    - [Example log 1: Ping/Pong keepalive message](#example-log-1-pingpong-keepalive-message)
    - [Example log 2: Creation of a Media Pipeline](#example-log-2-creation-of-a-media-pipeline)
    - [Example log 3: Creation of a WebRtcEndpoint](#example-log-3-creation-of-a-webrtcendpoint)
  - [KurentoWebSocketEventHandler: Events from KMS](#kurentowebsocketeventhandler-events-from-kms)
    - [Example log: IceCandidateFound event from WebRtcEndpoint](#example-log-icecandidatefound-event-from-webrtcendpoint)
  - [Kurento Client (Java)](#kurento-client-java)
    - [KMS is not running](#kms-is-not-running)
    - [KMS became unresponsive (due to network error or crash)](#kms-became-unresponsive-due-to-network-error-or-crash)

<!-- /TOC -->


## Introduction

Kurento Media Server is a complex piece of technology, encompassing multiple components and services, both written in-house and by third parties. All is well whenever every piece in the puzzle is playing along the others, however things can get messy when one little component breaks and it gets very difficult to pinpoint the exact reasons for the errors that can appear in a log file, or the misbehaviors that can occur.

This document will try to outline several bits of knowledge that can prove very useful when studying a failure or error in KMS.


## KurentoWebSocketTransport: RPC Messages

All the JSON Remote Procedure Calls which are exchanged between a KMS instance and the client application are handled by a class named *KurentoWebSocketTransport*. It uses this same name for the logging category.


### Example log 1: Ping/Pong keepalive message

The Kurento Client sends a "ping" message:

```
debug KurentoWebSocketTransport WebSocketTransport.cpp:422 processMessage()  \
Message: {"id":1,"method":"ping","params":{"interval":240000},"jsonrpc":"2.0"}
```

KMS replies with "pong":

```
info KurentoServerMethods      ServerMethods.cpp:794 ping()  WebSocket Ping/Pong

debug KurentoWebSocketTransport WebSocketTransport.cpp:424 processMessage()  \
Response: {"id":1,"jsonrpc":"2.0","result":{"value":"pong"}}
```


### Example log 2: Creation of a Media Pipeline

A client application written in Java would do something such as this to create a Media Pipeline:

```
KurentoClient kurento;
MediaPipeline pipeline = kurento.createMediaPipeline();
```

This would instruct KMS to instantiate the appropriate classes, and then answer the RPC:

```
debug KurentoWebSocketTransport WebSocketTransport.cpp:422 processMessage()  \
Message: {"id":2,"method":"create","params":{"type":"MediaPipeline",  \
"constructorParams":{},"properties":{}},"jsonrpc":"2.0"}

debug KurentoWebSocketTransport WebSocketTransport.cpp:424 processMessage()  \
Response: {"id":2,"jsonrpc":"2.0","result":  \
{"sessionId":"1f2b9252-e8e2-4c27-a2ae-edb8d6faddd9",  \
"value":"a83709cc-3f17-400a-9a22-d7f232d0c8ea_kurento.MediaPipeline"}}
```


### Example log 3: Creation of a WebRtcEndpoint

This Java code would instantiate a new WebRtcEndpoint:

```
WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
```

And KMS would answer the RPC call with a whole representation of the newly created instance:

```
debug KurentoWebSocketTransport WebSocketTransport.cpp:422 processMessage()  \
Message: {"id":3,"method":"create","params":{"type":"WebRtcEndpoint",  \
"constructorParams":{"mediaPipeline":  \
"a83709cc-3f17-400a-9a22-d7f232d0c8ea_kurento.MediaPipeline"},"properties":{},  \
"sessionId":"1f2b9252-e8e2-4c27-a2ae-edb8d6faddd9"},"jsonrpc":"2.0"}

debug KurentoWebSocketTransport WebSocketTransport.cpp:424 processMessage()  \
Response: {"id":3,"jsonrpc":"2.0","result":{"sessionId":  \
"1f2b9252-e8e2-4c27-a2ae-edb8d6faddd9","value":  \
"a83709cc-3f17-400a-9a22-d7f232d0c8ea_kurento.  \
MediaPipeline/e3fd45fc-630d-4c79-93a0-c1bedd6c5d07_kurento.WebRtcEndpoint"}}
```


## KurentoWebSocketEventHandler: Events from KMS

The class *KurentoWebSocketEventHandler* is in charge of catching any event generated from within KMS, and sending it to the client application for further processing. As with all RPC calls, the events are sent in JSON format.

These are some examples of logging messages for different types of RPC communications:


### Example log: IceCandidateFound event from WebRtcEndpoint

When the ICE Gathering process has been initiated in a WebRtcEndpoint, the underlying Nice Agent (part of the 3rd-party library 'libnice') will raise one event named *IceCandidateFound* for each local candidate that has been found. Then, the event would get sent from KMS to the client application:

```
debug KurentoWebSocketEventHandler WebSocketEventHandler.cpp:54 sendEvent()  \
Sending event: {"jsonrpc":"2.0","method":"onEvent","params":{"value":{"data":  \
{"candidate":{"__module__":"kurento","__type__":"IceCandidate","candidate":  \
"candidate:1 1 UDP 2013266431 fe80::811:97ff:fee3:1082 59405 typ host",  \
"sdpMLineIndex":0,"sdpMid":"audio"},"source":  \
"a83709cc-3f17-400a-9a22-d7f232d0c8ea_kurento.  \
MediaPipeline/e3fd45fc-630d-4c79-93a0-c1bedd6c5d07_kurento.WebRtcEndpoint",  \
"tags":[],"timestamp":"1503926521","type":"IceCandidateFound"},  \
"object":"a83709cc-3f17-400a-9a22-d7f232d0c8ea_kurento.  \
MediaPipeline/e3fd45fc-630d-4c79-93a0-c1bedd6c5d07_kurento.WebRtcEndpoint",  \
"type":"IceCandidateFound"}}}
```


## Kurento Client (Java)

These are some common errors found to affect Kurento client applications.


### KMS is not running

Usually, the Kurento Client library is directed to connect with an instance of KMS that the developer expects will be running in some remote server. If there is no instance of KMS running at the provided URL, the Kurento Client library will raise an exception which **the client application should catch** and handle accordingly.

This is a sample of what the console output will look like, with the logging level set to DEBUG:

```
$ mvn compile exec:java -Dkms.url=ws://localhost:8888/kurento
 INFO org.kurento.tutorial.player.Application  : Starting Application on TEST with PID 16448
DEBUG o.kurento.client.internal.KmsUrlLoader   : Executing getKmsUrlLoad(b843d6f6-02dd-49b4-96b6-f2fd2e8b1c8d) in KmsUrlLoader
DEBUG o.kurento.client.internal.KmsUrlLoader   : Obtaining kmsUrl=ws://localhost:8888/kurento from config file or system property
DEBUG org.kurento.client.KurentoClient         : Connecting to kms in ws://localhost:8888/kurento
DEBUG o.k.j.c.JsonRpcClientNettyWebSocket      :  Creating JsonRPC NETTY Websocket client
DEBUG o.kurento.jsonrpc.client.JsonRpcClient   :  Enabling heartbeat with an interval of 240000 ms
DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket   : [KurentoClient]  Connecting webSocket client to server ws://localhost:8888/kurento
 WARN o.kurento.jsonrpc.client.JsonRpcClient   : [KurentoClient]  Error sending heartbeat to server. Exception: [KurentoClient]  Exception connecting to WebSocket server ws://localhost:8888/kurento
 WARN o.kurento.jsonrpc.client.JsonRpcClient   : [KurentoClient]  Stopping heartbeat and closing client: failure during heartbeat mechanism
DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket   : [KurentoClient]  Connecting webSocket client to server ws://localhost:8888/kurento
DEBUG o.k.jsonrpc.internal.ws.PendingRequests  : Sending error to all pending requests
 WARN o.k.j.c.JsonRpcClientNettyWebSocket      : [KurentoClient]  Trying to close a JsonRpcClientNettyWebSocket with channel == null
 WARN ationConfigEmbeddedWebApplicationContext : Exception encountered during context initialization - cancelling refresh attempt: Factory method 'kurentoClient' threw exception; nested exception is org.kurento.commons.exception.KurentoException: Exception connecting to KMS
ERROR o.s.boot.SpringApplication               : Application startup failed
```

As opposed to that, the console output for when a connection is successfully done with an instance of KMS should look similar to this sample:

```
$ mvn compile exec:java -Dkms.url=ws://localhost:8888/kurento
 INFO org.kurento.tutorial.player.Application  : Starting Application on TEST with PID 21617
DEBUG o.kurento.client.internal.KmsUrlLoader   : Executing getKmsUrlLoad(af479feb-dc49-4a45-8b1c-eedf8325c482) in KmsUrlLoader
DEBUG o.kurento.client.internal.KmsUrlLoader   : Obtaining kmsUrl=ws://localhost:8888/kurento from config file or system property
DEBUG org.kurento.client.KurentoClient         : Connecting to kms in ws://localhost:8888/kurento
DEBUG o.k.j.c.JsonRpcClientNettyWebSocket      :  Creating JsonRPC NETTY Websocket client
DEBUG o.kurento.jsonrpc.client.JsonRpcClient   :  Enabling heartbeat with an interval of 240000 ms
DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket   : [KurentoClient]  Connecting webSocket client to server ws://localhost:8888/kurento
 INFO o.k.j.c.JsonRpcClientNettyWebSocket      : [KurentoClient]  Connecting native client
 INFO o.k.j.c.JsonRpcClientNettyWebSocket      : [KurentoClient]  Creating new NioEventLoopGroup
 INFO o.k.j.c.JsonRpcClientNettyWebSocket      : [KurentoClient]  Inititating new Netty channel. Will create new handler too!
DEBUG o.k.j.c.JsonRpcClientNettyWebSocket      : [KurentoClient]  channel active
DEBUG o.k.j.c.JsonRpcClientNettyWebSocket      : [KurentoClient]  WebSocket Client connected!
 INFO org.kurento.tutorial.player.Application  : Started Application in 1.841 seconds (JVM running for 4.547)
```


### KMS became unresponsive (due to network error or crash)

The Kurento Client library is programmed to start a retry-connect process whenever the other side of the RPC channel -ie. the KMS instance- becomes unresponsive. An error exception will raise, which again **the client application should handle**, and then the library will automatically start trying to reconnect with KMS.

This is how this process would look like. In this example, KMS was restarted so the Kurento Client library lost connectivity with KMS for a moment, but then it was able con reconnect and continue working normally:

```
 INFO org.kurento.tutorial.player.Application  : Started Application in 1.841 seconds (JVM running for 4.547)

(... Application is running normally at this point)
(... Now, KMS becomes unresponsive)

 INFO o.k.j.c.JsonRpcClientNettyWebSocket      : [KurentoClient]  channel closed
DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket   : [KurentoClient] JsonRpcWsClient disconnected from ws://localhost:8888/kurento because Channel closed.
DEBUG o.kurento.jsonrpc.client.JsonRpcClient   : Disabling heartbeat. Interrupt if running is false
DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket   : [KurentoClient]  JsonRpcWsClient reconnecting to ws://localhost:8888/kurento.
DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket   : [KurentoClient]  Connecting webSocket client to server ws://localhost:8888/kurento
 INFO o.k.j.c.JsonRpcClientNettyWebSocket      : [KurentoClient]  Connecting native client
 INFO o.k.j.c.JsonRpcClientNettyWebSocket      : [KurentoClient]  Closing previously existing channel when connecting native client
DEBUG o.k.j.c.JsonRpcClientNettyWebSocket      : [KurentoClient]  Closing client
 INFO o.k.j.c.JsonRpcClientNettyWebSocket      : [KurentoClient]  Inititating new Netty channel. Will create new handler too!
 WARN o.k.j.c.JsonRpcClientNettyWebSocket      : [KurentoClient]  Trying to close a JsonRpcClientNettyWebSocket with channel == null
DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket   : TryReconnectingForever=true
DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket   : TryReconnectingMaxTime=0
DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket   : maxTimeReconnecting=9223372036854775807
DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket   : currentTime=1510773733903
DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket   : Parar de reconectar=false
 WARN o.k.j.c.AbstractJsonRpcClientWebSocket   : [KurentoClient]  Exception trying to reconnect to server ws://localhost:8888/kurento. Retrying in 5000 millis

org.kurento.jsonrpc.JsonRpcException: [KurentoClient]  Exception connecting to WebSocket server ws://localhost:8888/kurento
    at (...)
Caused by: io.netty.channel.AbstractChannel$AnnotatedConnectException: Connection refused: localhost/127.0.0.1:8888
    at (...)

(... Now, KMS becomes responsive again)

DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket   : [KurentoClient]  JsonRpcWsClient reconnecting to ws://localhost:8888/kurento.
DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket   : [KurentoClient]  Connecting webSocket client to server ws://localhost:8888/kurento
 INFO o.k.j.c.JsonRpcClientNettyWebSocket      : [KurentoClient]  Connecting native client
 INFO o.k.j.c.JsonRpcClientNettyWebSocket      : [KurentoClient]  Creating new NioEventLoopGroup
 INFO o.k.j.c.JsonRpcClientNettyWebSocket      : [KurentoClient]  Inititating new Netty channel. Will create new handler too!
DEBUG o.k.j.c.JsonRpcClientNettyWebSocket      : [KurentoClient]  channel active
DEBUG o.k.j.c.JsonRpcClientNettyWebSocket      : [KurentoClient]  WebSocket Client connected!
DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket   : [KurentoClient]  Req-> {"id":2,"method":"connect","jsonrpc":"2.0"}
DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket   : [KurentoClient]  <-Res {"id":2,"result":{"serverId":"1a3b4912-9f2e-45da-87d3-430fef44720f","sessionId":"f2fd16b7-07f6-44bd-960b-dd1eb84d9952"},"jsonrpc":"2.0"}
DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket   : [KurentoClient]  Reconnected to the same session in server ws://localhost:8888/kurento

(... At this point, the Kurento Client is connected again to KMS)
```




----

Log without unique fields (timestamp, process ID, thread ID):

export GST_DEBUG_NO_COLOR=1
export GST_DEBUG=3,Kurento*:4,kms*:4,rtpendpoint:4,webrtcendpoint:4
kurento-media-server 2>&1 | awk '{$1=$2=$3=""; print $0}'
