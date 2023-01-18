kurento-metadata
================

Kurento Java Tutorial: WebRTC in loopback with two filters which using metadata.

This demo needs the kurento-module-datachannelexample module installed in the media server.
That module is available in the Kurento repositories, so it is possible to
install it with apt-get (`sudo apt-get install kurento-module-datachannelexample`).

This demo detects and draws faces into the webcam video. The demo connects two
filters, the KmsDetectFaces and the KmsShowFaces. The first one detects faces
into the image and it puts the info about the face (position and dimensions)
into the buffer metadata. The second one reads the buffer metadata to find info
about detected faces. If there is info about faces, the filter draws the faces
into the image.

Running this tutorial
---------------------

In order to run this tutorial, please read the following [instructions](https://kurento.openvidu.io/docs/current/tutorials/java/tutorial-metadata.html)

