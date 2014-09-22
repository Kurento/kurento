[![][KurentoImage]][website]

Copyright © 2013-2014 Kurento. Licensed under [LGPL License].

Kurento jsonrpc library for Node.js and browsers
===============
Kurento Web SDK RPC Builder

The Kurento Web SDK RPC Builer project is a small RPC library for browser and Node.js.

The source code of this project can be cloned from the [GitHub repository].

Installation instructions
-------------------------

Be sure to have installed the Node.js tools in your system. It's heavily
encouraged to use the latest Node.js and NPM versions from the
[Node.js project PPA] instead of the packages available on the oficial Ubuntu
repositories, since due to the fast-moving Node.js community and environment
these last ones get easily outdated and can lead to incompatibility errors:

```bash
sudo add-apt-repository ppa:chris-lea/node.js
sudo apt-get update

sudo apt-get install nodejs nodejs-legacy
```

To install the library, it's recomended to do that from the [NPM repository] :

```bash
npm install kurento-jsonrpc
```

Alternatively, you can download the code using git and install manually its
dependencies:

```bash
git clone https://github.com/Kurento/kurento-jsonrpc-js.git
cd kurento-jsonrpc-js
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
Tests are autonomous and based on [nodeunit] testing framework. Their only
requirement is to exec previously ```npm install``` to have installed all the
dev dependencies.

### Node.js

To exec test in Node.js, you only need to exec ```npm test```, that will launch
all the tests automatically.


Kurento
=======

What is Kurento
---------------
Kurento provides an open platform for video processing and streaming
based on standards.

This platform has several APIs and components which provide solutions
to the requirements of multimedia content application developers.
These include:

  * Kurento Media Server (KMS). A full featured media server providing
    the capability to create and manage dynamic multimedia pipelines.
  * Kurento Control Server (KCS). Signaling server for KMS. It provides
    extra capabilities such as security, load balance, and so on.
  * Kurento Clients. Libraries to create applications with media
    capabilities. Kurento provides libraries for Java, browser JavaScript,
    and Node.js.

Downloads
---------
To download binary releases of Kurento components visit http://kurento.org

Source
------
The source code of this project can be cloned from the [GitHub Repository].
Code for other Kurento projects can be found in the [GitHub Kurento Group].

News and Website
----------------
Information about Kurento can be found on our [website].
Follow us on Twitter @[kurentoms].

[GitHub Kurento group]: https://github.com/kurento
[GitHub repository]: https://github.com/kurento/kurento-jsonrpc-js
[grunt]: http://gruntjs.com/
[KurentoImage]: https://secure.gravatar.com/avatar/21a2a12c56b2a91c8918d5779f1778bf?s=120
[kurentoms]: http://twitter.com/kurentoms
[LGPL License]: http://www.gnu.org/licenses/lgpl-2.1.html
[Node.js project PPA]: https://github.com/joyent/node/wiki/Installing-Node.js-via-package-manager#ubuntu-mint-elementary-os
[NPM repository]: https://www.npmjs.org/package/kurento-jsonrpc
[nodeunit]: https://github.com/caolan/nodeunit
[website]: http://kurento.org
