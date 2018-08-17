======================
Troubleshooting Issues
======================

Kurento Media Server is a complex piece of technology, encompassing multiple components and services, both written in-house and by third parties. All is well whenever every piece in the puzzle is playing along the others, however things can get messy when one little component breaks and it gets very difficult to pinpoint the exact reasons for the errors that can appear in a log file, or the misbehaviours that can occur.

This document will try to outline several bits of knowledge that can prove very useful when studying a failure or error in KMS.

.. contents:: Table of Contents



Media Server
============

Media Server crashed
--------------------

If the Media Server crashes, it will generate an **error log** and also in typical Ubuntu systems, the Linux Kernel will generate a **crash core dump**.

However, these files won't contain much useful information if the relevant debug symbols are not installed. Before :ref:`filing a bug report <support-community>`, make sure to run your breaking test case *after* having installed all ``-dbg`` packages:

.. code-block:: bash

    PACKAGES=(
      # Third-party libraries
      libglib2.0-0-dbg
      libssl1.0.0-dbg

      # Kurento external libraries
      gstreamer1.5-plugins-base-dbg
      gstreamer1.5-plugins-good-dbg
      gstreamer1.5-plugins-ugly-dbg
      gstreamer1.5-plugins-bad-dbg
      gstreamer1.5-libav-dbg
      libgstreamer1.5-0-dbg
      libnice-dbg
      libsrtp1-dbg
      openwebrtc-gst-plugins-dbg
      kmsjsoncpp-dbg

      # KMS main components
      kms-jsonrpc-dbg
      kms-core-dbg
      kms-elements-dbg
      kms-filters-dbg
      kurento-media-server-dbg

      # KMS extra modules
      kms-chroma-dbg
      kms-crowddetector-dbg
      kms-platedetector-dbg
      kms-pointerdetector-dbg
    )

    sudo apt-get update
    sudo apt-get install "${PACKAGES[@]}"

As an example, see what an error log from Kurento looks like after a crash, when debug symbols are NOT installed:

.. code-block:: text

   $ cat /var/log/kurento-media-server/errors.log
   Segmentation fault (thread 139667051341568, pid 1)
   Stack trace:
   [kurento::MediaElementImpl::mediaFlowInStateChange(int, char*, KmsElementPadType)]
   /usr/lib/x86_64-linux-gnu/libkmscoreimpl.so.6:0x1025E0
   [virtual thunk to kurento::MediaElementImpl::getGstreamerDot(std::shared_ptr<kurento::GstreamerDotDetails>)]
   /usr/lib/x86_64-linux-gnu/libkmscoreimpl.so.6:0xFA469
   [g_closure_invoke]
   /usr/lib/x86_64-linux-gnu/libgobject-2.0.so.0:0xFFA5
   [g_signal_handler_disconnect]
   /usr/lib/x86_64-linux-gnu/libgobject-2.0.so.0:0x21FC1
   [g_signal_emit_valist]
   /usr/lib/x86_64-linux-gnu/libgobject-2.0.so.0:0x2AD5C
   [g_signal_emit]
   /usr/lib/x86_64-linux-gnu/libgobject-2.0.so.0:0x2B08F
   [check_if_flow_media]
   /usr/lib/x86_64-linux-gnu/libkmsgstcommons.so.6:0x1F9E4
   [gst_mini_object_steal_qdata]
   /usr/lib/x86_64-linux-gnu/libgstreamer-1.5.so.0:0x6C29B
   [g_hook_list_marshal]
   /lib/x86_64-linux-gnu/libglib-2.0.so.0:0x3A904
   [gst_mini_object_steal_qdata]
   /usr/lib/x86_64-linux-gnu/libgstreamer-1.5.so.0:0x6AAFB
   [gst_flow_get_name]
   /usr/lib/x86_64-linux-gnu/libgstreamer-1.5.so.0:0x6E98B
   [gst_pad_push]
   /usr/lib/x86_64-linux-gnu/libgstreamer-1.5.so.0:0x76533
   [gst_proxy_pad_chain_default]
   /usr/lib/x86_64-linux-gnu/libgstreamer-1.5.so.0:0x5F5E3
   [gst_flow_get_name]
   /usr/lib/x86_64-linux-gnu/libgstreamer-1.5.so.0:0x6E5CF
   [gst_pad_push]
   /usr/lib/x86_64-linux-gnu/libgstreamer-1.5.so.0:0x76533
   [gst_proxy_pad_chain_default]
   /usr/lib/x86_64-linux-gnu/libgstreamer-1.5.so.0:0x5F5E3

