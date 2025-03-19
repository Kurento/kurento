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

3. Now provide this Amazon S3 URL for the template:

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
* Debug symbols installed, as described in :ref:`dev-dbg`. This allows getting useful stack traces in case the KMS process crashes. If this happens, please `report a bug <https://github.com/Kurento/kurento/issues>`__.
* All **default settings** from the local installation, as found in ``/etc/kurento/``. For details, see :doc:`/user/configuration`.



Running
-------

Docker allows to fine-tune how a container runs, so you'll want to read the `Docker run reference <https://docs.docker.com/engine/reference/run/>`__ and find out the command options that are needed for your project.

This is a good starting point, which runs the latest Kurento Media Server image with default options:

.. code-block:: shell

   docker pull kurento/kurento-media-server:|VERSION_KMS|

   docker run -d --name kurento --network host \
       kurento/kurento-media-server:|VERSION_KMS|

By default, KMS listens on the port **8888**. Clients wanting to control the media server using the :doc:`/features/kurento_protocol` should open a WebSocket connection to that port, either directly or by means of one of the provided :doc:`/features/kurento_client` SDKs.

The `health checker script <https://github.com/Kurento/kurento/blob/main/docker/kurento-media-server/healthchecker.sh>`__ inside this Docker image does something very similar in order to check if the container is healthy.

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
       kurento/kurento-media-server:|VERSION_KMS|



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

With this method, you will install Kurento Media Server from the native Ubuntu packages built by us.

Officially supported platforms: **Ubuntu 24.04 (noble)** (64-bits).

Open a terminal and run these commands:

1. Make sure that GnuPG is installed.

   .. code-block:: shell

      sudo apt-get update ; sudo apt-get install --no-install-recommends \
          gnupg

2. Add the Kurento repository to your system configuration.

   Run these commands:

   .. code-block:: shell

      # Get DISTRIB_* env vars.
      source /etc/upstream-release/lsb-release 2>/dev/null || source /etc/lsb-release

      # Add Kurento repository key for apt-get.
      sudo gpg -k
      sudo gpg --no-default-keyring \
               --keyring /etc/apt/keyrings/kurento.gpg \
               --keyserver hkp://keyserver.ubuntu.com:80 \
               --recv-keys 234821A61B67740F89BFD669FC8A16625AFA7A83

      # Add Kurento repository line for apt-get.
      sudo tee "/etc/apt/sources.list.d/kurento.list" >/dev/null <<EOF
      # Kurento Media Server - Release packages
      deb [signed-by=/etc/apt/keyrings/kurento.gpg] http://ubuntu.openvidu.io/|VERSION_KMS| $DISTRIB_CODENAME main
      EOF

3. Install KMS:

   .. note::

      This step applies **only for a first time installation**. If you already have installed Kurento and want to upgrade it, follow instead the steps described here: :ref:`installation-local-upgrade`.

   .. code-block:: shell

      sudo apt-get update ; sudo apt-get install --no-install-recommends \
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

      sudo apt-get update ; sudo apt-get dist-upgrade

   However, don't do this inside a Docker container. Running *apt-get upgrade* or *apt-get dist-upgrade* is frowned upon by the `Docker best practices`_; instead, you should just move to a newer version of the `Kurento Docker images`_.

B. **Uninstall the old Kurento version**, before installing the new one.

   Note however that **apt-get is not good enough** to remove all of Kurento packages. We recommend that you use *aptitude* for this, which works much better than *apt-get*:

   .. code-block:: shell

      sudo aptitude remove '?installed?version(kurento)'

      sudo apt-get update ; sudo apt-get install --no-install-recommends \
          kurento-media-server

.. note::

   Be careful! If you fail to upgrade **all** Kurento packages, you will get wrong behaviors and **crashes**. Kurento is composed of several packages:

   - *kurento-media-server*
   - *kurento-module-creator*
   - *kurento-module-core*
   - *kurento-module-elements*
   - *kurento-module-filters*
   - *libnice10*
   - *openh264*
   - And more

   To use a newer version **you have to upgrade all Kurento packages**, not only the first one.



.. _installation-stun-turn:

STUN/TURN server install
========================

Working with WebRTC *requires* developers to learn and have a good understanding about everything related to NAT, ICE, STUN, and TURN. If you don't know about these, you should start reading here: :ref:`faq-nat-ice-stun-turn`.

Kurento Media Server, just like any WebRTC endpoint, will work fine on its own, for *LAN* connections or for servers which have a public IP address assigned to them. However, sooner or later you will want to make your application work in a cloud environment with NAT firewalls, and allow KMS to connect with remote clients. At the same time, remote clients will probably want to connect from behind their own :term:`NAT` router too, so your application needs to be prepared to perform :term:`NAT Traversal` in both sides. This can be done by setting up a :term:`STUN` server or a :term:`TURN` relay, and configuring it **in both KMS and the client browser**.

These links contain the information needed to finish configuring your Kurento Media Server with a STUN/TURN server:

