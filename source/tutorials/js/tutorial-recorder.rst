%%%%%%%%%%%%%%%%%%%%%
JavaScript - Recorder
%%%%%%%%%%%%%%%%%%%%%

This web application extends the :doc:`Hello World Tutorial<./tutorial-helloworld>`,
adding recording capabilities.

For the impatient: running this example
=======================================

You'll need to install Kurento Media Server before running this example.
Read :doc:`installation guide </user/installation>` for further
information.

Be sure to have installed `Node.js`:term: and `Bower`:term: in your system. In an Ubuntu machine, you can install both as follows:

.. sourcecode:: bash

   curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
   sudo apt-get install -y nodejs
   sudo npm install -g bower

Due to `Same-origin policy`:term:, this demo has to be served by a HTTP server from ``localhost``. A very simple way of doing this is by means of an HTTP Node.js server which can be installed using `npm`:term: :

.. sourcecode:: bash

   sudo npm install -g http-server

You also need the source code of this demo. You can clone it from GitHub, then start the HTTP server:

.. sourcecode:: bash

    git clone https://github.com/Kurento/kurento-tutorial-js.git
    cd kurento-tutorial-js/kurento-recorder
    git checkout |VERSION_TUTORIAL_JS|
    bower install
    http-server -p 8443 -S -C keys/server.crt -K keys/server.key

Finally, access the application by using a WebRTC-capable browser to open this URL:

https://localhost:8443/index.html?ws_uri=ws://localhost:8888/kurento

.. note::

   These instructions work only if Kurento Media Server is up and running in the same machine as the tutorial. However, it is possible to connect to a remote KMS in other machine, simply changing the parameter ``ws_uri`` in the URL, as follows:

   .. sourcecode:: bash

      https://localhost:8443/index.html?ws_uri=ws://localhost:8888/kurento

Understanding this example
==========================

In the first part of this demo, the local stream is sent to Kurento Media Server,
which returns it back to the client and records to the same time. In order to
implement this behavior we need to create a`Media Pipeline`:term: consisting of a
**WebRtcEndpoint** and a **RecorderEnpoint**.

The second part of this demo shows how to play recorded media. To achieve this,
we need to create a `Media Pipeline`:term: composed by a **WebRtcEndpoint** and
a **PlayerEndpoint**. The *uri* property of the player is the uri of the
recorded file.

There are two implementations for this demo to be found in github:

* Using `callbacks <https://github.com/Kurento/kurento-tutorial-js/tree/master/kurento-recorder>`_.
* Using `yield <https://github.com/Kurento/kurento-tutorial-js/tree/master/kurento-hello-world-recorder-generator>`_.

.. note::

   The snippets are based in demo with callbacks.


JavaScript Logic
================

This demo follows a *Single Page Application* architecture (`SPA`:term:). The
interface is the following HTML page:
`index.html <https://github.com/Kurento/kurento-tutorial-js/blob/master/kurento-recorder/index.html>`_.
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
`index.js <https://github.com/Kurento/kurento-tutorial-js/blob/master/kurento-recorder/js/index.js>`_.
In this file, there is a function which is called when the green button, labeled
as *Start* in the GUI, is clicked.

.. sourcecode:: js

   var startRecordButton = document.getElementById("start");

   startRecordButton.addEventListener("click", startRecording);

   function startRecording() {
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
endpoint. In this example, we assume it's listening in port 8433 at the same
host than the HTTP serving the application.

.. sourcecode:: js

   [...]

   var args = getopts(location.search,
   {
     default:
     {
       ws_uri: 'wss://' + location.hostname + ':8433/kurento',
       file_uri: 'file:///tmp/recorder_demo.webm', // file to be stored in media server
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
*RecorderEndpoint*. Then, these media elements are interconnected:

.. sourcecode:: js

     var elements =
        [
          {type: 'RecorderEndpoint', params: {uri : args.file_uri}},
          {type: 'WebRtcEndpoint', params: {}}
        ]

     pipeline.create(elements, function(error, elements){
       if (error) return onError(error);

       var recorder = elements[0]
       var webRtc   = elements[1]

       setIceCandidateCallbacks(webRtcPeer, webRtc, onError)

       webRtc.processOffer(offer, function(error, answer) {
         if (error) return onError(error);

         console.log("offer");

         webRtc.gatherCandidates(onError);
         webRtcPeer.processAnswer(answer);
       });

       client.connect(webRtc, webRtc, recorder, function(error) {
         if (error) return onError(error);

         console.log("Connected");

         recorder.record(function(error) {
           if (error) return onError(error);

           console.log("record");
         });
       });
     });


When stop button is clicked, the recoder element stops to record, and all
elements are released.

.. sourcecode:: javascript

   stopRecordButton.addEventListener("click", function(event){
       recorder.stop();
       pipeline.release();
       webRtcPeer.dispose();
       videoInput.src = "";
       videoOutput.src = "";

       hideSpinner(videoInput, videoOutput);

       var playButton = document.getElementById('play');
       playButton.addEventListener('click', startPlaying);
     })

In the second part, after play button is clicked, we have an instance of a media pipeline (variable
``pipeline`` in this example). With this instance, we are able to create
*Media Elements*. In this example we just need a *WebRtcEndpoint* and a
*PlayerEndpoint* with *uri* option like path where the media was recorded.
Then, these media elements are interconnected:

.. sourcecode:: javascript

       var options = {uri : args.file_uri}

       pipeline.create("PlayerEndpoint", options, function(error, player) {
         if (error) return onError(error);

         player.on('EndOfStream', function(event){
           pipeline.release();
           videoPlayer.src = "";

           hideSpinner(videoPlayer);
         });

         player.connect(webRtc, function(error) {
           if (error) return onError(error);

           player.play(function(error) {
             if (error) return onError(error);
             console.log("Playing ...");
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

Demo dependencies are located in file `bower.json <https://github.com/Kurento/kurento-tutorial-js/blob/master/kurento-recorder/bower.json>`_.
`Bower`:term: is used to collect them.

.. sourcecode:: js

   "dependencies": {
      "kurento-client": "|VERSION_CLIENT_JS|",
      "kurento-utils": "|VERSION_UTILS_JS|"
   }

.. note::

   We are in active development. You can find the latest version of
   Kurento JavaScript Client at `Bower <https://bower.io/search/?q=kurento-client>`_.
