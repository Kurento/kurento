%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Kurento Media Server Advanced Installation guide
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

Kurento Media Server Configuration
==================================

The KMS configuration file is located in ``/etc/kurento/kurento.conf.json``.
After a fresh installation this file is the following:

.. sourcecode:: js

   {
     "mediaServer" : {
       "net" : {
         // Uncomment just one of them
         /*
         "rabbitmq": {
           "address" : "127.0.0.1",
           "port" : 5672,
           "username" : "guest",
           "password" : "guest",
           "vhost" : "/"
         }
         */
         "websocket": {
           "port": 8888,
           //"secure": {
           //  "port": 8433,
           //  "certificate": "defaultCertificate.pem",
           //  "password": ""
           //},
           "path": "kurento",
           "threads": 10
         }
       }
     },
     "modules": {
       "kurento": {
         "SdpEndpoint" : {
           "sdpPattern" : "sdp_pattern.txt"
         },
         "HttpEndpoint" : {
           // "serverAddress" : "localhost",
           /*
             Announced IP Addess may be helpful under situations such as the server needs
             to provide URLs to clients whose host name is different from the one the
             server is listening in. If this option is not provided, http server will try
             to look for any available address in your system.
           */
           // "announcedAddress" : "localhost"
         },
         "WebRtcEndpoint" : {
           // "stunServerAddress" : "stun ip address",
           // "stunServerPort" : 3478,
           // turnURL gives the necessary info to configure TURN for WebRTC.
           //    'address' must be an IP (not a domain).
           //    'transport' is optional (UDP by default).
           // "turnURL" : "user:password@address:port(?transport=[udp|tcp|tls])",
           // "pemCertificate" : "file"
         },
         "PlumberEndpoint" : {
           // "bindAddress" : "localhost",
           /*
             Announced IP Address may be helpful under situations such as the endpoint needs
             to provide an IP address to clients whose host name is different from the one
             that the element is listening in. If this option is not provided, the bindAddress
             will be used instead.
           */
           // "announcedAddress" : "localhost"
         }
       }
       //"module1": { …. }
       //"module2": { …. }
     }
   }


Kurento Media Server behind a NAT
=================================

KMS can be installed on a private network behind a router with :term:`NAT`. The
picture below shows the typical scenario.

.. figure:: ../images/Kurento_nat_deployment.png
   :align: center
   :alt: Typical scenario of Kurento Media Server behind a NAT

   *Typical scenario of Kurento Media Server behind a NAT*


In this case, KMS should announce the router public IP in order to be reachable
from the outside. In the example example, sections ``HttpEndpoint`` and
``PlumberEndpoint`` within ``/etc/kurento/kurento.conf.json`` should be
configured as follows:

.. sourcecode:: js

   {
     "mediaServer" : {
       "net" : {
         // Uncomment just one of them
         /*
         "rabbitmq": {
           "address" : "127.0.0.1",
           "port" : 5672,
           "username" : "guest",
           "password" : "guest",
           "vhost" : "/"
         }
         */
         "websocket": {
           "port": 8888,
           //"secure": {
           //  "port": 8433,
           //  "certificate": "defaultCertificate.pem",
           //  "password": ""
           //},
           "path": "kurento",
           "threads": 10
         }
       }
     },
     "modules": {
       "kurento": {
         "SdpEndpoint" : {
           "sdpPattern" : "sdp_pattern.txt"
         },
         "HttpEndpoint" : {
           // "serverAddress" : "localhost",
           /*
             Announced IP Addess may be helpful under situations such as the server needs
             to provide URLs to clients whose host name is different from the one the
             server is listening in. If this option is not provided, http server will try
             to look for any available address in your system.
           */
           "announcedAddress" : "130.206.82.56"
         },
         "WebRtcEndpoint" : {
           // "stunServerAddress" : "stun ip address",
           // "stunServerPort" : 3478,
           // turnURL gives the necessary info to configure TURN for WebRTC.
           //    'address' must be an IP (not a domain).
           //    'transport' is optional (UDP by default).
           // "turnURL" : "user:password@address:port(?transport=[udp|tcp|tls])",
           // "pemCertificate" : "file"
         },
         "PlumberEndpoint" : {
           // "bindAddress" : "localhost",
           /*
             Announced IP Address may be helpful under situations such as the endpoint needs
             to provide an IP address to clients whose host name is different from the one
             that the element is listening in. If this option is not provided, the bindAddress
             will be used instead.
           */
           "announcedAddress" : "130.206.82.56"
         }
       }
       //"module1": { …. }
       //"module2": { …. }
     }
   }


Verifying Kurento Media Server installation
===========================================

Kurento Media Server Process
----------------------------

To verify that KMS is up and running use the command:

.. sourcecode:: sh

    ps -ef | grep kurento

The output should include the ``kurento-media-server`` process:

.. sourcecode:: sh

   nobody    1270     1  0 08:52 ?        00:01:00 /usr/bin/kurento-media-server


WebSocket Port
--------------

Unless configured otherwise, KMS will open the port **8888** to receive requests
and send responses to/from by means of the
:doc:`Kurento Protocol<kurento_protocol>`. To verify if this port is listening
execute the following command:

.. sourcecode:: sh

    sudo netstat -putan | grep kurento

The output should be similar to the following:

.. sourcecode:: sh

   tcp6    0    0 :::8888    :::*    LISTEN    1270/kurento-media-server


Kurento Media Server Log
------------------------

KMS has a log file located at
``/var/log/kurento-media-server/media-server.log``. You can check it for
example as follows:

.. sourcecode:: sh

   tail -f /var/log/kurento-media-server/media-server.log

When KMS starts correctly, this trace is written in the log file:

.. sourcecode:: sh

   0:00.4011 8003 0x26cb130 INFO KurentoMediaServer /.../main.cpp:194:main: Mediaserver started

