==================
Installation Guide
==================

.. contents:: Table of Contents

**Kurento Media Server (KMS)** is compiled and provided for installation by the Kurento team members, in a variety of forms. The only officially supported processor architecture is **64-bit x86**, so for other platforms (such as ARM) you will have to build from sources.

* :ref:`Using an EC2 instance <installation-aws>` in the `Amazon Web Services`_ (AWS) cloud service is suggested to users who don't want to worry about properly configuring a server and all software packages, because the provided template does all this automatically.

* :ref:`The Kurento Docker image <installation-docker>` allows to run KMS on top of any host machine, for example Fedora or CentOS. In theory it could even be possible to run under Windows, but so far that possibility hasn't been explored by the Kurento team, so you would be at your own risk.

* :ref:`Setting up a local installation <installation-local>` with ``apt-get install`` allows to have total control of the installation process. It also means that it's easier to make mistakes, so we don't recommend this installation method. Do this only if you are a seasoned System Administrator.

Besides installing KMS, a common need is also :ref:`installing a STUN/TURN server <installation-stun-turn>`, especially if KMS or any of its clients are located behind a :term:`NAT` router or firewall.

If you want to try **nightly builds** of KMS, then head to the section :doc:`/user/installation_dev`.



.. _installation-aws:

Amazon Web Services
===================

The *AWS CloudFormation* template file for `Amazon Web Services`_ (AWS) can be used to create an EC2 instance that comes with everything needed and totally pre-configured to run KMS, including a `Coturn`_ server.

Follow these steps to use it:

1. Access the `AWS CloudFormation Console <https://console.aws.amazon.com/cloudformation>`__.

2. Click on *Create Stack*.

3. Look for the section *Choose a template*, and choose the option *Specify an Amazon S3 template URL*. Then, in the text field that gets enabled, paste this URL:

   .. code-block:: text

      https://s3-eu-west-1.amazonaws.com/aws.kurento.org/KMS-Coturn-cfn-|VERSION_KMS|.yaml

