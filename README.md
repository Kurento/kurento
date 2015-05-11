[![][KurentoImage]][website]

Copyright © 2014-2015 Kurento. Licensed under [LGPL License].

Kurento JavaScript Tutorial
===========================
Kurento Client JavaScript demos

This project contains a set of simple applications built with JavaScript
Kurento Client APIs ([kurento-client-js] and [kurento-utils-js]).

The source code of this project can be cloned from the [GitHub repository].

Installation instructions
-------------------------

Be sure to have installed [Node.js] and [Bower] in your system:

```bash
curl -sL https://deb.nodesource.com/setup | sudo bash -
sudo apt-get install -y nodejs
sudo npm install -g bower
```

Each demo is located in a single folder (e.g. kurento-hello-world,
kurento-magic-mirror, and so on). For example, to install the kurento-hello-world
demo dependencies, run:

```bash
cd kurento-hello-world
bower install
```

An HTTP server is required for these demos. A very simple way of doing this is
by means of a [Node.js] server. This server can be installed as follows:

```bash
sudo npm install -g http-server
```

Then, in each demo folder execute this command:

```bash
http-server
```

Finally, open http://localhost:8080/ in your browser to access to the demo.

Take into account that demos with Generators (kurento-faceoverlay-generator,
kurento-recorder-filter-generator, and so on) require [co] (generator based
flow-control for [Node.js] and browser). In these demos, the experimental
JavaScript support must be enabled. In Chrome, this can done in the flags
configuration page:

chrome://flags/#enable-javascript-harmony

After enabling this flag, you'll need to restart your browser.

Optional parameters
-------------------

The demos accept some optional GET parameters given on the URL, you only need to
add them to the query string in the same way you would add them to the [Node.js]
executable on your command line:

```
http://example.com/index.html?ws_uri=ws://example.org/kurento
```

All the demos accept the parameters:

* *ws_uri*: the WebSocket Kurento MediaServer endpoint. By default it connects
  to a Kurento MediaServer instance listening on the port 8888 on the same
  machine where it's being hosted the demo.
* *ice_servers*: the TURN and STUN servers to use, formatted as a JSON string
  holding an array of [RTCIceServer] objects (the same structure used when
  configuring a [PeerConnection] object), or an empty array to disabled them
  (this is faster and more reliable when doing tests on a local machine or LAN
  network). By default it use some random servers from a pre-defined list.

  ```
  http://example.com/index.html?ice_servers=[{"urls":"stun:stun1.example.net"},{"urls":"stun:stun2.example.net"}]
  http://example.com/index.html?ice_servers=[{"urls":"turn:turn.example.org","username":"user","credential":"myPassword"}]
  ```

Other parameters specific to each demo can be found defined at the top of their
index.js file.


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

[Bower]: http://bower.io
[co]: https://github.com/visionmedia/co
[GitHub Kurento group]: https://github.com/kurento
[GitHub repository]: https://github.com/Kurento/kurento-tutorial-js
[KurentoImage]: https://secure.gravatar.com/avatar/21a2a12c56b2a91c8918d5779f1778bf?s=120
[kurentoms]: http://twitter.com/kurentoms
[kurento-client-js]: https://github.com/Kurento/kurento-client-js
[kurento-utils-js]: https://github.com/Kurento/kurento-utils-js
[LGPL License]: http://www.gnu.org/licenses/lgpl-2.1.html
[Node.js]: http://nodejs.org/
[PeerConnection]: http://www.w3.org/TR/webrtc/#rtcpeerconnection-interface
[RTCIceServer]: http://www.w3.org/TR/webrtc/#idl-def-RTCIceServer
[website]: http://kurento.org
