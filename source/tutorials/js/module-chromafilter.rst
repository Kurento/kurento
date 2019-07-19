%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
JavaScript Module - Chroma Filter
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application consists of a `WebRTC`:term: video communication in mirror
(*loopback*) with a chroma filter element.



Running this example
====================

First of all, install Kurento Media Server: :doc:`/user/installation`. Start the media server and leave it running in the background.

.. note::

   If you will run this tutorial from a remote machine (i.e. not from ``localhost``), then **you need to configure Secure WebSocket (wss://) in Kurento Media Server**. For instructions, check :ref:`features-security-kms-wss`.

   This is not an issue if you will run both KMS and the tutorial demo locally, because browsers (at least Chrome at the time of this writing) allow connecting to insecure WebSockets from HTTPS pages, as long as everything happens in ``localhost``.

Install :term:`Node.js`, :term:`Bower`, and a web server in your system:

.. code-block:: bash

   curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
   sudo apt-get install -y nodejs
   sudo npm install -g bower
   sudo npm install -g http-server

Here, we suggest using the simple Node.js ``http-server``, but you could use any other web server.

.. note::

   You need to configure the web server with HTTPS. For more information, check :ref:`features-security-js-https`.

You also need the source code of this demo; clone it from GitHub, then start the web server:

.. sourcecode:: bash

    git clone https://github.com/Kurento/kurento-tutorial-js.git
    cd kurento-tutorial-js/kurento-chroma
    git checkout |VERSION_TUTORIAL_JS|
    bower install
    http-server -p 8443 --ssl --cert keys/server.crt --key keys/server.key

Finally, access the web application by using a WebRTC-capable browser (Firefox, Chrome) to open the appropriate URL:

* If KMS is running in your local machine:

  .. code-block:: text

     https://localhost:8443/

* If KMS is running in a remote server:

  .. code-block:: text

     https://localhost:8443/index.html?ws_uri=wss://<KmsIp>:<KmsPort>/kurento

Understanding this example
==========================

This application uses computer vision and augmented reality techniques to detect
a chroma in a WebRTC stream based on color tracking.

The interface of the application (an HTML web page) is composed by two HTML5
video tags: one for the video camera stream (the local client-side stream) and
other for the mirror (the remote stream). The video camera stream is sent to
Kurento Media Server, which processes and sends it back to the client as a
remote stream. To implement this, we need to create a `Media Pipeline`:term:
composed by the following `Media Element`:term: s:

.. figure:: ../../images/kurento-module-tutorial-chroma-pipeline.png
   :align:   center
   :alt:     WebRTC with Chroma filter Media Pipeline

   *WebRTC with Chroma filter Media Pipeline*

The complete source code of this demo can be found in
`GitHub <https://github.com/Kurento/kurento-tutorial-java/tree/master/kurento-chroma>`_.

This example is a modified version of the
:doc:`Magic Mirror <./tutorial-magicmirror>` tutorial. In this case, this
demo uses a **Chroma** instead of **FaceOverlay** filter.

In order to perform chroma detection, there must be a color calibration stage.
To accomplish this step, at the beginning of the demo, a little square appears
in upper left of the video, as follows:

.. figure:: ../../images/kurento-module-tutorial-chroma-screenshot-01.png
   :align:   center
   :alt:     Chroma calibration stage

   *Chroma calibration stage*

In the first second of the demo, a calibration process is done, by detecting the
color inside that square. When the calibration is finished, the square
disappears and the chroma is substituted with the configured image. Take into
account that this process requires lighting condition. Otherwise the chroma
substitution will not be perfect. This behavior can be seen in the upper right
corner of the following screenshot:

.. figure:: ../../images/kurento-module-tutorial-chroma-screenshot-02.png
   :align:   center
   :alt:     Chroma filter in action

   *Chroma filter in action*

.. note::

   Modules can have options. For configure these options, you need get the constructor to them.
   In Javascript and Node, you have to use *kurentoClient.getComplexType('qualifiedName')* . There is
   an example in the code.

The media pipeline of this demo is is implemented in the JavaScript logic as
follows:

.. sourcecode:: javascript

    ...
    kurentoClient.register('kurento-module-chroma')
    const WindowParam = kurentoClient.getComplexType('chroma.WindowParam')
    ...

    kurentoClient(args.ws_uri, function(error, client) {
      if (error) return onError(error);

      client.create('MediaPipeline', function(error, _pipeline) {
        if (error) return onError(error);

        pipeline = _pipeline;

        console.log("Got MediaPipeline");

        pipeline.create('WebRtcEndpoint', function(error, webRtc) {
          if (error) return onError(error);

          setIceCandidateCallbacks(webRtcPeer, webRtc, onError)

          webRtc.processOffer(sdpOffer, function(error, sdpAnswer) {
            if (error) return onError(error);

            console.log("SDP answer obtained. Processing...");

            webRtc.gatherCandidates(onError);
            webRtcPeer.processAnswer(sdpAnswer);
          });

          console.log("Got WebRtcEndpoint");

          var options =
          {
            window: WindowParam({
              topRightCornerX: 5,
              topRightCornerY: 5,
              width: 30,
              height: 30
            })
          }

          pipeline.create('chroma.ChromaFilter', options, function(error, filter) {
            if (error) return onError(error);

            console.log("Got Filter");

            filter.setBackground(args.bg_uri, function(error) {
              if (error) return onError(error);

              console.log("Set Image");
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
`bower.json <https://github.com/Kurento/kurento-tutorial-js/blob/master/kurento-chroma/bower.json>`_
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
