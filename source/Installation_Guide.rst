%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
 Kurento Installation Guide
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

.. highlight:: bash

Introduction
============

This guide describes how to install Kurento. Kurento is composed of three
nodes, the Kurento Application Server (:term:`KAS`), the Kurento Media
Server (:term:`KMS`) and the KFM Media Connector (:term:`KMC`). These three
nodes can be co-located or installed on separate machines. This guide
focuses on the installation on a single machine, but comments are done
where appropriate explaining how to modify the configuration files for
a dual installation.

Prerequisites
-------------

Hardware minimal recommended requirements:

-  8GB RAM
-  16GB HDD (this figure is not taking into account that multimedia
   streams could be stored in the same machine. If so, HDD size must be
   increased accordingly)

Operating system requirements:

-  Currently Kurento Media Server (KMS) only runs on Ubuntu Linux 13.10
   64 Bits or newer. This is due to its dependency of the gstreamer
   1.2.0 version.
-  Kurento Application Server (KAS) can run in any platform that
   supports Open JDK version 7

Kurento Application Server (KAS)
--------------------------------

First, install *Open JDK 7* and  *unzip* packages:

.. sourcecode:: console

    $ sudo apt-get install openjdk-7-jdk unzip

Download *JBoss*, uncompress it and move it to */opt/jboss* by
executing:

.. sourcecode:: console

    $ sudo wget http://download.jboss.org/jbossas/7.1/jboss-as-7.1.1.Final/jboss-as-7.1.1.Final.tar.gz
    $ sudo tar xfvz jboss-as-7.1.1.Final.tar.gz && sudo mv jboss-as-7.1.1.Final /opt/jboss

To avoid running JBoss as root create the user *jboss*, the group
*jboss* and make that user the owner of JBoss files and folders:

.. sourcecode:: console

    $ sudo adduser --system jboss && sudo addgroup jboss
    $ sudo chown -R jboss:jboss /opt/jboss/

Create the startup/stop script by copying the following content to a new
file called */etc/init.d/jboss7*:

