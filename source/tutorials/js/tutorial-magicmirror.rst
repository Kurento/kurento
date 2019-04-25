%%%%%%%%%%%%%%%%%%%%%%%%%
JavaScript - Magic Mirror
%%%%%%%%%%%%%%%%%%%%%%%%%

This web application extends the :doc:`Hello World Tutorial<./tutorial-helloworld>`, adding
media processing to the basic `WebRTC`:term: loopback.

.. note::

   This tutorial has been configurated for using https. Follow these `instructions </features/security.html#configure-javascript-applications-to-use-https>`_
   for securing your application.

For the impatient: running this example
=======================================

First of all, install Kurento Media Server: :doc:`/user/installation`.

.. note::

   If you will run this tutorial from a remote machine (i.e. not from ``localhost``), then **you need to configure Secure WebSocket (wss://) in Kurento Media Server**. For instructions, check :ref:`features-security-kms-wss`.

   This is not an issue if you will run both KMS and the tutorial demo locally, because browsers (at least Chrome at the time of this writing) allow connecting to insecure WebSockets from HTTPS pages, as long as everything happens in ``localhost``.

After getting a properly configured KMS installation, leave the media server running in the background.

Install :term:`Node.js` and :term:`Bower` in your system:

.. sourcecode:: bash

   curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
   sudo apt-get install -y nodejs
   sudo npm install -g bower

WebRTC requires HTTPS, so this tutorial demo must be served by a secure web server. You can use whichever one you prefer, such as Nginx or Apache. For quick tests, a very straightforward option is to use Node.js to run the simple, zero-configuration `http-server <https://www.npmjs.com/package/http-server>`__:

.. sourcecode:: bash

   sudo npm install -g http-server

You also need the source code of this demo; clone it from GitHub, then start the web server:

.. sourcecode:: bash

    git clone https://github.com/Kurento/kurento-tutorial-js.git
    cd kurento-tutorial-js/kurento-magic-mirror
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

This application uses computer vision and augmented reality techniques to add a
funny hat on top of detected faces. The following picture shows a screenshot
of the demo running in a web browser:

.. figure:: ../../images/kurento-java-tutorial-2-magicmirror-screenshot.png
   :align:   center
   :alt:     Kurento Magic Mirror Screenshot: WebRTC with filter in loopback

   *Kurento Magic Mirror Screenshot: WebRTC with filter in loopback*

The interface of the application (an HTML web page) is composed by two HTML5
video tags: one for the video camera stream (the local client-side stream) and
other for the mirror (the remote stream). The video camera stream is sent to
the Kurento Media Server, processed and then is returned to the client as a
remote stream.

To implement this, we need to create a `Media Pipeline`:term: composed by the
following `Media Element`:term: s:

- **WebRtcEndpoint**: Provides full-duplex (bidirectional) `WebRTC`:term:
  capabilities.

- **FaceOverlay filter**: Computer vision filter that detects faces in the
  video stream and puts an image on top of them. In this demo the filter is
  configured to put a
  `Super Mario hat <http://files.openvidu.io/img/mario-wings.png>`_).

The media pipeline implemented is illustrated in the following picture:

.. figure:: ../../images/kurento-java-tutorial-2-magicmirror-pipeline.png
   :align:   center
   :alt:     WebRTC with filter in loopback Media Pipeline

   *WebRTC with filter in loopback Media Pipeline*

The complete source code of this demo can be found in
`GitHub <https://github.com/Kurento/kurento-tutorial-js/tree/master/kurento-magic-mirror>`_.


JavaScript Logic
================

This demo follows a *Single Page Application* architecture (`SPA`:term:). The
interface is the following HTML page:
`index.html <https://github.com/Kurento/kurento-tutorial-js/blob/master/kurento-magic-mirror/index.html>`_.
This web page links two Kurento JavaScript libraries:

* **kurento-client.js** : Implementation of the Kurento JavaScript Client.

* **kurento-utils.js** : Kurento utility library aimed to simplify the WebRTC
  management in the browser.

