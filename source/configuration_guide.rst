===================
Configuration Guide
===================

The main configuration file for KMS is located in ``/etc/kurento/kurento.conf.json``.
After a fresh installation this file is as follows:

.. sourcecode:: js

   {
     "mediaServer" : {
       "resources": {
       //  //Resources usage limit for raising an exception when an object creation is attempted
       //  "exceptionLimit": "0.8",
       //  // Resources usage limit for restarting the server when no objects are alive
       //  "killLimit": "0.7",
           // Garbage collector period in seconds
           "garbageCollectorPeriod": 240
       },
       "net" : {
         "websocket": {
           "port": 8888,
           //"secure": {
           //  "port": 8433,
           //  "certificate": "defaultCertificate.pem",
           //  "password": ""
           //},
           //"registrar": {
           //  "address": "ws://localhost:9090",
           //  "localAddress": "localhost"
           //},
           "path": "kurento",
           "threads": 10
         }
       }
     }
   }

In addition to this general configuration file, the specific features of KMS are tuned as individual modules. Each of these modules has its own configuration file:

- ``/etc/kurento/modules/kurento/MediaElement.conf.ini``: Generic parameters for Media Elements.
- ``/etc/kurento/modules/kurento/SdpEndpoint.conf.ini``: Audio/video parameters for *SdpEndpoints* (i.e. *WebRtcEndpoint* and *RtpEndpoint*).
- ``/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini``: Specific parameters for *WebRtcEndpoint*.
- ``/etc/kurento/modules/kurento/HttpEndpoint.conf.ini``: Specific parameters for *HttpEndpoint*.



Debug Logging
=============

Kurento Media Server generates log files that are stored in ``/var/log/kurento-media-server/``. The content of this folder is as follows:

- ``media-server_<timestamp>.<log_number>.<kms_pid>.log``: Output log of a currently running instance of KMS.
- ``media-server_error.log``: Errors logged by third-party libraries.
- ``logs``: Folder that contains older KMS logs. The logs in this folder are rotated, so it doesn't fill up all the space available in the disk.

Each line in a log produced by KMS has a fixed structure:

.. sourcecode:: text

   [timestamp] [pid] [memory] [level] [component] [filename:loc] [method] [message]

- ``[timestamp]``: Date and time of the logging message (e.g. ``2017-12-31 23:59:59,493295``).
- ``[pid]``: Process Identifier of *kurento-media-sever* (e.g. ``17521``).
- ``[memory]``: Memory address in which the *kurento-media-sever* component is running (e.g. ``0x00007fd59f2a78c0``).
- ``[level]``: Logging level. This value typically will be ``info`` and ``debug``. If unexpected error situations happen, the ``warning`` and ``error`` levels will contain information about the problem.
- ``[component]``: Kurento Media Server component name (e.g. ``KurentoModuleManager``, ``KurentoLoadConfig``, or ``KurentoMediaServer``, among others).
- ``[filename:loc]``: Source code file name (e.g. ``main.cp``) followed by the line of code (*loc*) number.
- ``[method]``: Name of the function in which the log message was generated (e.g. ``loadModule()``, ``doGarbageCollection()``, etc).
- ``[message]``: Specific log information.

For example, when KMS starts correctly, this trace is written in the log file:

.. sourcecode:: text

   [timestamp] [pid] [memory] info KurentoMediaServer main.cpp:255:main: Kurento Media Server started
