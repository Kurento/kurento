%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
 Kurento Installation Guide
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

.. highlight:: bash

This guide describes how to install Kurento. Kurento is composed of different
nodes, the Kurento Media Server (:term:`KMS`), and the Kurento Media Connector
(:term:`KMC`). These three nodes can be installed in the same or different hosts.
This guide focuses on the installation on a single machine. For other
installations, please visit the :doc:`Advanced Installation Guide <Installation_Guide>`.

The hardware minimal recommended requirements are as follows:

-  8GB RAM
-  16GB HDD (this figure is not taking into account that multimedia
   streams could be stored in the same machine. If so, HDD size must be
   increased accordingly)

Regarding the operating system requirements:

-  Kurento Media Server (KMS) only runs on Ubuntu Linux (32 or 64 bits).
   It is highly recommended using version 14.04 due to the dependency
   of KMS with gstreamer.
-  Kurento Media Connector (KMC) can run in any platform that supports
   JDK version 7.


Kurento Media Server (KMS)
==========================

Install KMS by typing the following commands, one at a time and in the
same order as listed here. When asked for any kind of confirmation,
reply affirmatively:

.. sourcecode:: console

    $ sudo add-apt-repository ppa:kurento/kurento
    $ sudo apt-get update
    $ sudo apt-get upgrade
    $ sudo apt-get install libevent-dev kurento

Finally, configure the server to run KMS when booted:

.. sourcecode:: console

    $ sudo update-rc.d kurento defaults

Now, KMS has been installed and started. Use the following commands to start
and stop KMS respectively:

.. sourcecode:: console

    $ sudo service kurento start
    $ sudo service kurento stop

KMS logs are located at ``/var/log/kurento/media-server.log``.


Kurento Media Connector (KMC)
=============================

The *Kurento Media Connector (KMC)* is a proxy that allows to clients connect to KMS through websockets. The main KMS interface is based on thrift technology, and this proxy made necessary conversions between websockets and thrift.

Download KMC and move it to ``/opt/kmf-media-connector`` by executing:

.. sourcecode:: console

	$ sudo wget http://builds.kurento.org/release/stable/kmf-media-connector.zip
	$ sudo mkdir /opt/kmf-media-connector && sudo mv kmf-media-connector.zip /opt/kmf-media-connector
	$ sudo apt-get install unzip
	$ cd /opt/kmf-media-connector && sudo unzip kmf-media-connector.zip

Install KMC as a service using the following script:

.. sourcecode:: console

	$ sudo ./bin/install.sh

Finally, configure the server to run KMC when booted:

.. sourcecode:: console

	$ sudo update-rc.d kmf-media-connector defaults

Now KMC has been installed and started. Use the following commands to start/stop KMC:

.. sourcecode:: console

	$ sudo service kmf-media-connector start
	$ sudo service kmf-media-connector stop

KMC logs are located at ``/var/log/kurento/media-connector.log``.

