%%%%%%%%%%%%%%%%%%%%%%%%%%%
JavaScript - Loopback stats
%%%%%%%%%%%%%%%%%%%%%%%%%%%

This web application extends :doc:`the Hello World tutorial <./tutorial-helloworld>` showing
how statistics are collected.



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
    cd kurento-tutorial-js/kurento-loopback-stats
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

The logic of the application is quite simple: the local stream is sent to the
Kurento Media Server, which returns it back to the client without
modifications. To implement this behavior we need to create a
`Media Pipeline`:term: composed by the `Media Element`:term: **WebRtcEndpoint**,
which holds the capability of exchanging full-duplex
(bidirectional) WebRTC media flows. This media element is connected to itself
so any received media (from browser) is send back (to browser). Using method
``getStats`` the application shows all stats of element **WebRtcEndpoint**.

The complete source code of this demo can be found in
`GitHub <https://github.com/Kurento/kurento-tutorial-js/tree/master/kurento-loopback-stats>`_.


JavaScript Logic
================

This demo follows a *Single Page Application* architecture (`SPA`:term:). The
interface is the following HTML page:
`index.html <https://github.com/Kurento/kurento-tutorial-js/blob/master/kurento-loopback-stats/index.html>`_.
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
`index.js <https://github.com/Kurento/kurento-tutorial-js/blob/master/kurento-loopback-stats/js/index.js>`_.
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

The function *WebRtcPeer.WebRtcPeerSendrecv* hides internal
details (i.e. PeerConnection and getUserStream) and makes possible to start a
full-duplex WebRTC communication, using the HTML video tag with id *videoInput*
to show the video camera (local stream) and the video tag *videoOutput* to show
the remote stream provided by the Kurento Media Server.

Inside this function, a call to *generateOffer* is performed. This function
accepts a callback in which the SDP offer is received. In this callback we
create an instance of the *KurentoClient* class that will manage communications
with the Kurento Media Server. So, we need to provide the URI of its WebSocket
endpoint. In this example, we assume it's listening in port 8433 at the same
host than the HTTP serving the application.

.. sourcecode:: js

   [...]

   var args = getopts(location.search,
   {
     default:
     {
       ws_uri: 'wss://' + location.hostname + ':8433/kurento',
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
*Media Elements*. In this example we just need a *WebRtcEndpoint*. Then,
this media elements is connected itself:

.. sourcecode:: js

   pipeline.create("WebRtcEndpoint", function(error, webRtc) {
      if (error) return onError(error);

      webRtcEndpoint = webRtc;

      setIceCandidateCallbacks(webRtcPeer, webRtc, onError)

      webRtc.processOffer(sdpOffer, function(error, sdpAnswer) {
        if (error) return onError(error);

        webRtc.gatherCandidates(onError);

        webRtcPeer.processAnswer(sdpAnswer, onError);
      });

      webRtc.connect(webRtc, function(error) {
        if (error) return onError(error);

        console.log("Loopback established");

        webRtcEndpoint.on('MediaStateChanged', function(event) {
          if (event.newState == "CONNECTED") {
            console.log("MediaState is CONNECTED ... printing stats...")
            activateStatsTimeout();
          }
        });
      });
   });

In the following snippet, we can see ``getStats`` method. This method returns several
statistic values of **WebRtcEndpoint**.

.. sourcecode:: javascript

   function getBrowserOutgoingVideoStats(webRtcPeer, callback) {
     var peerConnection = webRtcPeer.peerConnection;

     peerConnection.getStats(function(stats) {
       var results = stats.result();

       for (var i = 0; i < results.length; i++) {
         var res = results[i];
         if (res.type != 'ssrc') continue;

         //Publish it to be compliant with W3C stats draft
         var retVal = {
           timeStamp: res.timestamp,
           //StreamStats below
           associateStatsId: res.id,
           codecId: "--",
           firCount: res.stat('googFirsReceived'),
           isRemote: false,
           mediaTrackId: res.stat('googTrackId'),
           nackCount: res.stat('googNacksReceived'),
           pliCount: res.stat('googPlisReceived'),
           sliCount: 0,
           ssrc: res.stat('ssrc'),
           transportId: res.stat('transportId'),
           //Specific outbound below
           bytesSent: res.stat('bytesSent'),
           packetsSent: res.stat('packetsSent'),
           roundTripTime: res.stat('googRtt'),
           packetsLost: res.stat('packetsLost'),
           targetBitrate: "??",
           remb: "??"
         }
         return callback(null, retVal);
       }
       return callback("Error: could not find ssrc type on track stats", null);
     }, localVideoTrack);
   }

.. note::

   The :term:`TURN` and :term:`STUN` servers to be used can be configured simple adding
   the parameter ``ice_servers`` to the application URL, as follows:

   .. sourcecode:: bash

      https://localhost:8443/index.html?ice_servers=[{"urls":"stun:stun1.example.net"},{"urls":"stun:stun2.example.net"}]
      https://localhost:8443/index.html?ice_servers=[{"urls":"turn:turn.example.org","username":"user","credential":"myPassword"}]

Dependencies
============

Demo dependencies are located in file `bower.json <https://github.com/Kurento/kurento-tutorial-js/blob/master/kurento-loopback-stats/bower.json>`_.
`Bower`:term: is used to collect them.

.. sourcecode:: js

   "dependencies": {
      "kurento-client": "|VERSION_CLIENT_JS|",
      "kurento-utils": "|VERSION_UTILS_JS|"
   }

.. note::

   We are in active development. You can find the latest version of
   Kurento JavaScript Client at `Bower <https://bower.io/search/?q=kurento-client>`_.
