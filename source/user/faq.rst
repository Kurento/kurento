==========================
Frequently Asked Questions
==========================

.. _faq-nat-ice-stun-turn:

About NAT, ICE, STUN, TURN
==========================

What is NAT?
------------

*Network Address Translation* (:term:`NAT`) is a mechanism that hides from the public access the private IP addresses of machines inside a network. This NAT mechanism is typically found in all types of network devices, ranging from home routers to full-fledged corporate firewalls. In all cases the effect is the same: machines inside the NAT cannot be freely accessed from outside.

The effects of a NAT is very negative for WebRTC communications: machines inside the network will be able to send data to the outside, but they won't be able to receive data from remote endpoints that are sitting outside the network. In order to allow for this need, NAT devices typically allow to configure **NAT bindings** to let data come in from the outside part of the network; creating these NAT bindings is what is called :term:`NAT traversal`, also commonly referred as "opening ports".



What is ICE?
------------

*Interactive Connectivity Establishment* (:term:`ICE`) is a protocol used for :term:`NAT traversal`. It defines a technique that allows communication between two endpoints when one is inside a NAT and the other is outside of it. The net effect of the ICE process is that the NAT will be left with all needed ports open for communication, and both endpoints will have complete information about the IP address and ports where the other endpoint can be contacted.

ICE doesn't work standalone: it needs to use a helper protocol called STUN.



What are STUN and TURN?
-----------------------

*Session Traversal Utilities for NAT* (:term:`STUN`) is a protocol that complements :term:`ICE` in the task of solving the :term:`NAT traversal` issue. It can be used by any endpoints to determine the IP address and port allocated to it by a NAT. It can also be used to check connectivity between two endpoints, and as a keep-alive protocol to maintain NAT bindings. STUN works with many existing NATs, and does not require any special behavior from them.

*Traversal Using Relays around NAT* (:term:`TURN`) is an extension of STUN, used where the NAT security policies are too strict and the needed NAT bindings cannot be successfully created. In these situations, it is necessary for the host to use the services of an intermediate node that acts as a communication relay.

.. note::

   **Every TURN server supports STUN**, because TURN is just an extension of STUN, to provide for a network relay. This means that *you don't need to set a STUN server up if you have already configured a TURN server*.



.. _faq-stun:

When is STUN needed?
--------------------

:term:`STUN` (and possibly :term:`TURN`) is needed **for every endpoint behind a NAT**. All peers that try to connect from behind a :term:`NAT` will need to "*open*" their own NAT ports, a process that is known as :term:`NAT traversal`. This is achieved by using a STUN/TURN server that is deployed *outside of the NAT*.

The STUN/TURN server is configured to use a range of UDP & TCP ports. All those ports should also be opened to all traffic, in the server's network configuration or security group.

If you are installing Kurento in a NAT environment (e.g. if your server is behind a NAT firewall), you also need to configure an external STUN or TURN server in */etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini*. Similarly, all browser clients that are behind a NAT need to configure the STUN and/or TURN server details with the ``iceServers`` field of the `RTCPeerConnection constructor <https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection/RTCPeerConnection>`__.

**Example:**

The typical installation scenario for Kurento Media Server is to have a strict separation between Application Server and client. KMS and Application Server are running in a cloud machine **without any NAT** or port restriction on incoming connections, while a browser client runs from any (possibly restricted) network that forbids incoming connections on any port that hasn't been "opened" in advance (i.e., a NAT). The client may communicate with the Application Server for signaling purposes, but at the end of the day the bulk of the audio/video communication is done between the WebRTC engines of the browser and KMS.

.. figure:: /images/faq-stun-1.png
   :align:  center
   :alt:    NAT client without STUN

In scenarios such as this one, the client is able to send data to KMS because its NAT will allow outgoing packets. However, KMS will *not* be able to send data to the client, because the client's NAT is closed for incoming packets. This is solved by configuring the client to use a STUN server; this server will be used by the client's browser to open the appropriate ports in the NAT. After this operation, the client is now able to receive audio/video streams from KMS:

.. figure:: /images/faq-stun-2.png
   :align:  center
   :alt:    NAT client with STUN

This procedure is done by the :term:`ICE` implementation of the client's browser.

Note that you *can* also deploy KMS behind a NAT firewall, as long as KMS itself is also configured to use a STUN/TURN server.

Further reading:

* `WebRTC - How many STUN/TURN servers do I need to specify? <https://stackoverflow.com/questions/23292520/webrtc-how-many-stun-turn-servers-do-i-need-to-specify/23307588#23307588>`__.
* `What are STUN, TURN, and ICE? <https://www.twilio.com/docs/stun-turn/faq#faq-what-is-nat>`__ (`archive <https://web.archive.org/web/20181009181338/https://www.twilio.com/docs/stun-turn/faq>`__).



How to install Coturn?
----------------------

Coturn is a :term:`STUN` server and (optionally) a :term:`TURN` relay, supporting all features required for the :term:`ICE` protocol and allowing to establish WebRTC connections from behind a :term:`NAT`.

Coturn can be installed directly from the Ubuntu package repositories:

.. code-block:: bash

   sudo apt-get update && sudo apt-get install --no-install-recommends --yes \
       coturn

Then, follow these steps:

1. Edit */etc/turnserver.conf* and configure the server according to your needs.

   This example configuration is a good first step; it will work for using Coturn with Kurento Media Server for WebRTC streams. However, you may want to change it according to your needs:

   .. code-block:: text

      # This server's external/public address, if Coturn is behind a NAT.
      # It must be an IP address, not a domain name.
      external-ip=<CoturnIp>

      # STUN listener port for UDP and TCP.
      # Default: 3478.
      #listening-port=<CoturnPort>

      # TURN lower and upper bounds of the UDP relay ports.
      # Default: 49152, 65535.
      #min-port=49152
      #max-port=65535

      # Uncomment to run server in 'normal' 'moderate' verbose mode.
      # Default: verbose mode OFF.
      #verbose

      # Use fingerprints in the TURN messages.
      fingerprint

      # Use long-term credential mechanism.
      lt-cred-mech

      # Realm used for the long-term credentials mechanism.
      realm=kurento.org

      # 'Static' user accounts for long-term credentials mechanism.
      user=<TurnUser>:<TurnPassword>

      # Set the log file name.
      # The log file can be reset sending a SIGHUP signal to the turnserver process.
      log-file=/var/log/turnserver/turnserver.log

      # Disable log file rollover and use log file name as-is.
      simple-log

   - The *external-ip* is necessary in cloud providers which use internal NATs, such as **Amazon EC2** (**AWS**). Write in ``<CoturnIp>`` your server's **public** IP address, like ``198.51.100.1``. It must be an IP address, **not a domain name**.

   - The options *fingerprint*, *lt-cred-mech*, and *realm* are needed for WebRTC.

   - The *user* parameter is the most basic form of authorization to use the TURN relay capabilities. Write your desired user name and password in the fields ``<TurnUser>`` and ``<TurnPassword>``.

   - Other parameters can be tuned as needed. For more information, check the Coturn help pages:

     - https://github.com/coturn/coturn/wiki/turnserver
     - https://github.com/coturn/coturn/wiki/CoturnConfig
     - A fully commented example configuration file: https://raw.githubusercontent.com/coturn/coturn/master/examples/etc/turnserver.conf

2. Edit the file */etc/default/coturn* and set

   .. code-block:: text

      TURNSERVER_ENABLED=1

   so the server starts automatically as a system service daemon.

3. Configure Kurento Media Server and point it to where the STUN/TURN server is listening for connections. Edit the file */etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini* and set either the STUN or the TURN parameters:

   .. code-block:: text

      stunServerAddress=<CoturnIp>
      stunServerPort=<CoturnPort>

   .. code-block:: text

      turnURL=<TurnUser>:<TurnPassword>@<CoturnIp>:<CoturnPort>

   If you only configure the STUN parameters in KMS, then the TURN relay capability of Coturn won't be used. Of course, if you instead configure the whole TURN URL, then KMS will be able to use the Coturn server as a TURN relay when it needs to.

   .. note::

      **Every TURN server supports STUN**, because TURN is just an extension of STUN, to provide for a network relay. This means that *you don't need to set a STUN server up if you have already configured a TURN server*.

   The following ports should be open in the firewall or your cloud machine's *Security Groups*:

   - **<CoturnPort>** (Default: 3478) UDP & TCP.
   - **49152-65535** UDP & TCP: As per :rfc:`5766`, these are the ports that the TURN server will use to exchange media. These ports can be changed using Coturn's ``min-port`` and ``max-port`` parameters.

   .. note::

      The STUN/TURN ports that are configured in Coturn **must match those configured in Kurento Media Server**. This can be done in the file */etc/kurento/modules/kurento/BaseRtpEndpoint.conf.ini*, so it is possible to restrict the port range used by KMS to have a reduced set of open ports in your server.

4. (Re)Start both Coturn and Kurento servers:

   .. code-block:: bash

      sudo service coturn restart
      sudo service kurento-media-server restart

5. Check that your STUN/TURN server is working, by using the `Trickle ICE test page <https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/>`__:

   - If you configured Coturn to act just as a STUN server, use an URI with this format: ``stun:<CoturnIp>:<CoturnPort>``.
   - If you left the default settings, then Coturn will act as both STUN and TURN server. Use an URI like this: ``turn:<CoturnIp>:<CoturnPort>``, and also write the ``<TurnUser>`` and ``<TurnPassword>``.
   - Finally, click on "*Gather candidates*" and check that you get candidates of type "**srflx**" (STUN) and "**relay**" (TURN).



Readings
--------

Here is a collection of all Kurento material talking about NAT, ICE, STUN, TURN:

* Frequently Asked Questions: :ref:`faq-nat-ice-stun-turn`
* Glossary: :term:`ICE`; :term:`STUN`; :term:`TURN`; :term:`NAT traversal`
* Installing and configuring Kurento: :ref:`installation-stun-turn`
* Troubleshooting network issues: :ref:`troubleshooting-webrtc-connection`
* Advanced knowledge: :doc:`/knowledge/nat`



How To ...
==========

Know how many Media Pipelines do I need for my Application?
-----------------------------------------------------------

Media Elements can only communicate with each other when they are part of the same pipeline. Different MediaPipelines in the server are independent do not share audio, video, data or events.

A good heuristic is that you will need one pipeline per each set of communicating partners in a channel, and one Endpoint in this pipeline per audio/video streams reaching a partner.



Know how many Endpoints do I need?
----------------------------------

Your application will need to create an Endpoint for each media stream flowing to (or from) the pipeline. As we said in the previous answer, each set of communicating partners in a channel will be in the same Media Pipeline, and each of them will use one or more Endpoints. They could use more than one if they are recording or reproducing several streams.



Know to what client a given WebRtcEndPoint belongs or where is it coming from?
------------------------------------------------------------------------------

Kurento API currently offers no way to get application attributes stored in a Media Element. However, the application developer can maintain a hashmap or equivalent data structure mapping the ``WebRtcEndpoint`` internal Id (which is a string) to whatever application information is desired.
