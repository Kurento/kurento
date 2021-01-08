======================
Troubleshooting Issues
======================

If you are facing an issue with Kurento Media Server, follow this basic check list:

* **Step 1**: Test with the **latest version** of Kurento Media Server: **|VERSION_KMS|**. Follow the installation instructions here: :doc:`/user/installation`.

* **Step 2**: Test with the latest (unreleased) changes by installing a nightly version: :doc:`/user/installation_dev`.

* **Step 3**: Search for your issue in our `GitHub bugtracker <https://github.com/Kurento/bugtracker/issues>`__ and the `Kurento Public Mailing List <https://groups.google.com/forum/#!forum/kurento>`__.

* **Step 4**: If you want full attention from the Kurento team, get in contact with us to request :ref:`Commercial Support <support-commercial>`.

For more information about how to request support, and how to submit bug reports and commercial enquiries, have a look at the :doc:`/user/support` page.



**My Kurento Media Server doesn't work, what should I do?**

This document outlines several bits of knowledge that can prove very useful when studying a failure or error in KMS:

.. contents:: Table of Contents



.. _troubleshooting-crashes:

Media Server Crashes
====================

We want Kurento to be as stable as possible! When you notice a server crash, it's a good time to report a bug so we can know about the issue. But before that, you need to check a couple things:

1. Make sure that you are running the **latest version** of Kurento Media Server: **|VERSION_KMS|**.
2. Have :ref:`debug symbols installed <dev-dbg>`. Otherwise, *your bug report won't be useful*.

Then, please provide us with information about the crash:

* Kurento tries to write an **execution stack trace** in the file ``/var/log/kurento-media-server/errors.log``. Open the *errors.log* file and look for a line similar to this one:

  .. code-block:: text

     2019-09-19T13:44:48+02:00 -- New execution

  Then, see if you can find the stack trace that matches with the time when the crash occurred. Attach that stack trace to your bug report.

* If you installed Kurento with :ref:`apt-get install <installation-local>`, *and* `Apport <https://wiki.ubuntu.com/Apport>`__ is installed, then Ubuntu generates a **crash report** that you will find in ``/var/crash/_usr_bin_kurento-media-server.<PID>.crash``. This contains information that can be used to inspect KMS with a debugger, so it can be very useful to find the cause of the crash. Upload it somewhere, or attach it to your bug report.

  .. note::

     The *.crash* report file **is already compressed**, so you can go ahead and upload it to some file transfer service to share it with us.

  .. note::

     The *.crash* report file **must be deleted** afterwards. *If an old crash report exists, new ones will not be generated*. So if you are experiencing crashes, make sure that the crash report file is always deleted after having shared it with us, so future crashes will also generate new report files.

* Otherwise, you can manually enable the generation of a **core dump** whenever KMS crashes. For this, edit the file ``/etc/default/kurento-media-server`` and uncomment the setting *DAEMON_CORE_PATTERN*, which by default will tell the Linux Kernel to generate core dumps in ``/tmp/``.

  .. note::

     The *core dump* file **is NOT compressed**, so before uploading you should compress it, for a typically huge file size reduction, before uploading it to some file transfer service and sharing it with us.

* As a last resort, if no crash report can be obtained by any means, you may need to run KMS with a debugger. To do so, please follow the instructions here :ref:`dev-gdb`, to get a **backtrace** when the crash happens.

* Finally, if a developer suspects that the crash might be due to a memory corruption error, we could ask you to run with a special build of Kurento that comes bundled with support for `AddressSanitizer <https://github.com/google/sanitizers/wiki/AddressSanitizer>`__, a memory access error detector.

  To do this, you'll need to run a `Kurento Docker image with AddressSanitizer <https://hub.docker.com/r/kurento/kurento-media-server-dev/tags?name=asan>`__. If we ask for it, you would have to provide the `Docker logs <https://docs.docker.com/engine/reference/commandline/logs/>`__ from running this image.

  For this reason (and also for better test repeatability), it's a very good idea that you have your services planned in a way that it's possible to **run Kurento Media Server from Docker**, at any time, regardless of what is your normal / usual method of deploying Kurento.



Other Media Server issues
=========================

Reached limit / Resource temporarily unavailable
------------------------------------------------

If the server is malfunctioning or crashing and you can see a message similar to this one in the logs:

.. code-block:: text

   Reached KMS files limit: 819 (system max: 1024)

or

.. code-block:: text

   Error creating thread: Resource temporarily unavailable

then KMS is hitting resource limits imposed by the Kernel. The 2 most common reasons for this issue are:

