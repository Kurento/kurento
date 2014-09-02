%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Kurento Server Installation Guide
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

.. highlight:: bash

Kurento Server has to be installed on **Ubuntu 14.04 LTS** (32 or 64 bits).

In order to install the latest stable Kurento Server version you have to type
the following commands, one at a time and in the same order as listed here.
When asked for any kind of confirmation, reply affirmatively:

.. sourcecode:: console

   sudo add-apt-repository ppa:kurento/kurento
   sudo apt-get update
   sudo apt-get install kurento-server

Finally, configure the server to run Kurento Server when booted:

.. sourcecode:: console

    sudo update-rc.d kurento-server defaults

Now, Kurento Server has been installed and started. Use the following commands
to start and stop it respectively:

.. sourcecode:: console

    sudo service kurento-server start
    sudo service kurento-server stop

Kurento Server has two files for logging because internally is composed by two
components: **Kurento Media Server (KMS)** and
**Kurento Control Server (KCS)**. The respective logs are located at
``/var/log/kurento/kurento-media-server.log`` and
``/var/log/kurento/kurento-control-server.log``.