And now this is that same crash, after installing the packages *libglib2.0-0-dbg*, *libgstreamer1.5-0-dbg*, and *kms-core-dbg*:

.. code-block:: text

   # cat /var/log/kurento-media-server/errors.log
   Segmentation fault (thread 140672899761920, pid 15217)
   Stack trace:
   [kurento::MediaElementImpl::mediaFlowInStateChange(int, char*, KmsElementPadType)]
   /home/kurento/kms-omni-build/kms-core/src/server/implementation/objects/MediaElementImpl.cpp:479
   [std::__shared_count<(__gnu_cxx::_Lock_policy)2>::~__shared_count()]
   /usr/include/c++/5/bits/shared_ptr_base.h:659
   [closure_invoke_notifiers]
   /build/glib2.0-prJhLS/glib2.0-2.48.2/./gobject/gclosure.c:290
   [accumulate]
   /build/glib2.0-prJhLS/glib2.0-2.48.2/./gobject/gsignal.c:3134
   [g_signal_emit_valist]
   /build/glib2.0-prJhLS/glib2.0-2.48.2/./gobject/gsignal.c:3413 (discriminator 1)
   [g_signal_emit]
   /build/glib2.0-prJhLS/glib2.0-2.48.2/./gobject/gsignal.c:3443
   [cb_buffer_received]
   /home/kurento/kms-omni-build/kms-core/src/gst-plugins/commons/kmselement.c:578
   [probe_hook_marshal]
   /opt/kurento/gst/gstpad.c:3450
   [g_hook_list_marshal]
   /build/glib2.0-prJhLS/glib2.0-2.48.2/./glib/ghook.c:673
   [do_probe_callbacks]
   /opt/kurento/gst/gstpad.c:3605
   [gst_pad_chain_data_unchecked]
   /opt/kurento/gst/gstpad.c:4163
   [gst_pad_push]
   /opt/kurento/gst/gstpad.c:4556
   [gst_proxy_pad_chain_default]
   /opt/kurento/gst/gstghostpad.c:127
   [gst_pad_chain_data_unchecked]
   /opt/kurento/gst/gstpad.c:4185
   [gst_pad_push]
   /opt/kurento/gst/gstpad.c:4556
   [gst_proxy_pad_chain_default]
   /opt/kurento/gst/gstghostpad.c:127

Note how most lines of the stack trace are now indicating specific file names and line numbers, which would be a huge help for any developer to know where to start looking for any potential bug.



Media Server disconnects from Client Application
------------------------------------------------

E.g. Kurento keeps disconnecting every 30 minutes on high load peek time.

Checklist:

- Deploy a properly configured STUN or TURN server. coturn tends to work fine for this, and Kurento has some documentation about how to install and configure it: https://doc-kurento.readthedocs.io/en/latest/user/faq.html#install-coturn-turn-stun-server

- Use this WebRTC sample page to test that your STUN/TURN server is working properly: https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/

- Configure your STUN/TURN server in Kurento, as explained here: https://doc-kurento.readthedocs.io/en/latest/user/installation.html#stun-and-turn-servers

  .. note::

     The features provided by TURN are a superset of those provided by STUN. This means that *you don’t need to configure a STUN server if you are already using a TURN server*.

- Make sure your Kurento settings syntax is correct. For STUN servers, this would be:

  .. code-block:: text

     stunServerAddress=<serverAddress>
     stunServerPort=<serverPort>

  For TURN servers, the correct line is like this:

  .. code-block:: text

     turnURL=username:password@address:port

- Check the debug logs of the STUN/TURN server. Maybe the server is failing and some useful error messages are being printed there.

- Check the debug logs of KMS. In case of an incorrect configuration, you'll find these messages:

  .. code-block:: text

     INFO  STUN server Port not found in config; using default value: 3478
     INFO  STUN server IP address not found in config; NAT traversal requires either STUN or TURN server
     INFO  TURN server IP address not found in config; NAT traversal requires either STUN or TURN server

  In case of having correctly configured a STUN server in KMS, the log messages will read like this:

  .. code-block:: text

     INFO  Using STUN reflexive server IP: <IpAddress>
     INFO  Using STUN reflexive server Port: <Port>

  And in case of a TURN server:

  .. code-block:: text

     INFO  Using TURN relay server: <user:password>@<IpAddress>:<Port>
     INFO  TURN server info set: <user:password>@<IpAddress>:<Port>



Service init doesn't work
-------------------------

The package *kurento-media-server* provides a service file that integrates with the Ubuntu init system. This service file loads its user configuration from */etc/default/kurento-media-server*, where the user is able to configure several features as needed.