.. sourcecode:: sh

    #! /bin/sh
    ### BEGIN INIT INFO
    # Provides:          jboss
    # Required-Start:    kurentod
    # Required-Stop:
    # Default-Start:     2 3 4 5
    # Default-Stop:      0 1 6
    # Short-Description: JBoss Application Server
    # Description:       init script for JBoss Application Server
    ### END INIT INFO

    PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin
    NAME="jboss" 
    JBOSS_HOME="/opt/jboss" 
    DAEMON="$JBOSS_HOME/bin/standalone.sh" 
    SHUTDOWN_CMD="$JBOSS_HOME/bin/jboss-cli.sh" 
    DAEMON_USER=jboss

    PIDFILE=/var/run/$NAME.pid
    SCRIPTNAME=/etc/init.d/$NAME
    DESC="JBoss AS Server" 

    if [ -r "/lib/lsb/init-functions" ]; then
      . /lib/lsb/init-functions
    else
      echo "E: /lib/lsb/init-functions not found, package lsb-base needed" 
      exit 1
    fi

    # Include defaults if available
    if [ -f /etc/default/jboss7 ] ; then
        . /etc/default/jboss7
    fi

    verify_user () {
    # Only root can start Kurento
        if [ `id -u` -ne 0 ]; then
            log_failure_msg "Only root can start JBoss" 
            exit 1
        fi
    }

    if [ "$START_JBOSS" != "true" ]; then
        log_failure_msg "Review activate settings within file /etc/default/jboss7" 
        exit 1
    fi

    if [ ! -e $JBOSS_HOME ]; then
         log_failure_msg "Unable to access JBoss home directory at: $JBOSS_HOME" 
         exit 1
    fi

    #[ -z "$BIND_IP" ] && BIND_IP=12.0.0.1
    #[ -n "$DAR_PATH" ] && DAR_PATH="-Djavax.servlet.sip.dar=file://$DAR_PATH" 

    JBOSS_OPTS="$JBOSS_OPTS -Djboss.bind.address=0.0.0.0 -Djboss.bind.address.management=0.0.0.0" 

    case "$1" in
          start)
              log_daemon_msg "Starting $DESC" "$NAME" 
                    verify_user

              # Verify pid file directory exists
              if [ ! -e /var/run ]; then
                   install -d -m755 /var/run ||\
                         { log_failure_msg "Unable to access /var/run directory"; exit 1; }
              fi
              # Make sure HOME directory belongs to $DAEMON_USER
              sudo -u $DAEMON_USER -H [ -O $JBOSS_HOME/standalone/log ]
              if [ $? != 0 ]; then
                   chown -R $DAEMON_USER $JBOSS_HOME/* ||\
                         { log_failure_msg "Unable to access $JBOSS_HOME"; exit 1; }
              fi

              /sbin/start-stop-daemon --start --pidfile $PIDFILE \
                        --chuid $DAEMON_USER --chdir $JBOSS_HOME/bin --background --make-pidfile\
                        --no-close --startas $DAEMON -- $JBOSS_OPTS > /dev/null
              log_end_msg $?
              ;;

           stop)
                log_daemon_msg "Stopping $DESC" "$NAME" 
                # This will just kill the standalone script. Java process detaches :(
                /sbin/start-stop-daemon --stop --quiet --pidfile $PIDFILE \
                     --chuid $DAEMON_USER --startas $DAEMON
                if [ $? -eq 0 ]; then
                   # Send kill command to JBoss
                   $SHUTDOWN_CMD --connect command=:shutdown
                   rm -f $PIDFILE
                   log_end_msg 0
                fi
                ;;

          restart|force-reload)
                echo -n "Restarting $DESC: $NAME" 
                /sbin/start-stop-daemon --stop --quiet --pidfile $PIDFILE \
                        --exec $DAEMON
                rm -f $PIDFILE
                sleep 1
                echo -e
                $0 start
                ;;
          *)
                echo "Usage: $0 {start|stop|restart|force-reload}" >&2
                exit 1
                ;;
    esac

    exit 0

Grant *jboss* user *execution* rights to run the startup/stop script:

.. sourcecode:: console

    $ sudo chmod 755 /etc/init.d/jboss7

Create the file */etc/default/jboss7* with the following content (this
file is used by the startup/stop script):

.. sourcecode:: sh

    # Defaults for JBoss7 initscript
    # sourced by /etc/init.d/jboss7
    # installed at /etc/default/jboss7 by the maintainer scripts

    #
    # This is a POSIX shell fragment
    #

    #uncommment the next line to allow the init.d script to start jboss
    START_JBOSS=true

    # Additional options that are passed to the service.
    BIND_IP=0.0.0.0
    JBOSS_OPTS="" 

    # whom the daemons should run as
    JBOSS_USER=jboss

Finally, configure the server to run JBoss when booted:

.. sourcecode:: console

    $ sudo update-rc.d jboss7 defaults

Kurento Media Server (KMS)
--------------------------

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


Install KMS by typing the following commands, one at a time and in the
same order as listed here. When asked for any kind of confirmation,
reply afirmatively:

.. sourcecode:: console

    $ sudo add-apt-repository ppa:kurento/kurento
    $ sudo apt-get update
    $ sudo apt-get upgrade
    $ sudo apt-get install kurento

Finally, configure the server to run KMS when booted:

.. sourcecode:: console

    $ sudo update-rc.d kurento defaults


KMF Media Connector
-------------------

The KMF Media Connector is a proxy that allows to clients connect to the
Kurento Media Server through websockets. The main Kurento Media Server
interface is based on thrift technology, and this proxy makes the needed
conversions between websockets and thrift.

Kurento Network Configuration
-----------------------------

Running Kurento Without NAT configuration
=========================================

