=============
Debug Logging
=============

Kurento Media Server prints log messages by using the `GStreamer logging library <https://gstreamer.freedesktop.org/documentation/gstreamer/running.html>`__. This is a very flexible library that allows users to fine-tune the amount of verbosity that they want to get from the media server.

Logging verbosity is controlled by setting the *GST_DEBUG* environment variable with an appropriate string. In this section we'll show some useful examples, and then provide complete technical documentation about the logging features available for Kurento.

.. contents:: Table of Contents



Default levels
==============

This is the default value for the *GST_DEBUG* environment variable, as found after installing Kurento Media Server for the first time:

.. code-block:: shell

   export GST_DEBUG="2,Kurento*:4,kms*:4,sdp*:4,webrtc*:4,*rtpendpoint:4,rtp*handler:4,rtpsynchronizer:4,agnosticbin:4"

:ref:`Local installations <installation-local>` will have this value set in the service settings file, ``/etc/default/kurento-media-server`` (for Debian/Ubuntu packages). On the other hand, the official :ref:`Docker images <installation-docker>` come with this value already defined by default.



Verbose logging
===============

While KMS is able to log a lot of technical details, most of them are disabled by default because otherwise the logging output would be huge. Here is a list of some strings that can be **added** to the default value of *GST_DEBUG*, to help with :doc:`/user/troubleshooting`:



Flowing of media
----------------

.. code-block:: shell

   export GST_DEBUG="${GST_DEBUG:-2},KurentoMediaElementImpl:5"

* "KurentoMediaElementImpl:5" shows *MediaFlowIn* and *MediaFlowOut* state changes, showing if media is actually flowing between endpoints (see :ref:`events-mediaelement`).



Transcoding of media
--------------------

.. code-block:: shell

   export GST_DEBUG="${GST_DEBUG:-2},KurentoMediaElementImpl:5,agnosticbin*:5"

* "KurentoMediaElementImpl:5" shows *MediaTranscoding* state changes.
* "agnosticbin*:5" shows the requested and available codecs on Endpoints. When there is a mismatch, transcoding is automatically enabled.



WebRtcEndpoint and RtpEndpoint
------------------------------

.. code-block:: shell

   export GST_DEBUG="${GST_DEBUG:-2},Kurento*:5,KurentoWebSocket*:4"
   export GST_DEBUG="${GST_DEBUG:-2},kmssdpsession:5"
   export GST_DEBUG="${GST_DEBUG:-2},sdp*:5"
   export GST_DEBUG="${GST_DEBUG:-2},webrtcendpoint:5,kmswebrtcsession:5,kmsiceniceagent:5"

* "Kurento*:5" shows all state changes (*MediaFlowIn*, *MediaFlowOut*, *MediaTranscoding*, etc). Use "KurentoWebSocket*:4" to avoid getting all verbose logs about the WebSocket communications.
* "kmssdpsession:5" prints the SDP messages (SDP Offer/Answer negotiation) processed by KMS.
* "sdp*:5" shows internal messages related to the construction of SDP messages and media handlers.

* "webrtcendpoint:5", "kmswebrtcsession:5", and "kmsiceniceagent:5" all contain the logic that governs ICE gathering and ICE candidate selection for WebRTC.

  .. note::

     See also :ref:`logging-libnice` to enable advanced :term:`ICE` logging for WebRTC.

You can also see messages about the :term:`REMB` congestion control algorithm for WebRTC. However these will constantly be filling the log, so you shouldn't enable them unless explicitly working out an issue with REMB:

.. code-block:: shell

   export GST_DEBUG="${GST_DEBUG:-2},kmsremb:5"



PlayerEndpoint
--------------

.. code-block:: shell

   export GST_DEBUG="${GST_DEBUG:-2},KurentoUriEndpointImpl:5,playerendpoint:5,kmselement:5,appsrc:4,agnosticbin*:5,uridecodebin:6,rtspsrc:5,souphttpsrc:5,*CAPS*:3"



RecorderEndpoint
----------------

.. code-block:: shell

   export GST_DEBUG="${GST_DEBUG:-2},KurentoRecorderEndpointImpl:4,recorderendpoint:5,basemediamuxer:5,qtmux:5,curl*:5"



Other components
----------------

Other less commonly used logging levels are:

* **imageoverlay**, **logooverlay** (as used, for example, in some :doc:`Kurento Tutorials </user/tutorials>`):

  .. code-block:: shell

     export GST_DEBUG="${GST_DEBUG:-2},imageoverlay:5,logooverlay:5"