In Ubuntu, log messages from init scripts are managed by *systemd*, and can be checked in to ways:

- */var/log/syslog* contains a copy of all init service messages.
  You can open it to see past messages, or follow it in real time with this command:

  .. code-block:: bash

     tail -f /var/log/syslog

- You can query the status of the *kurento-media-server* service with this command:

  .. code-block:: bash

     systemctl status kurento-media-server.service



.. _troubleshooting-h264:

OpenH264 not found
------------------

**Problem**: Installing and running KMS on a clean Ubuntu installation shows this message:

.. code-block:: text

   (gst-plugin-scanner:15): GStreamer-WARNING **: Failed to load plugin
   '/usr/lib/x86_64-linux-gnu/gstreamer-1.5/libgstopenh264.so': libopenh264.so.0:
   cannot open shared object file: No such file or directory

Also these conditions apply:

- Packages *openh264-gst-plugins-bad-1.5* and *openh264* are already installed.
- The file */usr/lib/x86_64-linux-gnu/libopenh264.so* is a broken link to the unexisting file */usr/lib/x86_64-linux-gnu/libopenh264.so.0*.

**Reason**: The package *openh264* didn't install correctly. This package is just a wrapper that needs Internet connectivity during its installation stage, to download some binary blob files from this URL: http://ciscobinary.openh264.org/libopenh264-1.4.0-linux64.so.bz2

If the machine is disconnected during the actual installation of this package, the download will fail silently (albeit some error messages will be seen on the standard output).

**Solution**: Ensure that the machine has access to the required URL, and try reinstalling the package:

.. code-block:: bash

   sudo apt-get install --reinstall openh264



Missing video or audio streams
------------------------------

A typical cause for missing streams is that the network topology requires using either STUN or TURN, to overcome the NAT configuration of some intermediate router. If that's the case, the solution is to set up a STUN or a TURN server, and configure its details in the corresponding file, as explained in :ref:`installation-stun-turn`.

There are some logging messages that could indicate a bad configuration of STUN or TURN; these are useful to look for:

.. code-block:: text

   STUN server Port not found in config; using default value: 3478
   STUN server IP address not found in config; NAT traversal requires either STUN or TURN server
   TURN server IP address not found in config; NAT traversal requires either STUN or TURN server

If you see these messages, it's a clear indication that STUN or TURN are not properly configured in KMS.



Application Client
==================

These are some common errors found to affect Kurento client applications.


KMS is not running
------------------

Usually, the Kurento Client library is directed to connect with an instance of KMS that the developer expects will be running in some remote server. If there is no instance of KMS running at the provided URL, the Kurento Client library will raise an exception which **the client application should catch** and handle accordingly.

This is a sample of what the console output will look like, with the logging level set to DEBUG:

.. code-block:: text

   $ mvn -U clean spring-boot:run -Dkms.url=ws://localhost:8888/kurento
   INFO org.kurento.tutorial.player.Application  : Starting Application on TEST with PID 16448
   DEBUG o.kurento.client.internal.KmsUrlLoader  : Executing getKmsUrlLoad(b843d6f6-02dd-49b4-96b6-f2fd2e8b1c8d) in KmsUrlLoader
   DEBUG o.kurento.client.internal.KmsUrlLoader  : Obtaining kmsUrl=ws://localhost:8888/kurento from config file or system property
   DEBUG org.kurento.client.KurentoClient        : Connecting to kms in ws://localhost:8888/kurento
   DEBUG o.k.j.c.JsonRpcClientNettyWebSocket     : Creating JsonRPC NETTY Websocket client
   DEBUG o.kurento.jsonrpc.client.JsonRpcClient  : Enabling heartbeat with an interval of 240000 ms
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket  : [KurentoClient]  Connecting webSocket client to server ws://localhost:8888/kurento
   WARN o.kurento.jsonrpc.client.JsonRpcClient   : [KurentoClient]  Error sending heartbeat to server. Exception: [KurentoClient]  Exception connecting to WebSocket server ws://localhost:8888/kurento
   WARN o.kurento.jsonrpc.client.JsonRpcClient   : [KurentoClient]  Stopping heartbeat and closing client: failure during heartbeat mechanism
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket  : [KurentoClient]  Connecting webSocket client to server ws://localhost:8888/kurento
   DEBUG o.k.jsonrpc.internal.ws.PendingRequests : Sending error to all pending requests
   WARN o.k.j.c.JsonRpcClientNettyWebSocket      : [KurentoClient]  Trying to close a JsonRpcClientNettyWebSocket with channel == null
   WARN ationConfigEmbeddedWebApplicationContext : Exception encountered during context initialization - cancelling refresh attempt: Factory method 'kurentoClient' threw exception; nested exception is org.kurento.commons.exception.KurentoException: Exception connecting to KMS
   ERROR o.s.boot.SpringApplication              : Application startup failed

