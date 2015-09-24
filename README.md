[![][KurentoImage]][Kurento]

Copyright © 2013-2016 [Kurento]. Licensed under [LGPL v2.1 License].

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

It is recommended to update NPM to the latest version:

```bash
sudo npm install npm -g
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
[GitHub repository]: https://github.com/Kurento/kurento-tutorial-node
[kurento-client-js]: https://github.com/Kurento/kurento-client-js
[kurento-utils-js]: https://github.com/Kurento/kurento-utils-js
[Node.js]: http://nodejs.org/
