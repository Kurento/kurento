==================
Installation Guide
==================

**Kurento Media Server (KMS)** can be made available through two different methods: either a local native installation, or an EC2 instance in the `Amazon Web Services`_ (AWS) cloud service.

The local installation will make use of public package repositories that hold the latest released versions of KMS. Besides that, a common need is to also install a :term:`STUN` or :term:`TURN` server, especially if KMS or any of its clients are located behind a :term:`NAT`. This document includes some details about that topic.

On the other hand, the alternative to use AWS is suggested to users who don't want to worry about properly configuring a server and all software packages, because the provided setup does all this automatically.



Local Installation
==================

With this method, you will install KMS from the native package repositories made available by the Kurento project.

KMS has explicit support for two Long-Term Support (*LTS*) distributions of Ubuntu: **Ubuntu 14.04 (Trusty)** and **Ubuntu 16.04 (Xenial)**. Only the 64-bits editions are supported.

Currently, the main development environment for KMS is Ubuntu 16.04 (Xenial), so if you are in doubt, this is the preferred Ubuntu distribution to choose. However, all features and bug fixes are still being backported and tested on Ubuntu 14.04 (Trusty), so you can continue running this version if required.

**First Step**. Define which version of Ubuntu will be used for your system.

Open a terminal and copy **only one** of these lines:

.. sourcecode:: bash

   # Choose one:
   REPO="trusty"  # KMS Releases - Ubuntu 14.04 (Trusty)
   REPO="xenial"  # KMS Releases - Ubuntu 16.04 (Xenial)

**Second Step**. Type the following commands, **one at a time and in the same order as listed here**. When asked for any kind of confirmation, reply affirmatively:

.. sourcecode:: text

   sudo tee /etc/apt/sources.list.d/kurento.list > /dev/null <<EOF
   # Kurento Packages repository
   deb http://ubuntu.kurento.org $REPO kms6
   EOF
   wget http://ubuntu.kurento.org/kurento.gpg.key -O - | sudo apt-key add -
   sudo apt-get update
   sudo apt-get install kurento-media-server-6.0

At this point, Kurento Media Server has been installed. The server includes service files which integrate with the Ubuntu init system, so you can use the following commands to start and stop it:

.. sourcecode:: bash

   sudo service kurento-media-server-6.0 start
   sudo service kurento-media-server-6.0 stop



Check your installation
-----------------------

To verify that KMS is up and running, use this command:

.. sourcecode:: bash

   ps -ef | grep kurento-media-server

The output should include the ``kurento-media-server`` process:

.. sourcecode:: text

   nobody    1270     1  0 08:52 ?        00:01:00 /usr/bin/kurento-media-server

Unless configured otherwise, KMS will open the port ``8888`` to receive requests and send responses by means of the :doc:`Kurento Protocol<kurento_protocol>`.
Verify this port by running this command:

.. sourcecode:: bash

   sudo netstat -putan | grep kurento

The output should be similar to this:

.. sourcecode:: text

   tcp6    0    0 :::8888    :::*    LISTEN    1270/kurento-media-server



Pre-Release Builds
==================

Some components of KMS are built nightly, with the code developed during that same day. Other components are built immediately when code is merged into the source repositories.

These builds end up being uploaded to a *Development* repository so they can be installed by anyone. Use this if you want to develop KMS itself, or if you want to try the latest changes before they are officially released.

.. warning::
   The *Development* version is a representation of the current state on the software development for Kurento, so it can include undocumented changes, regressions, bugs or deprecations. **Never** use pre-release builds of Kurento in a production environment.

To install a pre-release version of Kurento, follow the steps described in `Local Installation`_, but choose one of these options during the first step:

.. sourcecode:: bash

   # Choose one:
   REPO="trusty-dev"  # KMS Development - Ubuntu 14.04 (Trusty)
   REPO="xenial-dev"  # KMS Development - Ubuntu 16.04 (Xenial)



STUN and TURN servers
=====================

If Kurento Media Server or any of its clients are located behind a :term:`NAT` (eg. in any cloud provider), you need to use a :term:`STUN` or a :term:`TURN` server in order to achieve :term:`NAT traversal`. In most cases, STUN is effective in addressing the NAT issue with most consumer network devices (routers). However, it doesn't work for many corporate networks, so a TURN server becomes necessary.

Apart from that, you need to open all UDP ports in your system configuration, as TURN/STUN will use any port available from the whole [0-65535] range.

.. note::

   The features provided by TURN are a superset of those provided by STUN. What this means is that you don't need to configure a STUN server if you are already using a TURN server.


STUN server
-----------

To configure a STUN server in KMS, uncomment the following lines in the WebRtcEndpoint configuration file, located at ``/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini``:

.. sourcecode:: bash

   stunServerAddress=<serverIp>
   stunServerPort=<serverPort>

.. note::

   Be careful since comments inline (with ``;``) are not allowed for parameters in the configuration files. Thus, the following line **is not correct**:

   .. sourcecode:: bash

      stunServerAddress=<serverIp> ; Only IP addresses are supported

   ... and must be changed to something like this:

   .. sourcecode:: bash

      ; Only IP addresses are supported
      stunServerAddress=<serverIp>

The parameter ``serverIp`` should be the public IP address of the STUN server. It must be an IP address, **not a domain name**.

It should be easy to find some public STUN servers that are made available for free. For example:

.. sourcecode:: text

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


TURN server
-----------

To configure a TURN server in KMS, uncomment the following lines in the WebRtcEndpoint configuration file, located at ``/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini``:

.. sourcecode:: bash

   turnURL=<user>:<password>@<serverIp>:<serverPort>

The parameter ``serverIp`` should be the public IP address of the TURN server. It must be an IP address, **not a domain name**.

See some examples of TURN configuration below:

.. sourcecode:: bash

   turnURL=kurento:kurento@111.222.333.444:3478

... or using a free access `Numb`_ TURN/STUN server:

.. sourcecode:: bash

   turnURL=user:password@66.228.45.110:3478

Note that it is somewhat easy to find free STUN servers available on the net, because their functionality is pretty limited and it is not costly to keep them working for free. However, this doesn't happen with TURN servers, which act as a media proxy between peers and thus the cost of maintaining one is much higher.

It is rare to find a TURN server which works for free while offering good performance. Usually, each user opts to maintain their own private TURN server instances.

`Coturn`_ is an open source implementation of a TURN/STUN server. In the :doc:`FAQ </faq>` section there is a description about how to install and configure it.



Amazon Web Services
===================

The Kurento project provides an `AWS CloudFormation`_ template file, which can be used to create an EC2 instance. It comes with everything needed and totally pre-configured to run KMS, including a `Coturn`_ server:

``https://s3-eu-west-1.amazonaws.com/aws.openvidu.io/TODO.json`` [TODO]

`Deploying on AWS`_ is our most up-to-date documentation about deploying on AWS. Please follow the steps outlined in the linked document in order to deploy a Kurento Media Server. However, make sure to use the Kurento CloudFormation template file, indicated above this paragraph.



.. _Amazon Web Services: https://aws.amazon.com
.. _AWS CloudFormation: https://aws.amazon.com/cloudformation/
.. _Coturn: http://coturn.net
.. _Deploying on AWS: http://openvidu.io/docs/deployment/deploying-demos-aws/
.. _Numb: http://numb.viagenie.ca/
