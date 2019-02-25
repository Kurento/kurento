==========================
Frequently Asked Questions
==========================

About NAT, ICE, STUN, TURN
==========================

What is NAT?
------------

*Network Address Translation* (**NAT**) is a mechanism that hides from the public access the private IP addresses of machines inside a network. This NAT mechanism is typically found in all types of network devices, ranging from home routers to full-fledged corporate firewalls. In all cases the effect is the same: machines inside the NAT cannot be freely accessed from outside.

The effects of a NAT is very negative for WebRTC communications: machines inside the network will be able to send data to the outside, but they won't be able to receive data from remote endpoints that are sitting outside the network. In order to allow for this need, NAT devices typically allow to configure **NAT bindings** to let data come in from the outside part of the network; creating these NAT bindings is what is called :term:`NAT traversal`, also commonly referred as "opening ports".



What is ICE?
------------

*Interactive Connectivity Establishment* (**ICE**) is a protocol used for :term:`NAT traversal`. It defines a technique that allows communication between two endpoints when one is inside a NAT and the other is outside of it. The net effect of the ICE process is that the NAT will be left with all needed ports open for communication, and both endpoints will have complete information about the IP address and ports where the other endpoint can be contacted.

ICE doesn't work standalone: it needs to use a helper protocol called STUN.



What are STUN and TURN?
-----------------------

*Session Traversal Utilities for NAT* (**STUN**) is a protocol that complements ICE in the task of solving the :term:`NAT traversal` issue. It can be used by any endpoints to determine the IP address and port allocated to it by a NAT. It can also be used to check connectivity between two endpoints, and as a keep-alive protocol to maintain NAT bindings. STUN works with many existing NATs, and does not require any special behavior from them.

*Traversal Using Relays around NAT* (**TURN**) is an extension of STUN, used where the NAT security policies are too strict and the needed NAT bindings cannot be successfully created. In these situations, it is necessary for the host to use the services of an intermediate node that acts as a communication relay.

.. note::

   **TURN is an extension of STUN**. This means that *you don't need to configure a STUN server if you are already using a TURN server*.



.. _faq-stun:

When is STUN needed?
====================

**STUN is needed for every peer behind a NAT**. All NAT-ed peers need to open their own NAT ports, doing :term:`NAT traversal` by using a STUN server that is *outside of the NAT*.

Let's see this with an example: The typical installation scenario for Kurento Media Server is to have a strict separation between Application Server and client. KMS and Application Server are running in a cloud machine **without any NAT** or port restriction on incoming connections, while a browser client runs from any (possibly restricted) network that forbids incoming connections on any port that hasn't been "opened" in advance. The client may communicate with the Application Server for signaling purposes, but at the end of the day the bulk of the communication is done between the WebRTC engines of the browser and KMS.

.. figure:: /images/faq-stun-1.png
   :align:  center
   :alt:    NAT client without STUN

In scenarios such as this one, the client is able to send data to KMS because its NAT will allow outgoing packets. However, KMS will *not* be able to send data to the client, because the client's NAT is closed for incoming packets. This is solved by configuring the client to use some STUN server, then opening the appropriate ports in the NAT by using the STUN protocol. After this operation, the client is now able to receive audio/video streams from KMS:

.. figure:: /images/faq-stun-2.png
   :align:  center
   :alt:    NAT client with STUN

This procedure is called :term:`ICE`. Note that in reality you *can* also deploy KMS behind a NAT firewall, as long as KMS itself is also configured to open its own NAT ports by following the same procedure (again, with a STUN server that is outside of the NAT).

.. note::

   The features provided by TURN are a superset of those provided by STUN. This means that *you don't need to configure a STUN server if you are already using a TURN server*.



How To ...
==========

Install Coturn (TURN/STUN server)
---------------------------------

If you are installing Kurento in a NAT environment (eg. in any cloud provider), you need to use a TURN/STUN server, and configure KMS appropriately in
``/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini``.

Apart from that, you need to open all UDP ports in your security group, as STUN/TURN will use any port available from the whole 0-65535 range.

On Ubuntu, Coturn can be installed directly from the package repositories:

.. code-block:: bash

   sudo apt-get install coturn

1. Edit the file ``/etc/turnserver.conf`` and configure the TURN server.

   - For Amazon EC2 or similar, the Local and External IPs should be configured via the ``relay-ip`` and ``external-ip`` parameters, respectively.

   - Enable the options needed for WebRTC:

     - ``fingerprint``
     - ``lt-cred-mech``
     - ``realm=kurento.org``

   - Create a user and a password for the TURN server. As an example, the user "kurento" and password "kurentopw" are used. Add them in the configuration file: ``user=kurento:kurentopw``.

   - Optionally, debug logging messages can be suppressed so they don't clutter the standard output, enabling the option ``no-stdout-log``.

   - Other parameters can be tuned as needed. For more information, check the Coturn help pages:

     - https://github.com/coturn/coturn/wiki/turnserver
     - https://github.com/coturn/coturn/wiki/CoturnConfig

2. Edit the file ``/etc/default/coturn`` and uncomment ``TURNSERVER_ENABLED=1``, so the TURN server initiates automatically as a system service daemon.

3. Configure KMS and point it to where the TURN server is listening for connections. Edit the file ``/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini`` and set the ``turnURL`` parameter:

   .. code-block:: bash

      turnURL=<user>:<password>@<serverIp>:<serverPort>
      turnURL=kurento:kurentopw@<serverIp>:3478

   The parameter ``serverIp`` should be the public IP address of the TURN server. It must be an IP address, **not a domain name**.

   The following ports should be open in the firewall:

   - 3478 TCP & UDP.
   - 49152 - 65535 UDP: As per :rfc:`5766`, these are the ports that the TURN server will use to exchange media. These ports can be changed using the ``min-port`` and ``max-port`` parameters on the TURN server.

   .. note::
      While the RFC specifies the ports used by TURN, if you are using STUN you will need to open **all UDP ports**, as STUN doesn't constrain the range of ports that might be used.

4. Lastly, start the ``Coturn`` server and the media server:

   .. code-block:: bash

      sudo service coturn start
      sudo service kurento-media-server restart

.. note::

   Make sure to check your installation using this test application:

   https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/



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



Why do I get the error ...
==========================

"Cannot create gstreamer element"?
----------------------------------

This is a typical error which happens when you update Kurento Media Server from version 4 to 5. The problem is related to the GStreamer dependency version. The solution is the following:

.. code-block:: bash

   sudo apt-get remove kurento*
   sudo apt-get autoremove
   sudo apt-get update
   sudo apt-get dist-upgrade
   sudo apt-get install kurento-media-server
