==================
Installation Guide
==================

**Kurento Media Server (KMS)** can be installed in multiple ways

- Using an EC2 instance in the `Amazon Web Services`_ (AWS) cloud service. Using AWS is suggested to users who don't want to worry about properly configuring a server and all software packages, because the provided setup does all this automatically.

- Using the Docker images provided by the Kurento team. Docker images allow to run Kurento in any host machine, so for example it's possible to run KMS on top of a Fedora or CentOS system. In theory it could even be possible to run under Windows, but so far that possibility hasn't been explored, so you would be at your own risk.

- A local installation with ``apt-get install``, in any Ubuntu machine. This method allows to have total control of the installation process. Besides installing KMS, a common need is to also install a :term:`STUN` or :term:`TURN` server, especially if KMS or any of its clients are located behind a :term:`NAT` firewall.

If you want to try nightly builds of KMS, then head to the section :doc:`/user/installation_dev`.



.. _installation-aws:

Amazon Web Services
===================

The Kurento project provides an *AWS CloudFormation* template file. It can be used to create an EC2 instance that comes with everything needed and totally pre-configured to run KMS, including a `Coturn`_ server.

Note that this template is specifically tailored to be deployed on the default *Amazon Virtual Private Cloud* (`Amazon VPC <https://aws.amazon.com/documentation/vpc/>`__) network. **You need an Amazon VPC to deploy this template**.

Follow these steps to use it:

1. Access the `AWS CloudFormation Console <https://console.aws.amazon.com/cloudformation>`__.

2. Click on *Create Stack*.

3. Look for the section *Choose a template*, and choose the option *Specify an Amazon S3 template URL*. Then, in the text field that gets enabled, paste this URL: ``https://s3-eu-west-1.amazonaws.com/aws.kurento.org/KMS-Coturn-cfn.yaml``.

4. Follow through the steps of the configuration wizard:

   4.1. **Stack name**: A descriptive name for your Stack.

   4.2. **InstanceType**: Choose an appropriate size for your instance. `Check the different ones <https://aws.amazon.com/ec2/instance-types/?nc1=h_ls>`__.

   4.3. **KeyName**: You need to create an RSA key beforehand in order to access the instance. Check AWS documentation on `how to create one <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-key-pairs.html>`__.

   4.4. **SSHLocation**: For security reasons you may need to restrict SSH traffic to allow connections only from specific locations. For example, from your home or office.

   4.5. **TurnUser**: User name for the TURN server.

   4.6. **TurnPassword**: Password required to use the TURN server.

        .. note::

           The template file includes *Coturn* as a :term:`TURN` server. The default user/password for this server is ``kurento``/``kurento``. You can optionally change the username, but **make sure to change the default password**.

5. Finish the Stack creation process. Wait until the status of the newly created Stack reads *CREATE_COMPLETE*.

6. Select the Stack and then open the *Outputs* tab, where you'll find the instance's public IP address, and the Kurento Media Server endpoint URL that must be used by :doc:`Application Servers </user/writing_applications>`.



.. _installation-docker:

Docker image
============

Kurento's Docker Hub contains images built from each KMS release. Just head to the `kurento-media-server Docker Hub page <https://hub.docker.com/r/kurento/kurento-media-server>`__, and follow the instructions you'll find there.



.. _installation-local:

Local Installation
==================

With this method, you will install KMS from the native Ubuntu package repositories made available by the Kurento project.

KMS has explicit support for two Long-Term Support (*LTS*) versions of Ubuntu: **Ubuntu 16.04 (Xenial)** and **Ubuntu 18.04 (Bionic)** (64-bits only). To install KMS, open a terminal and follow these steps:

1. Make sure that GnuPG is installed.

   .. code-block:: bash

      sudo apt-get update \
        && sudo apt-get install --no-install-recommends --yes \
           gnupg

2. Define what version of Ubuntu is installed in your system.

   Run **only one** of these lines:

   .. code-block:: bash

      # Run ONLY ONE of these lines:
      DISTRO="xenial"  # KMS for Ubuntu 16.04 (Xenial)
      DISTRO="bionic"  # KMS for Ubuntu 18.04 (Bionic)