1. You might have a custom service or Kurento plugin that is acquiring resources without releasing them afterwards. You should profile and debug your code to make sure that it is not leaking resources (such as open file descriptors, threads, sockets, etc) and exhausting the limits enforced by the Linux Kernel.

2. Congratulations! Your service is growing, time to deal with resource & concurrency issues.

   Check the running stats of your operating system, to see if the KMS process is being limited to the default 1024 file/device handles (*ulimit*), and increase that number.

   For local installations (:ref:`with apt-get install <installation-local>`), you can edit the file ``/etc/default/kurento-media-server`` to raise either of the *DAEMON_MAX_FILES* and/or *DAEMON_MAX_THREADS* and see if this solves the issue. For other installation methods such as Docker, you will need to use tool-specific mechanisms to change resource limits, like running with ``docker run --ulimit``.

   If **systemd** is configured, it might also be applying its own limit on process resources; for example you can check how many threads are being used by Kurento and what is the maximum, with these commands:

   .. code-block:: shell

      systemctl status kurento-media-server | grep Tasks
      systemctl show kurento-media-server | grep TasksMax

   In *systemd* it is possible to configure limits with parameters such as *DefaultTasksMax* in either ``/etc/systemd/system.conf`` (for the default system instance) or ``/etc/systemd/user.conf`` (for the custom user instance, if you have configured one in your machine). The current effective value of *DefaultTasksMax* can be queried with this command:

   .. code-block:: shell

      systemctl show --property DefaultTasksMax

   If left to its default setting, *DefaultTasksMax* will be 15% of the absolute maximum limit from the Kernel, that you can get or set through the file ``/proc/sys/kernel/pid_max``. If you change that, don't forget to run ``systemctl daemon-reexec`` to have *systemd* load the new value.

   .. note::

      You need to use ``systemctl daemon-reexec`` because ``systemctl daemon-reload`` `has a bug <https://github.com/systemd/systemd/issues/13419#issuecomment-527915700>`__ in *systemd* <= v242 (so this affects Ubuntu versions earlier than 20.04 "Focal").



``GStreamer-CRITICAL`` messages in the log
------------------------------------------

GLib and GStreamer are libraries that use a lot of internal ``assert()`` functions to check for valid conditions whenever a function is called. Normally, these are meant to catch programming bugs in their own source code; when (if) any of these checks fail, a warning message is printed to the logs. The media server won't be brought down in this situation, but a bug in any of Kurento's underlying 3rd-party libraries will have an adverse effect on Kurento itself sooner or later. So, it's in our best interest to watch out for these. Report them to us if you see any! ;-)

Here are a couple examples of such messages:

.. code-block:: text

   (kurento-media-server:4619): GStreamer-CRITICAL **: gst_element_query: assertion 'GST_IS_ELEMENT (element)' failed

.. code-block:: text

   (kurento-media-server:15636): GLib-CRITICAL **: g_error_free: assertion 'error != NULL' failed

The problem of these messages is that they don't really provide much information about *how* the error happens, of *where*. To find out, we'll need you to run KMS under a debug session. Please, follow the instructions here :ref:`dev-gdb`, to get a **backtrace** from the *GStreamer-CRITICAL* error.



CPU usage grows too high
------------------------

Kurento Media Pipelines can get pretty complex if your use case requires so, which would mean more processing power is required to run them; however, even for the simplest cases it's possible that you find out unexpected spikes in CPU usage, which in extreme cases could end up crashing the server due to resource exhaustion in the machine.

Check these points in an attempt to find possible causes for the high CPU usage:

* Kurento Media Server is known to work well with videos of up to **720p** resolution (1280x720) at **30fps** and around **2Mbps**. Using values beyond those might work fine, but the Kurento team hasn't done any factual analysis to prove it. With heavier data loads there is a chance that KMS will be unable to process all incoming data on time, and this will cause that buffers fill up and frames get dropped. Try reducing the resolution of your input videos if you see video stuttering.

* Source and destination video codecs must be compatible. This has always been a source of performance problems in WebRTC communications.

  - For example, if some participants are using Firefox and talking in a room, they will probably negotiate **VP8** codec with Kurento; then later someone enters with Safari, CPU usage explodes due to transcoding is now suddenly required, because Safari only supports **H.264** (VP8 support was added only since Desktop Safari v68).
  - Another example is you have some VP8 streams running nicely but then stream recording is enabled with the **MP4** recording profile, which uses H.264. Same story: video needs to be converted, and that uses a lot of CPU.

* Also check if other processes are running in the same machine and using the CPU. For example, if Coturn is running and using a lot of resources because too many users end up connecting via Relay (TURN).

