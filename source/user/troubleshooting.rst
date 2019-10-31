======================
Troubleshooting Issues
======================

If you are facing an issue with Kurento Media Server, follow this basic check list:

* Step 1. Test with the **latest version** of Kurento Media Server: **|VERSION_KMS|**. Follow the installation instructions here: :doc:`/user/installation`.

* Step 2: If the problem still happens in the latest version, and the Kurento developers have started working on a solution, they might instruct you to test with the latest (unreleased) changes by installing a nightly version of KMS: :doc:`/user/installation_dev`.

* Step 3: When your issue exists in both the latest and nightly versions, try resorting to the :ref:`Open-Source Community <support-community>`. Kurento users might be having the same issue and maybe you can find help in there.

* Step 4: If you want full attention from the Kurento team, get in contact with us to request :ref:`Commercial Support <support-commercial>`.



**My Kurento doesn't work, what should I do?**

This document outlines several bits of knowledge that can prove very useful when studying a failure or error in KMS:

.. contents:: Table of Contents



.. _troubleshooting-crashes:

Media Server Crashes
====================

We want Kurento to be as stable as possible! When you notice a server crash, it's a good time to report a bug so we can know about the issue. But before that, we need you to make sure that you are running the **latest version** of Kurento Media Server: **|VERSION_KMS|**, and then collect some information about the crash:

* Kurento tries to write an **execution stack trace** in the file ``/var/log/kurento-media-server/errors.log``. This stack trace will only be useful if you have **all debug packages installed**, by following these instructions: :ref:`dev-dbg`. If that's the case, open the *errors.log* file and look for a line similar to this one:

  .. code-block:: text

     2019-09-19T13:44:48+02:00 -- New execution

  Then, see if you can find the stack trace that matches with the time when the crash occured. Attach that stack trace to your bug report.

* If you installed Kurento with ``apt-get install``, then the Ubuntu system generates a **crash report** that you will find in ``/var/crash/_usr_bin_kurento-media-server.<PID>.crash``. This contains information that can be used to inspect KMS with a debugger, so it tends to be very useful. Attach it to your bug report (or upload it to any hosting provider).

  .. note::

     The crash report file does not get overwritten on each crash. **If an old crash report exists, the new one will not be generated**. So if you are experiencing crashes, then make sure that the crash report file is always deleted, after having shared it with us, so future crashes will also generate new crash reports.

* We might ask you to run with a special build of Kurento that comes with support for `AddressSanitizer <https://github.com/google/sanitizers/wiki/AddressSanitizer>`__, a memory access error detector.

  For this, you would need to make several high-impact changes in your system, so instead of breaking everybody's setup we decided it's better to just publish a Docker image that contains everything you need: `Kurento Docker images with AddressSanitizer <https://hub.docker.com/r/kurento/kurento-media-server-dev/tags?name=asan>`__. If we ask for it, you would have to provide the `Docker logs <https://docs.docker.com/engine/reference/commandline/logs/>`__ from running this.

  For this reason (and also for better test repeatability), it's a very good idea that you have your services thought out in a way that it's possible to **run Kurento Media Server from Docker**, at any time, regardless of what is your normal / usual method of deploying Kurento.



Other Media Server issues
=========================

``GStreamer-CRITICAL **`` messages in the log
---------------------------------------------

GLib and GStreamer use a lot of ``assert()`` functions to check for valid conditions whenever a function is called. If these conditions fail, messages such as these ones will appear in the log:

.. code-block:: text

   (kurento-media-server:4619): GStreamer-CRITICAL **: gst_element_query: assertion 'GST_IS_ELEMENT (element)' failed

.. code-block:: text

   (kurento-media-server:15636): GLib-CRITICAL **: g_error_free: assertion 'error != NULL' failed

However, these messages don't cause a crash in the server; instead, it will keep working, although there will be some session that is wrongly affected by this issue.

Finding the spot where the ``assert()`` fails is a bit hard, though; you need to:

1) Install debugging symbols: :ref:`dev-dbg`.

2) Enable debug breaks in the asserts:

   .. code-block:: bash

      export G_DEBUG=fatal-warnings

3) Enable kernel core dumps:

   .. code-block:: bash

      ulimit -c unlimited

4) Run with GDB and get a backtrace:

   .. code-block:: bash

      gdb /usr/bin/kurento-media-server
      (gdb) run
      # Wait until the assert happens and GDB breaks
      (gdb) info stack
      (gdb) backtrace