3. Add the Kurento repository to your system configuration.

   Run these two commands in the same terminal you used in the previous step:

   .. code-block:: text

      sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83

   .. code-block:: text

      sudo tee "/etc/apt/sources.list.d/kurento.list" >/dev/null <<EOF
      # Kurento Media Server - Release packages
      deb [arch=amd64] http://ubuntu.openvidu.io/|VERSION_KMS| $DISTRO kms6
      EOF

4. Install KMS:

   .. code-block:: text

      sudo apt-get update \
        && sudo apt-get install --yes kurento-media-server

This will install the KMS release version that was specified in the previous commands.

The server includes service files which integrate with the Ubuntu init system, so you can use the following commands to start and stop it:

.. code-block:: text

   sudo service kurento-media-server start
   sudo service kurento-media-server stop

Log messages from KMS will be available in ``/var/log/kurento-media-server/``. For more details about KMS logs, check :doc:`/features/logging`.



.. _installation-stun-turn:

STUN and TURN servers
=====================

If Kurento Media Server, its Application Server, or any of the clients are located behind a :term:`NAT`, you need to use a :term:`STUN` or a :term:`TURN` server in order to achieve :term:`NAT traversal`. In most cases, STUN is effective in addressing the NAT issue with most consumer network devices (routers). However, it doesn't work for many corporate networks, so a TURN server becomes necessary.

Apart from that, you need to open all UDP ports in your system configuration, as STUN will use any random port from the whole [0-65535] range.

.. note::

   The features provided by TURN are a superset of those provided by STUN. This means that *you don't need to configure a STUN server if you are already using a TURN server*.

For more information about why and when STUN/TURN is needed, check out the FAQ: :ref:`faq-stun`



STUN server
-----------

To configure a STUN server in KMS, uncomment the following lines in the WebRtcEndpoint configuration file, located at ``/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini``:

.. code-block:: bash

   stunServerAddress=<serverIp>
   stunServerPort=<serverPort>

.. note::

   Be careful since comments inline (with ``;``) are not allowed for parameters in the configuration files. Thus, the following line **is not correct**:

   .. code-block:: bash

      stunServerAddress=<serverIp> ; Only IP addresses are supported

   ... and must be changed to something like this:

   .. code-block:: bash

      ; Only IP addresses are supported
      stunServerAddress=<serverIp>

The parameter ``serverIp`` should be the public IP address of the STUN server. It must be an IP address, **not a domain name**.

It should be easy to find some public STUN servers that are made available for free. For example:

.. code-block:: text

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

.. code-block:: bash

   turnURL=<user>:<password>@<serverIp>:<serverPort>

The parameter ``serverIp`` should be the public IP address of the TURN server. It must be an IP address, **not a domain name**.

See some examples of TURN configuration below:

.. code-block:: bash

   turnURL=kurento:kurento@WWW.XXX.YYY.ZZZ:3478

... or using a free access `Numb`_ TURN/STUN server:

.. code-block:: bash

   turnURL=user:password@66.228.45.110:3478

Note that it is somewhat easy to find free STUN servers available on the net, because their functionality is pretty limited and it is not costly to keep them working for free. However, this doesn't happen with TURN servers, which act as a media proxy between peers and thus the cost of maintaining one is much higher.

It is rare to find a TURN server which works for free while offering good performance. Usually, each user opts to maintain their own private TURN server instances.

`Coturn`_ is an open source implementation of a TURN/STUN server. In the :doc:`FAQ </user/faq>` section there is a description about how to install and configure it.



Check your installation
=======================

To verify that KMS is up and running, use this command and look for the ``kurento-media-server`` process:

.. code-block:: text

   ps -ef | grep kurento-media-server

   > nobody  1270  1  0 08:52 ?  00:01:00  /usr/bin/kurento-media-server

Unless configured otherwise, KMS will open the port ``8888`` to receive requests and send responses by means of the :doc:`Kurento Protocol </features/kurento_protocol>`. Use this command to verify that this port is listening for incoming packets:

.. code-block:: text

   sudo netstat -tupan | grep kurento

   > tcp6  0  0 :::8888  :::*  LISTEN  1270/kurento-media-server



.. _Amazon Web Services: https://aws.amazon.com
.. _Coturn: http://coturn.net
.. _Numb: http://numb.viagenie.ca/