In addition, these two JavaScript libraries are also required:

* **Bootstrap** : Web framework for developing responsive web sites.

* **jquery.js** : Cross-platform JavaScript library designed to simplify the
  client-side scripting of HTML.

* **adapter.js** : WebRTC JavaScript utility library maintained by Google that
  abstracts away browser differences.

* **ekko-lightbox** : Module for Bootstrap to open modal images, videos, and
  galleries.

* **demo-console** : Custom JavaScript console.

The specific logic of this demo is coded in the following JavaScript page:
`index.js <https://github.com/Kurento/kurento-tutorial-js/blob/master/kurento-magic-mirror/js/index.js>`_.
In this file, there is a function which is called when the green button labeled
as *Start* in the GUI is clicked.

.. sourcecode:: js

   var startButton = document.getElementById("start");

   startButton.addEventListener("click", function() {
      var options = {
        localVideo: videoInput,
        remoteVideo: videoOutput
      };

      webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options, function(error) {
         if(error) return onError(error)
         this.generateOffer(onOffer)
      });

      [...]
   }

The function *WebRtcPeer.WebRtcPeerSendrecv* abstracts the WebRTC internal
details (i.e. PeerConnection and getUserStream) and makes possible to start a
full-duplex WebRTC communication, using the HTML video tag with id *videoInput*
to show the video camera (local stream) and the video tag *videoOutput* to show
the remote stream provided by the Kurento Media Server.

Inside this function, a call to *generateOffer* is performed. This function
accepts a callback in which the SDP offer is received. In this callback we
create an instance of the *KurentoClient* class that will manage communications
with the Kurento Media Server. So, we need to provide the URI of its WebSocket
endpoint. In this example, we assume it's listening in port 8888 at the same
host than the HTTP serving the application.

.. sourcecode:: js

   [...]

   var args = getopts(location.search,
   {
     default:
     {
       ws_uri: 'ws://' + location.hostname + ':8888/kurento',
       ice_servers: undefined
     }
   });

   [...]

   kurentoClient(args.ws_uri, function(error, client){
     [...]
   };

Once we have an instance of ``kurentoClient``, the following step is to create a
*Media Pipeline*, as follows:

.. sourcecode:: js

   client.create("MediaPipeline", function(error, _pipeline){
      [...]
   });

If everything works correctly, we have an instance of a media pipeline (variable
``pipeline`` in this example). With this instance, we are able to create
*Media Elements*. In this example we just need a *WebRtcEndpoint* and a
*FaceOverlayFilter*. Then, these media elements are interconnected:

.. sourcecode:: js

   pipeline.create('WebRtcEndpoint', function(error, webRtcEp) {
     if (error) return onError(error);

     setIceCandidateCallbacks(webRtcPeer, webRtcEp, onError)

     webRtcEp.processOffer(sdpOffer, function(error, sdpAnswer) {
       if (error) return onError(error);

       webRtcPeer.processAnswer(sdpAnswer, onError);
     });
     webRtcEp.gatherCandidates(onError);

     pipeline.create('FaceOverlayFilter', function(error, filter) {
       if (error) return onError(error);

       filter.setOverlayedImage(args.hat_uri, -0.35, -1.2, 1.6, 1.6,
       function(error) {
         if (error) return onError(error);

       });

       client.connect(webRtcEp, filter, webRtcEp, function(error) {
         if (error) return onError(error);

         console.log("WebRtcEndpoint --> filter --> WebRtcEndpoint");
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
`bower.json <https://github.com/Kurento/kurento-tutorial-js/blob/master/kurento-magic-mirror/bower.json>`_
file, as follows:

.. sourcecode:: js

   "dependencies": {
      "kurento-client": "|VERSION_CLIENT_JS|",
      "kurento-utils": "|VERSION_UTILS_JS|"
   }

.. note::

   We are in active development. You can find the latest version of
   Kurento JavaScript Client at `Bower <https://bower.io/search/?q=kurento-client>`_.