Of these, video transcoding is the main user of CPU cycles, because encoding video is a computationally expensive operation. As mentioned earlier, keep an eye on the *TRANSCODING* events sent from Kurento to your Application Server, or alternatively look for *TRANSCODING ACTIVE* messages in the media server logs.

If you see that transcoding is active at some point, you may get a bit more information about why, by enabling this line:

.. code-block:: shell

   export GST_DEBUG="${GST_DEBUG:-2},Kurento*:5,agnosticbin*:5"

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

If you are trying to establish whether Kurento Media Server has a memory leak, then neither *top* nor *ps* are the right tool for the job; **Valgrind** is.

If you are using *top* or *ps* to evaluate memory usage, keep in mind that these tools show memory usage *as seen by the Operating System*, not by the process of the media server. Even after freeing memory, there is no guarantee that the memory will get returned to the Operating System. Typically, it won't! Memory allocator implementations do not return *free*'d memory : it is available for use by the same program, but not by others. So *top* or *ps* won't be able to "see" the free'd memory.

See: `free() in C doesn't reduce memory usage <https://stackoverflow.com/questions/6005333/problem-with-free-on-structs-in-c-it-doesnt-reduce-memory-usage>`__

To run Kurento Media Server with Valgrind and find memory leaks, the process is just a matter of following the steps outlined in :ref:`dev-sources`, but instead of

.. code-block:: shell

   ./bin/kms-build-run.sh

you'll want to do

.. code-block:: shell

   ./bin/kms-build-run.sh --valgrind-memcheck

Also, please have a look at the information shown in :ref:`troubleshooting-crashes` about our special Docker image based on **AddressSanitizer**. Running Kurento with this image might help finding memory-related issues.



Service init doesn't work
-------------------------

The package *kurento-media-server* provides a service file that integrates with the Ubuntu init system. This service file loads its user configuration from ``/etc/default/kurento-media-server``, where the user is able to configure several features as needed.

In Ubuntu, log messages from init scripts are managed by *systemd*, and can be checked in to ways:

- ``/var/log/syslog`` contains a copy of all init service messages.
  You can open it to see past messages, or follow it in real time with this command:

  .. code-block:: shell

     tail -f /var/log/syslog

- You can query the status of the *kurento-media-server* service with this command:

  .. code-block:: shell

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
- The file ``/usr/lib/x86_64-linux-gnu/libopenh264.so`` is a broken link to the non-existing file ``/usr/lib/x86_64-linux-gnu/libopenh264.so.0``.

**Reason**: The package *openh264* didn't install correctly. This package is just a wrapper that needs Internet connectivity during its installation stage, to download a binary blob file from this URL: http://ciscobinary.openh264.org/libopenh264-1.4.0-linux64.so.bz2

If the machine is disconnected during the actual installation of this package, the download will fail silently with some error messages printed on the standard output, but the installation will succeed.

**Solution**: Ensure that the machine has access to the required URL, and try reinstalling the package:

.. code-block:: shell

   sudo apt-get update && sudo apt-get install --reinstall openh264



Missing audio or video streams
------------------------------

If the Kurento Tutorials are showing an spinner, or your application is missing media streams, that's a strong indication that the network topology requires using either a :term:`STUN` server or a :term:`TURN` relay, to traverse through the :term:`NAT` of intermediate routers. Check the section about :ref:`installing a STUN/TURN server <faq-coturn-install>`.



.. _troubleshooting-low-quality:

Low video quality
-----------------

You have several ways to override the default settings for variable bitrate and network bandwidth detection:

- Methods in `org.kurento.client.BaseRtpEndpoint <../_static/client-javadoc/org/kurento/client/BaseRtpEndpoint.html>`__:

  - *setMinVideoRecvBandwidth()* / *setMaxVideoRecvBandwidth()*
  - *setMinVideoSendBandwidth()* / *setMaxVideoSendBandwidth()*

- Methods in `org.kurento.client.MediaElement <../_static/client-javadoc/org/kurento/client/MediaElement.html>`__:

  - *setMinOutputBitrate()* / *setMaxOutputBitrate()*

    This setting is also configurable in ``/etc/kurento/modules/kurento/MediaElement.conf.ini``.

Also, note that web browsers will adapt their output video quality according to what they detect is the network quality. Most browsers will adapt the **video bitrate**; in addition, Chrome also adapts the **video resolution**.

Browsers offer internal stats through a special web address that you can use to verify what is being sent. For example, to check the outbound stats in Chrome:

#. Open this URL: chrome://webrtc-internals/
#. Look for the stat name "*Stats graphs for RTCOutboundRTPVideoStream (outbound-rtp)*".
#. You will find the effective output video bitrate in ``[bytesSent_in_bits/s]``, and the output resolution in ``frameWidth`` and ``frameHeight``.

