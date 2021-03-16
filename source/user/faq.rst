==========================
Frequently Asked Questions
==========================

.. contents:: Table of Contents



.. _faq-nat-ice-stun-turn:

About NAT, ICE, STUN, TURN
==========================

These are very important concepts that developers must understand well to work with WebRTC. Here is a collection of all Kurento material talking about these acronyms:

* Glossary:

  - :term:`What is NAT <NAT>`?
  - :term:`What is NAT Traversal <NAT Traversal>`?
  - :term:`What is ICE <ICE>`?
  - :term:`What is STUN <STUN>`?
  - :term:`What is TURN <TURN>`?
  - :ref:`faq-turn-works`

* Installing and configuring a STUN/TURN server:

  - :ref:`faq-coturn-install`
  - :ref:`faq-stun-test`
  - :ref:`faq-stun-configure`

* Troubleshooting :ref:`troubleshooting-webrtc`
* Advanced knowledge: :doc:`/knowledge/nat`



.. _faq-stun-needed:

When are STUN and TURN needed?
------------------------------

:term:`STUN` (and possibly :term:`TURN`) is needed **for every WebRTC participant behind a NAT**. All peers that try to connect from behind a :term:`NAT` will need to auto-discover their own external IP address, and also open up ports for RTP data transmission, a process that is known as :term:`NAT Traversal`. This is achieved by using a STUN server which must be deployed **outside of the NAT**.

The STUN server uses a single port for client connections (3478 by default), so this port should be opened up for the public in the server's network configuration or *Security Group*. If using TURN relay, then the whole range of TURN ports (49152 to 65535 by default) should be opened up too, besides the client port. Depending on the features of the STUN/TURN server, these might be only UDP or both UDP and TCP ports. For example, *Coturn* uses both UDP and TCP in its default configuration.

If you are installing Kurento in a NAT environment (e.g. if your media server is behind a NAT firewall), you also need to configure an external STUN server, in ``/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini`` (check :ref:`faq-stun-configure` for more details). Similarly, all browser clients that are behind a NAT need to use the STUN server through the *iceServers* field of the `RTCPeerConnection constructor <https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection/RTCPeerConnection>`__.

**Example:**

Kurento Media Server and its Application Server are running in a cloud machine **without any NAT** or port restriction on incoming connections, while a browser client runs from a possibly restricted :term:`NAT` network that forbids incoming connections on any port that hasn't been "opened" in advance

The browser client may communicate with the Application Server for signaling purposes, but at the end of the day the bulk of the audio/video RTP transmission is done between the WebRTC engines of the browser and KMS.

.. figure:: /images/faq-stun-1.png
   :align:  center
   :alt:    NAT client without STUN

In scenarios like this, the client is able to send data to KMS because its NAT will allow outgoing packets. However, KMS will *not* be able to send data to the client, because the client's NAT is closed for incoming packets. This is solved by configuring the client to use a STUN server; this server will be used by the client's browser to open the appropriate ports in its own NAT. After this operation, the client is now able to receive audio/video streams from KMS:

.. figure:: /images/faq-stun-2.png
   :align:  center
   :alt:    NAT client with STUN

This procedure is done by the :term:`ICE` implementation of the client's browser.

Note that you *can* also deploy KMS behind a NAT firewall, as long as KMS itself is also configured to use a STUN server.

Further reading:

* `WebRTC - How many STUN/TURN servers do I need to specify? <https://stackoverflow.com/questions/23292520/webrtc-how-many-stun-turn-servers-do-i-need-to-specify/23307588#23307588>`__.
* `What are STUN, TURN, and ICE? <https://www.twilio.com/docs/stun-turn/faq#faq-what-is-nat>`__ (`archive <https://web.archive.org/web/20181009181338/https://www.twilio.com/docs/stun-turn/faq>`__).



.. _faq-turn-works:

How does TURN work?
-------------------

This is a *very* simplified explanation of TURN; for the complete details on how it works, read the :rfc:`8656` (*Traversal Using Relays around NAT (TURN)*).

