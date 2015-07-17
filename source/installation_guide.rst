%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Kurento Media Server Installation
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

Kurento Media Server (KMS) has to be installed on **Ubuntu 14.04 LTS** (64 bits).

In order to install the latest stable Kurento Media Server version
(**|DOC_VERSION|**) you have to type the following commands, one at a time and
in the same order as listed here. When asked for any kind of confirmation,
reply affirmatively:

.. sourcecode:: console

   echo "deb http://ubuntu.kurento.org trusty kms6" | sudo tee /etc/apt/sources.list.d/kurento.list
   wget -O - http://ubuntu.kurento.org/kurento.gpg.key | sudo apt-key add -
   sudo apt-get update
   sudo apt-get install kurento-media-server-6.0

Take into account that if your are installing Kurento Media Server in Ubuntu
Server 14.04, the tool *add-apt-repository* is not installed by default. To
install it, run this command:

.. sourcecode:: console

   sudo apt-get install software-properties-common

Now, Kurento Media Server has been installed and started. Use the following
commands to start and stop it respectively:

.. sourcecode:: console

   sudo service kurento-media-server-6.0 start
   sudo service kurento-media-server-6.0 stop


Migrating from KMS v5 to v6
===========================

The current stable version of Kurento Media Server uses the **Trickle ICE**
protocol for WebRTC connections. :term:`Trickle ICE` is the name given to the
extension to the :term:`Interactive Connectivity Establishment` (ICE) protocol
that allows ICE agents (in this case Kurento Media Server and Kurento Client)
to send and receive candidates incrementally rather than exchanging complete
lists. In short, Trickle ICE allows to begin WebRTC connectivity much more
faster.

This feature makes the Kurento Media Server 6 **incompatible** with the former
versions. If you are using Kurento Media Server 5.1 or lower, it is strongly
recommended to upgrade your KMS. To do that, first you need to uninstall KMS as
follows:

.. sourcecode:: console

   sudo apt-get remove kurento-media-server
   sudo apt-get purge kurento-media-server

After that, install Kurento Media Server 6 as depicted at the top of this page.


STUN and TURN servers
=====================

If Kurento Media Server is located behind a :term:`NAT` you need to use a
:term:`STUN` or :term:`TURN` in order to achieve :term:`NAT traversal`. In most
of cases, a STUN server will do the trick. A TURN server is only necessary when
the NAT is symmetric.

In order to setup a **STUN** server you should uncomment the following lines in
the Kurento Media Server configuration file located on at
``/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini``:

.. sourcecode:: js

   stunServerAddress=<stun_ip_address> ; Only IP address are supported
   stunServerPort=<stun_port>

The parameter ``stunServerAddress`` should be an IP address (not domain name).
There is plenty of public STUN servers available, for example:

.. sourcecode:: js

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

In order to setup a **TURN** server you should uncomment the following lines in
the Kurento Media Server configuration file located on at
``/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini``:

.. sourcecode:: js

   turnURL=user:password@address:port

As before, TURN address should be an IP address (not domain name). See some
examples of TURN configuration below:

.. sourcecode:: js

   turnURL=kurento:kurento@193.147.51.36:3478

... or using a free access `numb <http://numb.viagenie.ca/>`_ STUN/TURN server
as follows:

.. sourcecode:: js

   turnURL=user:password@66.228.45.110:3478

An open source implementation of a TURN server is
`coturn <https://code.google.com/p/coturn/>`_. In the :doc:`FAQ <./faq>`
section there is description about how to install a coturn server.