* **RTP Synchronization**:

  .. code-block:: shell

     export GST_DEBUG="${GST_DEBUG:-2},kmsutils:5,rtpsynchronizer:5,rtpsynccontext:5,basertpendpoint:5"

* **JSON-RPC** API server calls:

  .. code-block:: shell

     export GST_DEBUG="${GST_DEBUG:-2},KurentoWebSocket*:5"

* **Unit tests**:

  .. code-block:: shell

     export GST_DEBUG="${GST_DEBUG:-2},check:5,test_base:5"



3rd-Party libraries
-------------------

.. _logging-libnice:

libnice
~~~~~~~

**libnice** is the `GLib implementation <https://nice.freedesktop.org>`__ of :term:`ICE`, the standard method used by :term:`WebRTC` to solve the issue of :term:`NAT Traversal`.

This library uses the standard *GLib* logging functions, which comes disabled by default but can be enabled very easily. This can prove useful in situations where a developer is studying an issue with the ICE process. However, the debug output of libnice is very verbose, so it makes sense that it is left disabled by default for production systems.

To enable debug logging on *libnice*, set the environment variable *G_MESSAGES_DEBUG* with one or more of these values (separated by commas):

- *libnice*: Required in order to enable logging in libnice.
- *libnice-verbose*: Enable extra verbose messages.
- *libnice-stun*: Log messages related to the :term:`STUN` protocol.
- *libnice-pseudotcp*: Log messages from the ICE-TCP module.
- *libnice-pseudotcp-verbose*: Enable extra verbose messages from ICE-TCP.
- *all*: Equivalent to using all previous flags.

After doing this, GLib messages themselves must be enabled in the Kurento logging system, by setting an appropriate level for the *glib* component.

Example:

.. code-block:: shell

   export G_MESSAGES_DEBUG="libnice,libnice-stun"
   export GST_DEBUG="${GST_DEBUG:-2},glib:5"
   /usr/bin/kurento-media-server

You can also set this configuration in the Kurento service settings file, which gets installed at ``/etc/default/kurento-media-server``.



libsoup
~~~~~~~

**libsoup** is the `GNOME HTTP client/server <https://wiki.gnome.org/Projects/libsoup>`__ library. It is used to perform HTTP requests, and currently this is used in Kurento by the *KmsImageOverlay* and the *KmsLogoOverlay* filters.

It is possible to enable detailed debug logging of the HTTP request/response headers, by defining the environment variable ``SOUP_DEBUG=1`` before running KMS:

.. code-block:: shell

   export SOUP_DEBUG=1
   /usr/bin/kurento-media-server



Logs Location
=============

KMS prints by default all its log messages to standard output (*stdout*). This happens when the media server is run directly with ``/usr/bin/kurento-media-server``, or when running from the official :ref:`Docker images <installation-docker>`.

Saving logs to file is enabled whenever the environment variable ``KURENTO_LOGS_PATH`` is set, or the ``--logs-path`` command-line flag is used. The KMS native packages take advantage of this, placing logs in a conventional location for the platform: ``/var/log/kurento-media-server/``. This path can be customized by exporting the mentioned variable, or editing the service settings file located at ``/etc/default/kurento-media-server`` (from Debian/Ubuntu packages).

Log files are named as follows:

.. code-block:: text

   {DateTime}.{LogNumber}.pid{PID}.log

- *{DateTime}*: Logging file creation date and time, in :wikipedia:`ISO 8601` Extended Notation for the date, and Basic Notation for the time. For example: *2018-12-31T235959*.
- *{LogNumber}*: Log file number. A new one will be created whenever the maximum size limit is reached (100 MB by default).
- *{PID}*: Process Identifier of *kurento-media-sever*.

When the KMS service starts correctly, a log file such as this one will be created:

.. code-block:: text

   2018-06-14T194426.00000.pid13006.log

Besides normal log files, an *errors.log* file stores error messages and stack traces, in case KMS crashes.



Logs Rotation
-------------

When saving logs to file (due to either the environment variable ``KURENTO_LOGS_PATH`` or the ``--logs-path`` command-line flag), log files will be rotated, and old files will get eventually deleted when new ones are created. This helps with preventing that all available disk space ends up filled with logs.

To configure this behavior:

* The ``KURENTO_LOG_FILE_SIZE`` env var or ``--log-file-size`` command-line flag control the maximum file size for rotating log files, in MB (default: 100 MB).
* The ``KURENTO_NUMBER_LOG_FILES`` env var or ``--number-log-files`` command-line flag set the maximum number of rotating log files to keep (default: 10 files).