TURN separates two network segments that cannot connect directly (otherwise, STUN and direct connections would be used). In order to allow for maximum probabilities of successful connections, TURN servers such as Coturn will enable both UDP and TCP protocols by default.

* When a WebRTC participant is behind a strict NAT or firewall that requires relay, it becomes a **TURN client**, contacting the TURN server on its client listening port (3478 by default, either UDP or TCP), and requesting a **TURN relay transport**.

  - The TURN server listens for client requests on both UDP and TCP ports, to maximize the chances that the client’s firewall will allow the connection.

  - The *TURN relay transport*, mentioned above, is a random port selected on the **TURN port range** of the TURN server. This range, again, can be either UDP or TCP, to maximize the chances that remote peers are also able to send RTP data to the server.

* When a remote WebRTC peer wants to send RTP data to the *TURN client*, it doesn’t send to it directly, instead it sends data towards the corresponding *TURN relay transport* of the TURN server. Then the server will relay this data through its client port (3478) towards the actual *TURN client*.



.. _faq-coturn-install:

How to install Coturn?
----------------------

Coturn is a :term:`STUN` server and :term:`TURN` relay, supporting all features required for the :term:`ICE` protocol and allowing to establish WebRTC connections from behind a :term:`NAT`.

Coturn can be installed directly from the Ubuntu package repositories:

.. code-block:: shell

   sudo apt-get update && sudo apt-get install --no-install-recommends --yes \
       coturn

To configure it for WebRTC, follow these steps:

1. Edit ``/etc/turnserver.conf``.

   This example configuration is a good baseline; it contains the minimum setup required for using Coturn with Kurento Media Server for WebRTC:

   .. code-block:: text

      # The external IP address of this server, if Coturn is behind a NAT.
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

      # TURN fingerprints in messages.
      fingerprint

      # TURN long-term credential mechanism.
      lt-cred-mech

      # TURN realm used for the long-term credential mechanism.
      realm=kurento.org

      # TURN static user account for long-term credential mechanism.
      user=<TurnUser>:<TurnPassword>

      # Set the log file name.
      # The log file can be reset sending a SIGHUP signal to the turnserver process.
      log-file=/var/log/turnserver/turnserver.log

      # Disable log file rollover and use log file name as-is.
      simple-log

   .. note::

      * The *external-ip* is necessary in cloud providers which use internal NATs, such as **Amazon EC2** (AWS). Write your server's **public** IP address, like *198.51.100.1*, in the field *<CoturnIp>*. **It must be an IP address, not a domain name**.

      * This example uses the "*long-term credential*" mechanism of Coturn with a static password, which is good enough for showcasing the setup. You write the desired user name and password in the fields *<TurnUser>* and *<TurnPassword>*, and provide them to KMS as static parameters.

        However, for real-world scenarios you might want to use dynamic passwords. Coturn can be integrated with external sources, such as PostgreSQL (`psql-userdb <https://github.com/coturn/coturn/blob/ae2ee1f4e4f4f4119425e3d890a7f6ca44b57d0b/examples/etc/turnserver.conf#L299>`__), MySQL (`mysql-userdb <https://github.com/coturn/coturn/blob/ae2ee1f4e4f4f4119425e3d890a7f6ca44b57d0b/examples/etc/turnserver.conf#L313>`__), MongoDB (`mongo-userdb <https://github.com/coturn/coturn/blob/ae2ee1f4e4f4f4119425e3d890a7f6ca44b57d0b/examples/etc/turnserver.conf#L331>`__), or Redis (`redis-userdb <https://github.com/coturn/coturn/blob/ae2ee1f4e4f4f4119425e3d890a7f6ca44b57d0b/examples/etc/turnserver.conf#L339>`__). You would handle this from your :doc:`Application Server </user/writing_applications>`, and then use the Kurento API to dynamically provide each individual WebRtcEndpoint with the correct parameters.

        Read :ref:`faq-stun-configure` for info about static and dynamic parameter configuration.

      * Comment out (or delete) all the TURN parameters if you only want Coturn acting as a STUN server.

      * Other settings can be tuned as needed. For more information, check the Coturn help pages:

        - Main project page: https://github.com/coturn/coturn/wiki/turnserver
        - Fully commented configuration file: https://github.com/coturn/coturn/blob/master/examples/etc/turnserver.conf
        - Additional docs on configuration: https://github.com/coturn/coturn/wiki/CoturnConfig

