.. Developing Browser JS apps with Kurento

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
Developing Browser JS apps with Kurento
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

.. todo:: Complete this section.

Kurento JavaScript Client provides the capabilities to control Kurento Server
from JavaScript.

To describe this API, we are going to show how to create a basic pipeline that
play a video file from its URL and stream it over HTTP. You can also download
and check this
`example full source code <https://github.com/Kurento/kws-media-api/tree/develop/example/PlayerEndpoint-HttpGetEndpoint>`_.

* Create an instance of the KurentoClient class that will manage the connection
  with the Kurento Server, so you'll need to provide the URI of its
  WebSocket endpoint. Alternatively, instead of using a constructor, you can
  also provide success and error callbacks:

.. sourcecode:: js

   var kurento = kurentoClient.KurentoClient("ws://localhost:8888/");   
   kurento.onConnect = function(kurento) {
     //…
   }; 
   
   kurento.onError = function(error) {
     //…
   }; 
   
   kwsMediaApi.KurentoClient("ws://localhost:8888/", function(kwsMedia) {
     //…
   }, function(error) {
     //…
   });

* Create a pipeline. This will host and connect the diferent elements. In case
  of error, it will be notified on the ```error``` parameter of the callback,
  otherwise this will be null as it's common on Node.js style APIs:

.. sourcecode:: js

   kwsMedia.createMediaPipeline(function(error, pipeline)
    {
     //…
    });

* Create the elements. The player need an object with the URL of the video,
  and and we'll also subscribe to the 'EndOfStream' event of the HTTP stream:

.. sourcecode:: js

   PlayerEndpoint.create(pipeline, {uri:
   "https://ci.kurento.com/video/small.webm"}, function(error, player) {
     //…
   });

   HttpGetEndpoint.create(pipeline, function(error, httpGet) {
     httpGet.on('EndOfStream', function(event) {
       //…
     });

     //…
   });

* Connect the elements, so the media stream can flow between them:

.. sourcecode:: js

   pipeline.connect(player, httpGet, function(error, pipeline) {
     //…
   });


* Get the URL where the media stream will be available:

.. sourcecode:: js

   httpGet.getUrl(function(error, url) {
     //…
   });


* Start the reproduction of the media:

.. sourcecode:: js

   player.play(function(error) {
     //…
   });