4. Follow through the steps of the configuration wizard:

   4.1. **Stack name**: A descriptive name for your Stack.

   4.2. **InstanceType**: Choose an appropriate size for your instance. `Check the different ones <https://aws.amazon.com/ec2/instance-types/>`__.

   4.3. **KeyName**: You need to create an RSA key beforehand in order to access the instance. Check AWS documentation on `how to create one <https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-key-pairs.html>`__.

   4.4. **SSHLocation**: For security reasons you may need to restrict SSH traffic to allow connections only from specific locations. For example, from your home or office.

   4.5. **TurnUser**: User name for the TURN relay.

   4.6. **TurnPassword**: Password required to use the TURN relay.

        .. note::

           The template file includes *Coturn* as a :term:`STUN` server and :term:`TURN` relay. The default user/password for this server is *kurento*/*kurento*. You can optionally change the username, but **make sure to change the default password**.

5. Finish the Stack creation process. Wait until the status of the newly created Stack reads *CREATE_COMPLETE*.

6. Select the Stack and then open the *Outputs* tab, where you'll find the instance's public IP address, and the Kurento Media Server endpoint URL that must be used by :doc:`Application Servers </user/writing_applications>`.

.. note::

   The Kurento CF template is written to deploy **on the default VPC** (see the `Amazon Virtual Private Cloud <https://docs.aws.amazon.com/vpc/>`__ docs). There is no VPC selector defined in this template, so you won't see a choice for it during the AWS CF wizard. If you need more flexibility than what this template offers, you have two options:

   A. Manually create an EC2 instance, assigning all the resources as needed, and then using the other installation methods to set Kurento Media Server up on it: :ref:`installation-docker`, :ref:`installation-local`.

   B. Download the current CF from the link above, and edit it to create your own custom version with everything you need from it.



.. _installation-docker:

Docker image
============

The `kurento-media-server Docker image <https://hub.docker.com/r/kurento/kurento-media-server>`__ is a nice *all-in-one* package for an easy quick start. It comes with all the default settings, which is enough to let you try the :doc:`/user/tutorials`.

If you need to insert or extract files from a Docker container, there is a variety of methods: You could use a `bind mount <https://docs.docker.com/storage/bind-mounts/>`__; a `volume <https://docs.docker.com/storage/volumes/>`__; `cp <https://docs.docker.com/engine/reference/commandline/cp/>`__ some files from an already existing container; change your `ENTRYPOINT <https://docs.docker.com/engine/reference/run/#entrypoint-default-command-to-execute-at-runtime>`__ to generate or copy the files at startup; or `base FROM <https://docs.docker.com/engine/reference/builder/#from>`__ this Docker image and build a new one with your own customizations. Check :ref:`faq-docker` for an example of how to use bind-mounts to provide your own configuration files.

These are the exact contents of the image:

* A local ``apt-get`` installation of KMS, as described in :ref:`installation-local`, plus all its extra plugins (chroma, platedetector, etc).
* Debug symbols installed, as described in :ref:`dev-dbg`. This allows getting useful stack traces in case the KMS process crashes. If this happens, please `report a bug <https://github.com/Kurento/bugtracker/issues>`__.
* All **default settings** from the local installation, as found in ``/etc/kurento/``. For details, see :doc:`/user/configuration`.



Running
-------

Docker allows to fine-tune how a container runs, so you'll want to read the `Docker run reference <https://docs.docker.com/engine/reference/run/>`__ and find out the command options that are needed for your project.

This is a good starting point, which runs the latest Kurento Media Server image with default options:

.. code-block:: shell

   docker pull kurento/kurento-media-server:latest

   docker run -d --name kms --network host \
       kurento/kurento-media-server:latest

By default, KMS listens on the port **8888**. Clients wanting to control the media server using the :doc:`/features/kurento_protocol` should open a WebSocket connection to that port, either directly or by means of one of the provided :doc:`/features/kurento_client` SDKs.

The `health checker script <https://github.com/Kurento/kurento-docker/blob/master/kurento-media-server/healthchecker.sh>`__ inside this Docker image does something very similar in order to check if the container is healthy.

Once the container is running, you can get its log output with the `docker logs <https://docs.docker.com/engine/reference/commandline/logs/>`__ command:

.. code-block:: shell

   docker logs --follow kms >"kms-$(date '+%Y%m%dT%H%M%S').log" 2>&1

For more details about KMS logs, check :doc:`/features/logging`.



Why host networking?
~~~~~~~~~~~~~~~~~~~~

Notice how our suggested ``docker run`` command uses ``--network host``? Using `Host Networking <https://docs.docker.com/network/host/>`__ is recommended for software like proxies and media servers, because otherwise publishing large ranges of container ports would consume a lot of memory. You can read more about this issue in our :ref:`Troubleshooting Guide <troubleshooting-docker-network-host>`.

The Host Networking driver **only works on Linux hosts**, so if you are using Docker for Mac or Windows then you'll need to understand that the Docker network gateway acts as a NAT between your host and your container. To use KMS without STUN (e.g. if you are just testing some of the :doc:`/user/tutorials`) you'll need to publish all required ports where KMS will listen for incoming data.

For example, if you use Docker for Mac and want to have KMS listening on the UDP port range **[5000, 5050]** (thus allowing incoming data on those ports), plus the TCP port **8888** for the :doc:`/features/kurento_client`, run:

.. code-block:: shell

   docker run --rm \
       -p 8888:8888/tcp \
       -p 5000-5050:5000-5050/udp \
       -e KMS_MIN_PORT=5000 \
       -e KMS_MAX_PORT=5050 \
       kurento/kurento-media-server:latest



Docker Upgrade
--------------

One of the nicest things about the Docker deployment method is that changing versions, or upgrading, is almost trivially easy. Just *pull* the new image version and use it to run your new container:

.. code-block:: shell

   # Download the new image version:
   docker pull kurento/kurento-media-server:|VERSION_KMS|

   # Create a new container based on the new version of KMS:
   docker run [...] kurento/kurento-media-server:|VERSION_KMS|



.. _installation-local:

Local Installation
==================

With this method, you will install Kurento Media Server from the native Ubuntu packages build by us. Kurento officially supports two Long-Term Support (*LTS*) versions of Ubuntu: **Ubuntu 16.04 (Xenial)** and **Ubuntu 18.04 (Bionic)** (64-bits x86 only).

Open a terminal and run these commands:

1. Make sure that GnuPG is installed.

   .. code-block:: shell

      sudo apt-get update && sudo apt-get install --no-install-recommends --yes \
          gnupg

2. Add the Kurento repository to your system configuration.

   Run these commands:

   .. code-block:: shell

      # Import the Kurento repository signing key
      sudo apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 5AFA7A83

      # Get Ubuntu version definitions
      source /etc/lsb-release

      # Add the repository to Apt
      sudo tee "/etc/apt/sources.list.d/kurento.list" >/dev/null <<EOF
      # Kurento Media Server - Release packages
      deb [arch=amd64] http://ubuntu.openvidu.io/|VERSION_KMS| $DISTRIB_CODENAME kms6
      EOF

3. Install KMS:

   .. note::

      This step applies **only for a first time installation**. If you already have installed Kurento and want to upgrade it, follow instead the steps described here: :ref:`installation-local-upgrade`.

   .. code-block:: shell

      sudo apt-get update && sudo apt-get install --no-install-recommends --yes \
          kurento-media-server

   This will install the release version of Kurento Media Server.



Running
-------

The server includes service files which integrate with the Ubuntu init system, so you can use the following commands to start and stop it:

.. code-block:: shell

   sudo service kurento-media-server start
   sudo service kurento-media-server stop

Log messages from KMS will be available in ``/var/log/kurento-media-server/``. For more details about KMS logs, check :doc:`/features/logging`.



.. _installation-local-upgrade:

Local Upgrade
-------------

To upgrade a local installation of Kurento Media Server, you have to write the new version number into the file ``/etc/apt/sources.list.d/kurento.list``, which was created during :ref:`installation-local`. After editing that file, you can choose between 2 options to actually apply the upgrade:

A. **Upgrade all system packages**.

   This is the standard procedure expected by Debian & Ubuntu maintainer methodology. Upgrading all system packages is a way to ensure that everything is set to the latest version, and all bug fixes & security updates are applied too, so this is the most recommended method:

   .. code-block:: shell

      sudo apt-get update && sudo apt-get dist-upgrade

   However, don't do this inside a Docker container. Running *apt-get upgrade* or *apt-get dist-upgrade* is frowned upon by the `Docker best practices`_; instead, you should just move to a newer version of the `Kurento Docker images`_.

B. **Uninstall the old Kurento version**, before installing the new one.

   Note however that **apt-get is not good enough** to remove all of Kurento packages. We recommend that you use *aptitude* for this, which works much better than *apt-get*:

   .. code-block:: shell

      sudo aptitude remove '?installed?version(kurento)'

      sudo apt-get update && sudo apt-get install --no-install-recommends --yes \
          kurento-media-server

.. note::

   Be careful! If you fail to upgrade **all** Kurento packages, you will get wrong behaviors and **crashes**. Kurento is composed of several packages:

   - *kurento-media-server*
   - *kurento-module-creator*
   - *kms-core*
   - *kms-elements*
   - *kms-filters*
   - *libnice10*
   - *libusrsctp*
   - *openh264*
   - *openwebrtc-gst-plugins*
   - And more

   To use a newer version **you have to upgrade all Kurento packages**, not only the first one.



.. _installation-stun-turn:

STUN/TURN server install
========================

Working with WebRTC *requires* developers to learn and have a good understanding about everything related to NAT, ICE, STUN, and TURN. If you don't know about these, you should start reading here: :ref:`faq-nat-ice-stun-turn`.

Kurento Media Server, just like any WebRTC endpoint, will work fine on its own, for *LAN* connections or for servers which have a public IP address assigned to them. However, sooner or later you will want to make your application work in a cloud environment with NAT firewalls, and allow KMS to connect with remote clients. At the same time, remote clients will probably want to connect from behind their own :term:`NAT` router too, so your application needs to be prepared to perform :term:`NAT Traversal` in both sides. This can be done by setting up a :term:`STUN` server or a :term:`TURN` relay, and configuring it **in both KMS and the client browser**.

These links contain the information needed to finish configuring your Kurento Media Server with a STUN/TURN server:

- :ref:`faq-coturn-install`
- :ref:`faq-stun-test`
- :ref:`faq-stun-configure`



Check your installation
=======================

To verify that the Kurento process is up and running, use this command and look for the *kurento-media-server* process:

.. code-block:: shell-session

   $ ps -fC kurento-media-server
   UID        PID  PPID  C STIME TTY          TIME CMD
   kurento   7688     1  0 13:36 ?        00:00:00 /usr/bin/kurento-media-server

Unless configured otherwise, KMS will listen on the port TCP 8888, to receive RPC Requests and send RPC Responses by means of the :doc:`Kurento Protocol </features/kurento_protocol>`. Use this command to verify that this port is open and listening for incoming packets:

.. code-block:: shell-session

   $ sudo netstat -tupln | grep -e kurento -e 8888
   tcp6  0  0  :::8888  :::*  LISTEN  7688/kurento-media-

You can change these parameters in the file ``/etc/kurento/kurento.conf.json``.

To check whether KMS is up and listening for connections, use the following command:

.. code-block:: shell

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
