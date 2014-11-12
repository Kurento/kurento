[![][KurentoImage]][website]

Copyright © 2014 Kurento. Licensed under [LGPL License].

Kurento tutorial for Node.js
============================
Examples on usage of the Kurento Node.js Client.

This project contains a set of simple applications built with JavaScript Kurento
Client APIs ([kurento-client-js] and [kurento-utils-js]) for [Node.js]:

  * kurento-chroma: WebRTC in loopback with a chroma filter.
  * kurento-crowddetector: WebRTC in loopback with a crowd detector filter.
  * kurento-hello-world: WebRTC loopback sending your webcam stream to a
    Kurento Media Server and back.
  * kurento-magic-mirror: WebRTC loopback with a filter that detect faces
    and put them an overlayed image of a hat.
  * kurento-one2many-call: This project makes possible one client to
    upstream to the server a WebRTC stream so that it can be distributed to
    other clients.
  * kurento-one2many-with-plumbers: Extension of kurento-one2many-call using
    plumbers to connect different pipelines.
  * kurento-one2one-call: Bidirectional videophone.
  * kurento-platedetector: WebRTC in loopback with a plate detector filter.
  * kurento-pointerdetector: WebRTC in loopback with a pointer detector
    filter.

The source code of this project can be cloned from the [GitHub repository].

Installation instructions
-------------------------

Be sure to have installed [Node.js] in your system:

```bash
curl -sL https://deb.nodesource.com/setup | sudo bash -
sudo apt-get install -y nodejs
```

Install node modules and bower components

```bash
npm install
```

Run the application and have fun ...

```bash
npm start
```

Parameters
----------

The Node.js server accept an optional parameter with the URI of the MediaServer
WebSocket endpoint, being set by default at ws://localhost:8888/kurento. You can
define its value by using the ```ws_uri``` flag:

```bash
npm start -- --ws_uri=ws://example.com:8888/kurento
```

It also accept an optional parameter with the URI of the application server root
that will serve the overlay image, being by default at http://localhost:8080/.
You can define its value by using the ```as_uri``` flag:

```bash
npm start -- --as_uri=http://example.org:8080/
```

For example, if you would like to start the node server in the localhost using
the port 8081, then the command is the following:

```bash
npm start -- --as_uri=http://localhost:8081/
```

Please notice that the double dash separator (```--```) is [on
purpose](https://www.npmjs.org/doc/cli/npm-run-script.html#description).


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
  * Kurento Clients. Libraries to create applications with media capabilities.
    Kurento provides libraries for Java, browser JavaScript, and Node.js.

Source
------
The source code of this project can be cloned from the [GitHub repository].
Code for other Kurento projects can be found in the [GitHub Kurento Group].

News and Website
----------------
Information about Kurento can be found on our [website].
Follow us on Twitter @[kurentoms].


[GitHub Kurento Group]: https://github.com/kurento
[GitHub repository]: https://github.com/Kurento/kurento-tutorial-node
[KurentoImage]: https://secure.gravatar.com/avatar/21a2a12c56b2a91c8918d5779f1778bf?s=120
[LGPL License]: http://www.gnu.org/licenses/lgpl-2.1.html
[kurentoms]: http://twitter.com/kurentoms
[kurento-client-js]: https://github.com/Kurento/kurento-client-js
[kurento-utils-js]: https://github.com/Kurento/kurento-utils-js
[website]: http://kurento.org