You can also check what is the network quality estimation in Chrome:

#. Look for the stat name "*Stats graphs for RTCIceCandidatePair (candidate-pair)*". Note that there might be several of these, but only one will be active.
#. Find the output network bandwidth estimation in ``availableOutgoingBitrate``. Chrome will try to slowly increase its output bitrate, until it reaches this estimation.



Video has green artifacts
-------------------------

This is typically caused by missing information in the video decoder, most probably due to a high packet loss rate in the network.

The *H.264* and `VP8 <https://tools.ietf.org/html/rfc6386#section-9.2>`__ video codecs use a color encoding system called `YCbCr <https://en.wikipedia.org/wiki/YCbCr>`__ (sometimes also written as *YCrCb*), which the decoder has to convert into the well known `RGB <https://en.wikipedia.org/wiki/RGB_color_model>`__ ("*Red-Green-Blue*") model that is used by computer screens. When there is data loss, the decoder will assume that all missing values are *0* (zero). It just turns out that a YCbCr value of *(0,0,0)* is equivalent to the **green** color in RGB.

When this problem happens, Kurento sends retransmission requests to the source of the RTP stream. However, in cases of heavy packet loss, there isn't much else that can be done and enough losses will build up until the video decoding gets negatively affected. In situations like this, the most effective change you can do is to reduce the video resolution and/or quality at the sender.

Cisco has too a nice paragraph covering this in their Knowledge Base: `Pink and green patches in a video stream <https://www.cisco.com/c/en/us/td/docs/telepresence/infrastructure/articles/cisco_telepresence_pink_green_patches_video_stream_kb_136.html>`__ (`archive <https://web.archive.org/web/20170506091043/http://www.cisco.com/c/en/us/td/docs/telepresence/infrastructure/articles/cisco_telepresence_pink_green_patches_video_stream_kb_136.html>`__):

    **Why do I see pink or green patches in my video stream [...]?**

    *Pink and green patches or lines seen in decoded video are often the result of packet loss or incorrect data in the video stream. Many video codecs (including H.261, H.263 and H.264) use the Y'CbCr system to represent color space, where Y' is the 'luma' (brightness) component and Cb and Cr are the blue and red chroma components respectively. For many Y'CbCr values there is no equivalent RGB value and the colour seen on the display depends on the details of the algorithm used. A Y'CbCr value of (0,0,0) is often converted into the green color while a Y'CbCr value of (255,255,255) leads to a pink color.*

    *If you encounter the symptoms described above, follow normal packet loss and network troubleshooting procedures.*



Application Server
==================

These are some common errors found to affect Kurento Application Servers:



KMS is not running
------------------

Usually, the Kurento Client library is directed to connect with an instance of KMS that the developer expects will be running in some remote server. If there is no instance of KMS running at the provided URL, the Kurento Client library will raise an exception which **the Application Server should catch** and handle accordingly.

This is a sample of what the console output will look like, with the logging level set to DEBUG:

.. code-block:: shell-session

   $ mvn -U clean spring-boot:run \
         -Dspring-boot.run.jvmArguments="-Dkms.url=ws://localhost:8888/kurento"
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

.. code-block:: shell-session

   $ mvn -U clean spring-boot:run \
         -Dspring-boot.run.jvmArguments="-Dkms.url=ws://localhost:8888/kurento"
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
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket : tryReconnectingForever = true
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket : tryReconnectingMaxTime = 0
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket : maxTimeReconnecting = 9223372036854775807
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket : currentTime = 1510773733903
   DEBUG o.k.j.c.AbstractJsonRpcClientWebSocket : Stop connection retries: false
   WARN o.k.j.c.AbstractJsonRpcClientWebSocket  : [KurentoClient]  Exception trying to reconnect to server ws://localhost:8888/kurento. Retrying in 5000 ms

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

Kurento Client does not currently support Node.js v10 (LTS), you will have to use Node.js v8 or below.



.. _troubleshooting-app-proxy:

Connection ends exactly after 60 seconds
----------------------------------------

This is typically caused by an intermediate proxy, which is prematurely ending the WebSocket session from the Application Server, and thus making the media server believe that all resources should be released.

For example, if **Nginx Reverse Proxy** is used, the default value of `proxy_read_timeout <http://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_read_timeout>`__ is **60 seconds**, but the default Kurento :ref:`Ping/Pong keep-alive <protocol-ping>` mechanism works in intervals of 240 seconds.

This issue can also manifest itself with this (misleading) error message in the browser's JavaScript console:

.. code-block:: text

   WebRTC: ICE failed, add a TURN server and see about:webrtc for more details

