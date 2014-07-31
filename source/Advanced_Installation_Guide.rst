%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
 Kurento Advanced Installation Guide
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

.. highlight:: bash


Kurento Media Server (KMS)
==========================

In order to add Personal Package Archive or PPA's repositories, the
software-properties-commons package must be installed. This package is
part of Ubuntu Desktop, but it might not be included if you are using
a VM or a server:

.. sourcecode:: console

    $ sudo apt-get install software-properties-common

.. note:: Transient problem with Intel Graphics video

    There is a `packaging bug in libopencv-ocl2.4 Ubuntu 13.10
    <https://bugs.launchpad.net/ubuntu/+source/opencv/+bug/1245260>`_,
    which causes some installation problems [#]_. There is a workaround
    while the bug is fixed. If your computer has not a nvidia chipset,
    you need to install one package before kurento. Do:

    .. sourcecode:: console

        $ sudo apt-get install ocl-icd-libopencl1



Kurento Media Connector (KMC)
=============================

KMC can be configured by editing a plain Java properties file located at ``/etc/kurento/media-connector.properties``. The accepted parameters are:

- ``server.port`` : The http/websocket port of the proxy. This port will be used for the clients to connect to the port. If not specified, the value 8888 will be used.
- ``kmf.transport`` : Transport layer to connect with KMS. Accepted value at this moment: ``thrift``.
- ``thrift.kmf.address`` : The IP address and port of the KMS. If not specified, the address 127.0.0.1:9090 will be used.
- ``thrift.kmf.address`` : The IP address and port that KMS will use to connect to the proxy. If not specified, the address 127.0.0.1:9900 will be used.
- ``oauthserver.url`` : The url of the ouath service used to authenticate the client requests. If not specified, all clients can use the proxy (that is, no authentication is enforced).


Kurento Network Configuration
=============================

Running Kurento Without NAT configuration
-----------------------------------------

KMS can receive requests from the KMC and from final users. KMS uses a easily
extensible service abstraction layer that enables it to attend application
requests from either Thrift or RabbitMQ altough other services can also be
deployed on it. The service in charge of attending all those requests is
configured in the configuration file ``/etc/kurento/media-server.conf``.
After a fresh installation that file looks like this:

.. sourcecode:: ini
    [Server]
    sdpPattern=pattern.sdp
    service=Thrift

    [HttpEPServer]
    #serverAddress=localhost

    # Announced IP Address may be helpful under situations such as the server needs
    # to provide URLs to clients whose host name is different from the one the
    # server is listening in. If this option is not provided, http server will try
    # to look for any available address in your system.
    # announcedAddress=localhost

    serverPort=9091

    [WebRtcEndPoint]
    #stunServerAddress = xxx.xxx.xxx.xxx
    #stunServerPort = xx
    #pemCertificate = file

    [Thrift]
    serverPort=9090

    [RabbitMQ]
    serverAddress = 127.0.0.1
    serverPort = 5672
    username = "guest"
    password = "guest"
    vhost = "/"

That configuration implies that only requests done through Thrift are
accepted. By default Thrift server will be attached in all available network
interfaces. The section ``[Thrift]`` allows to configure the port where KMS
will listen to KMC requests. The section ``[HttpEPServer]`` controls the IP
address and port to listen to the final users.

Running Kurento With NAT configuration
--------------------------------------

.. figure:: images/Kurento_nat_deployment.png
   :align:   center
   :alt:     Network with NAT

   Kurento deployment in a configuration with NAT

This network diagram depicts a scenario where a :term:`NAT` device is
present. In this case, the client will access the public IP 130.206.82.56,
which will connect him with the external interface of the NAT device.
KMS serves media on a specific address which, by default, is the IP of
the server where the service is running. This would have the server
announcing that the media served by an Http Endpoint can be consumed in
the private IP 172.30.1.122. Since this address is not accessible by
external clients, the administrator of the system will have to configure
KMS to announce, as connection address for clients, the public IP of the
NAT device. This is achieved by changing the value of announcedAddress
in the file /etc/kurento/media-server.conf with the appropriate value.
The following lines would be the contents of this configuration file for
the present scenario.

.. sourcecode:: ini

    [Server]
    serverAddress=localhost
    serverPort=9090
    sdpPattern=pattern.sdp

    [HttpEPServer]
    #serverAddress=localhost

    # Announced IP Address may be helpful under situations such as the server needs
    # to provide URLs to clients whose host name is different from the one the
    # server is listening in. If this option is not provided, http server will try
    # to look for any available address in your system.
    announcedAddress=130.206.82.56

    serverPort=9091

    [WebRtcEndPoint]
    #stunServerAddress = xxx.xxx.xxx.xxx
    #stunServerPort = xx
    #pemCertificate = file


Verifying KMS and KMC
=====================

List of Running Processes
-------------------------

To verify that KMS/KMC is up and running use the command:

.. sourcecode:: console

    $ ps -ef | grep kurento

The output should be similar to:

.. sourcecode:: console
	nobody    1494     1  0 13:00 ?        00:01:16 java -server -XX:+UseCompressedOops -XX:+TieredCompilation -jar /var/lib/kurento/kmf-media-connector.jar --spring.config.location=/etc/kurento/media-connector.properties
    nobody   22527     1  0 13:02 ?        00:00:00 /usr/bin/kurento
    kuser    22711  2326  0 13:10 pts/1    00:00:00 grep --color=auto kurento

Network interfaces Up & Open
----------------------------

Unless configured otherwise, Kurento will open the following ports:

-  KMS opens port 9091 to receive HTTP TCP requests from KMC and final users.
   KMS also opens the port 9090 to receive Thrift TCP requests from KMC.
-  KMC opens the port 8888 to receive HTTP TCP requests from final users.
   KMC also opens port 9900 to receive Thrift TCP requests from the KMS.

Ports 9091, and 8888 should be accessible from final users. Therefore these
ports should be open and forwarded on existing network elements, such as NAT
or Firewall.

To verify the ports opened by KMS execute the following command:

.. sourcecode:: console

    $ sudo netstat -putan | grep kurento

The output should be similar to the following:

.. sourcecode:: console

	tcp        0      0 0.0.0.0:9091            0.0.0.0:*               LISTEN      8752/kurento    
	tcp6       0      0 :::9090                 :::*                    LISTEN      8752/kurento 

To verify the ports opened by KMC execute the following command:

.. sourcecode:: console

    $ sudo netstat -putan | grep java

The output should be similar to the following:

.. sourcecode:: console

	tcp6       0      0 :::8888                 :::*                    LISTEN      21243/java      
	tcp6       0      0 127.0.0.1:9900          :::*                    LISTEN      21243/java    



.. rubric:: Footnotes

.. [#]

    The reason is that kurento uses :term:`openCV` and needs some resources
    from ``libopencv-dev``, which depends on ``libopencv-ocl2.4``, which depends
    on the virtual ``<libopencl1>``, that can be provided by either
    ``ocl-icd-libopencl1`` or one of the ``nvidia-*`` packages. If your machine
    has a nvidia chipset you should already have libopencl1, if not, it is better
    to install ocl-icd-libopencl1, as the nvidia packages sometimes break
    OpenGL and nowadays most linux desktops need a working OpenGL. The problem is
    further complicated because ``ocl-icd-libopencl1`` conflicts with the
    nvida packages.