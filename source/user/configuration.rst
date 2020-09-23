===================
Configuration Guide
===================

Kurento works by orchestrating a broad set of technologies that must be made to work together. Some of these technologies can accept different configuration parameters that Kurento makes available through several configuration files:

- ``/etc/kurento/kurento.conf.json``: The main configuration file. Provides settings for the behavior of Kurento Media Server itself.
- ``/etc/kurento/modules/kurento/MediaElement.conf.ini``: Generic parameters for all kinds of *MediaElement*.
- ``/etc/kurento/modules/kurento/SdpEndpoint.conf.ini``: Audio/video parameters for *SdpEndpoint*s (i.e. *WebRtcEndpoint* and *RtpEndpoint*).
- ``/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini``: Specific parameters for *WebRtcEndpoint*.
- ``/etc/kurento/modules/kurento/HttpEndpoint.conf.ini``: Specific parameters for *HttpEndpoint*.
- ``/etc/default/kurento-media-server``: This file is loaded by the system's service init files. Defines some environment variables, which have an effect on features such as the *Debug Logging*, or the *Core Dump* files that are generated when a crash happens.



Media Server
============

File: ``/etc/kurento/kurento.conf.json``.

[TODO] Explain parameters.



MediaElement
============

File: ``/etc/kurento/modules/kurento/MediaElement.conf.ini``.

[TODO] Explain parameters.



SdpEndpoint
===========

File: ``/etc/kurento/modules/kurento/SdpEndpoint.conf.ini``.

[TODO] Explain parameters.



WebRtcEndpoint
==============

File: ``/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini``.

[TODO] Explain parameters.



HttpEndpoint
============

File: ``/etc/kurento/modules/kurento/HttpEndpoint.conf.ini``.

[TODO] Explain parameters.



Debug Logging
=============

File: ``/etc/default/kurento-media-server``.



Service Init
============

The package *kurento-media-server* provides a service file that integrates with the Ubuntu init system. This service file loads its user configuration from ``/etc/default/kurento-media-server``, where the user is able to configure several features as needed.
