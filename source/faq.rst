.. _faq:

%%%%%%%%%%%
Kurento FAQ
%%%%%%%%%%%

This is a list of Frequently Asked Questions about Kurento. Feel free to suggest
new entries or different wording for answers!

How do I...
-----------

**...install Kurento Media Server in an Amazon EC2 instance?**

   You need to install a :term:`TURN` server, for example
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

            EXTERNAL_IP=<public-ip>
            LOCAL_IP=$(hostname -i)

      - Modify the DAEMON_ARGS var to take these IPs into account, along
        with the long-term credentials user and password (``kurento:kurento`` in
        this case, but could be different), realm and some other options::

             DAEMON_ARGS="-c /etc/turnserver.conf -f -o -a -v -r kurento.org -u kurento:kurento --no-stdout-log -o --external-ip $EXTERNAL_IP/$LOCAL_IP"

   4. Now, you have to tell the Kurento server where is the turnserver
   installed. For this, modify the turnURL key in ``/etc/kurento/kurento.conf.json``::

      "turnURL" : "kurento:kurento@<public-ip>:3478",

   The following ports should be open in the firewall:

      - 3478 TCP & UDP

      - 49152 - 65535 UDP: As per `RFC 5766 <http://tools.ietf.org/html/rfc5766>`__, these are the ports that the
        TURN server will use to exchange media. These ports can be changed
        using the ``--max-port`` and ``--min-port`` options from the turnserver.


**...configure Kurento Media Server to use Secure WebSocket (WSS)?**

   First, you need to change the configuration file of Kurento Media Server,
   i.e. ``/etc/kurento/kurento.conf.json``, uncommenting the following lines::

      "secure": {
        "port": 8433,
        "certificate": "defaultCertificate.pem",
        "password": ""
      },

   You will also need a PEM certificate that should be in the same path or
   the configuration file or you may need to specify the full path on ``certificate``
   field.

   Second, you have to change the WebSocket URI in your application logic. For
   instance, in the *hello-world* application within the tutorials, this would
   be done as follows:

   - Java: Changing this line in `HelloWorldApp.java <https://github.com/Kurento/kurento-tutorial-java/blob/master/kurento-hello-world/src/main/java/org/kurento/tutorial/helloworld/HelloWorldApp.java>`_::

      final static String DEFAULT_KMS_WS_URI = "wss://localhost:8433/kurento";

   - Browser JavaScript: Changing this line in `index.js <https://github.com/Kurento/kurento-tutorial-js/blob/master/kurento-hello-world/js/index.js>`_::

       const ws_uri = 'wss://' + location.hostname + ':8433/kurento';

   - Node.js: Changing this line in `app.js <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-hello-world/app.js>`_::

      const ws_uri = "wss://localhost:8433/kurento";

   If this PEM certificate is a signed certificate (by a Certificate Authority such
   as Verisign), then you are done. If you are going to use a self-signed certificate
   (suitable for development), then there is still more work to do.

   You can generate a self signed certificate by doing this::

      certtool --generate-privkey --outfile defaultCertificate.pem
      echo 'organization = your organization name' > certtool.tmpl
      certtool --generate-self-signed --load-privkey defaultCertificate.pem --template certtool.tmpl >> defaultCertificate.pem
      sudo chown nobody defaultCertificate.pem

   Due to the fact that the certificate is self-signed, applications will
   reject it by default. For this reason, you have to trust it.
   Regarding browser applications, it can be ignored by done via HTTPS in your browser
   to the WSS port (https://localhost:8433/ with the above configuration) and accepting
   the certificate permanently. Regarding Java applications, follow the instructions
   of this `link <http://www.mkyong.com/webservices/jax-ws/suncertpathbuilderexception-unable-to-find-valid-certification-path-to-requested-target/>`_
   (get ``InstallCert.java`` from `here <https://code.google.com/p/java-use-examples/source/browse/trunk/src/com/aw/ad/util/InstallCert.java>`_).
   Regarding Node applications, please take a look to this `link <https://github.com/coolaj86/node-ssl-root-cas/wiki/Painless-Self-Signed-Certificates-in-node.js>`_. 


**...know how many :rom:cls:`pipelines
<MediaPipeline>` do I need for my Application?**

    :rom:cls:`Media elements <MediaElement>` can only communicate with each
    other when they are part of the same pipeline. Different MediaPipelines in
    the server are independent do not share audio, video, data or events.

    A good heuristic is that you will need one pipeline per each set of
    communicating partners in a channel, and one Endpoint in this pipeline per
    audio/video streams reaching a partner.

**...know how many :rom:cls:`endpoints <Endpoint>` do I need?**

    Your application will need to create an endpoint for each media stream
    flowing to (or from) the pipeline. As we said in the previous answer, each
    set of communicating partners in a channel will be in the same *pipeline*,
    and each of them will use one oe more *endpoints*. They could use more than
    one if they are recording or reproducing several streams.

**...know to what client a given WebRtcEndPoint belongs or where is it coming from?**

    Kurento API currently offers no way to get application attributes stored
    in a :rom:cls:`MediaElement`. However, the application developer can
    maintain a hashmap or equivalent data structure mapping the
    :rom:cls:`WebRtcEndpoint`  internal Id (which is a string) to whatever
    application information is desired.

.. _intel_nvidia:

**...stop kurento installing nvidia drivers in my machine?**

    Kurento uses libopencv-dev to get auxiliary files for several Computer
    Vision algorythms in its filters. This package is part of the
    :wikipedia:`Open Source Computer Vision Library<en,OpenCV>`.

    libopencv-dev depends on libopencv-ocl-dev, which depends on
    libopencv-ocl2.4 which depends on virtual <libopencl1>, provided by any of

        * ocl-icd-libopencl1
        * nvidia-304
        * nvidia-304-updates
        * nvidia-319
        * nvidia-319-updates

    Further, ocl-icd-libopencl1 conflicts with all the nvidia-* packages.
    As the Ubuntu 13.10, when none of those packages are installed,
    the package manager chooses nvidia-319-updates, which breaks OpenGL
    acceleration for Intel graphics cards. To check for the problem,
    if you install kurento or need to install libopencv-dev on a Intel
    graphics computer, please do:

    .. sourcecode:: console

        $ dpkg-query -l nvidia*
        Desired=Unknown/Install/Remove/Purge/Hold
        | Status=Not/Inst/Conf-files/Unpacked/halF-conf/Half-inst/trig-aWait/Trig-pend
        |/ Err?=(none)/Reinst-required (Status,Err: uppercase=bad)
        ||/ Name                              Version           Architecture        Description
        +++-=================================-================-=============-==========================
        un  nvidia-304                        <none>                         (no description available)
        un  nvidia-304-updates                <none>                         (no description available)
        un  nvidia-319                        <none>                         (no description available)
        un  nvidia-319-updates                <none>                         (no description available)
        ii  ocl-icd-libopencl1:amd64          2.0.2-1ubuntu1   amd64         Generic OpenCL ICD Loader
        $ # if you have any of those five packages installed, all chances are that all will be ok
        $ # if you have neither, you should probably be installing ocl-icd-libopencl1 like:
        $ sudo apt-get install ocl-icd-libopencl1

.. Why do I get the error...
.. -------------------------


.. Why can't I...
.. --------------

