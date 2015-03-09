%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Node.js Module Tutorial 4 - Plate Detector Filter
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application consists on a `WebRTC`:term: video communication in mirror
(*loopback*) with a plate detector filter element.

For the impatient: running this example
=======================================

First of all, you should install Kurento Media Server to run this demo. Please
visit the :doc:`installation guide <../../installation_guide>` for further
information. In addition, the built-in module ``kms-platedetector`` should be
also installed:

.. sourcecode:: sh

    sudo apt-get install kms-platedetector

Be sure to have installed `Node.js`:term: in your system. In an Ubuntu machine,
you can install both as follows:

.. sourcecode:: sh

   curl -sL https://deb.nodesource.com/setup | sudo bash -
   sudo apt-get install -y nodejs

To launch the application you need to clone the GitHub project where this demo
is hosted and then install and run it, as follows:

.. sourcecode:: sh

    git clone https://github.com/Kurento/kurento-tutorial-node.git
    cd kurento-tutorial-node/kurento-platedetector
    npm install

If you have problems installing any of the dependencies, please remove them and
clean the npm cache, and try to install them again:

.. sourcecode:: sh

    rm -r node_modules
    npm cache clean

Finally access the application connecting to the URL http://localhost:8080/
through a WebRTC capable browser (Chrome, Firefox).

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
:doc:`Magic Mirror <./tutorial-1-magicmirror>` tutorial. In this case, this
demo uses a **PlateDetector** instead of **FaceOverlay** filter. An screenshot
of the running example is shown in the following picture:

.. figure:: ../../images/kurento-module-tutorial-plate-screenshot-01.png
   :align:   center
   :alt:     Plate detector demo in action

   *Plate detector demo in action*

The following snippet shows how the media pipeline is implemented in the Java
server-side code of the demo. An important issue in this code is that a
listener is added to the ``PlateDetectorFilter`` object
(``addPlateDetectedListener``). This way, each time a plate is detected in the
stream, a message is sent to the client side. As shown in the screenshot below,
this event is printed in the console of the GUI.

.. sourcecode:: javascript

   function start(sessionId, sdpOffer, callback) {

      if (!sessionId) {
         return callback("Cannot use undefined sessionId");
      }

      // Check if session is already transmitting
      if (pipelines[sessionId]) {
         return callback("Close current session before starting a new one or use " +
            "another browser to open a tutorial.")
      }

      getKurentoClient(function(error, kurentoClient) {
         if (error) {
            return callback(error);
         }

         kurentoClient.create('MediaPipeline', function(error, pipeline) {
            if (error) {
               return callback(error);
            }

            createMediaElements(pipeline, function(error, webRtcEndpoint,
                  plateDetectorFilter) {
               if (error) {
                  pipeline.release();
                  return callback(error);
               }

               connectMediaElements(webRtcEndpoint, plateDetectorFilter,
                  function(error) {
                     if (error) {
                        pipeline.release();
                        return callback(error);
                     }

                     plateDetectorFilter.on ('PlateDetected', function (data){
                        return callback(null, 'plateDetected', data);
                     });

                     webRtcEndpoint.processOffer(sdpOffer, function(
                           error, sdpAnswer) {
                        if (error) {
                           pipeline.release();
                           return callback(error);
                        }

                        pipelines[sessionId] = pipeline;
                        return callback(null, 'sdpAnswer', sdpAnswer);
                     });
                  });
            });
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
      "kurento-client": "^5.0.0",
   }

At the client side, dependencies are managed using Bower. Take a look to the
`bower.json <https://github.com/Kurento/kurento-tutorial-node/blob/master/kurento-platedetector/static/bower.json>`_
file and pay attention to the following section:

.. sourcecode:: js

   "dependencies": {
      "kurento-utils": "^5.0.0",
      "kurento-module-platedetector": "^1.0.0"
   }

Kurento framework uses `Semantic Versioning`:term: for releases. Notice that
ranges (``^5.0.0`` for *kurento-client* and *kurento-utils-js*,  and ``^1.0.0``
for *platedetector*) downloads the latest version of Kurento artifacts from NPM
and Bower.