2. Edit the file ``/etc/default/coturn`` and set

   .. code-block:: shell

      TURNSERVER_ENABLED=1

   so the server starts automatically as a system service daemon.

3. Follow with the next sections to test that Coturn is working, and then set it up as your STUN/TURN server in both Kurento Media Server and the WebRTC clients.



.. _faq-stun-test:

How to test my STUN/TURN server?
--------------------------------

To test if your :term:`STUN`/:term:`TURN` server is functioning properly, open the `Trickle ICE test page <https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/>`__. In that page, follow these steps:

1. Remove any server that might be filled in already by default.

2. Fill in your STUN/TURN server details.

   - To only test STUN (TURN relay will not be tested):

     .. code-block:: text

        stun:<StunServerIp>:<StunServerPort>

   - To test both STUN and TURN:

     .. code-block:: text

        turn:<TurnServerIp>:<TurnServerPort>

     ... and also fill in the *TURN username* and *TURN password*.

3. Click on *Add Server*. You should have only **one entry** in the list, with your server details.

4. Click on *Gather candidates*. **Verify** that you get candidates of type *srflx* if you are testing STUN. Likewise, you should get candidates of type *srflx* *and* type *relay* if you are testing TURN.

   If you are missing any of the expected candidate types, *your STUN/TURN server is not working well* and WebRTC will fail. Check your server configuration, and your cloud provider's network settings.



.. _faq-stun-configure:

How to configure STUN/TURN in Kurento?
--------------------------------------

To configure a :term:`STUN` server or :term:`TURN` relay with Kurento Media Server, you may use either of two methods:

* **Static config**. If the STUN or TURN parameters are well know and will not change over time, write them into the file ``/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini``.

  To only use STUN server (TURN relay will not be used):

  .. code-block:: text

     stunServerAddress=<StunServerIp>
     stunServerPort=<StunServerPort>

  *<StunServerIp>* should be the public IP address of the STUN server. **It must be an IP address, not a domain name**. For example:

  .. code-block:: text

     stunServerAddress=198.51.100.1
     stunServerPort=3478

  To use both STUN server and TURN relay:

  .. code-block:: text

     turnURL=<TurnUser>:<TurnPassword>@<TurnServerIp>:<TurnServerPort>

  *<TurnServerIp>* should be the public IP address of the TURN relay. **It must be an IP address, not a domain name**. For example:

  .. code-block:: text

     turnURL=myuser:mypassword@198.51.100.1:3478

* **Dynamic config**. If the STUN or TURN parameters are not known beforehand (for example, if your TURN credentials are dynamically generated during run-time), use the Kurento API methods to set them.

  To only use STUN server (TURN relay will not be used):

  .. code-block:: text

     webRtcEndpoint.setStunServerAddress("<StunServerIp>");
     webRtcEndpoint.setStunServerPort(<StunServerPort>);

  To use both STUN server and TURN relay:

  .. code-block:: text

     webRtcEndpoint.setTurnUrl("<TurnUser>:<TurnPassword>@<TurnServerIp>:<TurnServerPort>");

  Client API:

  * Java: `setStunServerAddress <../_static/client-javadoc/org/kurento/client/WebRtcEndpoint.html#setStunServerAddress-java.lang.String->`__, `setStunServerPort <../_static/client-javadoc/org/kurento/client/WebRtcEndpoint.html#setStunServerPort-int->`__, `setTurnUrl <../_static/client-javadoc/org/kurento/client/WebRtcEndpoint.html#setTurnUrl-java.lang.String->`__.
  * JavaScript: `setStunServerAddress <../_static/client-jsdoc/module-elements.WebRtcEndpoint.html#setStunServerAddress>`__, `setStunServerPort <../_static/client-jsdoc/module-elements.WebRtcEndpoint.html#setStunServerPort>`__, `setTurnUrl <../_static/client-jsdoc/module-elements.WebRtcEndpoint.html#setTurnUrl>`__.

