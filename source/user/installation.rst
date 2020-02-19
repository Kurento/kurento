==================
Installation Guide
==================

**Kurento Media Server (KMS)** can be installed in multiple ways

* :ref:`Using an EC2 instance <installation-aws>` in the `Amazon Web Services`_ (AWS) cloud service. Using AWS is suggested to users who don't want to worry about properly configuring a server and all software packages, because the provided setup does all this automatically.

* :ref:`Using the Kurento Docker images <installation-docker>`. Docker allows to run Kurento in any host machine, so for example it's possible to run KMS on top of a Fedora or CentOS system. In theory it could even be possible to run under Windows, but so far that possibility hasn't been explored by the Kurento team, so you would be at your own risk.

* :ref:`Setting up a local installation <installation-local>` with ``apt-get install``. This method allows to have total control of the installation process. Besides installing KMS, a common need is also :ref:`installing a STUN/TURN server <installation-stun-turn>`, especially if KMS or any of its clients are located behind a :ref:`Symmetric NAT <nat-symmetric>` or firewall.

If you want to try nightly builds of KMS, then head to the section :doc:`/user/installation_dev`.



.. _installation-aws:

Amazon Web Services
===================

The Kurento project provides an *AWS CloudFormation* template file for `Amazon Web Services`_ (AWS). It can be used to create an EC2 instance that comes with everything needed and totally pre-configured to run KMS, including a `Coturn`_ server.

Note that this template is specifically tailored to be deployed on the default *Amazon Virtual Private Cloud* (`Amazon VPC <https://aws.amazon.com/documentation/vpc/>`__) network. **You need an Amazon VPC to deploy this template**.

Follow these steps to use it:

1. Access the `AWS CloudFormation Console <https://console.aws.amazon.com/cloudformation>`__.

2. Click on *Create Stack*.

3. Look for the section *Choose a template*, and choose the option *Specify an Amazon S3 template URL*. Then, in the text field that gets enabled, paste this URL:

   .. code-block:: text

      https://s3-eu-west-1.amazonaws.com/aws.kurento.org/KMS-Coturn-cfn-|VERSION_KMS|.yaml

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

