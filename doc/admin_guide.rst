FIWARE Stream Oriented Generic Enabler - Installation and Administration Guide
______________________________________________________________________________

Introduction
============

This guide describes how to install the Stream-Oriented GE - Kurento. Kurento’s
core element is the **Kurento Media Server** (KMS), responsible for media
transmission, processing, loading and recording. It is implemented in low level
technologies based on GStreamer to optimize the resource consumption.

Requirements
------------

To guarantee the right working of the enabler RAM memory and HDD size should be
at least:

-   4 GB RAM
-   16 GB HDD (this figure is not taking into account that multimedia
    streams could be stored in the same machine. If so, HDD size must be
    increased accordingly)

Operating System
----------------

Kurento Media Server has to be installed on Ubuntu 14.04 LTS (64 bits).

Dependencies
------------

If end-to-end testing is going to be performed, the following tools must be also
installed in your system (Ubuntu):

- `Open JDK 7 <http://openjdk.java.net/projects/jdk7/>`__:

::

	sudo apt-get install openjdk-7-jdk

- `Git <http://git-scm.com/>`__:

::

	sudo apt-get install git

- `Chrome <https://www.google.com/chrome/browser/>`__ (latest stable version):

::

	sudo apt-get install libxss1
	wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
	sudo dpkg -i google-chrome*.deb

- `Maven <http://maven.apache.org/>`__:

::

	sudo apt-get install maven


Installation
============

In order to install the latest stable Kurento Media Server version you have to
type the following commands, one at a time and in the same order as listed
here. When asked for any kind of confirmation, reply affirmatively:

::

	echo "deb http://ubuntu.kurento.org trusty kms6" | sudo tee /etc/apt/sources.list.d/kurento.list
	wget -O - http://ubuntu.kurento.org/kurento.gpg.key | sudo apt-key add -
	sudo apt-get update
	sudo apt-get install kurento-media-server-6.0

After running these command, Kurento Media Server should be installed and
started.

Configuration
=============

The main Kurento Media Server configuration file is located in
`/etc/kurento/kurento.conf.json`. After a fresh installation this file is the
following:

::

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

As of Kurento Media Server version 6, in addition to this general configuration
file, the specific features of KMS are tuned as individual modules. Each of
these modules has its own configuration file:

-   `/etc/kurento/modules/kurento/MediaElement.conf.ini`: Generic parameters
    for Media Elements.
-   `/etc/kurento/modules/kurento/SdpEndpoint.conf.ini`: Audio/video
    parameters for SdpEndpoints (i.e. `WebRtcEndpoint` and `RtpEndpoint`).
-   `/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini`: Specific
    parameters for `WebRtcEndpoint`.
-   `/etc/kurento/modules/kurento/HttpEndpoint.conf.ini`: Specific
    parameters for `HttpEndpoint`.


If Kurento Media Server is located behind a NAT you need to use a
`STUN <https://en.wikipedia.org/wiki/STUN>`__ or
`TURN <https://en.wikipedia.org/wiki/Traversal_Using_Relays_around_NAT>`__ in
order to achieve
`NAT traversal <https://en.wikipedia.org/wiki/NAT_traversal>`__. In most of
cases, a STUN server will do the trick. A TURN server is only necessary when
the NAT is symmetric.

In order to setup a STUN server you should uncomment the following lines in the
Kurento Media Server configuration file located on at
`/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini`:

::

	stunServerAddress=<stun_ip_address>
	stunServerPort=<stun_port>

The parameter `stunServerAddress` should be an IP address (not domain name).
There is plenty of public STUN servers available, for example:

::

	173.194.66.127:19302
	173.194.71.127:19302
	74.125.200.127:19302
	74.125.204.127:19302
	173.194.72.127:19302
	74.125.23.127:3478
	77.72.174.163:3478
	77.72.174.165:3478
	77.72.174.167:3478
	77.72.174.161:3478
	208.97.25.20:3478
	62.71.2.168:3478
	212.227.67.194:3478
	212.227.67.195:3478
	107.23.150.92:3478
	77.72.169.155:3478
	77.72.169.156:3478
	77.72.169.164:3478
	77.72.169.166:3478
	77.72.174.162:3478
	77.72.174.164:3478
	77.72.174.166:3478
	77.72.174.160:3478
	54.172.47.69:3478

