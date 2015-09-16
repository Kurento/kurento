%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
JavaScript Module Tutorial 1 - Pointer Detector Filter
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application consists on a `WebRTC`:term: video communication in mirror
(*loopback*) with a pointer tracking filter element.

For the impatient: running this example
=======================================

First of all, you should install Kurento Media Server to run this demo. Please
visit the :doc:`installation guide <../../installation_guide>` for further
information. In addition, the built-in module ``kms-pointerdetector-6.0``
should be also installed:

.. sourcecode:: sh

    sudo apt-get install kms-pointerdetector-6.0

Be sure to have installed `Node.js`:term: and `Bower`:term: in your system. In
an Ubuntu machine, you can install both as follows:

.. sourcecode:: sh

   curl -sL https://deb.nodesource.com/setup | sudo bash -
   sudo apt-get install -y nodejs
   sudo npm install -g bower

Due to `Same-origin policy`:term:, this demo has to be served by an HTTP server.
A very simple way of doing this is by means of a HTTP Node.js server which can
be installed using `npm`:term: :

.. sourcecode:: sh

   sudo npm install http-server -g

You also need the source code of this demo. You can clone it from GitHub. Then
start the HTTP server:

.. sourcecode:: sh

    git clone https://github.com/Kurento/kurento-tutorial-js.git
    cd kurento-tutorial-js/kurento-pointerdetector
    git checkout |TUTORIAL_JS_VERSION|
    bower install
    http-server

Finally access the application connecting to the URL http://localhost:8080/
through a WebRTC capable browser (Chrome, Firefox).

.. note::

   These instructions work only if Kurento Media Server is up and running in the same machine
   than the tutorial. However, it is possible to locate the KMS in other machine simple adding
   the parameter ``ws_uri`` to the URL, as follows:

   .. sourcecode:: sh

      http://localhost:8080/index.html?ws_uri=ws://kms_host:kms_port/kurento

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
:doc:`Magic Mirror <./tutorial-2-magicmirror>` tutorial. In this case, this
demo uses a **PointerDetector** instead of **FaceOverlay** filter.

In order to perform pointer detection, there must be a calibration stage, in
which the color of the pointer is registered by the filter. To accomplish this
step, the pointer should be placed in a square in the upper left corner of the
video, as follows:

.. figure:: ../../images/kurento-module-tutorial-pointerdetector-screenshot-01.png
   :align:   center
   :alt:     Pointer calibration stage

   *Pointer calibration stage*

In that precise moment, a calibration operation should be carried out. This is
done by clicking on the *Calibrate* blue button of the GUI.

After that, the color of the pointer is tracked in real time by Kurento Media
Server. ``PointerDetectorFilter`` can also define regions in the screen called
*windows* in which some actions are performed when the pointer is detected when
the pointer enters (``WindowIn`` event) and exits (``WindowOut`` event) the
windows. This is implemented in the JavaScript logic as follows:

.. sourcecode:: javascript

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

          pipeline.create('PointerDetectorFilter', options, function(error, _filter) {
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

   .. sourcecode:: sh

      http://localhost:8080/index.html?ice_servers=[{"urls":"stun:stun1.example.net"},{"urls":"stun:stun2.example.net"}]
      http://localhost:8080/index.html?ice_servers=[{"urls":"turn:turn.example.org","username":"user","credential":"myPassword"}]

Dependencies
============

The dependencies of this demo has to be obtained using `Bower`:term:. The
definition of these dependencies are defined in the
`bower.json <https://github.com/Kurento/kurento-tutorial-js/blob/master/kurento-pointerdetector/bower.json>`_
file, as follows:

.. sourcecode:: js

   "dependencies": {
      "kurento-client": "|CLIENT_JS_VERSION|",
      "kurento-utils": "|UTILS_JS_VERSION|"
      "kurento-module-pointerdetector": "|CLIENT_JS_VERSION|"
   }

To get these dependencies, just run the following shell command:

.. sourcecode:: sh

   bower install

.. note::

   We are in active development. You can find the latest versions at `Bower <http://bower.io/search/>`_.