CPU usage grows too high
------------------------

Kurento Media Pipelines can get pretty complex if your use case requires so, which would mean more processing power is required to run them; however, even for the simplest cases it's possible that you find out unexpected spikes in CPU usage, which in extreme cases could end up crashing the server due to resource exhaustion in the machine.

Check these points in an attempt to find possible causes for the high CPU usage:

* Kurento Media Server is known to work well with videos of up to **720p** resolution (1280x720) at **30fps** and around **2Mbps**. Using values beyond those might work fine, but the Kurento team hasn't done any factual analysis to prove it. With heavier data loads there is a chance that KMS will be unable to process all incoming data on time, and this will cause that buffers fill up and frames get dropped. Try reducing the resolution of your input videos if you see video stuttering.

* Source and destination video codecs must be compatible. This has always been a source of performance problems in WebRTC communications.

  - For example, if some participants are using Firefox and talking in a room, they will probably negotiate **VP8** codec with Kurento; then later someone enters with Safari, CPU usage explodes due to transcoding is now suddenly required, because Safari only supports **H.264** (VP8 support was added only since Desktop Safari v68).
  - Another example is you have some VP8 streams running nicely but then stream recording is enabled with the **MP4** recording profile, which uses H.264. Same story: video needs to be converted, and that uses a lot of CPU.

* Also check if other processes are running in the same machine and using the CPU. For example, if Coturn is running and using a lot of resources because too many users end up connecting via Relay (TURN).

Of these, video transcoding is the main user of CPU cycles, because encoding video is a computationally expensive operation. As mentioned earlier, keep an eye on the *TRANSCODING* events sent from Kurento to your Application Server, or alternatively look for ``TRANSCODING ACTIVE`` messages in the media server logs.

If you see that transcoding is active at some point, you may get a bit more information about why, by enabling this line:

.. code-block:: bash

   export GST_DEBUG="${GST_DEBUG:-3},Kurento*:5,agnosticbin*:5"

in your daemon settings file, ``/etc/default/kurento-media-server``.

Then look for these messages in the media server log output:

* ``Upstream provided caps: (caps)``
* ``Downstream wanted caps: (caps)``
* ``Find TreeBin with wanted caps: (caps)``

Which will end up with either of these sets of messages:

* If source codec is compatible with destination:

  - ``TreeBin found! Use it for (audio|video)``
  - ``TRANSCODING INACTIVE for (audio|video)``

* If source codec is **not** compatible with destination:

  - ``TreeBin not found! Transcoding required for (audio|video)``
  - ``TRANSCODING ACTIVE for (audio|video)``

These messages can help understand what codec settings are being received by Kurento ("*Upstream provided caps*") and what is being expected at the other side by the stream receiver ("*Downstream wanted caps*").



Memory usage grows too high
---------------------------

If you are trying to establish whether Kurento Media Server has a memory leak, then neither ``top`` nor ``ps`` are the right tool for the job; **Valgrind** is.

If you are using *top* or *ps* to evaluate memory usage, keep in mind that these tools show memory usage *as seen by the Operating System*, not by the process of the media server. Even after freeing memory, there is no guarantee that the memory will get returned to the Operating System. Typically, it won't! Memory allocator implementations do not return ``free``'d memory : it is available for use by the same program, but not by others. So *top* or *ps* won't be able to "see" the free'd memory.

See: `free() in C doesn't reduce memory usage <https://stackoverflow.com/questions/6005333/problem-with-free-on-structs-in-c-it-doesnt-reduce-memory-usage>`__

To run Kurento Media Server with Valgrind and find memory leaks, the process is just a matter of following the steps outlined in :ref:`dev-sources`, but instead of

.. code-block:: text

   ./bin/kms-build-run.sh

you'll want to do

.. code-block:: text

   ./bin/kms-build-run.sh --valgrind-memcheck

Also, please have a look at the information shown in :ref:`troubleshooting-crashes` about our special Docker image based on **AddressSanitizer**. Running Kurento with this image might help finding memory-related issues.



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
- The file */usr/lib/x86_64-linux-gnu/libopenh264.so* is a broken link to the non-existing file */usr/lib/x86_64-linux-gnu/libopenh264.so.0*.

**Reason**: The package *openh264* didn't install correctly. This package is just a wrapper that needs Internet connectivity during its installation stage, to download a binary blob file from this URL: http://ciscobinary.openh264.org/libopenh264-1.4.0-linux64.so.bz2