.. note::

   You shouldn't expose a large port range in your Docker containers; instead, prefer using ``--network host``.

   To elaborate a bit more, as `Yorgos Saslis <https://github.com/gsaslis>`__ mentioned `here <https://github.com/kubernetes/kubernetes/issues/23864#issuecomment-387070644>`__:

       the problem is that - given the current state of Docker - it seems you should NOT even be trying to expose large numbers of ports. You are advised to use the host network anyway, due to the overhead involved with large port ranges. (it adds both latency, as well as consumes significant resources - e.g. see https://www.percona.com/blog/2016/02/05/measuring-docker-cpu-network-overhead/ )

       If you are looking for a more official source, there is still (for years) an open issue in Docker about this:
       `moby/moby#11185 (comment) <https://github.com/moby/moby/issues/11185#issuecomment-245983651>`__



.. _installation-local:

Local Installation
==================

With this method, you will install Kurento Media Server from the native Ubuntu package repositories made available by the Kurento project. KMS has explicit support for two Long-Term Support (*LTS*) versions of Ubuntu: **Ubuntu 16.04 (Xenial)** and **Ubuntu 18.04 (Bionic)** (64-bits only).

.. note::

   This section applies **only for a first time installation**. If you already have installed Kurento and want to upgrade it, follow instead the steps described here: :ref:`installation-local-upgrade`.

To install KMS, start from a clean machine (**with no KMS or any of its dependencies already installed**). Open a terminal, and follow these steps:

1. Make sure that GnuPG is installed.

   .. code-block:: bash

      sudo apt-get update && sudo apt-get install --no-install-recommends --yes \
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

      sudo apt-get update && sudo apt-get install --yes kurento-media-server

This will install the release KMS version.

The server includes service files which integrate with the Ubuntu init system, so you can use the following commands to start and stop it:

.. code-block:: text

   sudo service kurento-media-server start
   sudo service kurento-media-server stop

Log messages from KMS will be available in ``/var/log/kurento-media-server/``. For more details about KMS logs, check :doc:`/features/logging`.



.. _installation-local-upgrade:

Local Upgrade
=============

To upgrade a previous installation of Kurento Media Server, you'll need to edit the file */etc/apt/sources.list.d/kurento.list*, setting the new version number. After this file has been changed, there are 2 options to actually apply the upgrade:

A. Simply upgrade all system packages. This is the standard procedure expected by Debian & Ubuntu maintainer methodology. Upgrading all system packages is a way to ensure that everything is set to the latest version, and all bug fixes & security updates are applied too, so this is the most recommended method:

   .. code-block:: bash

      sudo apt-get update && sudo apt-get dist-upgrade

  Keep in mind that this is the recommended method only for server installations of Debian/Ubuntu, not for Docker containers. The `Best practices for writing Dockerfiles <https://docs.docker.com/develop/develop-images/dockerfile_best-practices/#apt-get>`__ recommends against running ``upgrade`` or ``dist-upgrade`` inside Docker containers.

B. Completely uninstall the old Kurento version, and install the new one.

   Note however that **apt-get doesn't remove all dependencies** that were installed with Kurento. You will need to use *aptitude* for this, which works better than *apt-get*:

   .. code-block:: bash

      sudo aptitude remove kurento-media-server
      sudo apt-get update && sudo apt-get install kurento-media-server

Be careful! If you don't follow one of these methods, then you'll probably end up with a **mixed installation of old and new packages**. You don't want that to happen: it is a surefire way to get wrong behaviors and crashes.

.. note::

   A Kurento installation is composed of **several packages**:

   - ``kurento-media-server``
   - ``kurento-module-creator``
   - ``kms-core``
   - ``kms-elements``
   - ``kms-filters``
   - ``libnice10``
   - ``libusrsctp``
   - ``openh264``
   - ``openwebrtc-gst-plugins``
   - And more

   When installing a new version, **you have to upgrade all of them**, not only the first one.



.. _installation-stun-turn:

STUN and TURN servers
=====================

If Kurento Media Server, its Application Server, or any of the clients are located behind a :term:`NAT`, you need to use a :term:`STUN` or a :term:`TURN` server in order to achieve :term:`NAT traversal`. You can read more about this topic here: :doc:`/knowledge/nat`.

In most cases, STUN is effective in addressing the NAT issue with most consumer network devices (routers). However, it doesn't work for many corporate networks, so a TURN server becomes necessary.

.. note::

   **Every TURN server supports STUN**, because a TURN server is just really a STUN server with added relaying functionality built in. This means that *you don't need to set a STUN server up if you have already configured a TURN server*.

The STUN/TURN server is configured to use a range of UDP & TCP ports. All those ports should also be opened to all traffic, in the server's network configuration or security group.

For more information about why and when STUN/TURN is needed, check out the FAQ: :ref:`faq-stun`.



STUN server
-----------

To configure a STUN server in KMS, uncomment the following lines in the WebRtcEndpoint configuration file, located at */etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini*:

.. code-block:: bash

   stunServerAddress=<StunServerIp>
   stunServerPort=<StunServerPort>

The parameter ``StunServerIp`` should be the public IP address of the STUN server. It must be an IP address, **not a domain name**. For example:

.. code-block:: bash

   stunServerAddress=198.51.100.1
   stunServerPort=3478

.. note::

   Be careful since inline comments (with ``;``) are not allowed for parameters in the configuration files. Thus, the following line **is not correct**:

   .. code-block:: text

      stunServerAddress=198.51.100.1  ; My STUN server

   ... and must be changed to something like this:

   .. code-block:: text

      ; My STUN server
      stunServerAddress=198.51.100.1

STUN is a very lightweight protocol and maintaining a STUN server is very cheap. For this reason, it should be easy to find some public STUN servers that are made available free of charge, if you don't want to maintain your own.



TURN server
-----------

To configure a TURN server in KMS, uncomment the following lines in the WebRtcEndpoint configuration file, located at */etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini*:

.. code-block:: bash

   turnURL=<TurnUser>:<TurnPassword>@<TurnServerIp>:<TurnServerPort>

The parameter ``TurnServerIp`` should be the public IP address of the TURN server. It must be an IP address, **not a domain name**. For example:

.. code-block:: bash

   turnURL=myuser:mypassword@198.51.100.1:3478

TURN servers are used to relay audio/video traffic between peers, and for this reason they are expensive to run. It is rare to find public ones that work free of charge while offering good performance, so we recommend that you deploy and maintain your own private TURN server.

`Coturn`_ is an open source implementation of a STUN+TURN server. In the :ref:`STUN FAQ <faq-stun>` section you'll find instructions to install and configure it.



Check your installation
=======================

To verify that the Kurento process is up and running, use this command and look for the ``kurento-media-server`` process:

.. code-block:: text

   $ ps -fC kurento-media-server
   UID        PID  PPID  C STIME TTY          TIME CMD
   kurento   7688     1  0 13:36 ?        00:00:00 /usr/bin/kurento-media-server

Unless configured otherwise, KMS will listen on the IPv6 port ``8888`` to receive RPC Requests and send RPC Responses by means of the :doc:`Kurento Protocol </features/kurento_protocol>`. Use this command to verify that this port is open and listening for incoming packets:

.. code-block:: text

   $ sudo netstat -tupln | grep -e kurento -e 8888
   tcp6  0  0  :::8888  :::*  LISTEN  7688/kurento-media-

You can change these parameters in the file */etc/kurento/kurento.conf.json*.

Lastly, you can check whether the RPC WebSocket of Kurento is healthy and able to receive and process messages. For this, send a dummy request and check that the response is as expected:

.. code-block:: text

   $ curl -i -N \
       -H "Connection: Upgrade" \
       -H "Upgrade: websocket" \
       -H "Host: 127.0.0.1:8888" \
       -H "Origin: 127.0.0.1" \
       http://127.0.0.1:8888/kurento

You should get a response similar to this one:

.. code-block:: text

   HTTP/1.1 500 Internal Server Error
   Server: WebSocket++/0.7.0

Ignore the error line: it is an expected error, because ``curl`` does not talk the Kurento protocol. We just checked that the ``WebSocket++`` server is actually up, and listening for connections. If you wanted, you could automate this check with a script similar to `healthchecker.sh`_, the one we use in Kurento Docker images.



.. _Amazon Web Services: https://aws.amazon.com
.. _Coturn: https://github.com/coturn/coturn
.. _healthchecker.sh: https://github.com/Kurento/kurento-docker/blob/master/kurento-media-server/healthchecker.sh