As opposed to that, the console output for when a connection is successfully done with an instance of KMS should look similar to this sample:

.. code-block:: text

   $ mvn -U clean spring-boot:run -Dkms.url=ws://localhost:8888/kurento
   INFO org.kurento.tutorial.player.Application : Starting Application on TEST with PID 21617
   DEBUG o.kurento.client.internal.KmsUrlLoader : Executing getKmsUrlLoad(af479feb-dc49-4a45-8b1c-eedf8325c482) in KmsUrlLoader
   DEBUG o.kurento.client.internal.KmsUrlLoader : Obtaining kmsUrl=ws://localhost:8888/kurento from config file or system property
   DEBUG org.kurento.client.KurentoClient       : Connecting to kms in ws://localhost:8888/kurento
   DEBUG o.k.j.c.JsonRpcClientNettyWebSocket    : Creating JsonRPC NETTY Websocket client
   DEBUG o.kurento.jsonrpc.client.JsonRpcClient : Enabling heartbeat with an interval of 240000 ms
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket : [KurentoClient]  Connecting webSocket client to server ws://localhost:8888/kurento
   INFO o.k.j.c.JsonRpcClientNettyWebSocket     : [KurentoClient]  Connecting native client
   INFO o.k.j.c.JsonRpcClientNettyWebSocket     : [KurentoClient]  Creating new NioEventLoopGroup
   INFO o.k.j.c.JsonRpcClientNettyWebSocket     : [KurentoClient]  Inititating new Netty channel. Will create new handler too!
   DEBUG o.k.j.c.JsonRpcClientNettyWebSocket    : [KurentoClient]  channel active
   DEBUG o.k.j.c.JsonRpcClientNettyWebSocket    : [KurentoClient]  WebSocket Client connected!
   INFO org.kurento.tutorial.player.Application : Started Application in 1.841 seconds (JVM running for 4.547)



KMS became unresponsive (due to network error or crash)
-------------------------------------------------------

The Kurento Client library is programmed to start a retry-connect process whenever the other side of the RPC channel -ie. the KMS instance- becomes unresponsive. An error exception will raise, which again **the client application should handle**, and then the library will automatically start trying to reconnect with KMS.

This is how this process would look like. In this example, KMS was restarted so the Kurento Client library lost connectivity with KMS for a moment, but then it was able con reconnect and continue working normally:

.. code-block:: text

   INFO org.kurento.tutorial.player.Application  : Started Application in 1.841 seconds (JVM running for 4.547)

   (... Application is running normally at this point)
   (... Now, KMS becomes unresponsive)

   INFO o.k.j.c.JsonRpcClientNettyWebSocket     : [KurentoClient]  channel closed
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket : [KurentoClient]  JsonRpcWsClient disconnected from ws://localhost:8888/kurento because Channel closed.
   DEBUG o.kurento.jsonrpc.client.JsonRpcClient : Disabling heartbeat. Interrupt if running is false
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket : [KurentoClient]  JsonRpcWsClient reconnecting to ws://localhost:8888/kurento.
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket : [KurentoClient]  Connecting webSocket client to server ws://localhost:8888/kurento
   INFO o.k.j.c.JsonRpcClientNettyWebSocket     : [KurentoClient]  Connecting native client
   INFO o.k.j.c.JsonRpcClientNettyWebSocket     : [KurentoClient]  Closing previously existing channel when connecting native client
   DEBUG o.k.j.c.JsonRpcClientNettyWebSocket    : [KurentoClient]  Closing client
   INFO o.k.j.c.JsonRpcClientNettyWebSocket     : [KurentoClient]  Inititating new Netty channel. Will create new handler too!
   WARN o.k.j.c.JsonRpcClientNettyWebSocket     : [KurentoClient]  Trying to close a JsonRpcClientNettyWebSocket with channel == null
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket : TryReconnectingForever=true
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket : TryReconnectingMaxTime=0
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket : maxTimeReconnecting=9223372036854775807
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket : currentTime=1510773733903
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket : Parar de reconectar=false
   WARN o.k.j.c.AbstractJsonRpcClientWebSocket  : [KurentoClient]  Exception trying to reconnect to server ws://localhost:8888/kurento. Retrying in 5000 millis

   org.kurento.jsonrpc.JsonRpcException: [KurentoClient]  Exception connecting to WebSocket server ws://localhost:8888/kurento
      at (...)
   Caused by: io.netty.channel.AbstractChannel$AnnotatedConnectException: Connection refused: localhost/127.0.0.1:8888
      at (...)

   (... Now, KMS becomes responsive again)

   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket : [KurentoClient]  JsonRpcWsClient reconnecting to ws://localhost:8888/kurento.
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket : [KurentoClient]  Connecting webSocket client to server ws://localhost:8888/kurento
   INFO o.k.j.c.JsonRpcClientNettyWebSocket     : [KurentoClient]  Connecting native client
   INFO o.k.j.c.JsonRpcClientNettyWebSocket     : [KurentoClient]  Creating new NioEventLoopGroup
   INFO o.k.j.c.JsonRpcClientNettyWebSocket     : [KurentoClient]  Inititating new Netty channel. Will create new handler too!
   DEBUG o.k.j.c.JsonRpcClientNettyWebSocket    : [KurentoClient]  channel active
   DEBUG o.k.j.c.JsonRpcClientNettyWebSocket    : [KurentoClient]  WebSocket Client connected!
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket : [KurentoClient]  Req-> {"id":2,"method":"connect","jsonrpc":"2.0"}
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket : [KurentoClient]  <-Res {"id":2,"result":{"serverId":"1a3b4912-9f2e-45da-87d3-430fef44720f","sessionId":"f2fd16b7-07f6-44bd-960b-dd1eb84d9952"},"jsonrpc":"2.0"}
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket : [KurentoClient]  Reconnected to the same session in server ws://localhost:8888/kurento

   (... At this point, the Kurento Client is connected again to KMS)



