%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Kurento Media Server Installation
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

Kurento Media Server has to be installed on **Ubuntu 14.04 LTS** (32 or 64 bits).

In order to install the latest stable Kurento Media Server version you have to
type the following commands, one at a time and in the same order as listed
here. When asked for any kind of confirmation, reply affirmatively:

.. sourcecode:: console

   sudo add-apt-repository ppa:kurento/kurento
   sudo apt-get update
   sudo apt-get install kurento-media-server

Take into account that if your are installing Kurento Media Server in Ubuntu
Server 14.04, the tool *add-apt-repository* is not installed by default. To
install it, run this command:

.. sourcecode:: console

   sudo apt-get install software-properties-common

Now, Kurento Media Server has been installed and started. Use the following
commands to start and stop it respectively:

.. sourcecode:: console

    sudo service kurento-media-server start
    sudo service kurento-media-server stop

Kurento Media Server has a log file located at
``/var/log/kurento-media-server/media-server.log``.


STUN and TURN servers
=====================

If Kurento Media Server is located behind a :term:`NAT` you need to use a
:term:`STUN` or :term:`TURN` in order to achieve NAT traversal. In most of
cases, a STUN server will do the trick. A TURN server is only necessary when
the NAT is symmetric.

In order to setup a **STUN** server you should uncomment the following lines in
the Kurento Media Server configuration file located on at
``/etc/kurento/kurento.conf.json``:

.. sourcecode:: js

   "stunServerAddress" : "stun ip address",
   "stunServerPort" : 3478

Be careful with the JSON format. If the ``stunServerPort`` line is the last
within the ``WebRtcEndpoint`` section, then this line must not finish with a
comma (``,``).

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
``/etc/kurento/kurento.conf.json``:

.. sourcecode:: js

   "turnURL" : "user:password@address:port(?transport=[udp|tcp|tls])"

As before, be careful with the JSON format and the final comma (``,``).

An open source implementation of a TURN server is
`coturn <https://code.google.com/p/coturn/>`_. In the :doc:`FAQ <./faq>`
section there is description about how to install a coturn server.