- :doc:`/user/configuration`
- :ref:`faq-coturn-install`
- :ref:`faq-stun-test`



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
       "http://127.0.0.1:8888/kurento"

You should get a response similar to this one:

.. code-block:: text

   HTTP/1.1 500 Internal Server Error
   Server: WebSocket++/0.8.1

Ignore the "*Server Error*" message: this is expected because we didn't send any actual message, but it is enough to prove that Kurento is up and listening for WebSocketconnections.

If you need to automate this, you could write a script similar to `healthchecker.sh`_, the one we use in `Kurento Docker images`_.



Checking RTP port connectivity
------------------------------

This section explains how you can verify that Kurento Media Server can be reached from a remote client machine, in scenarios where **the media server is not behind a NAT**.

You will take the role of an end user application, such as a web browser, wanting to send audio and video to the media server. For that, we'll use *Netcat* in the server, and either *Netcat* or `Ncat <https://nmap.org/ncat/>`__ in the client (because Ncat has more installation choices for Linux, Windows, and Mac clients).

The check proposed here will not work if the media server sits behind a NAT, because we are not punching holes in it (e.g. with STUN, see :ref:`faq-stun-needed`); doing so is outside of the scope for this section, but you could also do it by hand if needed (like shown in :ref:`nat-diy-holepunch`).

**First part: Server**

Follow these steps on the machine where Kurento Media Server is running.

* First, install Netcat, which is available for most Linux distributions. For example:

  .. code-block:: shell

     # For Debian/Ubuntu:
     sudo apt-get update ; sudo apt-get install netcat-openbsd

* Then, start a Netcat server, listening on any port of your choice:

  .. code-block:: shell

     # To test a TCP port:
     nc -vnl <server_port>

     # To test an UDP port:
     nc -vnul <server_port>

**Second part: Client**

Now move to a client machine, and follow the next steps.

* Install either of Netcat or Ncat. On Linux, Netcat is probably available as a package. On MacOS and Windows, it might be easier to download a prebuilt installer from the `Ncat downloads page <https://nmap.org/download.html>`__.

* Now, run Netcat or Ncat to connect with the server and send some test data. These examples use ``ncat``, but the options are the same if you use ``nc``:

  .. code-block:: shell

     # Linux, MacOS:
     ncat -vn  -p <client_port> <server_ip> <server_port>  # TCP
     ncat -vnu -p <client_port> <server_ip> <server_port>  # UDP

     # Windows:
     ncat.exe -vn  -p <client_port> <server_ip> <server_port>  # TCP
     ncat.exe -vnu -p <client_port> <server_ip> <server_port>  # UDP

  .. note::

     The ``-p <client_port>`` is optional. We're using it here so the source port is well known, allowing us to expect it on the server's Ncat output, or in the IP packet headers if packet analysis is being done (e.g. with *Wireshark* or *tcpdump*). Otherwise, the O.S. would assign a random source port for our client.

* When the connection has been established, try typing some words and press Return or Enter. If you see the text appearing on the server side of the connection, **the test has been successful**.

* If the test is successful, you will see the client's source port in the server output. If this number is *different* than the ``<client_port>`` you used, this means that the client is behind a :ref:`Symmetric NAT <nat-symmetric>`, and **a TURN relay will be required for WebRTC**.

* If the test data is not reaching the server, or the client command fails with a message such as ``Ncat: Connection refused``, it means the connection has failed. You should review the network configuration to make sure that a firewall or some other filtering device is not blocking the connection. This is an indication that there are some issues in the network, which gives you a head start to troubleshoot missing media in your application.

For example: Assume you want to connect from the port *3000* of a client whose public IP is *198.51.100.2*, to the port *55000* of your server at *203.0.113.2*. This is what both client and server terminals could look like:

.. code-block:: shell-session
   :emphasize-lines: 4

   # CLIENT

   $ ncat -vn -p 3000 203.0.113.2 55000
   Ncat: Connected to 203.0.113.2:55000
   (input) THIS IS SOME TEST DATA

.. code-block:: shell-session
   :emphasize-lines: 5

   # SERVER

   $ nc -vnl 55000
   Listening on 0.0.0.0 55000
   Connection received on 198.51.100.2 3000
   (output) THIS IS SOME TEST DATA

Notice how the server claims to have received a connection from the client's IP (*198.51.100.2*) and port (*3000*). This means that the client's NAT, if any, does not alter the source port of its outbound packets. If we saw here a different port, it would mean that the client's NAT is Symmetric, which usually requires using a TURN relay for WebRTC.



.. Links

.. _Amazon Web Services: https://aws.amazon.com
.. _Coturn: https://github.com/coturn/coturn
.. _Docker best practices: https://docs.docker.com/develop/develop-images/dockerfile_best-practices/#apt-get
.. _healthchecker.sh: https://github.com/Kurento/kurento/blob/main/docker/kurento-media-server/healthchecker.sh
.. _Kurento Docker images: https://hub.docker.com/r/kurento/kurento-media-server
