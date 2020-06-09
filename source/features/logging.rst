=============
Debug Logging
=============

When running Kurento Media Server manually with ``/usr/bin/kurento-media-server``, all logging messages are by default printed to standard out (*stdout*).

The KMS native packages modify this behavior to ensure logging information is placed in a more conventional location for the platform. By default logs should be made available in */var/log/kurento-media-server/*, unless customized in the service settings file, */etc/default/kurento-media-server* (for Debian/Ubuntu packages). Log files are named as follows:

.. code-block:: text

   {DateTime}.{LogNumber}.pid{PID}.log

- ``{DateTime}``: Logging file creation date and time, in :wikipedia:`ISO 8601` Extended Notation for the date, and Basic Notation for the time. For example: *2018-12-31T235959*.
- ``{LogNumber}``: Log file number. A new one will be created whenever the maximum size limit is reached (100 MB by default).
- ``{PID}``: Process Identifier of *kurento-media-sever*.

When the KMS service starts correctly, a log file such as this one will be created:

.. code-block:: text

   2018-06-14T194426.00000.pid13006.log

Besides normal log files, an ``errors.log`` file stores error messages and stack traces, in case KMS crashes.

.. note::

   Log files in this folder are rotated, and old files will get eventually deleted when new ones are created. This helps with preventing that all available disk space ends up filled with logs.

Each line in a log file has a fixed structure:

.. code-block:: text

   {DateTime} {PID} {ThreadID} {Level} {Component} {FileLine} {Function} {Object}? {Message}

- ``{DateTime}``: Date and time of the logging message, in :wikipedia:`ISO 8601` Extended Notation, with six decimal places for the seconds fraction. For example: *2018-12-31T23:59:59,123456*.
- ``{PID}``: Process Identifier of *kurento-media-sever*.
- ``{ThreadID}``: Thread ID from which the message was issued. For example: *0x0000111122223333*.
- ``{Level}``: Logging level. This value will typically be *INFO* or *DEBUG*. If unexpected error situations happen, the *WARNING* and *ERROR* levels will contain information about the problem.
- ``{Component}``: Name of the component that generated the log line. For example: *KurentoModuleManager*, *webrtcendpoint*, *qtmux*, etc.
- ``{FileLine}``: File name and line number, separated by a colon. For example: *main.cpp:255*.
- ``{Function}``: Name of the function in which the log message was generated. For example: *main()*, *loadModule()*, *kms_webrtc_endpoint_gather_candidates()*, etc.
- ``{Object}``: [Optional] Name of the object that issued the message, if one was specified for the log message. For example: *<kmswebrtcendpoint0>*, *<fakesink1>*, *<audiotestsrc0:src>*, etc.
- ``{Message}``: The actual log message.

For example, when KMS starts correctly, a message like this will be printed:

.. code-block:: text

   2018-06-14T19:44:26,918243  13006  0x00007f59401f5880  info  KurentoMediaServer  main.cpp:255  main()  Kurento Media Server started



.. _logging-levels:

Logging levels and components
=============================

Each different ``{Component}`` of KMS is able to generate its own logging messages. Besides that, each individual logging message has a severity ``{Level}``, which defines how critical (or superfluous) the message is.

These are the different message levels, as defined by the `GStreamer logging library <https://gstreamer.freedesktop.org/data/doc/gstreamer/head/gstreamer/html/gst-running.html>`__:

- **(1) ERROR**: Logs all *fatal* errors. These are errors that do not allow the core or elements to perform the requested action. The application can still recover if programmed to handle the conditions that triggered the error.
- **(2) WARNING**: Logs all warnings. Typically these are *non-fatal*, but user-visible problems that *are expected to happen*.
- **(3) FIXME**: Logs all "fixme" messages. Fixme messages are messages that indicate that something in the executed code path is not fully implemented or handled yet. The purpose of this message is to make it easier to spot incomplete/unfinished pieces of code when reading the debug log.
- **(4) INFO**: Logs all informational messages. These are typically used for events in the system that *happen only once*, or are important and rare enough to be logged at this level.
- **(5) DEBUG**: Logs all debug messages. These are general debug messages for events that *happen only a limited number of times* during an object's lifetime; these include setup, teardown, change of parameters, etc.
- **(6) LOG**: Logs all log messages. These are messages for events that *happen repeatedly* during an object's lifetime; these include streaming and steady-state conditions.
- **(7) TRACE**: Logs all trace messages. These messages for events that *happen repeatedly* during an object's lifetime such as the ref/unref cycles.
- **(8) MEMDUMP**: Log all memory dump messages. Memory dump messages are used to log (small) chunks of data as memory dumps in the log. They will be displayed as hexdump with ASCII characters.

Logging categories and levels can be filtered by two methods:

- Use a command-line argument if you are manually running KMS. For example, run:

  .. code-block:: text

     /usr/bin/kurento-media-server \
       --gst-debug-level=3 \
       --gst-debug="Kurento*:4,kms*:4"

- You can also replace the command-line arguments with the environment variable *GST_DEBUG*. For example, run:

  .. code-block:: bash

     export GST_DEBUG="3,Kurento*:4,kms*:4"
     /usr/bin/kurento-media-server

If you are using the native packages (installing KMS with *apt-get*) and running KMS as a system service, then you can also configure the *GST_DEBUG* variable in the KMS service settings file, */etc/default/kurento-media-server*:

  .. code-block:: bash

     # Logging level.
     export GST_DEBUG="3,Kurento*:4,kms*:4"

