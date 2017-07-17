.. _faq:

%%%%%%%%%%%
Kurento FAQ
%%%%%%%%%%%

This is a list of Frequently Asked Questions about Kurento. Feel free to suggest
new entries or different wording for answers!

How do I...
===========

...install Kurento Media Server in an Amazon EC2 instance?
----------------------------------------------------------

   If you are installing Kurento in a NAT environment (i.e. in any cloud
   provider), you'll need to provide a STUN server configuration in
   ``/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini``. Apart from that,
   you will have to open all UDP ports in your security group, as STUN will use
   any port available from the whole 0-65535 range.

   Though for most situations it's enough to configure a STUN server in the KMS
   configuration files, you might need to install a :term:`TURN` server, for example
   `coturn <http://coturn.net/>`_.

   On Ubuntu 16.04 (Xenial), this TURN server can be installed directly from the
   package repositories:

   .. sourcecode:: bash

      sudo apt-get install coturn

   However, Ubuntu 14.04 (Trusty) lacks this package, but it can be downloaded
   and installed manually from the Debian repositories:

   1. Download the file ``coturn_<...>_amd64.deb`` from any of the mirrors
      listed here: https://packages.debian.org/jessie-backports/amd64/coturn/download

   2. Install it, together with all dependencies.

      .. sourcecode:: bash

         sudo apt-get update
         sudo apt-get install gdebi-core
         sudo gdebi coturn*.deb

   3. Edit the file ``/etc/turnserver.conf`` and configure the TURN server.

      - For Amazon EC2 or similar, the Local and External IPs should be configured
        via the ``relay-ip`` and ``external-ip`` parameters, respectively.

      - Enable the options needed for WebRTC:

        - ``fingerprint``
        - ``lt-cred-mech``
        - ``realm=kurento.org``

      - Create a user and a password in the system, which will be used by the
        long-term credentials mechanism. As an example, the user "kurento"
        and password "kurentopw" are used. Add them in the configuration file:
        ``user=kurento:kurentopw``.

      - Optionally, debug log messages can be prevented to be printed on the
        standard output, enabling the option ``no-stdout-log``.

      - Other parameters can be tuned as needed. For more information, check the
        coturn help pages:

        - https://github.com/coturn/coturn/wiki/turnserver
        - https://github.com/coturn/coturn/wiki/CoturnConfig

   4. Edit the file ``/etc/default/coturn`` and uncomment ``TURNSERVER_ENABLED=1``,
      so the TURN server run automatically as a system service daemon.

   5. Configure KMS and point it to where the TURN server is listening for
      connections. Edit the file ``/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini``
      and set the ``turnURL`` parameter::

         turnURL=kurento:kurentopw@<PublicIp>:3478

   The following ports should be open in the firewall:

   - 3478 TCP & UDP
   - 49152 - 65535 UDP: As per :rfc:`5766`, these are the ports that the
     TURN server will use to exchange media. These ports can be changed
     using the ``min-port`` and ``max-port`` parameters on the TURN server.

   .. note::
      While the RFC specifies the ports used by TURN, if you are using STUN you
      will need to open all UDP ports, as those ports are not constrained.

   6. The last thing to do, is to start the ``coturn`` server and the media
   server:

   .. sourcecode:: bash

      sudo service coturn start \
        && sudo service kurento-media-server-6.0 restart

   .. note::
      Make sure to check your installation using this test application:
      https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/

...know how many Media Pipelines do I need for my Application?
--------------------------------------------------------------

    Media Elements can only communicate with each other when they are part
    of the same pipeline. Different MediaPipelines in the server are
    independent do not share audio, video, data or events.

    A good heuristic is that you will need one pipeline per each set of
    communicating partners in a channel, and one Endpoint in this pipeline per
    audio/video streams reaching a partner.

...know how many Endpoints do I need?
-------------------------------------

    Your application will need to create an Endpoint for each media stream
    flowing to (or from) the pipeline. As we said in the previous answer, each
    set of communicating partners in a channel will be in the same Media
    Pipeline, and each of them will use one or more Endpoints. They could use
    more than one if they are recording or reproducing several streams.

...know to what client a given WebRtcEndPoint belongs or where is it coming from?
---------------------------------------------------------------------------------

    Kurento API currently offers no way to get application attributes stored
    in a Media Element. However, the application developer can maintain a
    hashmap or equivalent data structure mapping the ``WebRtcEndpoint``
    internal Id (which is a string) to whatever application information is
    desired.


Why do I get the error...
=========================

..."Cannot create gstreamer element"?
-------------------------------------

   This is a typical error which happens when you update Kurento Media
   Server from version 4 to 5. The problem is related to the GStreamer
   dependency version. The solution is the following:

   .. sourcecode:: bash

      sudo apt-get remove kurento*
      sudo apt-get autoremove
      sudo apt-get update
      sudo apt-get dist-upgrade
      sudo apt-get install kurento-media-server-6.0
