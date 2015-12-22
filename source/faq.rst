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

   Though for most situations it's enough to configure a STUN server in the KMS
   configuration files, you might need to install a :term:`TURN` server, for example
   `coturn <https://code.google.com/p/coturn/>`__. Here are some instructions
   on how to install this TURN server for Kurento:

   1. Download the package from the
   `project's page <https://code.google.com/p/coturn/wiki/Downloads>`__.

   2. Extract the contents. You should have a ``INSTALL`` file with
   instructions, and a ``.deb`` package. Follow the instructions to install the
   package.

   3. Once the package is installed, you'll need to modify the startup script
   in ``/etc/init.d/coturn``.

      - Add the external and local IPs as vars::

            EXTERNAL_IP=$(curl http://169.254.169.254/latest/meta-data/public-ipv4)
            LOCAL_IP=$(curl http://169.254.169.254/latest/meta-data/local-ipv4)

      - Modify the DAEMON_ARGS var to take these IPs into account, along
        with the long-term credentials user and password (``kurento:kurento`` in
        this case, but could be different), realm and some other options::

             DAEMON_ARGS="-c /etc/turnserver.conf -f -o -a -v -r kurento.org
             -u kurento:kurento --no-stdout-log --external-ip $EXTERNAL_IP/$LOCAL_IP"

   4. Then let's enable the turnserver to run as an automatic service daemon. For this,
   open the file ``/etc/default/coturn`` and uncomment the key::

      TURNSERVER_ENABLED=1

   5. Now, you have to tell the Kurento server where is the turnserver
   installed. For this, modify the turnURL key in ``/etc/kurento/modules/kurento/WebRtcEndpoint.conf.ini``::

      turnURL=kurento:kurento@<public-ip>:3478

   The following ports should be open in the firewall:

      - 3478 TCP & UDP

      - 49152 - 65535 UDP: As per `RFC 5766 <http://tools.ietf.org/html/rfc5766>`__, these are the ports that the
        TURN server will use to exchange media. These ports can be changed
        using the ``--max-port`` and ``--min-port`` options from the turnserver.
        

   6. The last thing to do, is to start the coturn server and the media
   server::

      sudo service coturn start && sudo service kurento-media-server-6.0 restart

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
    dependency version. The solution is the following::

       sudo apt-get remove kurento*
       sudo apt-get autoremove
       sudo apt-get update
       sudo apt-get dist-upgrade
       sudo apt-get install kurento-media-server-6.0


.. Why can't I...
.. --------------