Logs will be colored by default, but colors can be explicitly disabled in the same two ways: either with ``--gst-debug-no-color`` or with ``export GST_DEBUG_NO_COLOR=1``. When running KMS as a system service, this option is enabled in order to generate clean logs without strange terminal ANSI color escape sequences.



Suggested levels
================

Here are some tips on what logging components and levels could be most useful depending on what is the issue to be analyzed. They are given in the environment variable form, so they can be copied directly into the KMS KMS service settings file, */etc/default/kurento-media-server*:

The **default suggested level** is what KMS sets automatically when it is started as a system service from the init scripts:

  .. code-block:: text

     export GST_DEBUG="3,Kurento*:4,kms*:4,sdp*:4,webrtc*:4,*rtpendpoint:4,rtp*handler:4,rtpsynchronizer:4,agnosticbin:4"

From that baseline, one can add any other values to extend the amount of information that gets logged:

* **MediaFlowIn**, **MediaFlowOut** state changes, important to know if media is actually flowing between endpoints (see :ref:`events-mediaelement`):

  .. code-block:: text

     export GST_DEBUG="${GST_DEBUG:-3},KurentoMediaElementImpl:5"

* **WebRTC** related logging:

  - SDP Offer/Answer messages:

    .. code-block:: text

       export GST_DEBUG="${GST_DEBUG:-3},kmssdpsession:5"

  - ICE candidate gathering:

    .. code-block:: text

       export GST_DEBUG="${GST_DEBUG:-3},webrtcendpoint:5,kmswebrtcsession:5,kmsiceniceagent:5"

    .. note::

       - See also :ref:`logging-libnice` to enable advanced logging.
       - *webrtcendpoint* shows detailed messages from the WebRtcEndpoint (good enough for most cases).
       - *kmswebrtcsession* shows messages from the internal WebRtcSession class (broarder decision logic).
       - *kmsiceniceagent* shows messages from the *libnice* Agent (very low-level, probably too verbose for day to day troubleshooting).

  - REMB congestion control:

    .. code-block:: text

       export GST_DEBUG="${GST_DEBUG:-3},kmsremb:5"

    .. note::

       - *kmsremb:5* (debug level 5) shows only effective REMB send/recv values.
       - *kmsremb:6* (debug level 6) shows full (very verbose) handling of all source SSRCs.

* **PlayerEndpoint**:

  .. code-block:: text

     export GST_DEBUG="${GST_DEBUG:-3},kmselement:5,playerendpoint:5,appsrc:4,agnosticbin*:5,uridecodebin:6,rtspsrc:5,souphttpsrc:5,*CAPS*:3"

* **RecorderEndpoint**:

  .. code-block:: text

     export GST_DEBUG="${GST_DEBUG:-3},KurentoRecorderEndpointImpl:4,recorderendpoint:5,qtmux:5"

* **JSON-RPC** API server calls:

  .. code-block:: text

     export GST_DEBUG="${GST_DEBUG:-3},KurentoWebSocket*:5"

* **RTP Synchronization**:

  .. code-block:: text

     export GST_DEBUG="${GST_DEBUG:-3},kmsutils:5,rtpsynchronizer:5,rtpsynccontext:5,basertpendpoint:5"

* **Transcoding of media**:

  .. code-block:: text

     export GST_DEBUG="${GST_DEBUG:-3},Kurento*:5,agnosticbin*:5"

* **Unit tests**:

  .. code-block:: text

     export GST_DEBUG="${GST_DEBUG:-3},check:5,test_base:5"



3rd-Party libraries
===================

.. _logging-libnice:

libnice
-------

**libnice** is the `GLib implementation <https://nice.freedesktop.org>`__ of :term:`ICE`, the standard method used by :term:`WebRTC` to solve the issue of :term:`NAT Traversal`.

This library uses the standard *GLib* logging functions, which comes disabled by default but can be enabled very easily. This can prove useful in situations where a developer is studying an issue with the ICE process. However, the debug output of libnice is very verbose, so it makes sense that it is left disabled by default for production systems.

To enable debug logging on *libnice*, set the environment variable ``G_MESSAGES_DEBUG`` with one or more of these values (separated by commas):

- ``libnice``: Required in order to enable logging in libnice.
- ``libnice-verbose``: Enable extra verbose messages.
- ``libnice-stun``: Log messages related to the :term:`STUN` protocol.
- ``libnice-pseudotcp``: Log messages from the ICE-TCP module.
- ``libnice-pseudotcp-verbose``: Enable extra verbose messages from ICE-TCP.
- ``all``: Equivalent to using all previous flags.

After doing this, GLib messages themselves must be enabled in the Kurento logging system, by setting an appropriate level for the ``glib`` component.

Example:

.. code-block:: bash

   export G_MESSAGES_DEBUG="libnice,libnice-stun"
   export GST_DEBUG="${GST_DEBUG:-3},glib:5"
   /usr/bin/kurento-media-server

You can also set this configuration in the Kurento service settings file, which gets installed at ``/etc/default/kurento-media-server``.



libsoup
-------

**libsoup** is the `GNOME HTTP client/server <https://wiki.gnome.org/Projects/libsoup>`__ library. It is used to perform HTTP requests, and currently this is used in Kurento by the *KmsImageOverlay* and the *KmsLogoOverlay* filters.

It is possible to enable detailed debug logging of the HTTP request/response headers, by defining the environment variable ``SOUP_DEBUG=1`` before running KMS:

.. code-block:: bash

   export SOUP_DEBUG=1
   /usr/bin/kurento-media-server