KMS can receive requests from the Kurento Application Server (KAS) and
from final users. KMS uses a easily extensible service abstraction layer
that enables it to attend application requests from either Thrift or
RabbitMQ altough other services can also be deployed on it.
The service in charge of attending all those requests is configured in the
configuration file ``/etc/kurento/kurento.conf``.
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
accepted. By default Thrift server will be attached in all availables network
interfaces. The section ``[Thrift]`` allows to configure the port where KMS
will listen to KAS requests. The section ``[HttpEPServer]`` controls the IP
address and port to listen to the final users.

Running Kurento With NAT configuration
======================================


.. figure:: images/Kurento_nat_deployment.png
   :align:   center
   :alt:     Network with NAT

   Kurento deployment in a configuration with NAT

This network diagram depicts a scenario where a :term:`NAT` device is
present. In this case, the client will access the public IP 130.206.82.56,
which will connect him with the external intertface of the NAT device.
KMS serves media on a specific address which, by default, is the IP of
the server where the service is running. This would have the server
announcing that the media served by an Http Endpoint can be consumed in
the private IP 172.30.1.122. Since this address is not accessible by
external clients, the administrator of the system will have to confgure
KMS to announce, as connection addres for clients, the public IP of the
NAT device. This is acheived by changing the value of announcedAddress
in the file /etc/kurento/kurento.conf with the appropriate value.
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


Sample application and videos
-----------------------------

To test part of the functionality of Kurento, a sample app called
fi-lab-demo can be used. Next steps in this document focus on how to
download the sample app and the complementary video files that are
needed.

Download the test video with the following commands:

::

    $ sudo wget https://ci.kurento.com/video/video.tar.gz --no-check-certificate 
    $ sudo tar xfvz video.tar.gz && sudo mv video/ /opt/video &&\
    > sudo chown -R jboss:jboss /opt/video

And downlad the fi-lab-demo.war file using the following command:

::

    $ sudo wget https://ci.kurento.com/apps/fi-lab-demo.war --no-check-certificate 
    $ sudo mv fi-lab-demo.war /opt/jboss/standalone/deployments &&\
    > sudo chown -R jboss:jboss /opt/jboss/standalone/deployments/fi-lab-demo.war

Verifying and starting the servers
----------------------------------

To verify that the installation has finished successfully start JBoss by
typing:

::

    $ sudo /etc/init.d/jboss7 start

Open a browser and verify that the default root web page work properly:

::

    http://<Service_IP_address>:8080/

To verify that the installation has finished successfully start KMS by
typing:

::

    $ sudo /etc/init.d/kurento start

Sanity check procedures
=======================

The Sanity Check Procedures are the steps that a System Administrator
will take to verify that an installation is ready to be tested. This is
therefore a preliminary set of tests to ensure that obvious or basic
malfunctioning is fixed before proceeding to unit tests, integration
tests and user validation.

End to End testing
------------------

Open a Chrome or Firefox web browser and type the URL:

::

    http://<Replace_with_KMS_IP_Address>:8080/fi-lab-demo/

This will show the web page of the fi-lab-demo sample application. From
this web page you can view two links:

HTTP Player
~~~~~~~~~~~

If you click on this link you can see a drop-down control in the top of
the web page. This drop-down show you the different media formats used
in this demo. Please select one and click over the Play button:

-  WEBM video: After clicking over the "Play" button you can see a short
   film of “Sintel”, independently produced by the Blender Foundation.
-  MOV video: After clicking over the "Play" button you can see a short
   film of “Big Buck Bunny”, independently produced by the Blender
   Foundation.
-  MKV video: After clicking over the "Play" button you can see a short
   film of Japanese animation.
-  3GP video: After clicking over the "Play" button you can see a short
   tv ad of Blackberry mobile phones.
-  OGV video:After clicking over the "Play" button you can see a short
   video of Pacman.
-  MP4 video: After clicking over the "Play" button you can see a short
   tv ad of Google Chrome.
-  JackVader Filter video: After clicking over the "Play" button you can
   see a video showing the use of filters, in this video a overlayed
   "pirate hat" is used when a face is detected in the right side of the
   screen and "Dark Vader mask" is used when a face is detected in the
   left side of the screen.