If the machine is disconnected during the actual installation of this package, the download will fail silently with some error messages printed on the standard output, but the installation will succeed.

**Solution**: Ensure that the machine has access to the required URL, and try reinstalling the package:

.. code-block:: bash

   sudo apt-get update && sudo apt-get install --reinstall openh264



Missing audio or video streams
------------------------------

If the Kurento Tutorials are showing an spinner, or your application is missing media streams, that's a strong indication that the network topology requires using either a STUN or TURN server, to traverse through the NAT firewall of intermediate routers. Check :ref:`installation-stun-turn`.

There are some KMS log messages that could indicate a bad configuration of STUN or TURN; these are useful to look for:

.. code-block:: text

   STUN server Port not found in config; using default value: 3478
   STUN server IP address not found in config; NAT traversal requires either STUN or TURN server
   TURN server IP address not found in config; NAT traversal requires either STUN or TURN server

If you see these messages, it's a clear indication that STUN or TURN are not properly configured in KMS.



Low video quality
-----------------

You have several ways to override the default settings for variable bitrate:

- Methods in `org.kurento.client.BaseRtpEndpoint <https://doc-kurento.readthedocs.io/en/stable/_static/client-javadoc/org/kurento/client/BaseRtpEndpoint.html>`__:

  - *setMinVideoRecvBandwidth()* / *setMaxVideoRecvBandwidth()*
  - *setMinVideoSendBandwidth()* / *setMaxVideoSendBandwidth()*

- Methods in `org.kurento.client.MediaElement <https://doc-kurento.readthedocs.io/en/stable/_static/client-javadoc/org/kurento/client/MediaElement.html>`__:

  - *setMinOutputBitrate()* / *setMaxOutputBitrate()*

    This setting is also configurable in */etc/kurento/modules/kurento/MediaElement.conf.ini*



Application Server
==================

These are some common errors found to affect Kurento Application Servers:



KMS is not running
------------------

Usually, the Kurento Client library is directed to connect with an instance of KMS that the developer expects will be running in some remote server. If there is no instance of KMS running at the provided URL, the Kurento Client library will raise an exception which **the Application Server should catch** and handle accordingly.

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
   INFO o.k.j.c.JsonRpcClientNettyWebSocket     : [KurentoClient]  Initiating new Netty channel. Will create new handler too!
   DEBUG o.k.j.c.JsonRpcClientNettyWebSocket    : [KurentoClient]  channel active
   DEBUG o.k.j.c.JsonRpcClientNettyWebSocket    : [KurentoClient]  WebSocket Client connected!
   INFO org.kurento.tutorial.player.Application : Started Application in 1.841 seconds (JVM running for 4.547)



KMS became unresponsive (due to network error or crash)
-------------------------------------------------------

The Kurento Client library is programmed to start a retry-connect process whenever the other side of the RPC channel -ie. the KMS instance- becomes unresponsive. An error exception will raise, which again **the Application Server should handle**, and then the library will automatically start trying to reconnect with KMS.

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
   INFO o.k.j.c.JsonRpcClientNettyWebSocket     : [KurentoClient]  Initiating new Netty channel. Will create new handler too!
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
   INFO o.k.j.c.JsonRpcClientNettyWebSocket     : [KurentoClient]  Initiating new Netty channel. Will create new handler too!
   DEBUG o.k.j.c.JsonRpcClientNettyWebSocket    : [KurentoClient]  channel active
   DEBUG o.k.j.c.JsonRpcClientNettyWebSocket    : [KurentoClient]  WebSocket Client connected!
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket : [KurentoClient]  Req-> {"id":2,"method":"connect","jsonrpc":"2.0"}
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket : [KurentoClient]  <-Res {"id":2,"result":{"serverId":"1a3b4912-9f2e-45da-87d3-430fef44720f","sessionId":"f2fd16b7-07f6-44bd-960b-dd1eb84d9952"},"jsonrpc":"2.0"}
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket : [KurentoClient]  Reconnected to the same session in server ws://localhost:8888/kurento

   (... At this point, the Kurento Client is connected again to KMS)



Node.js / NPM failures
----------------------

Kurento Client does not currently support Node v10 (LTS), you will have to use Node v8 or below.



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



Networking issues
=================

WebRTC connection is not established
------------------------------------

There is a multitude of possible reasons for a failed WebRTC connection, so you can start by following this checklist:

