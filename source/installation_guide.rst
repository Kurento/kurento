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

Now, Kurento Media Server has been installed and started. Use the following
commands to start and stop it respectively:

.. sourcecode:: console

    sudo service kurento-media-server start
    sudo service kurento-media-server stop

Kurento Media Server has a log file located at
``/var/log/kurento-media-server/media-server.log``.