The solution is to increase the timeout value in your proxy settings.



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
- The H.264 codec must be enabled in the corresponding Kurento settings file: ``/etc/kurento/modules/kurento/SdpEndpoint.conf.json``.
  Ensure that the entry corresponding to this codec does exist and is not commented out. For example:

  .. code-block:: js

     "videoCodecs": [
       { "name": "VP8/90000" },
       { "name": "H264/90000" }
     ]



"Error: 'operationParams' is required"
--------------------------------------

This issue is commonly caused by setting an invalid ID to any of the client method calls. The usual solution is to provide a null identifier, forcing the server to generate a new one for the object.

For example, a Node.js application wanting to use the *ImageOverlayFilter* API (`Java <../_static/client-javadoc/org/kurento/client/ImageOverlayFilter.html>`__, `JavaScript <../_static/client-jsdoc/module-filters.ImageOverlayFilter.html>`__) might mistakenly try to provide an invalid ID in the `addImage() <../_static/client-jsdoc/module-filters.ImageOverlayFilter.html#.addImage>`__ call:

.. code-block:: js

   const filter = await pipeline.create("ImageOverlayFilter");
   await filter.addImage("IMAGE_ID", "https://IMAGE_URL", 0.5, 0.5, 0.5, 0.5, true, true);
   await webRtcEndpoint.connect(filter);
   await filter.connect(webRtcEndpoint);

This will fail, causing a *MARSHALL_ERROR* in the media server, and showing the following stack trace in the client side:

.. code-block:: text

   Trace: { Error: 'operationParams' is required
       at node_modules/kurento-client/lib/KurentoClient.js:373:24
       at Object.dispatchCallback [as callback] (node_modules/kurento-jsonrpc/lib/index.js:546:9)
       at processResponse (node_modules/kurento-jsonrpc/lib/index.js:667:15)
       [...]
       at WebsocketStream.onMessage (node_modules/websocket-stream/index.js:45:15) code: 40001, data: { type: 'MARSHALL_ERROR' } }

The solution is to simply use *null* for the first argument of the method:

.. code-block:: js

   await filter.addImage(null, "https://IMAGE_URL", 0.5, 0.5, 0.5, 0.5, true, true);



.. _troubleshooting-webrtc:

WebRTC failures
===============

There is a multitude of possible reasons for a failed WebRTC connection, so you can start by following this checklist:

* Deploy a :term:`STUN`/:term:`TURN` server (such as Coturn), to make remote WebRTC connections possible: :ref:`faq-coturn-install`.

* Test if your :term:`STUN`/:term:`TURN` server is working correctly: :ref:`faq-stun-test`.

* Configure your :term:`STUN`/:term:`TURN` server in Kurento Media Server: :ref:`faq-stun-configure`.

* Check the debug logs of your :term:`STUN`/:term:`TURN` server. Maybe the server is failing and some useful error messages are being printed in there.

* Check the debug logs of Kurento Media Server. Look for messages that confirm a correct configuration:

  .. code-block:: text

     INFO  Using STUN reflexive server IP: <IpAddress>
     INFO  Using STUN reflexive server Port: <Port>

     INFO  Using TURN relay server: <user:password>@<IpAddress>:<Port>
     INFO  TURN server info set: <user:password>@<IpAddress>:<Port>

* Check that any SDP mangling you (or any of your third-party libraries) might be doing in your Application Server is being done correctly.

  This is one of the most hard to catch examples we've seen in our `mailing list <https://groups.google.com/d/topic/kurento/t25_QQSc_Bo/discussion>`__:

      > The problem was that our Socket.IO client did not correctly *URL-Encode* its JSON payload when *xhr-polling*, which resulted in all "plus" signs ('+') being changed into spaces (' ') on the server. This meant that the *ufrag* in the client's SDP was invalid if it contained a plus sign! Only some of the connections failed because not all *ufrag* contain plus signs.

* If WebRTC seems to disconnect exactly after some amount of time, every single time, **watch out for proxy timeouts**. Sometimes you have to extend the timeout for the site that is being hit with the problem. See also: :ref:`troubleshooting-app-proxy`.

* Have a look at these articles about troubleshooting WebRTC:

  - `Troubleshooting WebRTC Connection Issues <https://blog.addpipe.com/troubleshooting-webrtc-connection-issues/>`__ (`archive <https://web.archive.org/web/20200219144706/https://blog.addpipe.com/troubleshooting-webrtc-connection-issues/>`__).

  - `Common (beginner) mistakes in WebRTC <https://bloggeek.me/common-beginner-mistakes-in-webrtc/>`__ (`archive <https://web.archive.org/web/20200219144856/https://bloggeek.me/common-beginner-mistakes-in-webrtc/>`__).



