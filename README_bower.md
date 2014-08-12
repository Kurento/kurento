[![][KurentoImage]][website]

Copyright © 2014 Kurento. Licensed under [LGPL License].

KWS Media API for Bower
=============
Media API for Kurento Web SDK

The KWS Media API project contains the implementation of the Kurento client
side Media API for web applications and Node.js.

The source code of this project can be cloned from the [GitHub repository].

Installation instructions
-------------------------

Be sure to have installed [Bower] in your system:

```bash
sudo npm install -g bower
```

To install the library, it's recomended to do that from the [Bower repository] :

```bash
bower install kurento-client
```

Alternatively, you can download the code using git and install manually its
dependencies:

```bash
git clone https://github.com/Kurento/kurento-client-bower
cd kurento-client-bower
bower install
```

Need to know, currently the library can't be able to talk directly with the
Kurento Media Server, so it require to use a [Kurento Media Connector] instace.
You can download the latest release from http://ci.kurento.com/apps/kmf-media-connector.zip.
When you decompress it you'll find a README file with installation instructions.


How to create a basic pipeline
------------------------------

For tutorial purposes, we are going to create a basic pipeline that play a video
file from its URL and stream it over HTTP. You can also download and check this
[example full source code] or run it directly from [JsFiddle] :

1. Create an instance of the KurentoClient class that will manage the connection
   with the Kurento Media Server, so you'll need to provide the URI of its
   WebSocket endpoint. Alternatively, instead of using a constructor, you can
   also provide success and error callbacks:

   ```Javascript
   var kurento = kurentoClient.KurentoClient(ws_uri);
   
   kurento.then(function(kurento)
   {
     // Connection success
     …
   },
   function(error)
   {
     // Connection error
     …
   });
   ```

   ```Javascript
   kurentoClient.KurentoClient(ws_uri, function(kurento)
   {
     // Connection success
     …
   },
   function(error)
   {
     // Connection error
     …
   });
   ```

2. Create a pipeline. This will host and connect the diferent elements. In case
   of error, it will be notified on the ```error``` parameter of the callback,
   otherwise this will be null as it's common on Node.js style APIs:

   ```Javascript
   kurento.create('MediaPipeline', function(error, pipeline)
   {
     …
   });
   ```

3. Create the elements. The player need an object with the URL of the video, and
   we'll also subscribe to the 'EndOfStream' event of the HTTP stream:

   ```Javascript
   pipeline.create('PlayerEndpoint',
   {uri: "https://ci.kurento.com/video/small.webm"},
   function(error, player)
   {
     …
   });

   pipeline.create('HttpGetEndpoint', function(error, httpGet)
   {
     httpGet.on('EndOfStream', function(event)
     {
       …
     });

     …
   });
   ```

4. Connect the elements, so the media stream can flow between them:

   ```Javascript
   player.connect(httpGet, function(error, pipeline)
   {
     …
   });
   ```

5. Get the URL where the media stream will be available:

   ```Javascript
   httpGet.getUrl(function(error, url)
   {
     …
   });
   ```

6. Start the reproduction of the media:

   ```Javascript
   player.play(function(error)
   {
     …
   });
   ```


Kurento
=======

What is Kurento
---------------
Kurento provides an open platform for video processing and streaming based on
standards.

This platform has several APIs and components which provide solutions to the
requirements of multimedia content application developers. These include:

* Kurento Media Server (KMS). A full featured media server providing the
capability to create and manage dynamic multimedia pipelines.
* Kurento Media Framework (KMF). A Java server-side API providing the required
abstractions for creating applications that manage multimedia content, calls
and conferences involving audio, video and data information.
* Kurento Web SDK (KWS). A client-side HTML5 and Javascript SDK for accessing
KMF capabilities
* Kurento Android SDK (KAS). A Java and native SDK allowing easy integration of
KMF into any kind of Android application.

Downloads
---------
To download binary releases of Kurento components visit http://kurento.org

Code for other Kurento projects can be found in the [GitHub Kurento group].

News and Website
----------------
Information about Kurento can be found on our [website].
Follow us on Twitter @[kurentoms].

[example full source code]: https://github.com/Kurento/kws-media-api/tree/develop/example/PlayerEndpoint-HttpGetEndpoint
[Bower]: http://bower.io
[Bower repository]: https://github.com/KurentoReleases/kws-media-api
[GitHub Kurento group]: https://github.com/kurento
[GitHub repository]: https://github.com/kurento/kws-media-api
[JsFiddle]: http://jsfiddle.net/gh/get/library/pure/kurento/kws-media-api/contents/example/PlayerEndpoint-HttpGetEndpoint
[Kurento Media Connector]: https://github.com/Kurento/kmf-media-connector
[KurentoImage]: https://secure.gravatar.com/avatar/21a2a12c56b2a91c8918d5779f1778bf?s=120
[kurentoms]: http://twitter.com/kurentoms
[LGPL License]: http://www.gnu.org/licenses/lgpl-2.1.html
[website]: http://kurento.org