- Deploy a properly configured STUN or TURN server. Coturn tends to work fine for this, and Kurento has some documentation about how to install and configure it: https://doc-kurento.readthedocs.io/en/latest/user/faq.html#install-coturn-turn-stun-server

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

- Check that any SDP mangling you (or any of your third-party libraries) might be doing in your Application Server is being done correctly.

  This is one of the most hard to catch examples we've seen in our `mailing list <https://groups.google.com/d/topic/kurento/t25_QQSc_Bo/discussion>`__:

      > The problem was that our Socket.IO client did not correctly *URL-Encode* its JSON payload when *xhr-polling*, which resulted in all "plus" signs ('+') being changed into spaces (' ') on the server. This meant that the ``ufrag`` in the client's SDP was invalid if it contained a plus sign! Only some of the connections failed because not all ``ufrag``s contain plus signs.



mDNS ICE candidate fails: Name or service not known
---------------------------------------------------

**Problem**:

When the browser conceals the local IP address behing an mDNS candidate, these errors appear in Kurento logs:

.. code-block:: text

   kmsicecandidate  [...] Error code 0: 'Error resolving '2da1b2bb-a601-44e8-b672-dc70e3493bc4.local': Name or service not known'
   kmsiceniceagent  [...] Cannot parse remote candidate: 'candidate:2382557538 1 udp 2113937151 2da1b2bb-a601-44e8-b672-dc70e3493bc4.local 50635 typ host generation 0 ufrag /Og/ network-cost 999'
   kmswebrtcsession [...] Adding remote candidate to ICE Agent: Agent failed, stream_id: '1'

**Solution**:

mDNS name resolution must be enabled in the system. Check out the contents of */etc/nsswitch.conf*, you should see something similar to this:

.. code-block:: text

   hosts: files mdns4_minimal [NOTFOUND=return] dns

If not, try fully reinstalling the package *libnss-mdns*:

.. code-block:: sh

   sudo apt-get purge --yes libnss-mdns
   sudo apt-get update
   sudo apt-get install --yes libnss-mdns

Installing this package does automatically edit the config file in an appropriate way. Now the *mdns4_minimal* module should appear listed in the hosts line.

**Caveat**: **mDNS does not work from within Docker**

See `mDNS and Crossbar.io Fabric (Docker) #21 <https://github.com/crossbario/crossbar-fabric-public/issues/21>`__:

    Docker does not play well with mDNS/zeroconf/Bonjour: resolving ``.local`` hostnames from inside containers does not work (easily).
    [...]
    The reasons run deep into how Docker configures DNS *inside* a container.

So if you are running a Docker image, ``.local`` names won't be correctly resolved even if you install the required packages. This happens with Kurento or whatever other software; it seems to be a Docker configuration problem / bug.



ICE fails with timeouts
-----------------------

**Problem**:

- You have configured a STUN/TURN server in a different machine than Kurento Media Server.
- The ICE connection tests fail due to timeout on trying pairs.

**Solution**:

Make sure that all required UDP ports for media content are open on the sever; otherwise, not only the ICE process will fail, but also the video or audio streams themselves won't be able to reach either server.



Multicast fails in Docker
-------------------------

**Problem**:

- Your Kurento Media Server is running in a Docker container.
- MULTICAST streams playback fail with an error such as this one:

  .. code-block:: text

     DEBUG rtspsrc gstrtspsrc.c:7553:gst_rtspsrc_handle_message:<source> timeout on UDP port

  Note that in this example, to see this message you would need to enable ``DEBUG`` log level for the ``rtspsrc`` category; see :ref:`logging-levels`.

**Solution**:

For Multicast streaming to work properly, you need to disable Docker's network namespacing and use ``--net host``. Note that this gives the container direct access to the host interfaces, and you'll need to connect through published ports to access others containers.

This is a limitation of Docker; you can follow the current status with this issue: https://github.com/moby/moby/issues/23659

If using Docker Compose, use ``network_mode: host`` such as this:

.. code-block:: text

   version: "3.7"
   services:
     kms:
       image: kurento/kurento-media-server:6.9.0
       container_name: kms
       restart: always
       network_mode: host
       environment:
         - GST_DEBUG=2,Kurento*:5

References:

- https://github.com/Kurento/bugtracker/issues/349
- https://stackoverflow.com/questions/51737969/how-to-support-multicast-network-in-docker



Element-specific info
=====================

PlayerEndpoint
--------------

RTSP broken video
~~~~~~~~~~~~~~~~~