ICE connection problems
-----------------------

If your application receives an :ref:`events-icecomponentstatechange` event with state *FAILED* from Kurento Media Server, it means that the WebRTC ICE connectivity has been abruptly interrupted. In general terms, this implies that **there is some network connectivity issue** between KMS and the remote peer (typically, a web browser), but the exact reason can fall into a myriad possible causes. You will need to investigate what happened on the user's and the server's network when the failure happened.

Here are some tips to keep in mind:

* Check that you have correctly configured a :term:`STUN` server or :term:`TURN` relay, both in Kurento Media Server (file *WebRtcEndpoint.conf.ini*), and in the client browsers (through the `RTCPeerConnection's iceServers setting <https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection/RTCPeerConnection>`__).

* Check that the :term:`TURN` credentials are correct, by using the `Trickle ICE test page <https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/>`__ to test your STUN/TURN server, as explained here: :ref:`faq-stun-test`.

* It is always a good idea to work out the **correlation between ICE failures on KMS with ICE failures on the client browser**. The combined logs of both sides might shed some light into what caused the disconnection.

* Analyze all :ref:`events-newcandidatepairselected` events emitted by Kurento. A lot of ICE candidates are tested for connectivity during the WebRTC session establishment, but only the actual working ones are reported with the *NewCandidatePairSelected* event. A **careful examination of all selected local and remote candidates** might reveal useful information about the kind of connectivity issues that clients might be having.

  For example, maybe you see that most or all of the selected local or remote candidates are of ``typ relay``, i.e. using a :term:`TURN` relay as a proxy for the audio/video streams. This would mean two things:

  1. That the :term:`TURN` relay will be under high server load, possibly saturating the machine's resources.
  2. That **direct peer-to-peer WebRTC connections are not being established**, giving you a good starting point to investigate why this is happening. Usually, when you see usage of the :term:`TURN` relay, this is caused by overzealous hardware or software firewalls, or the presence of Symmetric :term:`NAT` modem/routers somewhere in the network path.

* If you see messages about ICE connection tests failing due to **timeout on trying pairs**, make sure that all required UDP ports for media content are open on the sever; otherwise, not only the ICE process will fail, but also the video or audio streams themselves won't be able to reach each WebRTC peer.



mDNS ICE candidate fails: Name or service not known
---------------------------------------------------

**Problem**:

When the browser conceals the local IP address behind an mDNS candidate, these errors appear in Kurento logs:

.. code-block:: text

   kmsicecandidate  [...] Error code 0: 'Error resolving '2da1b2bb-a601-44e8-b672-dc70e3493bc4.local': Name or service not known'
   kmsiceniceagent  [...] Cannot parse remote candidate: 'candidate:2382557538 1 udp 2113937151 2da1b2bb-a601-44e8-b672-dc70e3493bc4.local 50635 typ host generation 0 ufrag /Og/ network-cost 999'
   kmswebrtcsession [...] Adding remote candidate to ICE Agent: Agent failed, stream_id: '1'

**Solution**:

mDNS name resolution must be enabled in the system. Check out the contents of ``/etc/nsswitch.conf``, you should see something similar to this:

.. code-block:: text

   hosts: files mdns4_minimal [NOTFOUND=return] dns

If not, try fully reinstalling the package *libnss-mdns*:

.. code-block:: shell

   sudo apt-get purge --yes libnss-mdns
   sudo apt-get update && sudo apt-get install --yes libnss-mdns

Installing this package does automatically edit the config file in an appropriate way. Now the *mdns4_minimal* module should appear listed in the hosts line.

**Caveat**: **mDNS does not work from within Docker**

See `mDNS and Crossbar.io Fabric (Docker) #21 <https://github.com/crossbario/crossbar-fabric-public/issues/21>`__:

    Docker does not play well with mDNS/zeroconf/Bonjour: resolving *.local* hostnames from inside containers does not work (easily).
    [...]
    The reasons run deep into how Docker configures DNS *inside* a container.

So if you are running a Docker image, *.local* names won't be correctly resolved even if you install the required packages. This happens with Kurento or whatever other software; it seems to be a Docker configuration problem / bug.

**Disabling mDNS in Chrome**

Chrome allows disabling mDNS, which is something that could be useful during development. However when development is finished, don't forget to test your application with default settings, including with this option enabled!

To disable mDNS, open this URL: ``chrome://flags/#enable-webrtc-hide-local-ips-with-mdns`` and change the setting to "Disabled".



Docker issues
=============

.. _troubleshooting-docker-network-host:

Publishing Docker ports eats memory
-----------------------------------

