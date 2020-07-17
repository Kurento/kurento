%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Node.js Module - Plate Detector Filter
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application consists of a `WebRTC`:term: video communication in mirror
(*loopback*) with a plate detector filter element.

.. note::

   Web browsers require using *HTTPS* to enable WebRTC, so the web server must use SSL and a certificate file. For instructions, check :ref:`features-security-node-https`.

   For convenience, this tutorial already provides dummy self-signed certificates (which will cause a security warning in the browser).

For the impatient: running this example
=======================================

First of all, you should install Kurento Media Server to run this demo. Please
visit the :doc:`installation guide </user/installation>` for further
information. In addition, the built-in module ``kms-platedetector`` should
be also installed:

.. sourcecode:: bash

    sudo apt-get install kms-platedetector

.. warning::

   Plate detector module is a prototype and its results is not
   always accurate. Consider this if you are planning to use this
   module in a production environment.

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
    cd kurento-tutorial-node/kurento-platedetector
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
   In Javascript and Node, you have to use *kurentoClient.getComplexType('qualifiedName')* . There is
   an example in the code.

The following snippet shows how the media pipeline is implemented in the Java
server-side code of the demo. An important issue in this code is that a
listener is added to the ``PlateDetectorFilter`` object
(``addPlateDetectedListener``). This way, each time a plate is detected in the
stream, a message is sent to the client side. As shown in the screenshot below,
this event is printed in the console of the GUI.

.. sourcecode:: javascript

   ...
   kurento.register('kurento-module-platedetector');
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

                       filter.on('PlateDetected', function (data){
                           return callback(null, 'plateDetected', data);
                       });

                       webRtcEndpoint.processOffer(sdpOffer, function(error, sdpAnswer) {
                           if (error) {
                               pipeline.release();
                               return callback(error);
                           }

                           sessions[sessionId] = {
                               'pipeline' : pipeline,
                               'webRtcEndpoint' : webRtcEndpoint
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

           pipeline.create('platedetector.PlateDetectorFilter', function(error, filter) {
               if (error) {
                   return callback(error);
               }

               return callback(null, webRtcEndpoint, filter);
           });
       });
   }

Dependencies
============

Dependencies of this demo are managed using NPM. Our main dependency is the
Kurento Client JavaScript (*kurento-client*). The relevant part of the
`package.json <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-platedetector/package.json>`_
file for managing this dependency is:

.. sourcecode:: js

   "dependencies": {
      "kurento-client" : "|VERSION_CLIENT_JS|"
   }

At the client side, dependencies are managed using Bower. Take a look to the
`bower.json <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-platedetector/static/bower.json>`_
file and pay attention to the following section:

.. sourcecode:: js

   "dependencies": {
      "kurento-utils" : "|VERSION_UTILS_JS|",
      "kurento-module-pointerdetector": "|VERSION_CLIENT_JS|"
   }

.. note::

   We are in active development. You can find the latest versions at
   `npm <https://npmsearch.com/>`_ and `Bower <https://bower.io/search/>`_.