In order to setup a TURN server you should uncomment the following lines in the
Kurento Media Server configuration file located on at
`/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini`:

::

	turnURL=user:password@address:port

As before, TURN address should be an IP address (not domain name). See some
examples of TURN configuration below:

::

	turnURL=kurento:kurento@193.147.51.36:3478

... or using a free access numb STUN/TURN server as follows:

::

	turnURL=user:password@66.228.45.110:3478

An open source implementation of a TURN server is
`coturn <https://code.google.com/p/coturn/>`__.

Sanity check Procedures
=======================

End to End testing
------------------

Kurento Media Server must be installed and started before running the following
example, which is called `magic-mirror` and it is developed with the
`Kurento Java Client`. You should run this example in a machine with camera and
microphone since live media is needed. To launch the application first you need
to clone the GitHub project where it is hosted and then run the main class, as
follows:

::

	git clone https://github.com/Kurento/kurento-tutorial-java.git
	cd kurento-tutorial-java/kurento-magic-mirror
	mvn compile exec:java

These commands starts an HTTP server at the localhost in the port 8080.
Therefore, please open the web application connecting to the URL
http://localhost:8080/ through a WebRTC capable browser (e.g. Chrome). Click on
the `Start` button and grant the access to the camera and microphone. After the
SDP negotiation an enhanced video mirror should start. Kurento Media Server is
processing media in real time, detecting faces and overlying an image on the
top of them. This is a simple example of augmented reality in real time with
Kurento.

Take into account that this setup is assuming that port TCP 8080 is available in
your system. If you would like to use another one, simply launch the demo as
follows:

::

	mvn compile exec:java -Dserver.port=<custom-port>

... and open the application on http://localhost:custom-port/.

List of Running Processes
-------------------------

To verify that Kurento Media Server is up and running use the command:

::

	ps -ef | grep kurento

The output should include the kurento-media-server process:

::

	nobody    1270     1  0 08:52 ?        00:01:00 /usr/bin/kurento-media-server

Network interfaces Up & Open
----------------------------

Unless configured otherwise, Kureno Media Server will open the port TCP 8888 to
receive requests and send responses to/from by means of the Kurento clients (by
means of the Kurento Protocol Open API). To verify if this port is listening,
execute the following command:

::

	sudo netstat -putan | grep kurento

The output should be similar to the following:

::

	tcp6      0      0 :::8888      :::*      LISTEN      1270/kurento-media-server

Diagnosis Procedures
====================

Resource consumption
--------------------

Resource consumption documented in this section has been measured in two
different scenarios:

-   Low load: all services running, but no stream being served.
-   High load: heavy load scenario where 20 streams are requested at the
    same time.

Under the above circumstances, the `top` command showed the following results in
the hardware described below:

+----------------------+------------------------------------------+
| **Machine Type**     | Physical Machine                         |
+----------------------+------------------------------------------+
| **CPU**              | Intel(R) Core(TM) i5-3337U CPU @ 1.80GHz |
+----------------------+------------------------------------------+
| **RAM**              | 16 GB                                    |
+----------------------+------------------------------------------+
| **HDD**              | 500 GB                                   |
+----------------------+------------------------------------------+
| **Operating System** | Ubuntu 14.04                             |
+----------------------+------------------------------------------+

Kurento Media Server gave the following result:

+---------+---------------+-----------------+
|         | **Low Usage** | **Heavy Usage** |
+---------+---------------+-----------------+
| **CPU** | 0.0 %         | 76.9 %          |
+---------+---------------+-----------------+
| **RAM** | 81.92 MB      | 655.36 MB       |
+---------+---------------+-----------------+

I/O flows
---------

Use the following commands to start and stop Kurento Media Server respectively:

::

	sudo service kurento-media-server-6.0 start
	sudo service kurento-media-server-6.0 stop

Kurento Media Server logs file are stored in the folder
`/var/log/kurento-media-server/`. The content of this folder is as follows:

-   `media-server\_<timestamp>.<log_number>.<kms_pid>.log`: Current log for
    Kurento Media Server
-   `media-server\_error.log`: Third-party errors
-   `logs`: Folder that contains the KMS rotated logs

When KMS starts correctly, this trace is written in the log file:

::

	[time] [0x10b2f880] [info]    KurentoMediaServer main.cpp:239 main() Mediaserver started

