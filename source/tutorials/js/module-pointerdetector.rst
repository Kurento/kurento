%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
JavaScript Module - Pointer Detector Filter
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application consists of a `WebRTC`:term: video communication in mirror
(*loopback*) with a pointer tracking filter element.



Running this example
====================

First of all, install Kurento Media Server: :doc:`/user/installation`. Start the media server and leave it running in the background.

Install :term:`Node.js`, :term:`Bower`, and a web server in your system:

.. code-block:: bash

   curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
   sudo apt-get install -y nodejs
   sudo npm install -g bower
   sudo npm install -g http-server

Here, we suggest using the simple Node.js ``http-server``, but you could use any other web server.

You also need the source code of this tutorial. Clone it from GitHub, then start the web server:

.. code-block:: bash

    git clone https://github.com/Kurento/kurento-tutorial-js.git
    cd kurento-tutorial-js/kurento-pointerdetector/
    git checkout |VERSION_TUTORIAL_JS|
    bower install
    http-server -p 8443 --ssl --cert keys/server.crt --key keys/server.key

Note that HTTPS is required by browsers to enable WebRTC, so the web server must use SSL and a certificate file. For instructions, check :ref:`features-security-js-https`. For convenience, this tutorial already provides dummy self-signed certificates (which will cause a security warning in the browser).

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

2. In *index.js*, change the ``ws_uri`` to use Secure WebSocket (``wss://`` instead of ``ws://``) and the correct KMS port (8433 instead of 8888).

3. As explained in the link from step 1, if you configured KMS to use Secure WebSocket with a self-signed certificate you now have to browse to ``https://{KMS_HOST}:8433/kurento`` and click to accept the untrusted certificate.



Understanding this example
==========================

This application uses computer vision and augmented reality techniques to detect
a pointer in a WebRTC stream based on color tracking.

The interface of the application (an HTML web page) is composed by two HTML5
video tags: one for the video camera stream (the local client-side stream) and
other for the mirror (the remote stream). The video camera stream is sent to
Kurento Media Server, which processes and sends it back to the client as a
remote stream. To implement this, we need to create a `Media Pipeline`:term:
composed by the following `Media Element`:term: s:

.. figure:: ../../images/kurento-module-tutorial-pointerdetector-pipeline.png
   :align:   center
   :alt:     WebRTC with PointerDetector filter in loopback Media Pipeline

   *WebRTC with PointerDetector filter in loopback Media Pipeline*

The complete source code of this demo can be found in
`GitHub <https://github.com/Kurento/kurento-tutorial-js/tree/master/kurento-pointerdetector>`_.

This example is a modified version of the
:doc:`Magic Mirror <./tutorial-magicmirror>` tutorial. In this case, this
demo uses a **PointerDetector** instead of **FaceOverlay** filter.

In order to perform pointer detection, there must be a calibration stage, in
which the color of the pointer is registered by the filter. To accomplish this
step, the pointer should be placed in a square in the upper left corner of the
video, as follows:

.. figure:: ../../images/kurento-module-tutorial-pointerdetector-screenshot-01.png
   :align:   center
   :alt:     Pointer calibration stage

   *Pointer calibration stage*

.. note::

   Modules can have options. For configuring these options, you'll need to get the constructor for them.
   In Javascript and Node, you have to use *kurentoClient.getComplexType('qualifiedName')* . There is
   an example in the code.

In that precise moment, a calibration operation should be carried out. This is
done by clicking on the *Calibrate* blue button of the GUI.

After that, the color of the pointer is tracked in real time by Kurento Media
Server. ``PointerDetectorFilter`` can also define regions in the screen called
*windows* in which some actions are performed when the pointer is detected when
the pointer enters (``WindowIn`` event) and exits (``WindowOut`` event) the
windows. This is implemented in the JavaScript logic as follows:

.. sourcecode:: javascript

    ...
    kurentoClient.register('kurento-module-pointerdetector')
    const PointerDetectorWindowMediaParam = kurentoClient.getComplexType('pointerdetector.PointerDetectorWindowMediaParam')
    const WindowParam                     = kurentoClient.getComplexType('pointerdetector.WindowParam')
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

            console.log("SDP answer obtained. Processing ...");

            webRtc.gatherCandidates(onError);
            webRtcPeer.processAnswer(sdpAnswer);
          });

          var options =
          {
            calibrationRegion: WindowParam({
              topRightCornerX: 5,
              topRightCornerY:5,
              width:30,
              height: 30
            })
          };

          pipeline.create('pointerdetector.PointerDetectorFilter', options, function(error, _filter) {
            if (error) return onError(error);

            filter = _filter;

            var options = PointerDetectorWindowMediaParam({
              id: 'window0',
              height: 50,
              width:50,
              upperRightX: 500,
              upperRightY: 150
            });

            filter.addWindow(options, onError);

            var options = PointerDetectorWindowMediaParam({
              id: 'window1',
              height: 50,
              width:50,
              upperRightX: 500,
              upperRightY: 250
            });

            filter.addWindow(options, onError);

            filter.on ('WindowIn', function (data){
              console.log ("Event window in detected in window " + data.windowId);
            });

            filter.on ('WindowOut', function (data){
              console.log ("Event window out detected in window " + data.windowId);
            });

            console.log("Connecting ...");
            client.connect(webRtc, filter, webRtc, function(error) {
              if (error) return onError(error);

              console.log("WebRtcEndpoint --> Filter --> WebRtcEndpoint");
            });
          });
        });
      });
    });


The following picture illustrates the pointer tracking in one of the defined
windows:

.. figure:: ../../images/kurento-module-tutorial-pointerdetector-screenshot-02.png
   :align:   center
   :alt:     Pointer tracking over a window

   *Pointer tracking over a window*

In order to carry out the calibration process, this JavaScript function is used:

.. sourcecode:: javascript

   function calibrate() {
     if(filter) filter.trackColorFromCalibrationRegion(onError);
   }

   function onError(error) {
     if(error) console.error(error);
   }

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
`bower.json <https://github.com/Kurento/kurento-tutorial-js/blob/master/kurento-pointerdetector/bower.json>`_
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
