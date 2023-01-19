Kurento JavaScript Tutorial
===========================

Kurento Client JavaScript demos.

This project contains a set of simple applications built with JavaScript
Kurento Client APIs (kurento-client-js and kurento-utils-js).

The source code of this project can be cloned from the [GitHub repository](https://github.com/Kurento/kurento).

Installation instructions
-------------------------

Be sure to have installed Node.js and Bower in your system:

```bash
curl -sSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs
sudo npm install -g bower
```

Each demo is located in a single folder (e.g. kurento-hello-world,
kurento-magic-mirror, and so on). For example, to install the kurento-hello-world
demo dependencies, run:

```bash
cd kurento-hello-world/
bower install
```

An HTTP server is required for these demos. A very simple way of doing this is
by means of a Node.js server. This server can be installed as follows:

```bash
sudo npm install -g http-server
```

Then, in each demo folder execute this command:

```bash
http-server -p 8443 -S -C keys/server.crt -K keys/server.key
```

Finally, open https://localhost:8443/ in your browser to access to the demo.

Take into account that one demo with Generators (kurento-hello-world-recorder-generator) require [co](https://github.com/visionmedia/co),
a generator based flow-control for Node.js and browser. In these demos, the experimental JavaScript support must be enabled. In Chrome, this can done in the flags configuration page:

chrome://flags/#enable-javascript-harmony

After enabling this flag, you'll need to restart your browser.

Optional parameters
-------------------

The demos accept some optional GET parameters given on the URL, you only need to
add them to the query string in the same way you would add them to the Node.js
executable on your command line:

```
https://example.com/index.html?ws_uri=wss://example.org/kurento
```

All demos accept following parameters:

* *ws_uri*: the WebSocket Kurento MediaServer endpoint. By default it connects
  to a Kurento MediaServer instance listening on the port 8433 on the same
  machine where it's being hosted the demo. The KMS must allow WSS (WebSocket Secure).
* *ice_servers*: the TURN and STUN servers to use, formatted as a JSON string
  holding an array of [RTCIceServer](http://www.w3.org/TR/webrtc/#idl-def-RTCIceServer) objects (the same structure used when
  configuring a [PeerConnection](http://www.w3.org/TR/webrtc/#rtcpeerconnection-interface) object), or an empty array to disabled them
  (this is faster and more reliable when doing tests on a local machine or LAN
  network). By default it use some random servers from a pre-defined list.

  ```
  https://example.com/index.html?ice_servers=[{"urls":"stun:stun1.example.net"},{"urls":"stun:stun2.example.net"}]
  https://example.com/index.html?ice_servers=[{"urls":"turn:turn.example.org","username":"user","credential":"myPassword"}]
  ```

Other parameters specific to each demo can be found defined at the top of their
index.js file.
