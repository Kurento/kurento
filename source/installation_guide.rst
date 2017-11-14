%%%%%%%%%%%%%%%%%%%%%%%%%%
Kurento Installation Guide
%%%%%%%%%%%%%%%%%%%%%%%%%%

Kurento Media Server (KMS) has explicit support for two Long-Term Support (*LTS*)
distributions of Ubuntu: **Ubuntu 14.04 (Trusty)** and **Ubuntu 16.04 (Xenial)**.
Only the 64-bits editions are supported.

Currently, the main development environment for KMS is Ubuntu 16.04 (Xenial),
so if you are in doubt, this is the preferred Ubuntu distribution to choose.
However, all features and bugfixes are still being backported and tested on
Ubuntu 14.04 (Trusty), so you can continue running this version if needed.

KMS is made available in *four* different editions. The difference between them
is just whether they are packaged to work on *Trusty* or *Xenial*, and there is
also the choice of using *Release* or *Development* versions:

- Use the *Release* version for any kind of service or product intended to reach
  a **Production** stage. Also use this version if you are not a developer.
- The *Development* version is a representation of the current state on the
  software development for Kurento, so it can include undocumented changes,
  regressions, bugs or deprecations. Use this if you want to develop KMS itself.

In order to install Kurento Media Server, you need to decide what combination
of distribution and version you need. Open a terminal and type **only one** of
these lines:

.. sourcecode:: console

   # Choose one:
   REPO="trusty"      # KMS Release     - Ubuntu 14.04 (Trusty)
   REPO="trusty-dev"  # KMS Development - Ubuntu 14.04 (Trusty)
   REPO="xenial"      # KMS Release     - Ubuntu 16.04 (Xenial)
   REPO="xenial-dev"  # KMS Development - Ubuntu 16.04 (Xenial)

Now type the following commands, **one at a time and in the same order as listed
here**. When asked for any kind of confirmation, reply affirmatively:

.. sourcecode:: console

   echo "deb http://ubuntu.kurento.org $REPO kms6" | sudo tee /etc/apt/sources.list.d/kurento.list
   wget http://ubuntu.kurento.org/kurento.gpg.key -O - | sudo apt-key add -
   sudo apt-get update
   sudo apt-get install kurento-media-server-6.0

Now, Kurento Media Server has been installed. Use the following commands to
start and stop it, respectively:

.. sourcecode:: console

   sudo service kurento-media-server-6.0 start
   sudo service kurento-media-server-6.0 stop


STUN and TURN servers
=====================

If Kurento Media Server or any of its clients are located behind a :term:`NAT`,
you need to use a :term:`STUN` or a :term:`TURN` server in order to achieve
:term:`NAT traversal`. In most cases, STUN is effective in addressing the NAT
issue with most consumer network devices (routers). However, it doesn't work for
many corporate networks, so a TURN server becomes necessary.

In order to setup a **STUN** server you should uncomment the following lines in
the Kurento Media Server configuration file, located at
``/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini``:

.. sourcecode:: bash

   stunServerAddress=<serverIpAddress>
   stunServerPort=<serverPort>

.. note::

   Be careful since comments inline (with ``;``) are not allowed for parameters
   in the configuration files. Thus, the following line **is not correct**:

   .. sourcecode:: bash

      stunServerAddress=<serverIpAddress> ; Only IP addresses are supported

   ... and must be changed to something like this:

   .. sourcecode:: bash

      ; Only IP addresses are supported
      stunServerAddress=<serverIpAddress>

The parameter ``serverIpAddress`` should be an IP address (not a domain name).
There is plenty of public STUN servers available, for example:

.. sourcecode:: javascript

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
the Kurento Media Server configuration file located at
``/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini``:

.. code-block:: javascript

   turnURL=<user>:<password>@<serverIpAddress>:<serverPort>

As before, ``serverIpAddress`` should be an IP address (not a domain name). See
some examples of TURN configuration below:

.. code-block:: javascript

   turnURL=kurento:kurento@111.222.333.444:3478

... or using a free access `numb <http://numb.viagenie.ca/>`__ STUN/TURN server
as follows:

.. code-block:: javascript

   turnURL=user:password@66.228.45.110:3478

Note that it is somewhat easy to find free STUN servers available on the net,
because their functionality is pretty limited and it is not costly to keep them
working for free. However this doesn't happen with TURN servers, which act as
a proxy between peers and thus the cost of maintaining one are much higher. It
is rare to find a TURN server which works for free while being performant;
usually each user opts to maintain their own private TURN server instances.

An open source implementation of a TURN server is
`coturn <http://coturn.net/>`__. In the :doc:`FAQ <./faq>`
section there is description about how to install a coturn server.
