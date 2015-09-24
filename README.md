[![][KurentoImage]][Kurento]

Copyright © 2013-2016 [Kurento]. Licensed under [LGPL v2.1 License].

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

Kurento is an open source software project providing a platform suitable 
for creating modular applications with advanced real-time communication
capabilities. For knowing more about Kurento, please visit the Kurento
project website: http://www.kurento.org.

Kurento is part of [FIWARE]. For further information on the relationship of 
FIWARE and Kurento check the [Kurento FIWARE Catalog Entry]

Kurento is part of the [NUBOMEDIA] research initiative.

Documentation
-------------

The Kurento project provides detailed [documentation] including tutorials,
installation and development guides.

Source
------

Code for other Kurento projects can be found in the [GitHub Kurento Group].

News and Website
----------------

Check the [Kurento blog]
Follow us on Twitter @[kurentoms].

Issue tracker
-------------

Issues and bug reports should be posted to the [GitHub Kurento bugtracker]

Licensing and distribution
--------------------------

Software associated to Kurento is provided as open source under GNU Library or
"Lesser" General Public License, version 2.1 (LGPL-2.1). Please check the
specific terms and conditions linked to this open source license at
http://opensource.org/licenses/LGPL-2.1. Please note that software derived as a
result of modifying the source code of Kurento software in order to fix a bug
or incorporate enhancements is considered a derivative work of the product.
Software that merely uses or aggregates (i.e. links to) an otherwise unmodified
version of existing software is not considered a derivative work.

Contribution policy
-------------------

You can contribute to the Kurento community through bug-reports, bug-fixes, new
code or new documentation. For contributing to the Kurento community, drop a
post to the [Kurento Public Mailing List] providing full information about your
contribution and its value. In your contributions, you must comply with the
following guidelines

* You must specify the specific contents of your contribution either through a
  detailed bug description, through a pull-request or through a patch.
* You must specify the licensing restrictions of the code you contribute.
* For newly created code to be incorporated in the Kurento code-base, you must
  accept Kurento to own the code copyright, so that its open source nature is
  guaranteed.
* You must justify appropriately the need and value of your contribution. The
  Kurento project has no obligations in relation to accepting contributions
  from third parties.
* The Kurento project leaders have the right of asking for further
  explanations, tests or validations of any code contributed to the community
  before it being incorporated into the Kurento code-base. You must be ready to
  addressing all these kind of concerns before having your code approved.

Support
-------

The Kurento project provides community support through the  [Kurento Public
Mailing List] and through [StackOverflow] using the tags *kurento* and
*fiware-kurento*.

Before asking for support, please read first the [Kurento Netiquette Guidelines]

[documentation]: http://www.kurento.org/documentation
[FIWARE]: http://www.fiware.org
[GitHub Kurento bugtracker]: https://github.com/Kurento/bugtracker/issues
[GitHub Kurento Group]: https://github.com/kurento
[kurentoms]: http://twitter.com/kurentoms
[Kurento]: http://kurento.org
[Kurento Blog]: http://www.kurento.org/blog
[Kurento FIWARE Catalog Entry]: http://catalogue.fiware.org/enablers/stream-oriented-kurento
[Kurento Netiquette Guidelines]: http://www.kurento.org/blog/kurento-netiquette-guidelines
[Kurento Public Mailing list]: https://groups.google.com/forum/#!forum/kurento
[KurentoImage]: https://secure.gravatar.com/avatar/21a2a12c56b2a91c8918d5779f1778bf?s=120
[LGPL v2.1 License]: http://www.gnu.org/licenses/lgpl-2.1.html
[NUBOMEDIA]: http://www.nubomedia.eu
[StackOverflow]: http://stackoverflow.com/search?q=kurento
[GitHub repository]: https://github.com/kurento/kurento-client-js
[grunt]: http://gruntjs.com/
[Kurento Module Creator]: https://github.com/Kurento/kurento-module-creator
[KurentoImage]: https://secure.gravatar.com/avatar/21a2a12c56b2a91c8918d5779f1778bf?s=120
[JavaScript Kurento Client for Bower]: https://github.com/Kurento/kurento-client-bower
[Node.js]: http://nodejs.org/
[NPM repository]: https://www.npmjs.org/package/kurento-client
[QUnit]: http://qunitjs.com
[QUnit-cli]: https://github.com/devongovett/qunit-cli
