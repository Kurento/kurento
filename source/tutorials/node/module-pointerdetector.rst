%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Node.js Module - Pointer Detector Filter
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application consists of a `WebRTC`:term: video communication in mirror
(*loopback*) with a pointer tracking filter element.

.. note::

   Web browsers require using *HTTPS* to enable WebRTC, so the web server must use SSL and a certificate file. For instructions, check :ref:`features-security-node-https`.

   For convenience, this tutorial already provides dummy self-signed certificates (which will cause a security warning in the browser).

For the impatient: running this example
=======================================

First of all, you should install Kurento Media Server to run this demo. Please
visit the :doc:`installation guide </user/installation>` for further
information. In addition, the built-in module ``kms-pointerdetector``
should be also installed:

.. sourcecode:: bash

    sudo apt-get install kms-pointerdetector

Be sure to have installed `Node.js`:term: and `Bower`:term: in your system. In
an Ubuntu machine, you can install both as follows:

.. sourcecode:: bash

   curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
   sudo apt-get install -y nodejs
   sudo npm install -g bower

To launch the application, you need to clone the GitHub project where this demo
is hosted, install it and run it:

.. sourcecode:: bash

    git clone https://github.com/Kurento/kurento-tutorial-node.git
    cd kurento-tutorial-node/kurento-pointerdetector
    git checkout |VERSION_TUTORIAL_NODE|
    npm install

If you have problems installing any of the dependencies, please remove them and
clean the npm cache, and try to install them again:

.. sourcecode:: bash

    rm -r node_modules
    npm cache clean

Finally, access the application connecting to the URL https://localhost:8443/
through a WebRTC capable browser (Chrome, Firefox).

.. note::

   These instructions work only if Kurento Media Server is up and running in the same machine
   as the tutorial. However, it is possible to connect to a remote KMS in other machine, simply adding
   the argument ``ws_uri`` to the npm execution command, as follows:

   .. sourcecode:: bash

      npm start -- --ws_uri=ws://{KMS_HOST}:8888/kurento

   In this case you need to use npm version 2. To update it you can use this command:

   .. sourcecode:: bash

      sudo npm install npm -g

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
   In JavaScript and Node.js, you have to use *kurentoClient.getComplexType('qualifiedName')* . There is
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
   kurento.register('kurento-module-pointerdetector');
   const PointerDetectorWindowMediaParam = kurento.getComplexType('pointerdetector.PointerDetectorWindowMediaParam');
   const WindowParam                     = kurento.getComplexType('pointerdetector.WindowParam');
   ...

   function start(sessionId, ws, sdpOffer, callback) {
       if (!sessionId) {
           return callback('Cannot use undefined sessionId');
       }

       getKurentoClient(function(error, kurentoClient) {
           if (error) {
               return callback(error);
           }

           kurentoClient.create('MediaPipeline', function(error, pipeline) {
               if (error) {
                   return callback(error);
               }

               createMediaElements(pipeline, ws, function(error, webRtcEndpoint, filter) {
                   if (error) {
                       pipeline.release();
                       return callback(error);
                   }

                   if (candidatesQueue[sessionId]) {
                       while(candidatesQueue[sessionId].length) {
                           var candidate = candidatesQueue[sessionId].shift();
                           webRtcEndpoint.addIceCandidate(candidate);
                       }
                   }

                   connectMediaElements(webRtcEndpoint, filter, function(error) {
                       if (error) {
                           pipeline.release();
                           return callback(error);
                       }

                       webRtcEndpoint.on('OnIceCandidate', function(event) {
                           var candidate = kurento.getComplexType('IceCandidate')(event.candidate);
                           ws.send(JSON.stringify({
                               id : 'iceCandidate',
                               candidate : candidate
                           }));
                       });

                       filter.on('WindowIn', function (_data) {
                           return callback(null, 'WindowIn', _data);
                       });

                       filter.on('WindowOut', function (_data) {
                           return callback(null, 'WindowOut', _data);
                       });

                       var options1 = PointerDetectorWindowMediaParam({
                           id: 'window0',
                           height: 50,
                           width: 50,
                           upperRightX: 500,
                           upperRightY: 150
                       });
                       filter.addWindow(options1, function(error) {
                           if (error) {
                               pipeline.release();
                               return callback(error);
                           }
                       });

                       var options2 = PointerDetectorWindowMediaParam({
                           id: 'window1',
                           height: 50,
                           width:50,
                           upperRightX: 500,
                           upperRightY: 250
                       });
                       filter.addWindow(options2, function(error) {
                           if (error) {
                               pipeline.release();
                               return callback(error);
                           }
                       });

                       webRtcEndpoint.processOffer(sdpOffer, function(error, sdpAnswer) {
                           if (error) {
                               pipeline.release();
                               return callback(error);
                           }

                           sessions[sessionId] = {
                               'pipeline' : pipeline,
                               'webRtcEndpoint' : webRtcEndpoint,
                               'pointerDetector' : filter
                           }
                           return callback(null, 'sdpAnswer', sdpAnswer);
                       });

                       webRtcEndpoint.gatherCandidates(function(error) {
                           if (error) {
                               return callback(error);
                           }
                       });
                   });
               });
           });
       });
   }

   function createMediaElements(pipeline, ws, callback) {
       pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint) {
           if (error) {
               return callback(error);
           }

           var options = {
               calibrationRegion: WindowParam({
                   topRightCornerX: 5,
                   topRightCornerY:5,
                   width:30,
                   height: 30
               })
           };

           pipeline.create('pointerdetector.PointerDetectorFilter', options, function(error, filter) {
               if (error) {
                   return callback(error);
               }

               return callback(null, webRtcEndpoint, filter);
           });
       });
   }

The following picture illustrates the pointer tracking in one of the defined
windows:

.. figure:: ../../images/kurento-module-tutorial-pointerdetector-screenshot-02.png
   :align:   center
   :alt:     Pointer tracking over a window

   *Pointer tracking over a window*

In order to carry out the calibration process, this JavaScript function is used:

.. sourcecode:: javascript

   function calibrate() {
      if (webRtcPeer) {
         console.log("Calibrating...");
         var message = {
            id : 'calibrate'
         }
         sendMessage(message);
      }
   }

Dependencies
============

Dependencies of this demo are managed using NPM. Our main dependency is the
Kurento Client JavaScript (*kurento-client*). The relevant part of the
`package.json <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-pointerdetector/package.json>`_
file for managing this dependency is:

.. sourcecode:: js

   "dependencies": {
      "kurento-client" : "|VERSION_CLIENT_JS|"
   }

At the client side, dependencies are managed using Bower. Take a look to the
`bower.json <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-pointerdetector/static/bower.json>`_
file and pay attention to the following section:

.. sourcecode:: js

   "dependencies": {
      "kurento-utils" : "|VERSION_UTILS_JS|",
      "kurento-module-pointerdetector": "|VERSION_CLIENT_JS|"
   }

.. note::

   We are in active development. You can find the latest versions at
   `npm <https://npmsearch.com/>`_ and `Bower <https://bower.io/search/>`_.
