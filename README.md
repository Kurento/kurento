[![][KurentoImage]][website]

Copyright © 2014 Kurento. Licensed under [LGPL License].

JavaScript Kurento Client
=========================

The project contains the implementation of the JavaScript Kurento Client
for web applications and Node.js.

The source code of this project can be cloned from the [GitHub repository].

Installation instructions
-------------------------

These instructions are intended for code contributors or people willing to
compile the browser version themselves. If you are a browser-only developer,
it's better that you have a look at the [JavaScript Kurento Client for Bower]
instructions.

### Node.js

Be sure to have installed [Node.js] in your system:

```bash
curl -sL https://deb.nodesource.com/setup | sudo bash -
sudo apt-get install -y nodejs
```

To install the library, it's recommended to do that from the [NPM repository] :

```bash
npm install kurento-client
```

Alternatively, or if you want to modify the JavaScript Kurento Client code or
generate yourself the browser version of the library, you can download the
development code files using git and install manually its dependencies:

```bash
git clone https://github.com/Kurento/kurento-client-js
cd kurento-client-js
npm install
```

In this last case, you will also need to have installed [Kurento Module Creator]
so you can be able to generate the client libraries code.

### Browser

To build the browser version of the library, after downloading the development
code files, you'll only need to exec the [grunt] task runner from the root of
the project and they will be generated on the ```dist``` folder. Alternatively,
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
append a *ws_uri* parameter pointing to the alternative WebSocket endpoint:

```bash
node_modules/.bin/qunit-cli -c kurentoClient:. -c wock:node_modules/wock -c test/_common.js -c test/_proxy.js test/*.js --ws_uri=ws://localhost:8080
```


How to create a basic pipeline
------------------------------

For tutorial purposes, we are going to create a basic pipeline that play a video
file from its URL and stream it over HTTP :

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

  * Kurento Media Server (KMS). A full featured media server providing
    the capability to create and manage dynamic multimedia pipelines.
  * Kurento Clients. Libraries to create applications with media
    capabilities. Kurento provides libraries for Java, browser JavaScript,
    and Node.js.

Downloads
---------
To download binary releases of Kurento components visit http://kurento.org

Code for other Kurento projects can be found in the [GitHub Kurento group].

News and Website
----------------
Information about Kurento can be found on our [website].
Follow us on Twitter @[kurentoms].

[GitHub Kurento group]: https://github.com/kurento
[GitHub repository]: https://github.com/kurento/kurento-client-js
[grunt]: http://gruntjs.com/
[Kurento Module Creator]: https://github.com/Kurento/kurento-module-creator
[KurentoImage]: https://secure.gravatar.com/avatar/21a2a12c56b2a91c8918d5779f1778bf?s=120
[kurentoms]: http://twitter.com/kurentoms
[JavaScript Kurento Client for Bower]: https://github.com/Kurento/kurento-client-bower
[LGPL License]: http://www.gnu.org/licenses/lgpl-2.1.html
[Node.js]: http://nodejs.org/
[NPM repository]: https://www.npmjs.org/package/kurento-client
[QUnit]: http://qunitjs.com
[QUnit-cli]: https://github.com/devongovett/qunit-cli
[website]: http://kurento.org