Log Contents
============

Each line in a log file has a fixed structure:

.. code-block:: text

   {DateTime} {PID} {ThreadID} {Level} {Component} {FileLine} {Function} {Object}? {Message}

* *{DateTime}*: Date and time of the logging message, in :wikipedia:`ISO 8601` Extended Notation, with six decimal places for the seconds fraction. For example: *2018-12-31T23:59:59,123456*.
* *{PID}*: Process Identifier of *kurento-media-sever*.
* *{ThreadID}*: Thread ID from which the message was issued. For example: *0x0000111122223333*.
* *{Level}*: Logging level. This value will typically be *INFO* or *DEBUG*. If unexpected error situations happen, the *WARNING* and *ERROR* levels will contain information about the problem.
* *{Component}*: Name of the component that generated the log line. For example: *KurentoModuleManager*, *webrtcendpoint*, *qtmux*, etc.
* *{FileLine}*: File name and line number, separated by a colon. For example: *main.cpp:255*.
* *{Function}*: Name of the function in which the log message was generated. For example: *main()*, *loadModule()*, *kms_webrtc_endpoint_gather_candidates()*, etc.
* *{Object}*: [Optional] Name of the object that issued the message, if one was specified for the log message. For example: *<kmswebrtcendpoint0>*, *<fakesink1>*, *<audiotestsrc0:src>*, etc.
* *{Message}*: The actual log message.

For example, when KMS starts correctly, a message like this will be printed:

.. code-block:: text

   2018-06-14T19:44:26,918243  13006  0x00007f59401f5880  info  KurentoMediaServer  main.cpp:255  main()  Kurento Media Server started



Log colors
----------

Logs will be colored by default, but colors can be explicitly disabled: either with ``--gst-debug-no-color`` or with ``export GST_DEBUG_NO_COLOR=1``.

When running KMS as a system service, the default settings will disable colors. This is done to write clean log files, otherwise the logs would end up filled with strange escape sequences (ANSI color codes).



.. _logging-levels:

Logging levels and components
=============================

Each different *{Component}* of KMS is able to generate its own logging messages. Besides that, each individual logging message has a severity *{Level}*, which defines how critical (or superfluous) the message is.

These are the different message levels, as defined by the `GStreamer logging library <https://gstreamer.freedesktop.org/data/doc/gstreamer/head/gstreamer/html/gst-running.html>`__:

* **(1) ERROR**: Logs all *fatal* errors. These are errors that do not allow the core or elements to perform the requested action. The application can still recover if programmed to handle the conditions that triggered the error.
* **(2) WARNING**: Logs all warnings. Typically these are *non-fatal*, but user-visible problems that *are expected to happen*.
* **(3) FIXME**: Logs all "fixme" messages. Fixme messages are messages that indicate that something in the executed code path is not fully implemented or handled yet. The purpose of this message is to make it easier to spot incomplete/unfinished pieces of code when reading the debug log.
* **(4) INFO**: Logs all informational messages. These are typically used for events in the system that *happen only once*, or are important and rare enough to be logged at this level.
* **(5) DEBUG**: Logs all debug messages. These are general debug messages for events that *happen only a limited number of times* during an object's lifetime; these include setup, teardown, change of parameters, etc.
* **(6) LOG**: Logs all log messages. These are messages for events that *happen repeatedly* during an object's lifetime; these include streaming and steady-state conditions.
* **(7) TRACE**: Logs all trace messages. These messages for events that *happen repeatedly* during an object's lifetime such as the ref/unref cycles.
* **(8) MEMDUMP**: Log all memory dump messages. Memory dump messages are used to log (small) chunks of data as memory dumps in the log. They will be displayed as hexdump with ASCII characters.

Logging categories and levels can be filtered by two methods:

* Use a command-line argument if you are manually running KMS. For example, run:

  .. code-block:: shell

     /usr/bin/kurento-media-server \
       --gst-debug-level=2 \
       --gst-debug="Kurento*:4,kms*:4"

* You can also replace the command-line arguments with the *GST_DEBUG* environment variable. This command is equivalent to the previous one:

  .. code-block:: shell

     export GST_DEBUG="2,Kurento*:4,kms*:4"
     /usr/bin/kurento-media-server

  If you are using the native packages (installing KMS with *apt-get*) and running KMS as a system service, then you can also configure the *GST_DEBUG* variable in the KMS service settings file, ``/etc/default/kurento-media-server``:

  .. code-block:: shell

     # Logging level.
     export GST_DEBUG="2,Kurento*:4,kms*:4"
