=============
Debug Logging
=============

Kurento Media Server generates log files that are stored in ``/var/log/kurento-media-server/``. The content of this folder is as follows:

- ``media-server_<timestamp>.<log_number>.<kms_pid>.log``: Output log of a currently running instance of KMS.
- ``media-server_error.log``: Errors logged by third-party libraries.
- ``logs``: Folder that contains older KMS logs. The logs in this folder are rotated, so they don't fill up all the space available in the disk.

Each line in a log produced by KMS has a fixed structure:

.. code-block:: text

   [timestamp] [pid] [memory] [level] [component] [filename:loc] [method] [message]

- ``[timestamp]``: Date and time of the logging message (e.g. *2017-12-31 23:59:59,493295*).
- ``[pid]``: Process Identifier of *kurento-media-sever* (e.g. *17521*).
- ``[memory]``: Memory address in which the *kurento-media-sever* component is running (e.g. *0x00007fd59f2a78c0*).
- ``[level]``: Logging level. This value typically will be *INFO* or *DEBUG*. If unexpected error situations happen, the *WARN* and *ERROR* levels will contain information about the problem.
- ``[component]``: Name of the component that generated the log line. E.g. *KurentoModuleManager*, *webrtcendpoint*, or *qtmux*, among others.
- ``[filename:loc]``: Source code file name (e.g. *main.cpp*) followed by the line of code number.
- ``[method]``: Name of the function in which the log message was generated (e.g. *loadModule()*, *doGarbageCollection()*, etc).
- ``[message]``: Specific log information.

For example, when KMS starts correctly, this trace is written in the log file:

.. code-block:: text

   [timestamp] [pid] [memory]  info  KurentoMediaServer  main.cpp:255  main()  Kurento Media Server started



Logging levels and components
=============================

Each different **component** of KMS is able to generate its own logging messages. Besides that, each individual logging message has a severity **level**, which defines how critical (or superfluous) the message is.

These are the different message levels, as defined by the `GStreamer logging library <https://gstreamer.freedesktop.org/data/doc/gstreamer/head/gstreamer/html/gst-running.html>`_:

- **(1) ERROR**: Logs all *fatal* errors. These are errors that do not allow the core or elements to perform the requested action. The application can still recover if programmed to handle the conditions that triggered the error.
- **(2) WARNING**: Logs all warnings. Typically these are *non-fatal*, but user-visible problems that *are expected to happen*.
- **(3) FIXME**: Logs all "fixme" messages. Fixme messages are messages that indicate that something in the executed code path is not fully implemented or handled yet. The purpose of this message is to make it easier to spot incomplete/unfinished pieces of code when reading the debug log.
- **(4) INFO**: Logs all informational messages. These are typically used for events in the system that *happen only once*, or are important and rare enough to be logged at this level.
- **(5) DEBUG**: Logs all debug messages. These are general debug messages for events that *happen only a limited number of times* during an object's lifetime; these include setup, teardown, change of parameters, etc.
- **(6) LOG**: Logs all log messages. These are messages for events that *happen repeatedly* during an object's lifetime; these include streaming and steady-state conditions.
- **(7) TRACE**: Logs all trace messages. These messages for events that *happen repeatedly* during an object's lifetime such as the ref/unref cycles.
- **(8) MEMDUMP**: Log all memory dump messages. Memory dump messages are used to log (small) chunks of data as memory dumps in the log. They will be displayed as hexdump with ASCII characters.

Logging categories and levels can be set by two methods:

- Use the specific command-line argument while launching KMS. For example, run:

  .. code-block:: text

     /usr/bin/kurento-media-server \
       --gst-debug-level=3 \
       --gst-debug=Kurento*:4,kms*:4

- Use the environment variable `GST_DEBUG`. For example, run:

  .. code-block:: bash

     export GST_DEBUG="3,Kurento*:4,kms*:4"
     /usr/bin/kurento-media-server



Suggested levels
================

Here are some tips on what logging components and levels could be most useful depending on what is the issue to be analyzed:

- Global level: **3** (higher than 3 would mean too much noise from GStreamer)
- Unit tests: ``check:5``
- SDP processing: ``kmssdpsession:4``
- COMEDIA port discovery: ``rtpendpoint:4``
- ICE candidate gathering:

  - At the Nice Agent (handling of candidates): ``kmsiceniceagent:5``
  - At the KMS WebRtcSession (decision logic): ``kmswebrtcsession:5``
  - At the WebRtcEndpoint (very basic logging): ``webrtcendpoint:4``

- REMB congestion control:

  - Only effective REMB send/recv values: ``kmsremb:5``
  - Full handling of all source SSRCs: ``kmsremb:6``

- MediaFlow{In|Out} state changes: ``KurentoMediaElementImpl:5``
- RPC calls: ``KurentoWebSocketTransport:5``
- RTP Sync: ``kmsutils:5,rtpsynchronizer:5,rtpsynccontext:5,basertpendpoint:5``
- Player: ``playerendpoint:5``
- Recorder: ``KurentoRecorderEndpointImpl:4,recorderendpoint:5,qtmux:5``



3rd-party libraries
===================

.. _logging-libnice:

libnice
-------

**libnice** is `the GLib implementation <https://nice.freedesktop.org>`_ of :term:`ICE`, the standard method used by :term:`WebRTC` to solve the issue of :term:`NAT Traversal`.

This library has its own logging system that comes disabled by default, but can be enabled very easily. This can prove useful in situations where a developer is studying an issue with the ICE process. However, the debug output of libnice is very verbose, so it makes sense that it is left disabled by default for production systems.

Run KMS with these environment variables defined: ``G_MESSAGES_DEBUG`` and ``NICE_DEBUG``. They must have one or more of these values, separated by commas:

- libnice
- libnice-stun
- libnice-tests
- libnice-socket
- libnice-pseudotcp
- libnice-pseudotcp-verbose
- all

Example:

.. code-block:: bash

   export G_MESSAGES_DEBUG="libnice,libnice-stun"
   export NICE_DEBUG="$G_MESSAGES_DEBUG"
   /usr/bin/kurento-media-server
