%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Node.js Module Tutorial 2 - Chroma Filter
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application consists on a `WebRTC`:term: video communication in mirror
(*loopback*) with a chroma filter element.

For the impatient: running this example
=======================================

First of all, you should install Kurento Media Server to run this demo. Please
visit the :doc:`installation guide <../../installation_guide>` for further
information. In addition, the built-in module ``kms-chroma-6.0`` should be also
installed:

.. sourcecode:: sh

    sudo apt-get install kms-chroma-6.0

Be sure to have installed `Node.js`:term: in your system. In an Ubuntu machine,
you can install both as follows:

.. sourcecode:: sh

   curl -sL https://deb.nodesource.com/setup | sudo bash -
   sudo apt-get install -y nodejs

To launch the application you need to clone the GitHub project where this demo
is hosted and then install and run it, as follows:

.. sourcecode:: sh

    git clone https://github.com/Kurento/kurento-tutorial-node.git
    cd kurento-tutorial-node/kurento-chroma
    git checkout |TUTORIAL_NODE_VERSION|
    npm install

If you have problems installing any of the dependencies, please remove them and
clean the npm cache, and try to install them again:

.. sourcecode:: sh

    rm -r node_modules
    npm cache clean

Finally access the application connecting to the URL http://localhost:8080/
through a WebRTC capable browser (Chrome, Firefox).

.. note::

   These instructions work only if Kurento Media Server is up and running in the same machine
   than the tutorial. However, it is possible to locate the KMS in other machine simple adding
   the argument ``ws_uri`` to the npm execution command, as follows:

   .. sourcecode:: sh

      npm start -- --ws_uri=ws://kms_host:kms_host:kms_port/kurento

   In this case you need to use npm version 2. To update it you can use this command:

   .. sourcecode:: sh

      sudo npm install npm -g

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
:doc:`Magic Mirror <./tutorial-2-magicmirror>` tutorial. In this case, this
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

The media pipeline of this demo is is implemented in the JavaScript logic as
follows:

.. sourcecode:: javascript

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
                           var candidate = kurento.register.complexTypes.IceCandidate(event.candidate);
                           ws.send(JSON.stringify({
                               id : 'iceCandidate',
                               candidate : candidate
                           }));
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
                           return callback(null, sdpAnswer);
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
               window: kurento.register.complexTypes.WindowParam({
                   topRightCornerX: 5,
                   topRightCornerY: 5,
                   width: 30,
                   height: 30
               })
           }
           pipeline.create('ChromaFilter', options, function(error, filter) {
               if (error) {
                   return callback(error);
               }

               return callback(null, webRtcEndpoint, filter);
           });
       });
   }

   function connectMediaElements(webRtcEndpoint, filter, callback) {
       webRtcEndpoint.connect(filter, function(error) {
           if (error) {
               return callback(error);
           }

           filter.setBackground(url.format(asUrl) + 'img/mario.jpg', function(error) {
               if (error) {
                   return callback(error);
               }

               filter.connect(webRtcEndpoint, function(error) {
                   if (error) {
                       return callback(error);
                   }

                   return callback(null);
               });
           });
       });
   }

Dependencies
============

Dependencies of this demo are managed using NPM. Our main dependency is the
Kurento Client JavaScript (*kurento-client*). The relevant part of the
`package.json <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-chroma/package.json>`_
file for managing this dependency is:

.. sourcecode:: js

   "dependencies": {
      "kurento-client" : "|CLIENT_JS_VERSION|"
   }

At the client side, dependencies are managed using Bower. Take a look to the
`bower.json <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-chroma/static/bower.json>`_
file and pay attention to the following section:

.. sourcecode:: js

   "dependencies": {
      "kurento-utils" : "|UTILS_JS_VERSION|",
      "kurento-module-pointerdetector": "|CLIENT_JS_VERSION|"
   }

.. note::

   We are in active development. You can find the latest versions at
   `npm <http://npmsearch.com/>`_ and `Bower <http://bower.io/search/>`_.