RecorderEndpoint: Zero-size video files
---------------------------------------

If you are trying to generate a video recording, but the resulting file is of size zero (0 KB), keep in mind that **the endpoint will wait until all requested tracks start arriving**.

Quoting from the `Client documentation <https://doc-kurento.readthedocs.io/en/latest/_static/client-javadoc/org/kurento/client/RecorderEndpoint.html>`__:

    It is recommended to start recording only after media arrives, either to the endpoint that is the source of the media connected to the recorder, to the recorder itself, or both. Users may use the MediaFlowIn and MediaFlowOut events, and synchronize the recording with the moment media comes in. In any case, nothing will be stored in the file until the first media packets arrive.

Follow this checklist to see if everything is correctly configured:

- The RecorderEndpoint is configured for both audio and video, but only video (or only audio) is being provided by the application.
- Availability of audio/video devices at recorder client initialization, and just before starting the recording.
- User is disconnecting existing hardware, or maybe connecting new hardware (usb webcams, mic, etc).
- User is clicking "*Deny*" when asked to allow access to microphone/camera by the browser.
- User is sleeping/hibernating the computer, and then possibly waking it up, while recording.
- Check the browser information about the required media tracks, e.g. ``track.readyState``.
- Track user agents, ICE candidates, etc.



"Expects at least 4 fields"
---------------------------

This message can manifest in multiple variations of what is essentially the same error:

.. code-block:: text

   DOMException: Failed to parse SessionDescription: m=video 0 UDP/TLS/RTP/SAVPF Expects at least 4 fields

   OperationError (DOM Exception 34): Expects at least 4 fields

The reason for this is that Kurento hasn't enabled support for the video codec H.264, but it needs to communicate with another peer which only supports H.264, such as the Safari browser. Thus, the SDP Offer/Answer negotiation rejects usage of the corresponding media stream, which is what is meant by ``m=video 0``.

The solution is to ensure that both peers are able to find a match in their supported codecs. To enable H.264 support in Kurento, check these points:

- The package *openh264-gst-plugins-bad-1.5* must be installed in the system.
- The package *openh264* must be **correctly** installed. Specifically, the post-install script of this package requires Internet connectivity, because it downloads a codec binary blob from the Cisco servers. See :ref:`troubleshooting-h264`.
- The H.264 codec must be enabled in the corresponding Kurento settings file: */etc/kurento/modules/kurento/SdpEndpoint.conf.json*.
  Ensure that the entry corresponding to this codec does exist and is not commented out. For example:

  .. code-block:: js

     "videoCodecs": [
       { "name": "VP8/90000" },
       { "name": "H264/90000" }
     ]



Browser
=======

Safari doesn't work
-------------------

Apple Safari is a browser that follows some policies that are much more restrictive than those of other common browsers such as Google Chrome or Mozilla Firefox.

For some tips about how to ensure the best compatibility with Safari, check :doc:`/knowledge/safari`.
