[![][KurentoImage]][website]

Copyright © 2014 Kurento. Licensed under [LGPL License].

Kurento JavaScript Tutorial
===========================
Kurento Client JavaScript demos

This project contains a set of simple applications built with JavaScript
Kurento Client APIs (kurento-client-js and kurento-utils-js).

The source code of this project can be cloned from the [GitHub repository].

Installation instructions
-------------------------

Be sure to have installed [Node.js] in your system:

```bash
sudo add-apt-repository ppa:chris-lea/node.js
sudo apt-get update
sudo apt-get install nodejs
```
Also be sure to have installed [Bower] in your system:

```bash
sudo npm install -g bower
```

Each demo is located in a single folder (e.g. kurento-hello-world, kurento-magic-mirror,
and so on). For example, to launch the kurento-hello-world demo, run:

```bash
cd kurento-hello-world
bower install
```

An HTTP server is required for these demos. A very simple way of doing this is by
means of a NodeJS server. This server can be installed as follows:

```bash
sudo npm install http-server -g
```

Then, in each demo folder execute this command:

```bash
http-server
```

Finally, open this URL in your browser: http://localhost:8080/

Take into account that demos with Generators (FaceOverlayGenerator,
RecorderFilterGenerator, and so on) require [co] (generator based
flow-control for nodejs). In these demos, the experimental JavaScript
support must be enabled. In Chrome, this can done in this configuration page:

chrome://flags/#enable-javascript-harmony


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
[LGPL License]: http://www.gnu.org/licenses/lgpl-2.1.html
[Node.js]: http://nodejs.org/
[website]: http://kurento.org