HTTP Player with JSON protocol
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This link will load another web page in your browser where you can see
the same videos using JSON-based representations for information
exchange.The JSON protocol enhances a HTTP Player by implementing
signaling communications between the client (:term:`JavaScript API <KWS>`) and the
Kurento Application Server (:term:`KAS`). Using this protocol the client will be
able to negotiate the transfer of media using :term:`SDP` (Session Description
Protocol), and also it will be notified with media and flow execution
events.

Select one of the videos from the drop-down control located in the top
of the web page.

-  WEBM video: After clicking over the "Play" button you can see a short
   film of “Sintel”, independently produced by the Blender Foundation.

-  MOV video: After clicking over the "Play" button you can see a short
   film of “Big Buck Bunny”, independently produced by the Blender
   Foundation.

-  MKV video: After clicking over the "Play" button you can see a short
   film of Japanese animation.

-  3GP video: After clicking over the "Play" button you can see a short
   tv ad of Blackberry mobile phones.

-  OGV video:After clicking over the "Play" button you can see a short
   video of Pacman.

-  MP4 video: After clicking over the "Play" button you can see a short
   tv ad of Google Chrome.

-  JackVader Filter video: After clicking over the "Play" button you can
   see a video showing the use of filters, in this video a overlayed
   "pirate hat" is used when a face is detected in the right side of the
   screen and "Dark Vader mask" is used when a face is detected in the
   left side of the screen.

-  ZBar Filer video:After clicking over the "Play" button you can see a
   video to show the potential of filters. In this video three QR Codes
   are shown, in the media event text box you can see how the media
   server detects the different QR codes.

In the text boxes Status, Flow Events and Media Events you can see the
results of the different actions that are interpreted by the media
server.

List of Running Processes
-------------------------

To verify that KAS is up and running type the following:

.. sourcecode:: console

    $ ps -ef | grep jboss

The output should be similar to:

.. sourcecode:: console

    jboss     4115     1  0 15:16 ?        00:00:00 /bin/sh /opt/jboss/bin/standalone.sh -Djboss.bi
    nd.address=0.0.0.0 -Djboss.bind.address.management=0.0.0.0
    jboss     4159  4115 30 15:16 ?        00:00:08 java -D[Standalone] -server -XX:+UseCompressedO
    ops -XX:+TieredCompilation -Xms64m -Xmx512m -XX:MaxPermSize=256m -Djava.net.preferIPv4Stack=tru
    e -Dorg.jboss.resolver.warning=true -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.serve
    r.gcInterval=3600000 -Djboss.modules.system.pkgs=org.jboss.byteman -Djava.awt.headless=true -Dj
    boss.server.default.config=standalone.xml -Dorg.jboss.boot.log.file=/opt/jboss/standalone/log/b
    oot.log -Dlogging.configuration=file:/opt/jboss/standalone/configuration/logging.properties -ja
    r /opt/jboss/jboss-modules.jar -mp /opt/jboss/modules -jaxpmodule javax.xml.jaxp-provider org.j
    boss.as.standalone -Djboss.home.dir=/opt/jboss -Djboss.bind.address=0.0.0.0 -Djboss.bind.addres
    s.management=0.0.0.0
    kuser     4256  2371  0 15:16 pts/0    00:00:00 grep --color=auto jboss

To verify that KMS is up and running use the command:

.. sourcecode:: console

    $ ps -ef | grep kurento

The output should be similar to:

.. sourcecode:: console

    nobody   22527     1  0 13:02 ?        00:00:00 /usr/bin/kurento
    kuser    22711  2326  0 13:10 pts/1    00:00:00 grep --color=auto kurento

Network interfaces Up & Open
----------------------------

Unless configured otherwise, KAS listens on the port 8080 to receive
HTTP requests from final users. It additionally opens port 9990, a
handler port which is used by KMS to send events to KAS.

To verify the ports opened by KAS execute the following command:

.. sourcecode:: console

    $ sudo netstat -putan | grep java

The output should be similar to the following:

.. sourcecode:: console

    tcp        0      0 0.0.0.0:4447            0.0.0.0:*               LISTEN      4424/java       
    tcp        0      0 0.0.0.0:9990            0.0.0.0:*               LISTEN      4424/java       
    tcp        0      0 0.0.0.0:9999            0.0.0.0:*               LISTEN      4424/java       
    tcp        0      0 0.0.0.0:8080            0.0.0.0:*               LISTEN      4424/java       

The two additional ports listened are 4447, jBoss remoting port, and
9999, a port for jBoss native management interface.

Unless configured otherwise, KMS opens the port 9090 to receive HTTP TCP
requests from KAS and port 9091 for HTTP TCP requests from final users.
To verify the open ports type the command:

.. sourcecode:: console

    $ sudo netstat -putan | grep kurento

The output should be similar to the following:

.. sourcecode:: console

    tcp        0      0 127.0.0.1:9091          0.0.0.0:*               LISTEN      22527/kurento  
    tcp6       0      0 :::9090                 :::*                    LISTEN      22527/kurento

Databases
---------

N/A

Diagnosis Procedures
====================

The Diagnosis Procedures are the first steps that a System Administrator
will take to locate the source of an error in a GE. Once the nature of
the error is identified with these tests, the system admin will very
often have to resort to more concrete and specific testing to pinpoint
the exact point of error and a possible solution. Such specific testing
is out of the scope of this section.

Resource availability
---------------------

To guarantee the right working of the enabler RAM memory and HDD size
shoud be at least:

-  8GB RAM
-  16GB HDD (this figure is not taking into account that multimedia
   streams could be stored in the same machine. If so, HDD size must be
   increased accordingly)

Remote Service Access
---------------------

If KMS and KAS are deployed as separate GEs, the admin needs to ensure
that the KMS GE can reach the KAS Handler port (default 9990) and that
the KAS GE can reach the KMS service port (default 9090)

Resource consumption
--------------------

Resource consumption documented in this section has been measured in two
different scenarios:

-  Low load: all services running, but no stream being served.
-  High load: heavy load scenario where 100 streams are requested at the
   same time.

Under the above circumstances, the "top" command showed the following
results in the hardware described below:


.. table:: Machines used for performance testing

    ==================== =========================== ============
    Machine Info         KAS                         KMS
    ==================== =========================== ============
        Machine Type     Virtual Machine             Physical Machine
    -------------------- --------------------------- ------------
            CPU          1 Intel Core 2 Duo @ 2,4Ghz Intel(R) Xeon(R) CPU E5-2620 0 @ 2GHz
            RAM          4GB                         4GB
            HDD          250GB                       10GB
      Operating System   Mac OS X 10.6.8             Ubuntu 13.10
    ==================== =========================== ============


.. table:: Resource usage of Kurento Application Server

    ======== ============ ============
    KAS      Low Usage    Heavy Usage
    ======== ============ ============
    RAM      96MB         200,6MB
    -------- ------------ ------------
    CPU      0.2%         44.9%
    I/O HDD	 1.44GB       1.69GB
    ======== ============ ============


.. table:: Resource usage of Kurento Media Server

    ======== ============ ============
    KMS      Low Usage    Heavy Usage
    ======== ============ ============
    RAM      122.88MB     1.56GB
    -------- ------------ ------------
    CPU      0.3%         34.6%
    I/O HDD	 1.18GB	      2.47GB
    ======== ============ ============

I/O flows
---------

Unless configured otherwise, the GE will open the following ports:

-  KAS opens the port 8080 to receive HTTP TCP requests from final users
   and port 9990 to receive HTTP TCP requests from the KMS event
   callbacks (so called "handler" port).
-  KMS opens the port 9090 to receive HTTP TCP requests from KAS and
   port 9091 for HTTP TCP requests from final users. Also it needs fully
   opened UDP port range.

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
