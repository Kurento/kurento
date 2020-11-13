%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
JavaScript Module - Plate Detector Filter
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application consists of a `WebRTC`:term: video communication in mirror
(*loopback*) with a plate detector filter element.

.. note::

   Web browsers require using *HTTPS* to enable WebRTC, so the web server must use SSL and a certificate file. For instructions, check :ref:`features-security-js-https`.

   For convenience, this tutorial already provides dummy self-signed certificates (which will cause a security warning in the browser).



Running this example
====================

First of all, install Kurento Media Server: :doc:`/user/installation`. Start the media server and leave it running in the background.

Install :term:`Node.js`, :term:`Bower`, and a web server in your system:

.. code-block:: console

   curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
   sudo apt-get install -y nodejs
   sudo npm install -g bower
   sudo npm install -g http-server

Here, we suggest using the simple Node.js ``http-server``, but you could use any other web server.

You also need the source code of this tutorial. Clone it from GitHub, then start the web server:

.. code-block:: console

    git clone https://github.com/Kurento/kurento-tutorial-js.git
    cd kurento-tutorial-js/kurento-platedetector/
    git checkout |VERSION_TUTORIAL_JS|
    bower install
    http-server -p 8443 --ssl --cert keys/server.crt --key keys/server.key

When your web server is up and running, use a WebRTC compatible browser (Firefox, Chrome) to open the tutorial page:

* If KMS is running in your local machine:

  .. code-block:: text

     https://localhost:8443/

* If KMS is running in a remote machine:

  .. code-block:: text

     https://localhost:8443/index.html?ws_uri=ws://{KMS_HOST}:8888/kurento

.. note::

   By default, this tutorial works out of the box by using non-secure WebSocket (``ws://``) to establish a client connection between the browser and KMS. This only works for ``localhost``. *It will fail if the web server is remote*.

If you want to run this tutorial from a **remote web server**, then you have to do 3 things:

1. Configure **Secure WebSocket** in KMS. For instructions, check :ref:`features-security-kms-wss`.

2. In *index.js*, change the ``ws_uri`` to use Secure WebSocket (``wss://`` instead of ``ws://``) and the correct KMS port (TCP 8433 instead of TCP 8888).

3. As explained in the link from step 1, if you configured KMS to use Secure WebSocket with a self-signed certificate you now have to browse to ``https://{KMS_HOST}:8433/kurento`` and click to accept the untrusted certificate.



.. note::

   This demo uses the **kms-platedetector** module, which must be installed in the media server. That module is available in the Kurento Apt repositories, so it is possible to install it with this command:

   .. code-block:: console

      sudo apt-get update && sudo apt-get install --yes kms-platedetector


Understanding this example
==========================

This application uses computer vision and augmented reality techniques to detect
a plate in a WebRTC stream on optical character recognition (OCR).

The interface of the application (an HTML web page) is composed by two HTML5
video tags: one for the video camera stream (the local client-side stream) and
other for the mirror (the remote stream). The video camera stream is sent to
Kurento Media Server, which processes and sends it back to the client as a
remote stream. To implement this, we need to create a `Media Pipeline`:term:
composed by the following `Media Element`:term: s:

.. figure:: ../../images/kurento-module-tutorial-platedetector-pipeline.png
   :align:   center
   :alt:     WebRTC with plateDetector filter Media Pipeline

   *WebRTC with plateDetector filter Media Pipeline*

The complete source code of this demo can be found in
`GitHub <https://github.com/Kurento/kurento-tutorial-java/tree/master/kurento-platedetector>`_.

This example is a modified version of the
:doc:`Magic Mirror <./tutorial-magicmirror>` tutorial. In this case, this
demo uses a **PlateDetector** instead of **FaceOverlay** filter. An screenshot
of the running example is shown in the following picture:

.. figure:: ../../images/kurento-module-tutorial-plate-screenshot-01.png
   :align:   center
   :alt:     Plate detector demo in action

   *Plate detector demo in action*

.. note::

   Modules can have options. For configuring these options, you'll need to get the constructor for them.
   In JavaScript and Node.js, you have to use *kurentoClient.getComplexType('qualifiedName')* . There is
   an example in the code.

The following snippet shows how the media pipeline is implemented in the Java
server-side code of the demo. An important issue in this code is that a
listener is added to the ``PlateDetectorFilter`` object
(``addPlateDetectedListener``). This way, each time a plate is detected in the
stream, a message is sent to the client side. As shown in the screenshot below,
this event is printed in the console of the GUI.

.. sourcecode:: javascript

    ...
    kurentoClient.register('kurento-module-platedetector')
    ...

    kurentoClient(args.ws_uri, function(error, client) {
      if (error) return onError(error);

      client.create('MediaPipeline', function(error, _pipeline) {
        if (error) return onError(error);

        pipeline = _pipeline;

        console.log("Got MediaPipeline");

        pipeline.create('WebRtcEndpoint', function(error, webRtc) {
          if (error) return onError(error);

          console.log("Got WebRtcEndpoint");

          setIceCandidateCallbacks(webRtcPeer, webRtc, onError)

          webRtc.processOffer(sdpOffer, function(error, sdpAnswer) {
            if (error) return onError(error);

            console.log("SDP answer obtained. Processing...");

            webRtc.gatherCandidates(onError);
            webRtcPeer.processAnswer(sdpAnswer);
          });

          pipeline.create('platedetector.PlateDetectorFilter', function(error, filter) {
            if (error) return onError(error);

            console.log("Got Filter");

            filter.on('PlateDetected', function (data){
              console.log("License plate detected " + data.plate);
            });

            client.connect(webRtc, filter, webRtc, function(error) {
              if (error) return onError(error);

              console.log("WebRtcEndpoint --> filter --> WebRtcEndpoint");
            });
          });
        });
      });
    });

.. note::

   The :term:`TURN` and :term:`STUN` servers to be used can be configured simple adding
   the parameter ``ice_servers`` to the application URL, as follows:

   .. sourcecode:: bash

      https://localhost:8443/index.html?ice_servers=[{"urls":"stun:stun1.example.net"},{"urls":"stun:stun2.example.net"}]
      https://localhost:8443/index.html?ice_servers=[{"urls":"turn:turn.example.org","username":"user","credential":"myPassword"}]

Dependencies
============

The dependencies of this demo has to be obtained using `Bower`:term:. The
definition of these dependencies are defined in the
`bower.json <https://github.com/Kurento/kurento-tutorial-js/blob/master/kurento-platedetector/bower.json>`_
file, as follows:

.. sourcecode:: js

   "dependencies": {
      "kurento-client": "|VERSION_CLIENT_JS|",
      "kurento-utils": "|VERSION_UTILS_JS|"
      "kurento-module-pointerdetector": "|VERSION_CLIENT_JS|"
   }

To get these dependencies, just run the following shell command:

.. sourcecode:: bash

   bower install

.. note::

   We are in active development. You can find the latest versions at `Bower <https://bower.io/search/>`_.