Some users have reported huge macro-blocks or straight out broken video frames when using a PlayerEndpoint to receive an RTSP stream containing H.264 video. A possible solution to fix this issue is to fine-tune the PlayerEndpoint's **networkCache** parameter. It basically sets the buffer size (in milliseconds) that the underlying GStreamer decoding element will use to cache the stream.

There's no science for that parameter, though. The perfect value depends on your network topology and efficiency, so you should proceed in a trial-and-error approach. For some situations, values lower than **100ms** have worked fine; some users have reported that 10ms was required to make their specific camera work, others have seen good results with setting this parameter to **0ms**.



RTSP Video stuttering
~~~~~~~~~~~~~~~~~~~~~

The GStreamer element in charge of RTSP reception is `rtspsrc <https://gstreamer.freedesktop.org/data/doc/gstreamer/head/gst-plugins-good/html/gst-plugins-good-plugins-rtspsrc.html>`__, and this element contains an `rtpjitterbuffer <https://gstreamer.freedesktop.org/data/doc/gstreamer/head/gst-plugins-good/html/gst-plugins-good-plugins-rtpjitterbuffer.html>`__.

This jitter buffer gets full when network packets arrive faster than what Kurento is able to process. If this happens, then PlayerEndpoint will start dropping packets, which will show up as video stuttering on the output streams, while triggering a warning in Kurento logs:

.. code-block:: text

   WARNING  kmsutils  discont_detection_probe() <kmsagnosticbin0:sink>  Stream discontinuity detected on non-keyframe

You can check if this problem is affecting you by running with DEBUG :ref:`logging level <logging-levels>` enabled for the *rtpjitterbuffer* component, and searching for a specific message:

.. code-block:: bash

   export GST_DEBUG="${GST_DEBUG:-3},rtpjitterbuffer:5"
   /usr/bin/kurento-media-server 2>&1 | grep -P 'rtpjitterbuffer.*(Received packet|Queue full)'

With this command, a new line will get printed for each single *Received packet*, plus an extra line will appear informing about *Queue full* whenever a packet is dropped.

There is not much you can fine tune in KMS to solve this problem; the most practical solution is to reduce the amount of data, mostly by decreasing either video resolution or video bitrate.

Kurento Media Server is known to work well receiving videos of up to **720p** resolution (1280x720) at **30fps** and around **2Mbps**. If you are using values beyond those, there is a chance that KMS will be unable to process all incoming data on time, and this will cause that buffers fill up and frames get dropped. Try reducing the resolution of your input videos to see if this helps solving the issue.



RecorderEndpoint
----------------

Zero-size video files
~~~~~~~~~~~~~~~~~~~~~

If you are trying to generate a video recording, keep in mind that **the endpoint will wait until all tracks (audio, video) start arriving**.

.. ifconfig:: "|VERSION_RELEASE|" == "true"

   Quoting from the `Client documentation <https://doc-kurento.readthedocs.io/en/|VERSION_DOC|/_static/client-javadoc/org/kurento/client/RecorderEndpoint.html>`__:

.. ifconfig:: "|VERSION_RELEASE|" != "true"

   Quoting from the `Client documentation <https://doc-kurento.readthedocs.io/en/latest/_static/client-javadoc/org/kurento/client/RecorderEndpoint.html>`__:

    It is recommended to start recording only after media arrives, either to the endpoint that is the source of the media connected to the recorder, to the recorder itself, or both. Users may use the MediaFlowIn and MediaFlowOut events, and synchronize the recording with the moment media comes in. In any case, nothing will be stored in the file until the first media packets arrive.

Follow this checklist to see if any of these problems is preventing the RecorderEndpoint from working correctly:

- The RecorderEndpoint is configured for both audio and video, but only video (or only audio) is being provided by the application.
- Availability of audio/video devices at recorder client initialization, and just before starting the recording.
- User is disconnecting existing hardware, or maybe connecting new hardware (usb webcams, mic, etc).
- User is clicking "*Deny*" when asked to allow access to microphone/camera by the browser.
- User is sleeping/hibernating the computer, and then possibly waking it up, while recording.
- Check the browser information about the required media tracks, e.g. ``track.readyState``.
- Track user agents, ICE candidates, etc.



Browser
=======

Safari doesn't work
-------------------

Apple Safari is a browser that follows some policies that are much more restrictive than those of other common browsers such as Google Chrome or Mozilla Firefox.

For some tips about how to ensure the best compatibility with Safari, check :doc:`/knowledge/safari`.