.. note::

   **You don't need to configure both STUN and TURN**, because TURN already includes STUN functionality.

The following ports should be open in the firewall or your cloud provider *Security Group*:

- **<CoturnPort>** (Default: 3478) UDP & TCP, unless you disable either UDP or TCP in Coturn (for example, with ``no-tcp``).
- **49152 to 65535** UDP & TCP: As per :rfc:`8656`, this port range will be used by a TURN relay to exchange media by default. These ports can be changed using Coturn's ``min-port`` and ``max-port`` settings. Again, you can disable using either TCP or UDP for the relay port range (for example, with ``no-tcp-relay``).

.. note::

   **Port ranges do NOT need to match between Coturn and Kurento Media Server**.

   If you happen to deploy both Coturn and KMS in the same machine, we recommend that their port ranges do not overlap.

When you are done, (re)start both Coturn and Kurento servers:

.. code-block:: shell

   sudo service coturn restart
   sudo service kurento-media-server restart



.. _faq-docker:

About using Kurento with Docker
===============================

Docker is the recommended method of deploying Kurento Media Server, because it makes it easy to bundle all of the different modules and dependencies into a single, manageable unit. This makes installation and upgrades a trivial operation. However, due to the nature of containers, it also makes configuration slightly more inconvenient, so in this section we'll provide a heads up in Docker concepts that could be very useful for users of `Kurento Docker images <https://hub.docker.com/r/kurento/kurento-media-server>`__.



How to provide configuration files?
-----------------------------------

To edit the configuration files that Kurento will use from within a Docker container, the first thing you'll need are the actual files; run these commands to get the default ones from a temporary container:

.. code-block:: shell

   CONTAINER="$(docker create kurento/kurento-media-server:latest)"
   docker cp "$CONTAINER":/etc/kurento/. ./etc-kurento
   docker rm "$CONTAINER"

After editing these files as needed, provide them to newly created Kurento Docker containers, with any of the mechanisms offered by Docker. Here we show examples for two of them:



FROM image
~~~~~~~~~~

Creating a custom Docker image is a good choice for changing Kurento configuration files when you don't have direct control of the host environment. The `FROM <https://docs.docker.com/engine/reference/builder/#from>`__ feature of *Dockerfiles* can be used to derive directly from the official `Kurento Docker image <https://hub.docker.com/r/kurento/kurento-media-server>`__ and create your own fully customized image.

A ``Dockerfile`` such as this one would be a good enough starting point:

.. code-block:: docker

   FROM kurento/kurento-media-server:latest
   COPY etc-kurento/* /etc/kurento/

Now, build the new image:

.. code-block:: shell-session

   $ docker build --tag kms-with-my-config:latest .
   Step 1/2 : FROM kurento/kurento-media-server:latest
   Step 2/2 : COPY etc-kurento/* /etc/kurento/
   Successfully built 3d2bedb31a9d
   Successfully tagged kms-with-my-config:latest

And use the new image "*kms-with-my-config:latest*" in place of the original one.



Bind mount
~~~~~~~~~~

A `bind-mount <https://docs.docker.com/storage/bind-mounts/>`__ will replace the default set of config files inside the official Kurento Docker image, with the ones you provide from the host filesystem. This method can be used if you are in control of the host system:

.. code-block:: shell

   docker run -d --name kms --network host \
       --mount type=bind,src="$PWD/etc-kurento",dst=/etc/kurento \
       kurento/kurento-media-server:latest

The equivalent definition for Docker Compose would look like this:

.. code-block:: yaml

   version: "3.8"
   services:
     kms:
       image: kurento/kurento-media-server:latest
       network_mode: host
       volumes:
         - type: bind
           source: ./etc-kurento
           target: /etc/kurento



Where are my recordings?
------------------------

A frequent question, by users who are new to Docker, is where the *RecorderEndpoint* files are being stored, because they don't show up anywhere in the host file system. The answer is that KMS is recording files *inside the container's local storage*, in the path defined by the *RecorderEndpoint* constructor (`Java <../_static/client-javadoc/org/kurento/client/RecorderEndpoint.Builder.html#Builder-org.kurento.client.MediaPipeline-java.lang.String->`__, `JavaScript <../_static/client-jsdoc/module-elements.RecorderEndpoint.html#.constructorParams>`__).

In general, running a Docker container **won't modify your host system** and **won't create new files** in it, at least by default. This is an integral part of how Docker containers work. To get those files out, you should use the mechanisms that Docker offers, like for example a `bind-mount <https://docs.docker.com/storage/bind-mounts/>`__ to the recording path.



About Kurento Media Pipelines
=============================

These questions relate to the concept of :term:`Media Pipeline` in Kurento, touching topics about architecture or performance.



How many simultaneous participants are supported?
-------------------------------------------------

This depends entirely on the performance of the machine where Kurento Media Server is running. The best thing you can do is performing an actual load test under your particular conditions.

The folks working on `OpenVidu <https://openvidu.io/>`__ (a WebRTC platform based on Kurento) conducted a study that you might find interesting:

* `OpenVidu load testing: a systematic study of OpenVidu platform performance <https://medium.com/@openvidu/openvidu-load-testing-a-systematic-study-of-openvidu-platform-performance-b1aa3c475ba9>`__.



How many Media Pipelines do I need for my Application?
------------------------------------------------------

A Pipeline is a top-level container that handles every resource that should be able to achieve any kind of interaction with each other. A :term:`Media Element` can only communicate when they are part of the same Pipeline. Different Pipelines in the server are independent and isolated, so they do not share audio, video, data or events.

99% times, this translates to using 1 Pipeline object for each "room"-like videoconference. It doesn't matter if there is 1 single presenter and N viewers ("one-to-many"), or if there are N participants Skype-style ("many-to-many"), all of them are managed by the same Pipeline. So, most actual real-world applications would only ever create 1 Pipeline, because that's good enough for most needs.

A good heuristic is that you will need one Pipeline per each set of communicating partners in a channel, and one Endpoint in this Pipeline per audio/video streams exchanged with a participant.



How many Endpoints do I need?
-----------------------------

Your application will need to create at least one Endpoint for each media stream flowing to (or from) each participant. You might actually need more, if the streams are to be recorded or if streams are being duplicated for other purposes.



Which participant corresponds to which Endpoint?
------------------------------------------------

The Kurento API offers no way to get application-level semantic attributes stored in a Media Element. However, the application developer can maintain a HashMap or equivalent data structure, storing the Endpoint identifiers (which are plain strings) to whatever application information is desired, such as the names of the participants.



How to retrieve known objects from the Media Server?
----------------------------------------------------

The usual workflow for an Application Server is to connect with the Media Server, and use RPC methods to *create* new MediaPipelines and Endpoints inside it. However, if you want to connect your Application Server with objects that *already exist* in the Media Server (as opposed to creating new ones), you can achieve it by querying by their ID. This is done with the "*describe*" method of the JSON-RPC API, as described in :doc:`/features/kurento_protocol`.

Client API:

* Java: `KurentoClient.getById <../_static/client-javadoc/org/kurento/client/KurentoClient.html#getById-java.lang.String-java.lang.Class->`__.
* JavaScript: `KurentoClient.getMediaobjectById <../_static/client-jsdoc/module-kurentoClient.KurentoClient.html#getMediaobjectById>`__.