Docker will consume a lot of memory when `publishing <https://docs.docker.com/config/containers/container-networking/#published-ports>`__ big enough port ranges. As of this writing, there is no quick and easy solution to this issue.

**You should not expose a large port range** in your Docker containers; instead, prefer using `Host Networking <https://docs.docker.com/network/host/>`__ (``--network host``). To elaborate a bit more, as mentioned `here <https://github.com/kubernetes/kubernetes/issues/23864#issuecomment-387070644>`__:

    the problem is that - given the current state of Docker - it seems you should NOT even be trying to expose large numbers of ports. You are advised to use the host network anyway, due to the overhead involved with large port ranges. (it adds both latency, as well as consumes significant resources - e.g. see https://www.percona.com/blog/2016/02/05/measuring-docker-cpu-network-overhead/)

    If you are looking for a more official source, there is still (for years) an open issue in Docker about this:
    `moby/moby#11185 (comment) <https://github.com/moby/moby/issues/11185#issuecomment-245983651>`__



Multicast fails in Docker
-------------------------

**Problem**:

- Your Kurento Media Server is running in a Docker container.
- MULTICAST streams playback fail with an error such as this one:

  .. code-block:: text

     DEBUG rtspsrc gstrtspsrc.c:7553:gst_rtspsrc_handle_message:<source> timeout on UDP port

  Note that in this example, to see this message you would need to enable *DEBUG* log level for the *rtspsrc* category; see :ref:`logging-levels`.

**Solution**:

For Multicast streaming to work properly, you need to disable Docker network isolation and use ``--network host``. Note that this gives the container direct access to the host interfaces, and you'll need to connect through published ports to access others containers.

This is a limitation of Docker; you can follow the current status with this issue: `#23659 Cannot receive external multicast inside container <https://github.com/moby/moby/issues/23659>`__.

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

RTSP broken audio
~~~~~~~~~~~~~~~~~

If you have your own RTSP tool generating OPUS encoded audio to be consumed in Kurento with a *PlayerEndpoint* (`Java <../_static/client-javadoc/org/kurento/client/PlayerEndpoint.html>`__, `JavaScript <../_static/client-jsdoc/module-elements.PlayerEndpoint.html>`__), and the resulting audio is very choppy and robotic, you should start by verifying that your encoding process is configured correctly for the OPUS frame size used in WebRTC.

This was the case for a user who later shared with us the reasons for the bad quality audio they were perceiving:

    `Bad audio quality <https://groups.google.com/g/kurento/c/nq-BNeZn2P8>`__

    > *There was a mismatch between the incoming raw audio frame size and the opus encoding frame size,
    which resulted in a bad encoding cadence causing irregular encoded frame intervals.*

    > *We remedied this by ensuring that the incoming audio frame size and the opus encoding frame size are
    the same --- or the incoming frame size is a divisor of the encoding frame size.*



RTSP broken video
~~~~~~~~~~~~~~~~~

Some users have reported huge macro-blocks or straight out broken video frames when using a PlayerEndpoint to receive an RTSP stream containing H.264 video. A possible solution to fix this issue is to fine-tune the PlayerEndpoint's **networkCache** parameter. It basically sets the buffer size (in milliseconds) that the underlying GStreamer decoding element will use to cache the stream.

There's no science for that parameter, though. The perfect value depends on your network topology and efficiency, so you should proceed in a trial-and-error approach. For some situations, values lower than **100ms** have worked fine; some users have reported that 10ms was required to make their specific camera work, others have seen good results with setting this parameter to **0ms**. However, these are outlier cases and normally a higher *networkCache* is needed.

In principle, *networkCache = 0* would mean that all RTP packets must be exactly on point at the expected times in the RTSP stream, or else they will be dropped. So even a slight amount of jitter or delay in the network might cause packets to be dropped when they arrive to the PlayerEndpoint.

*networkCache* translates directly to the *latency* property of GStreamer's `rtspsrc <https://gstreamer.freedesktop.org/documentation/rtsp/rtspsrc.html>`__ element, which in turn is passed to the `rtpbin <https://gstreamer.freedesktop.org/documentation/rtpmanager/rtpbin.html>`__ and ultimately the `rtpjitterbuffer <https://gstreamer.freedesktop.org/documentation/rtpmanager/rtpjitterbuffer.html>`__ inside it.



RTSP Video stuttering
~~~~~~~~~~~~~~~~~~~~~

The GStreamer element in charge of RTSP reception is `rtspsrc <https://gstreamer.freedesktop.org/data/doc/gstreamer/head/gst-plugins-good/html/gst-plugins-good-plugins-rtspsrc.html>`__, and this element contains an `rtpjitterbuffer <https://gstreamer.freedesktop.org/data/doc/gstreamer/head/gst-plugins-good/html/gst-plugins-good-plugins-rtpjitterbuffer.html>`__.

This jitter buffer gets full when network packets arrive faster than what Kurento is able to process. If this happens, then PlayerEndpoint will start dropping packets, which will show up as video stuttering on the output streams, while triggering a warning in Kurento logs:

.. code-block:: text

   WARNING  kmsutils  discont_detection_probe() <kmsagnosticbin0:sink>  Stream discontinuity detected on non-keyframe

You can check if this problem is affecting you by running with DEBUG :ref:`logging level <logging-levels>` enabled for the *rtpjitterbuffer* component, and searching for a specific message:

.. code-block:: shell

   export GST_DEBUG="${GST_DEBUG:-2},rtpjitterbuffer:5"
   /usr/bin/kurento-media-server 2>&1 | grep -P 'rtpjitterbuffer.*(Received packet|Queue full)'

With this command, a new line will get printed for each single *Received packet*, plus an extra line will appear informing about *Queue full* whenever a packet is dropped.

There is not much you can fine tune in KMS to solve this problem; the most practical solution is to reduce the amount of data, mostly by decreasing either video resolution or video bitrate.

Kurento Media Server is known to work well receiving videos of up to **720p** resolution (1280x720) at **30fps** and around **2Mbps**. If you are using values beyond those, there is a chance that KMS will be unable to process all incoming data on time, and this will cause that buffers fill up and frames get dropped. Try reducing the resolution of your input videos to see if this helps solving the issue.



RecorderEndpoint
----------------

Zero-size video files
~~~~~~~~~~~~~~~~~~~~~

If you are trying to generate a video recording, keep in mind that **the endpoint will wait until all tracks (audio, video) start arriving**.

Quoting from the `Client documentation <../_static/client-javadoc/org/kurento/client/RecorderEndpoint.html>`__:

    It is recommended to start recording only after media arrives, either to the endpoint that is the source of the media connected to the recorder, to the recorder itself, or both. Users may use the MediaFlowIn and MediaFlowOut events, and synchronize the recording with the moment media comes in. In any case, nothing will be stored in the file until the first media packets arrive.

Follow this issue checklist to see if any of them is preventing the RecorderEndpoint from working correctly:

* The RecorderEndpoint was connected with the default ``connect(MediaElement)`` method (`Java <../_static/client-javadoc/org/kurento/client/MediaElement.html#connect-org.kurento.client.MediaElement->`__, `JavaScript <../_static/client-jsdoc/module-core_abstracts.MediaElement.html#.connect>`__), which assumes both audio and video, but only video (or only audio) is arriving:

  - Monitor the :ref:`MediaFlowInStateChange <events-mediaflowin>` and :ref:`MediaFlowOutStateChange <events-mediaflowout>` events from all MediaElements.
  - Make sure that the element providing media (the *source*) is firing a *MediaFlowOut* event, and that the RecorderEndpoint is firing a corresponding *MediaFlowIn* event.
  - If your recording should be only-audio or only-video, use the ``connect(MediaElement, MediaType)`` method (`Java <../_static/client-javadoc/org/kurento/client/MediaElement.html#connect-org.kurento.client.MediaElement-org.kurento.client.MediaType->`__, `JavaScript <../_static/client-jsdoc/module-core_abstracts.MediaElement.html#.connect>`__).

* Check the availability of audio/video devices at recorder client initialization, and just before starting the recording.
* User is disconnecting existing hardware, or maybe connecting new hardware (usb webcams, mic, etc).
* User is clicking "*Deny*" when asked to allow access to microphone/camera by the browser.
* User is sleeping/hibernating the computer, and then possibly waking it up, while recording.
* Check the browser information about the required media tracks, e.g. ``track.readyState``.
* Track user agents, ICE candidates, etc.



Smaller or low quality video files
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Kurento will just record whatever arrives as input, so if your recordings have less quality or lower resolution than expected, this is because the source video was already sent like that.

In most situations, the real cause of this issue is the web browser encoding and sending a low bitrate or a low resolution video. Keep in mind that some browsers (Chrome, as of this writing) are able to dynamically adjust the output resolution; this means that the real size of the video coming out from Chrome will vary over time. Normally it starts small, and after some time it improves, when the browser detects that the available network bandwidth allows for it.

Check this section to get some advice about how to investigate low quality issues: :ref:`troubleshooting-low-quality`.



Browser
=======

Safari doesn't work
-------------------

Apple Safari is a browser that follows some policies that are much more restrictive than those of other common browsers such as Google Chrome or Mozilla Firefox.

For some tips about how to ensure the best compatibility with Safari, check :doc:`/knowledge/safari`.
