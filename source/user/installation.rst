==================
Installation Guide
==================

**Kurento Media Server (KMS)** can be installed in multiple ways

* :ref:`Using an EC2 instance <installation-aws>` in the `Amazon Web Services`_ (AWS) cloud service. Using AWS is suggested to users who don't want to worry about properly configuring a server and all software packages, because the provided setup does all this automatically.

* :ref:`Using the Kurento Docker images <installation-docker>`. Docker allows to run Kurento in any host machine, so for example it's possible to run KMS on top of a Fedora or CentOS system. In theory it could even be possible to run under Windows, but so far that possibility hasn't been explored by the Kurento team, so you would be at your own risk.

* :ref:`Setting up a local installation <installation-local>` with ``apt-get install``. This method allows to have total control of the installation process.

Besides installing KMS, a common need is also :ref:`installing a STUN/TURN server <installation-stun-turn>`, especially if KMS or any of its clients are located behind a :term:`NAT` router or firewall.

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

   4.5. **TurnUser**: User name for the TURN relay.

   4.6. **TurnPassword**: Password required to use the TURN relay.

        .. note::

           The template file includes *Coturn* as a :term:`STUN` server and :term:`TURN` relay. The default user/password for this server is ``kurento``/``kurento``. You can optionally change the username, but **make sure to change the default password**.

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

Open a terminal and run these commands:

1. Make sure that GnuPG is installed.

   .. code-block:: bash

      sudo apt-get update && sudo apt-get install --no-install-recommends --yes \
          gnupg

2. Add the Kurento repository to your system configuration.

   Run these commands:

   .. code-block:: text

      # Import the Kurento repository signing key
      sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83

      # Get Ubuntu version definitions
      source /etc/upstream-release/lsb-release 2>/dev/null || source /etc/lsb-release

      # Add the repository to Apt
      sudo tee "/etc/apt/sources.list.d/kurento.list" >/dev/null <<EOF
      # Kurento Media Server - Release packages
      deb [arch=amd64] http://ubuntu.openvidu.io/|VERSION_KMS| $DISTRIB_CODENAME kms6
      EOF

3. Install KMS:

   .. note::

      This step applies **only for a first time installation**. If you already have installed Kurento and want to upgrade it, follow instead the steps described here: :ref:`installation-local-upgrade`.

   .. code-block:: text

      sudo apt-get update && sudo apt-get install --no-install-recommends --yes \
          kurento-media-server

   This will install the release version of Kurento Media Server.

The server includes service files which integrate with the Ubuntu init system, so you can use the following commands to start and stop it:

.. code-block:: text

   sudo service kurento-media-server start
   sudo service kurento-media-server stop

Log messages from KMS will be available in ``/var/log/kurento-media-server/``. For more details about KMS logs, check :doc:`/features/logging`.



.. _installation-local-upgrade:

Local Upgrade
=============

To upgrade a local installation of Kurento Media Server, you have to write the new version number into the file ``/etc/apt/sources.list.d/kurento.list``, which was created during :ref:`installation-local`. After editing that file, you can choose between 2 options to actually apply the upgrade:

A. **Upgrade all system packages**.

   This is the standard procedure expected by Debian & Ubuntu maintainer methodology. Upgrading all system packages is a way to ensure that everything is set to the latest version, and all bug fixes & security updates are applied too, so this is the most recommended method:

   .. code-block:: bash

      sudo apt-get update && sudo apt-get dist-upgrade

   However, don't do this inside a Docker container. Running *apt-get upgrade* or *apt-get dist-upgrade* is frowned upon by the `Docker best practices`_; instead, you should just move to a newer version of the `Kurento Docker images`_.

B. **Uninstall the old Kurento version**, before installing the new one.

   Note however that **apt-get is not good enough** to remove all of Kurento packages. We recommend that you use *aptitude* for this, which works much better than *apt-get*:

   .. code-block:: bash

      sudo aptitude remove '?installed?version(kurento)'

      sudo apt-get update && sudo apt-get install --no-install-recommends --yes \
          kurento-media-server

.. note::

   Be careful! If you fail to upgrade **all** Kurento packages, you will get wrong behaviors and **crashes**. Kurento is composed of several packages:

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

   To use a newer version **you have to upgrade all Kurento packages**, not only the first one.



.. _installation-stun-turn:

STUN/TURN server install
========================

Working with WebRTC *requires* developers to know and have a good understanding about everything related to NAT, ICE, STUN, and TURN. If you don't know about these, you should start reading here: :ref:`faq-nat-ice-stun-turn`.

Kurento Media Server, just like any WebRTC endpoint, will work fine on its own, for *localhost* connections. You only need to install KMS if all you need are local network connections.

However, sooner or later you will want to make your application work in a cloud environment, and allow KMS to connect with remote clients. The problem is, remote clients will probably want to connect from behind a :term:`NAT` router, so your application needs to perform :term:`NAT Traversal` in the client's router. This can be done by setting up a :term:`STUN` server or a :term:`TURN` relay, and configuring it **in both KMS and the client browser**.

These links contain the information needed to finish configuring your Kurento Media Server with a STUN/TURN server:

- :ref:`faq-coturn-install`
- :ref:`faq-stun-test`
- :ref:`faq-stun-configure`



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

To check whether KMS is up and listening for connections, use the following command:

.. code-block:: text

   curl \
     --include \
     --header "Connection: Upgrade" \
     --header "Upgrade: websocket" \
     --header "Host: 127.0.0.1:8888" \
     --header "Origin: 127.0.0.1" \
     http://127.0.0.1:8888/kurento

You should get a response similar to this one:

.. code-block:: text

   HTTP/1.1 500 Internal Server Error
   Server: WebSocket++/0.7.0

Ignore the "*Server Error*" message: this is expected, and it actually proves that KMS is up and listening for connections.

If you need to automate this, you could write a script similar to `healthchecker.sh`_, the one we use in `Kurento Docker images`_.



.. Links

.. _Amazon Web Services: https://aws.amazon.com
.. _Coturn: https://github.com/coturn/coturn
.. _Docker best practices: https://docs.docker.com/develop/develop-images/dockerfile_best-practices/#apt-get
.. _healthchecker.sh: https://github.com/Kurento/kurento-docker/blob/master/kurento-media-server/healthchecker.sh
.. _Kurento Docker images: https://hub.docker.com/r/kurento/kurento-media-server
