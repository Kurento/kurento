[![][KurentoImage]][website]

Copyright © 2014 Kurento. Licensed under [LGPL License].

KWS Utils
=============
Utilities for Kurento Web SDK

The KWS Utils project contains a set of reusable components that have been
found useful during the development of the Kurento Web SDK, and the different 
projects that serve as demo.

The source code of this project can be cloned from the [GitHub repository].

Installation instructions
-------------------------

Be sure to have installed the Node.js tools in your system.

In ubuntu:

```bash
sudo apt-get install nodejs nodejs-legacy npm
```

It's heavily encourage to use the latest Node.js and NPM versions from the
[Node.js project PPA] instead of the packages available on the oficial Ubuntu
repositories, since due to the fast-moving Node.js community and environment
these last ones get easily outdated and can lead to incompatibility errors. In
that case you must not install the ```npm``` package since the tool is included
by default in the community driven ```nodejs``` Ubuntu package:

```bash
sudo apt-get install nodejs nodejs-legacy
```

To install the library, it's recomended to do that from the [NPM repository] :

```bash
npm install kws-utils
```

Alternatively, you can download the code using git and install manually its
dependencies:

```bash
git clone https://github.com/Kurento/kws-utils.git
cd kws-utils
npm install
```

### Browser

To build the browser version of the library you'll only need to exec the [grunt]
task runner and they will be generated on the ```dist``` folder. Alternatively,
if you don't have it globally installed, you can run a local copy by executing

```bash
node_modules/.bin/grunt
```


How to test
-----------
Tests are autonomous and based on [QUnit] testing framework. Their only
requirement is to exec previously ```npm install``` to have installed all the
dev dependencies.

### Browser

After building the web browser version of the library, just open the file
```test/index.html``` with any browser, and the tests will launch automatically.
In case of the browser raise some security policy errors, you can host the tests
code by running any static web server at the source code root folder, for
example by launching the command

```bash
python -m SimpleHTTPServer 8000
```

You can be able to configure to what WebSocket endpoint you want to connect on
the dropdown list at the top of the tests page.

### Node.js

To exec test in Node.js, you only need to exec ```npm test```, that will launch
all the tests automatically using [QUnit-cli].

At this moment, the default WebSocket endpoint can not be changed due to limits
of the current implementation of NPM. If you need to use a different WebSocket
endpoint from the default one, you can exec the underlying test command and
append a *ws_uri* parameter pointing the the alternative WebSocket endpoint:

```bash
node_modules/.bin/qunit-cli -c KwsMedia:. -c wock:node_modules/wock -c test/_common.js -c test/_proxy.js test/*.js --ws_uri=ws://localhost:8080
```


How to create a basic pipeline
------------------------------

For tutorial purposes, we are going to create a basic pipeline that play a video
file from its URL and stream it over HTTP. You can also download and check this
[example full source code] or run it directly from [JsFiddle] :

1. Create an instance of the KwsMedia class that will manage the connection with
   the Kurento Media Server, so you'll need to provide the URI of its WebSocket
   endpoint. Alternatively, instead of using a constructor, you can also provide
   success and error callbacks:

   ```Javascript
   var kwsMedia = kwsMediaApi.KwsMedia(ws_uri);
   
   kwsMedia.onconnect = function(kwsMedia)
   {
     …
   };
   kwsMedia.onerror = function(error)
   {
     …
   };
   ```

   ```Javascript
   kwsMediaApi.KwsMedia(ws_uri, function(kwsMedia)
   {
     …
   },
   function(error)
   {
     …
   });
   ```

2. Create a pipeline. This will host and connect the diferent elements. In case
   of error, it will be notified on the ```error``` parameter of the callback,
   otherwise this will be null as it's common on Node.js style APIs:

   ```Javascript
   kwsMedia.createMediaPipeline(function(error, pipeline)
   {
     …
   });
   ```

3. Create the elements. The player need an object with the URL of the video, and
   and we'll also subscribe to the 'EndOfStream' event of the HTTP stream:

   ```Javascript
   PlayerEndpoint.create(pipeline,
   {uri: "https://ci.kurento.com/video/small.webm"},
   function(error, player)
   {
     …
   });

   HttpGetEndpoint.create(pipeline, function(error, httpGet)
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
   pipeline.connect(player, httpGet, function(error, pipeline)
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

[GitHub Kurento group]: https://github.com/kurento
[GitHub repository]: https://github.com/kurento/kws-utils
[grunt]: http://gruntjs.com/
[Kurento Media Connector]: https://github.com/Kurento/kmf-media-connector
[KurentoImage]: https://secure.gravatar.com/avatar/21a2a12c56b2a91c8918d5779f1778bf?s=120
[kurentoms]: http://twitter.com/kurentoms
[LGPL License]: http://www.gnu.org/licenses/lgpl-2.1.html
[Node.js project PPA]: https://github.com/joyent/node/wiki/Installing-Node.js-via-package-manager#ubuntu-mint-elementary-os
[NPM repository]: https://www.npmjs.org/package/kws-utils
[QUnit]: http://qunitjs.com
[QUnit-cli]: https://github.com/devongovett/qunit-cli
[website]: http://kurento.org
