[![][KurentoImage]][Kurento]

Copyright © 2013-2016 [Kurento]. Licensed under [Apache 2.0 License].

JavaScript Kurento Client for Bower
===================================

The project contains the implementation of the JavaScript Kurento Client
for [Bower].

The source code of this project can be cloned from the [GitHub repository].

Installation instructions
-------------------------

Be sure to have installed [Node.js] and [Bower] in your system:

```bash
curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
sudo apt-get install -y nodejs
sudo npm install -g bower
```

To install the library, it's recommended to do that from the [Bower repository] :

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
     ...
   },
   function(error)
   {
     // Connection error
     ...
   });
   ```

   ```Javascript
   kurentoClient.KurentoClient(ws_uri, function(kurento)
   {
     // Connection success
     ...
   },
   function(error)
   {
     // Connection error
     ...
   });
   ```

2. Create a pipeline. This will host and connect the diferent elements. In case
   of error, it will be notified on the ```error``` parameter of the callback,
   otherwise this will be null as it's common on Node.js style APIs:

   ```Javascript
   kurento.create('MediaPipeline', function(error, pipeline)
   {
     ...
   });
   ```

3. Create the elements. The player need an object with the URL of the video, and
   we'll also subscribe to the 'EndOfStream' event of the HTTP stream:

   ```Javascript
   pipeline.create('PlayerEndpoint',
   {uri: "https://ci.kurento.com/video/format/small.webm"},
   function(error, player)
   {
     ...
   });

   pipeline.create('HttpGetEndpoint', function(error, httpGet)
   {
     httpGet.on('EndOfStream', function(event)
     {
       ...
     });

     ...
   });
   ```

4. Connect the elements, so the media stream can flow between them:

   ```Javascript
   player.connect(httpGet, function(error, pipeline)
   {
     ...
   });
   ```

5. Get the URL where the media stream will be available:

   ```Javascript
   httpGet.getUrl(function(error, url)
   {
     ...
   });
   ```

6. Start the reproduction of the media:

   ```Javascript
   player.play(function(error)
   {
     ...
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
installation and development guides. A simplified version of the documentation
can be found on [readthedocs.org]. The [Open API specification] a.k.a. Kurento
Protocol is also available on [apiary.io].

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

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

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
[Apache 2.0 License]: http://www.apache.org/licenses/LICENSE-2.0
[NUBOMEDIA]: http://www.nubomedia.eu
[StackOverflow]: http://stackoverflow.com/search?q=kurento
[Read-the-docs]: http://read-the-docs.readthedocs.org/
[readthedocs.org]: http://kurento.readthedocs.org/
[Open API specification]: http://kurento.github.io/doc-kurento/
[apiary.io]: http://docs.streamoriented.apiary.io/
[GitHub repository]: https://github.com/kurento/kurento-client-js
[grunt]: http://gruntjs.com/
[Kurento Module Creator]: https://github.com/Kurento/kurento-module-creator
[KurentoImage]: https://secure.gravatar.com/avatar/21a2a12c56b2a91c8918d5779f1778bf?s=120
[JavaScript Kurento Client for Bower]: https://github.com/Kurento/kurento-client-bower
[Node.js]: http://nodejs.org/
[NPM repository]: https://www.npmjs.org/package/kurento-client
[QUnit]: http://qunitjs.com
[QUnit-cli]: https://github.com/devongovett/qunit-cli
